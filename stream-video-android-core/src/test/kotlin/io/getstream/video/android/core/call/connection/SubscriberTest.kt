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

package io.getstream.video.android.core.call.connection

import io.getstream.result.Result
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.call.utils.TrackOverridesHandler
import io.getstream.video.android.core.internal.module.SfuConnectionModule
import io.getstream.video.android.core.model.AudioTrack
import io.getstream.video.android.core.trace.Tracer
import io.getstream.video.android.core.trySetEnabled
import io.getstream.webrtc.MediaStream
import io.getstream.webrtc.MediaStreamTrack
import io.getstream.webrtc.PeerConnection
import io.getstream.webrtc.RtpReceiver
import io.getstream.webrtc.RtpTransceiver
import io.getstream.webrtc.SessionDescription
import io.getstream.webrtc.VideoTrack
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.shadows.ShadowTrace.setEnabled
import stream.video.sfu.models.PeerType
import stream.video.sfu.models.TrackType
import stream.video.sfu.signal.ICERestartRequest
import stream.video.sfu.signal.UpdateSubscriptionsResponse
import io.getstream.webrtc.AudioTrack as RtcAudioTrack

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriberTest {

    //region Mocked dependencies
    @RelaxedMockK
    lateinit var mockSignalServer: SignalServerService

    @RelaxedMockK
    lateinit var mockPeerConnection: PeerConnection

    @RelaxedMockK
    internal lateinit var mockTrackOverridesHandler: TrackOverridesHandler

    @RelaxedMockK
    internal lateinit var mockSfuConnectionModule: SfuConnectionModule

    @Suppress("UNCHECKED_CAST")
    class MockMediaStream(val mockedId: String, nativeStream: Long) : MediaStream(nativeStream) {

        override fun getId(): String {
            return mockedId
        }
        override fun addTrack(track: io.getstream.webrtc.AudioTrack?): Boolean {
            val audioTracksField = MediaStream::class.java.getDeclaredField("audioTracks")
            audioTracksField.isAccessible = true
            val audioTracks = audioTracksField.get(
                this,
            ) as MutableList<io.getstream.webrtc.AudioTrack>
            audioTracks.add(track!!)
            return true
        }

        override fun addTrack(track: io.getstream.webrtc.VideoTrack?): Boolean {
            val videoTracksField = MediaStream::class.java.getDeclaredField("videoTracks")
            videoTracksField.isAccessible = true
            val videoTracks = videoTracksField.get(
                this,
            ) as MutableList<io.getstream.webrtc.VideoTrack>
            videoTracks.add(track!!)
            return true
        }
    }
    //endregion

    private lateinit var subscriber: Subscriber
    private val coroutineContext = UnconfinedTestDispatcher()
    private val testScope = TestScope(coroutineContext)

    private val fakeOffer = "fake-offer-sdp"
    private val fakeAnswer = SessionDescription(SessionDescription.Type.ANSWER, "fake-answer-sdp")

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        every { mockTrackOverridesHandler.applyOverrides(any()) } answers { firstArg() }

        // Spy the subscriber so we can override the underlying connection
        subscriber = spyk(
            Subscriber(
                sessionId = "session-id",
                sfuClient = mockSignalServer,
                coroutineScope = testScope,
                tracer = Tracer("subscriber").also { setEnabled(false) },
                rejoin = {},
                fastReconnect = {},
                onIceCandidateRequest = null,
                sfuConnectionModule = mockSfuConnectionModule,
            ),
            recordPrivateCalls = true,
        ) {
            every { this@spyk.connection } returns mockPeerConnection
        }

        // Stub out super‑class SDP related suspend functions
        justRun { subscriber["setRemoteDescription"](any<SessionDescription>()) }
        justRun { subscriber["setLocalDescription"](any<SessionDescription>()) }
        // coEvery { subscriber["createAnswer"](any()) } returns Result.Success(fakeAnswer)

        // Stub SFU calls
        coEvery { mockSignalServer.sendAnswer(any()) } returns mockk(relaxed = true)
        coEvery { mockSignalServer.iceRestart(any()) } returns mockk(relaxed = true)
        coEvery {
            mockSignalServer.updateSubscriptions(any())
        } returns UpdateSubscriptionsResponse() // default instance
    }

    //region Negotiation & ICE

    @Test
    fun `restartIce sends request to SFU`() = testScope.runTest {
        subscriber.restartIce()

        coVerify {
            mockSignalServer.iceRestart(
                match<ICERestartRequest> {
                    it.session_id == "session-id" && it.peer_type == PeerType.PEER_TYPE_SUBSCRIBER
                },
            )
        }
    }
    //endregion

    //region Transceivers & Tracks management
    @Test
    fun `addTransceivers adds video and audio recv_only transceivers`() = runTest {
        // Given
        every {
            mockPeerConnection.addTransceiver(
                any<MediaStreamTrack.MediaType>(),
                any(),
            )
        } returns mockk<RtpTransceiver>()

        // When
        subscriber.addTransceivers()

        // Then
        verify(exactly = 1) {
            mockPeerConnection.addTransceiver(
                MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
                any(),
            )
        }
        verify(exactly = 1) {
            mockPeerConnection.addTransceiver(
                MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
                any(),
            )
        }
    }

    @Test
    fun `setTrack and getTrack store and retrieve the same instance`() = runTest {
        val audioTrack = mockk<AudioTrack>()
        subscriber.setTrack("participant-1", TrackType.TRACK_TYPE_AUDIO, audioTrack)

        val retrieved = subscriber.getTrack("participant-1", TrackType.TRACK_TYPE_AUDIO)
        assertEquals(audioTrack, retrieved)
    }

    @Test
    fun `clear empties internal collections`() = runTest {
        subscriber.setTrack("p1", TrackType.TRACK_TYPE_AUDIO, mockk())
        subscriber.clear()

        assertFalse(subscriber.subscriptions().isNotEmpty())
        assertFalse(subscriber.viewportDimensions().isNotEmpty())
        assertFalse(subscriber.getTrack("p1", TrackType.TRACK_TYPE_AUDIO) != null)
    }

    @Test
    fun `disable turns off all receiver tracks`() = runTest {
        val rtcTrack = mockk<RtcAudioTrack>(relaxed = true)
        every { rtcTrack.trySetEnabled(false) } answers { }
        val receiver = mockk<RtpReceiver>(relaxed = true) {
            every { track() } returns rtcTrack
        }
        val transceiver = mockk<RtpTransceiver>(relaxed = true) {
            every { this@mockk.receiver } returns receiver
        }
        every { mockPeerConnection.transceivers } returns listOf(transceiver)

        subscriber.disable()

        verify { rtcTrack.trySetEnabled(false) }
    }

    @Test
    fun `enable turns on all receiver tracks`() = runTest {
        val rtcTrack = mockk<RtcAudioTrack>(relaxed = true)
        every { rtcTrack.trySetEnabled(true) } answers { }
        val receiver = mockk<RtpReceiver>(relaxed = true) { every { track() } returns rtcTrack }
        val transceiver =
            mockk<RtpTransceiver>(relaxed = true) { every { this@mockk.receiver } returns receiver }
        every { mockPeerConnection.transceivers } returns listOf(transceiver)

        subscriber.enable()

        verify { rtcTrack.trySetEnabled(true) }
    }
    //endregion

    //region Track dimensions & viewport
    //endregion

    //region Video subscriptions

    //endregion

    //region Utils
    private fun mockParticipant(
        userId: String,
        sessionId: String,
        videoEnabled: Boolean,
    ): ParticipantState {
        return mockk(relaxed = true) {
            every { this@mockk.userId } returns MutableStateFlow(
                userId,
            )
            every { this@mockk.sessionId } returns sessionId
            every { this@mockk.videoEnabled } returns MutableStateFlow(
                videoEnabled,
            )
            every { this@mockk.screenSharingEnabled } returns MutableStateFlow(
                false,
            )
        }
    }
    //endregion

    @Test
    fun `negotiate calls setRemoteDescription and setLocalDescription`() = runTest {
        val sdp = "fake-sdp"
        val subscriberSpy = spyk(subscriber, recordPrivateCalls = true)
        val answer = SessionDescription(SessionDescription.Type.ANSWER, "fake-answer")

        coEvery { subscriberSpy.setRemoteDescription(any()) } returns Result.Success(Unit)
        coEvery { subscriberSpy.createAnswer() } returns Result.Success(answer)
        coEvery { subscriberSpy.setLocalDescription(any()) } returns Result.Success(Unit)

        subscriberSpy.negotiate(sdp)

        coVerify { subscriberSpy.setRemoteDescription(match { it.description == sdp }) }
        coVerify { subscriberSpy.createAnswer() }
        coVerify { subscriberSpy.setLocalDescription(any()) }
    }

    @Test
    fun `participantLeft removes tracks and trackDimensions for participant`() = runTest {
        // Arrange: Add a track and track dimension for a participant
        val sessionId = "session-to-remove"
        val trackType = TrackType.TRACK_TYPE_VIDEO
        val audioTrack = mockk<AudioTrack>()
        subscriber.setTrack(sessionId, trackType, audioTrack)
        subscriber.setTrackDimension(
            viewportId = "viewport1",
            sessionId = sessionId,
            trackType = trackType,
            visible = true,
            dimensions = Subscriber.defaultVideoDimension,
        )
        // Sanity check: track and dimension exist
        assertNotNull(subscriber.getTrack(sessionId, trackType))
        assertTrue(subscriber.viewportDimensions()[sessionId]?.containsKey(trackType) == true)

        // Act: Remove participant
        val participant = mockk<stream.video.sfu.models.Participant> {
            every { session_id } returns sessionId
        }
        subscriber.participantLeft(participant)

        // Assert: track and dimension are removed
        assertNull(subscriber.getTrack(sessionId, trackType))
        assertTrue(subscriber.viewportDimensions()[sessionId]?.containsKey(trackType) != true)
    }

    @Test
    fun `onNewStream adds audio and video tracks to internal maps`() = runTest {
        val sessionId = "session-id"
        val audioTrack = mockk<io.getstream.webrtc.AudioTrack>(relaxed = true)
        val videoTrack = mockk<VideoTrack>(relaxed = true)
        subscriber.setTrackLookupPrefixes(
            mapOf(
                "test-prefix-1" to sessionId,
                "test-prefix-2" to sessionId,
            ),
        )

        val videoStream = MockMediaStream("test-prefix-1:1", 1)
        val audioStream = MockMediaStream("test-prefix-2:2", 2)
        audioStream.addTrack(audioTrack)
        videoStream.addTrack(videoTrack)

        // Call onNewStream
        subscriber.onNewStream(audioStream)
        subscriber.onNewStream(videoStream)

        // Assert that tracks are added and can be retrieved
        val audio = subscriber.getTrack(sessionId, TrackType.TRACK_TYPE_AUDIO)
        val video = subscriber.getTrack(sessionId, TrackType.TRACK_TYPE_VIDEO)
        assertNotNull(audio)
        assertNotNull(video)
    }

    @Test
    fun `onNewStream handles empty stream gracefully`() = runTest {
        val stream = MediaStream(1)
        // Should not throw
        subscriber.onNewStream(stream)
        // No tracks should be added
        assertNull(subscriber.getTrack("empty-session", TrackType.TRACK_TYPE_AUDIO))
        assertNull(subscriber.getTrack("empty-session", TrackType.TRACK_TYPE_VIDEO))
    }

    @Test
    fun `onNewStream handles null stream gracefully`() = runTest {
        // Should not throw
        subscriber.onAddStream(null)
        // No tracks should be added
        assertNull(subscriber.getTrack("empty-session", TrackType.TRACK_TYPE_AUDIO))
        assertNull(subscriber.getTrack("empty-session", TrackType.TRACK_TYPE_VIDEO))
    }

    @Test
    fun `onAddStream calls onNewStream`() = runTest {
        val stream = MockMediaStream("test-prefix-1:1", 1)
        subscriber.onAddStream(stream)

        coVerify { subscriber.onNewStream(stream) }
    }

    @Test
    fun `onNewStream populates trackIdToParticipant correctly`() = runTest {
        val sessionId = "session-id"
        val audioTrack = mockk<io.getstream.webrtc.AudioTrack>(relaxed = true) {
            every { id() } returns "audio-id"
        }
        val videoTrack = mockk<VideoTrack>(relaxed = true) {
            every { id() } returns "video-id"
        }
        subscriber.setTrackLookupPrefixes(
            mapOf(
                "test-prefix-1" to sessionId,
                "test-prefix-2" to sessionId,
            ),
        )
        val videoStream = MockMediaStream("test-prefix-1:2", 1)
        val audioStream = MockMediaStream("test-prefix-2:1", 2)
        audioStream.addTrack(audioTrack)
        videoStream.addTrack(videoTrack)
        subscriber.onNewStream(audioStream)
        subscriber.onNewStream(videoStream)
        val map = subscriber.trackIdToParticipant()
        assertEquals(sessionId, map["audio-id"])
        assertEquals(sessionId, map["video-id"])
    }

    //region Track Removal Tests

    @Test
    fun `onRemoveStream removes tracks from internal tracking maps`() = runTest {
        val sessionId = "session-id"
        val trackId = "audio-track-id"
        val audioTrack = mockk<org.webrtc.AudioTrack>(relaxed = true) {
            every { id() } returns trackId
        }

        subscriber.setTrackLookupPrefixes(mapOf("prefix" to sessionId))
        val stream = MockMediaStream("prefix:${TrackType.TRACK_TYPE_AUDIO.value}:0", 1)
        stream.addTrack(audioTrack)

        subscriber.onNewStream(stream)
        assertNotNull(subscriber.getTrack(sessionId, TrackType.TRACK_TYPE_AUDIO))

        subscriber.onRemoveStream(stream)

        assertNull(subscriber.getTrack(sessionId, TrackType.TRACK_TYPE_AUDIO))
        assertNull(subscriber.trackIdToParticipant()[trackId])
    }

    @Test
    fun `onRemoveStream handles null stream gracefully`() = runTest {
        // Should not throw
        subscriber.onRemoveStream(null)
    }

    @Test
    fun `onRemoveStream handles unknown track IDs gracefully`() = runTest {
        val unknownTrack = mockk<VideoTrack>(relaxed = true) {
            every { id() } returns "unknown-track-id"
        }

        // Create a stream with a track that was never added via onNewStream
        val stream = MockMediaStream("unknown:1", 1)
        stream.addTrack(unknownTrack)

        // Should not throw
        subscriber.onRemoveStream(stream)
    }
    //endregion
}
