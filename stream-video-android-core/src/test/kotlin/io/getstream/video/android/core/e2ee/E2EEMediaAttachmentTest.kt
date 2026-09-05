/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-video-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.video.android.core.e2ee

import io.getstream.video.android.core.MediaManagerImpl
import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.call.connection.Publisher
import io.getstream.video.android.core.call.connection.StreamPeerConnectionFactory
import io.getstream.video.android.core.call.connection.Subscriber
import io.getstream.video.android.core.call.connection.transceivers.TransceiverCache
import io.getstream.video.android.core.internal.module.SfuConnectionModule
import io.getstream.video.android.core.model.StreamPeerType
import io.getstream.video.android.core.trace.PeerConnectionTraceKey
import io.getstream.video.android.core.trace.Tracer
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import org.webrtc.RtpSender
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import stream.video.sfu.models.AudioBitrateProfile
import stream.video.sfu.models.Codec
import stream.video.sfu.models.PublishOption
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension

/**
 * Covers the two points where the SDK hands media to an [E2EEManager]: the publisher installing an
 * encryptor on outgoing senders, and the subscriber installing a decryptor on incoming receivers.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class E2EEMediaAttachmentTest {

    /** Records what the SDK asked for, and can be made to fail on demand. */
    private class RecordingE2EEManager(
        private val failOnEncrypt: Boolean = false,
        private var decryptFailuresRemaining: Int = 0,
    ) : E2EEManager {
        val encrypted = mutableListOf<Triple<RtpSender, String?, E2EETrackType?>>()
        val decrypted = mutableListOf<Triple<RtpReceiver, String, E2EETrackType?>>()
        var decryptAttempts = 0

        override fun encrypt(
            sender: RtpSender,
            codec: String?,
            trackType: E2EETrackType?,
        ): Result<Unit> {
            if (failOnEncrypt) {
                return Result.failure(IllegalStateException("no key material"))
            }
            encrypted += Triple(sender, codec, trackType)
            return Result.success(Unit)
        }

        override fun decrypt(
            receiver: RtpReceiver,
            userId: String,
            trackType: E2EETrackType?,
        ): Result<Unit> {
            decryptAttempts++
            if (decryptFailuresRemaining > 0) {
                decryptFailuresRemaining--
                return Result.failure(IllegalStateException("decryptor unavailable"))
            }
            decrypted += Triple(receiver, userId, trackType)
            return Result.success(Unit)
        }
    }

    @RelaxedMockK
    lateinit var mockMediaManager: MediaManagerImpl

    @RelaxedMockK
    lateinit var mockPeerConnectionFactory: StreamPeerConnectionFactory

    @RelaxedMockK
    lateinit var mockSignalServer: SignalServerService

    @RelaxedMockK
    lateinit var mockPeerConnection: PeerConnection

    @RelaxedMockK
    internal lateinit var mockSfuConnectionModule: SfuConnectionModule

    private val testScope = TestScope(UnconfinedTestDispatcher())
    private val transceiverCache = spyk(TransceiverCache())

    // Real tracers rather than mocks: the assertions below are about what reaches the trace buffer,
    // since that buffer is what ships to the SFU with call stats.
    private val publisherTracer = Tracer("publisher")
    private val subscriberTracer = Tracer("subscriber")

    /** The payload of the single trace carrying [key], failing if there is not exactly one. */
    private fun Tracer.singleTrace(key: PeerConnectionTraceKey): Any? =
        take().snapshot.single { it.tag == key.value }.data

    private val videoPublishOption = PublishOption(
        id = 1,
        track_type = TrackType.TRACK_TYPE_VIDEO,
        bitrate = 1_000_000,
        fps = 30,
        max_spatial_layers = 1,
        max_temporal_layers = 1,
        codec = Codec(name = "VP8"),
        video_dimension = VideoDimension(1280, 720),
    )

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { mockMediaManager.microphone.audioBitrateProfile.value } returns
            AudioBitrateProfile.AUDIO_BITRATE_PROFILE_VOICE_STANDARD_UNSPECIFIED
    }

    // region Publisher

    private fun publisherWith(manager: E2EEManager?): Publisher = spyk(
        Publisher(
            mediaManager = mockMediaManager,
            peerConnectionFactory = mockPeerConnectionFactory,
            publishOptions = listOf(videoPublishOption),
            coroutineScope = testScope,
            type = StreamPeerType.PUBLISHER,
            mediaConstraints = MediaConstraints(),
            onStreamAdded = null,
            onNegotiationNeeded = { _, _ -> },
            onIceCandidate = null,
            maxBitRate = 1_500_000,
            sfuClient = mockSignalServer,
            sessionId = "session-id",
            rejoin = {},
            fastReconnect = {},
            transceiverCache = transceiverCache,
            tracer = publisherTracer,
            e2eeManager = manager,
        ),
    ) {
        every { connection } returns mockPeerConnection
    }

    private fun stubAddTransceiver(sender: RtpSender?): RtpTransceiver {
        val transceiver = mockk<RtpTransceiver>(relaxed = true)
        every { transceiver.sender } returns sender
        every {
            mockPeerConnection.addTransceiver(
                any<MediaStreamTrack>(),
                any<RtpTransceiver.RtpTransceiverInit>(),
            )
        } returns transceiver
        return transceiver
    }

    @Test
    fun `publisher encrypts the sender with the codec hint and track type`() = runTest {
        val manager = RecordingE2EEManager()
        val sender = mockk<RtpSender>(relaxed = true)
        stubAddTransceiver(sender)

        publisherWith(manager).addTransceiver(
            streamIdList = listOf("stream-id"),
            captureFormat = null,
            track = mockk<MediaStreamTrack>(relaxed = true),
            publishOption = videoPublishOption,
        )

        assertEquals(1, manager.encrypted.size)
        val (encryptedSender, codec, trackType) = manager.encrypted.single()
        assertEquals(sender, encryptedSender)
        // The SFU reports codecs in mixed case; the encryption layer expects lowercase.
        assertEquals("vp8", codec)
        assertEquals(E2EETrackType.VIDEO, trackType)
    }

    @Test
    fun `publisher passes through a mime-typed codec it has no special knowledge of`() = runTest {
        val manager = RecordingE2EEManager()
        stubAddTransceiver(mockk<RtpSender>(relaxed = true))
        val option = videoPublishOption.copy(codec = Codec(name = "video/AV1"))

        publisherWith(manager).addTransceiver(
            streamIdList = listOf("stream-id"),
            captureFormat = null,
            track = mockk<MediaStreamTrack>(relaxed = true),
            publishOption = option,
        )

        // Publish options carry either "AV1" or "video/AV1", and the hint must survive either way
        // rather than being dropped for not being on a hard-coded codec list.
        assertEquals("av1", manager.encrypted.single().second)
    }

    @Test
    fun `publisher drops the transceiver when the encryptor cannot be attached`() = runTest {
        val transceiver = stubAddTransceiver(mockk<RtpSender>(relaxed = true))

        publisherWith(RecordingE2EEManager(failOnEncrypt = true)).addTransceiver(
            streamIdList = listOf("stream-id"),
            captureFormat = null,
            track = mockk<MediaStreamTrack>(relaxed = true),
            publishOption = videoPublishOption,
        )

        // Caching it would publish plaintext on a call the app believes is encrypted.
        verify(exactly = 0) { transceiverCache.add(videoPublishOption, any()) }
        verify { transceiver.stop() }
        assertNull(transceiverCache.get(videoPublishOption))
    }

    @Test
    fun `publisher traces why a track was not published`() = runTest {
        stubAddTransceiver(mockk<RtpSender>(relaxed = true))

        publisherWith(RecordingE2EEManager(failOnEncrypt = true)).addTransceiver(
            streamIdList = listOf("stream-id"),
            captureFormat = null,
            track = mockk<MediaStreamTrack>(relaxed = true),
            publishOption = videoPublishOption,
        )

        // The transceiver is dropped before it reaches the SFU, so this trace is the only record
        // that the track was deliberately withheld rather than never attempted.
        assertEquals(
            "track=TRACK_TYPE_VIDEO codec=VP8 reason=no key material",
            publisherTracer.singleTrace(PeerConnectionTraceKey.E2EE_ENCRYPTOR_FAILED),
        )
    }

    @Test
    fun `publisher publishes normally when no manager is attached`() = runTest {
        stubAddTransceiver(mockk<RtpSender>(relaxed = true))

        publisherWith(null).addTransceiver(
            streamIdList = listOf("stream-id"),
            captureFormat = null,
            track = mockk<MediaStreamTrack>(relaxed = true),
            publishOption = videoPublishOption,
        )

        verify(exactly = 1) { transceiverCache.add(videoPublishOption, any()) }
    }

    // endregion

    // region Subscriber

    /** [MediaStream] is final-ish and native-backed, so tracks are injected reflectively. */
    @Suppress("UNCHECKED_CAST")
    private class MockMediaStream(private val mockedId: String, nativeStream: Long) :
        MediaStream(nativeStream) {

        override fun getId(): String = mockedId

        override fun addTrack(track: org.webrtc.AudioTrack?): Boolean {
            val field = MediaStream::class.java.getDeclaredField("audioTracks")
            field.isAccessible = true
            (field.get(this) as MutableList<org.webrtc.AudioTrack>).add(track!!)
            return true
        }
    }

    private fun subscriberWith(
        manager: E2EEManager?,
        userIdForSession: (String) -> String?,
    ): Subscriber = spyk(
        Subscriber(
            sessionId = "my-session",
            sfuClient = mockSignalServer,
            coroutineScope = testScope,
            tracer = subscriberTracer,
            rejoin = {},
            fastReconnect = {},
            onIceCandidateRequest = null,
            sfuConnectionModule = mockSfuConnectionModule,
            e2eeManager = manager,
            userIdForSession = userIdForSession,
        ),
        recordPrivateCalls = true,
    ) {
        every { this@spyk.connection } returns mockPeerConnection
    }.also {
        justRun { it["setRemoteDescription"](any<SessionDescription>()) }
        justRun { it["setLocalDescription"](any<SessionDescription>()) }
    }

    private fun stubReceiverFor(trackId: String): RtpReceiver {
        val receiver = mockk<RtpReceiver>(relaxed = true) {
            every { track() } returns mockk<MediaStreamTrack>(relaxed = true) {
                every { id() } returns trackId
            }
        }
        every { mockPeerConnection.transceivers } returns listOf(
            mockk<RtpTransceiver>(relaxed = true) { every { this@mockk.receiver } returns receiver },
        )
        return receiver
    }

    private fun audioStreamFor(prefix: String, trackId: String): MockMediaStream {
        val stream = MockMediaStream("$prefix:${TrackType.TRACK_TYPE_AUDIO.value}", 1)
        stream.addTrack(
            mockk<org.webrtc.AudioTrack>(relaxed = true) { every { id() } returns trackId },
        )
        return stream
    }

    @Test
    fun `subscriber decrypts an incoming track with the publishing user's id`() = runTest {
        val manager = RecordingE2EEManager()
        val receiver = stubReceiverFor("remote-audio")
        val subscriber = subscriberWith(manager) { if (it == "their-session") "alice" else null }

        subscriber.setTrackLookupPrefixes(mapOf("their-prefix" to "their-session"))
        subscriber.onNewStream(audioStreamFor("their-prefix", "remote-audio"))

        assertEquals(1, manager.decrypted.size)
        val (decryptedReceiver, userId, trackType) = manager.decrypted.single()
        assertEquals(receiver, decryptedReceiver)
        assertEquals("alice", userId)
        assertEquals(E2EETrackType.AUDIO, trackType)
    }

    @Test
    fun `subscriber parks a track whose user is unknown and decrypts it once resolved`() = runTest {
        val manager = RecordingE2EEManager()
        stubReceiverFor("remote-audio")
        // The participant list can lag the track, so the first lookup misses.
        var knownUser: String? = null
        val subscriber = subscriberWith(manager) { knownUser }

        subscriber.setTrackLookupPrefixes(mapOf("their-prefix" to "their-session"))
        subscriber.onNewStream(audioStreamFor("their-prefix", "remote-audio"))
        assertEquals(0, manager.decrypted.size)

        knownUser = "alice"
        subscriber.setTrackLookupPrefixes(mapOf("their-prefix" to "their-session"))

        assertEquals(1, manager.decrypted.size)
        assertEquals("alice", manager.decrypted.single().second)
    }

    @Test
    fun `subscriber does not decrypt the same track twice`() = runTest {
        val manager = RecordingE2EEManager()
        stubReceiverFor("remote-audio")
        val subscriber = subscriberWith(manager) { "alice" }

        subscriber.setTrackLookupPrefixes(mapOf("their-prefix" to "their-session"))
        subscriber.onNewStream(audioStreamFor("their-prefix", "remote-audio"))
        subscriber.onNewStream(audioStreamFor("their-prefix", "remote-audio"))

        assertEquals(1, manager.decrypted.size)
    }

    @Test
    fun `subscriber attaches a new decryptor when a removed track is re-added`() = runTest {
        val manager = RecordingE2EEManager()
        val firstReceiver = stubReceiverFor("remote-audio")
        val subscriber = subscriberWith(manager) { "alice" }
        val firstStream = audioStreamFor("their-prefix", "remote-audio")

        subscriber.setTrackLookupPrefixes(mapOf("their-prefix" to "their-session"))
        subscriber.onNewStream(firstStream)
        subscriber.onRemoveStream(firstStream)

        val secondReceiver = stubReceiverFor("remote-audio")
        subscriber.onNewStream(audioStreamFor("their-prefix", "remote-audio"))

        assertEquals(listOf(firstReceiver, secondReceiver), manager.decrypted.map { it.first })
    }

    @Test
    fun `subscriber retries a track when decryptor attachment fails`() = runTest {
        val manager = RecordingE2EEManager(decryptFailuresRemaining = 1)
        stubReceiverFor("remote-audio")
        val subscriber = subscriberWith(manager) { "alice" }

        subscriber.setTrackLookupPrefixes(mapOf("their-prefix" to "their-session"))
        subscriber.onNewStream(audioStreamFor("their-prefix", "remote-audio"))

        assertEquals(1, manager.decryptAttempts)
        assertEquals(0, manager.decrypted.size)

        // A participant update flushes pending decryptors. The failed track must not have been
        // marked as successfully handled.
        subscriber.setTrackLookupPrefixes(mapOf("their-prefix" to "their-session"))

        assertEquals(2, manager.decryptAttempts)
        assertEquals(1, manager.decrypted.size)
    }

    @Test
    fun `subscriber traces a track it could not decrypt`() = runTest {
        stubReceiverFor("remote-audio")
        val subscriber = subscriberWith(RecordingE2EEManager(decryptFailuresRemaining = 1)) {
            "alice"
        }

        subscriber.setTrackLookupPrefixes(mapOf("their-prefix" to "their-session"))
        subscriber.onNewStream(audioStreamFor("their-prefix", "remote-audio"))

        // A track with no decryptor renders nothing, and the retry can keep failing quietly.
        assertEquals(
            "track=TRACK_TYPE_AUDIO user=alice reason=decryptor unavailable",
            subscriberTracer.singleTrace(PeerConnectionTraceKey.E2EE_DECRYPTOR_FAILED),
        )
    }

    // endregion
}
