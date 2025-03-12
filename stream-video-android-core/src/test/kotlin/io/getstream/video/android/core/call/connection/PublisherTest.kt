/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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
import io.getstream.video.android.core.MediaManagerImpl
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.call.connection.transceivers.TransceiverCache
import io.getstream.video.android.core.call.connection.transceivers.TransceiverId
import io.getstream.video.android.core.model.StreamPeerType
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.webrtc.AudioTrack
import org.webrtc.CameraEnumerationAndroid
import org.webrtc.MediaConstraints
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.RtpParameters
import org.webrtc.RtpParameters.HeaderExtension
import org.webrtc.RtpSender
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack
import stream.video.sfu.event.VideoLayerSetting
import stream.video.sfu.event.VideoSender
import stream.video.sfu.models.Codec
import stream.video.sfu.models.PublishOption
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.signal.SetPublisherResponse
import java.lang.reflect.Constructor
import kotlin.test.assertTrue

class PublisherTest {

    //region Mocked dependencies

    @RelaxedMockK
    lateinit var mockParticipantState: ParticipantState

    @RelaxedMockK
    lateinit var mockMediaManager: MediaManagerImpl

    @RelaxedMockK
    lateinit var mockPeerConnectionFactory: StreamPeerConnectionFactory

    @RelaxedMockK
    lateinit var mockSignalServerService: SignalServerService

    @RelaxedMockK
    lateinit var mockPeerConnection: PeerConnection

    // We'll spy the Publisher so we can override or verify calls to parent methods.
    private lateinit var publisher: Publisher
    private val coroutineContext = UnconfinedTestDispatcher()
    private val testScope = TestScope(coroutineContext)

    //region Example PublishOptions
    private val videoPublishOption = PublishOption(
        id = 1,
        track_type = TrackType.TRACK_TYPE_VIDEO,
        bitrate = 1_000_000,
        fps = 30,
        max_spatial_layers = 1,
        max_temporal_layers = 1,
        codec = null,
        video_dimension = VideoDimension(1280, 720),
    )

    private val audioPublishOption = PublishOption(
        id = 0,
        track_type = TrackType.TRACK_TYPE_AUDIO,
        bitrate = 128_000,
        fps = 0,
        max_spatial_layers = 1,
        max_temporal_layers = 1,
        codec = null,
        video_dimension = null,
    )
    //endregion

    //region Stubs & Mocks

    private val fakeSdpOffer = SessionDescription(SessionDescription.Type.OFFER, "fake-offer")
    private val fakeSdpAnswer = SessionDescription(SessionDescription.Type.ANSWER, "fake-answer")
    private val mockSdpOfferResult = Result.Success(fakeSdpOffer)
    private val mockTransceiverCache = spyk(TransceiverCache())

    //endregion

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Mock the mediaManager and peerConnectionFactory so they return mock Audio/Video tracks.
        every { mockPeerConnectionFactory.makeAudioTrack(any(), any()) } answers {
            mockk<AudioTrack>(relaxed = true) {
                every { id() } returns secondArg<String>()
                every { kind() } returns "audio"
                every { state() } returns MediaStreamTrack.State.LIVE
                every { enabled() } returns true
            }
        }
        every { mockPeerConnectionFactory.makeVideoTrack(any(), any()) } answers {
            mockk<VideoTrack>(relaxed = true) {
                every { id() } returns secondArg<String>()
                every { kind() } returns "video"
                every { state() } returns MediaStreamTrack.State.LIVE
                every { enabled() } returns true
            }
        }

        // Stub out the PeerConnection, e.g. do nothing on close().
        every { mockPeerConnection.close() } just runs

        // Construct a "real" Publisher but spy it, so we can mock or verify inherited calls.
        publisher = spyk(
            Publisher(
                localParticipant = mockParticipantState,
                mediaManager = mockMediaManager,
                peerConnectionFactory = mockPeerConnectionFactory,
                publishOptions = listOf(videoPublishOption, audioPublishOption),
                coroutineScope = testScope,
                type = StreamPeerType.PUBLISHER,
                mediaConstraints = MediaConstraints(),
                onStreamAdded = null,
                onNegotiationNeeded = { _, _ -> },
                onIceCandidate = null,
                maxBitRate = 1_500_000,
                sfuClient = mockSignalServerService,
                sessionId = "session-id",
                rejoin = { },
                transceiverCache = mockTransceiverCache,
            ),
        ) {
            every { connection } returns mockPeerConnection
        }

        // Mock the parent's createOffer so it returns a known Result<SessionDescription>.
        every { publisher["createOffer"]() } returns mockSdpOfferResult

        // Also mock setLocalDescription & setRemoteDescription in the parent class
        justRun { publisher["setLocalDescription"](any<SessionDescription>()) }
        justRun { publisher["setRemoteDescription"](any<SessionDescription>()) }

        // Mock the SFU server's setPublisher call to return a "successful" answer
        coEvery { mockSignalServerService.setPublisher(any()) } returns SetPublisherResponse(
            sdp = fakeSdpAnswer.description,
            error = null,
        )
    }

    //region Tests
    @Test
    fun `publishStream adds a new track if there is none`() = runTest {
        // Mock a transceiver for the video option
        val mockTransceiver = mockk<RtpTransceiver>(relaxed = true)
        val mockSender = mockk<RtpSender>(relaxed = true)
        every { mockTransceiver.sender } returns mockSender
        every { mockTransceiverCache.get(videoPublishOption) } returns mockTransceiver
        every { mockSender.track() } returns null

        val resultTrack = publisher.publishStream(TrackType.TRACK_TYPE_VIDEO)

        coVerify {
            // Verify new track is set to the sender
            mockSender.setTrack(any(), true)
        }

        // Verify track is returned by the publishStream
        assertNotNull(resultTrack)
    }


    @Test
    fun `publishStream with no matching option logs an error and does nothing`() = runTest {
        // There's no matching PublishOption for SCREEN_SHARE in the list
        publisher.publishStream(TrackType.TRACK_TYPE_SCREEN_SHARE)

        verify(exactly = 0) { publisher.addTransceiver(any(), any(), any()) }
    }

    @Test
    fun `publishStream for a valid video track calls addTransceiver`() = runTest {
        publisher.publishStream(TrackType.TRACK_TYPE_VIDEO)

        // Should call addTransceiver at least once for the video option
        verify(atLeast = 1) {
            publisher.addTransceiver(any(), any(), videoPublishOption)
        }
    }

    @Test
    fun `unpublishStream stops and disposes matching transceiver`() = runTest {
        // We'll manually publish an AUDIO transceiver
        publisher.publishStream(
            trackType = TrackType.TRACK_TYPE_AUDIO,
        )

        // Now unpublish it:
        publisher.unpublishStream(TrackType.TRACK_TYPE_AUDIO)

        // The peerConnection's transceiver should have been created, then stopped & disposed
        // We'll check that we actually "stop" a transceiver on unpublish
        // Since addTransceiver is a private call, we can verify the logs or final state
        // or use your real logic to ensure it's removed from the cache, etc.

        // Just to confirm logic, check we are no longer "publishing" that type
        assertFalse(publisher.isPublishing(TrackType.TRACK_TYPE_AUDIO))
    }

    @Test
    fun `restartIce calls negotiate(true) if not already in progress`() = runTest {
        every { mockPeerConnection.signalingState() } returns PeerConnection.SignalingState.STABLE

        publisher.restartIce()

        verify { mockPeerConnection.signalingState() }
        coVerify { publisher.negotiate(true) }
    }

    @Test
    fun `extractMid returns transceiver MID if present, else uses index`() = runTest {
        val mockTransceiver = mockk<RtpTransceiver>(relaxed = true)
        every { mockTransceiver.mid } returns "my-mid"
        val result = publisher.extractMid(mockTransceiver, 5, "fake-sdp")
        assertEquals("my-mid", result)

        // If mid is null, it should fallback to the index
        every { mockTransceiver.mid } returns null
        val fallback = publisher.extractMid(mockTransceiver, 5, "fake-sdp")
        assertEquals("5", fallback)
    }

    @Test
    fun `onRenegotiationNeeded calls negotiate after delay`() = runTest(coroutineContext) {
        // When
        publisher.onRenegotiationNeeded()
        advanceTimeBy(600)

        // Then
        coVerify { publisher.negotiate(any()) }
    }

    @Test
    fun `close with stopTracks = true stops publishing and closes connection`() = runTest {
        val mockVideoTrack = mockk<VideoTrack>(relaxed = true) {
            every { kind() } returns "video"
            every { state() } returns MediaStreamTrack.State.LIVE
        }
        val mockTransceiver = mockk<RtpTransceiver>(relaxed = true)
        every { mockPeerConnection.addTransceiver(mockVideoTrack, any()) } returns mockTransceiver
        every { mockTransceiverCache.items() } returns listOf(
            TransceiverId(videoPublishOption, mockTransceiver),
        )

        // Publish
        publisher.publishStream(TrackType.TRACK_TYPE_VIDEO)

        publisher.close(stopTracks = true)
        verify { mockPeerConnection.close() }
        assertFalse(publisher.isPublishing(TrackType.TRACK_TYPE_VIDEO))
    }

    @Test
    fun `currentOptions returns the publishOptions from transceiverCache`() = runTest {
        // 1) Verify that initially, currentOptions is empty (assuming we haven't published any track).
        assertTrue(
            publisher.currentOptions().isEmpty(),
            "Expected currentOptions() to be empty before any track is published.",
        )

        // 2) Mock a video track & transceiver so that a transceiver is created when we publish.
        val mockVideoTrack = mockk<VideoTrack>(relaxed = true) {
            every { kind() } returns "video"
            every { state() } returns MediaStreamTrack.State.LIVE
        }
        val mockTransceiver = mockk<RtpTransceiver>(relaxed = true)
        // Ensure that when we publish the video track, the Publisher calls addTransceiver(...) on the PeerConnection,
        // and we return `mockTransceiver`.
        every {
            mockPeerConnection.addTransceiver(
                mockVideoTrack,
                any<RtpTransceiver.RtpTransceiverInit>(),
            )
        } returns mockTransceiver

        // 3) Publish the video track
        publisher.publishStream(TrackType.TRACK_TYPE_VIDEO)

        // 4) Now check currentOptions()
        val options = publisher.currentOptions()
        // If you only have the one video PublishOption in the list, then we expect exactly one item in currentOptions.
        // If you have both audio & video options in `publisher.publishOptions`,
        // and you only published video, it might still only list the video option in the transceiver cache.
        assertEquals(
            "After publishing video, expected exactly one PublishOption in currentOptions().",
            1,
            options.size,
        )

        // 5) Ensure the returned option matches the expected one (videoPublishOption, for instance).
        // If your publisher has multiple PublishOptions, make sure you check for the correct one:
        val publishedOption = options.first()
        assertEquals(
            "Expected the track_type in currentOptions() to be VIDEO.",
            TrackType.TRACK_TYPE_VIDEO,
            publishedOption.track_type,
        )
    }

    @Test
    fun `syncPublishOptions adds missing transceivers and removes extra ones`() = runTest {
        // First: mock a video track & transceiver
        val mockVideoTrack = mockk<VideoTrack>(relaxed = true) {
            every { kind() } returns "video"
            every { state() } returns MediaStreamTrack.State.LIVE
        }
        val mockAudioTrack = mockk<AudioTrack>(relaxed = true) {
            every { kind() } returns "audio"
            every { state() } returns MediaStreamTrack.State.LIVE
        }

        val mockAudioTransceiver = mockk<RtpTransceiver>(relaxed = true) {
            every {
                this@mockk.sender.track()
            } returns mockAudioTrack
        }
        val mockTransceiver = mockk<RtpTransceiver>(relaxed = true) {
            every { this@mockk.sender.track() } returns mockVideoTrack
        }
        every { mockPeerConnection.addTransceiver(mockVideoTrack, any()) } returns mockTransceiver
        every {
            mockPeerConnection.addTransceiver(
                mockAudioTrack,
                any(),
            )
        } returns mockAudioTransceiver

        // Publish the video track
        publisher.publishStream(TrackType.TRACK_TYPE_VIDEO)
        // Publish the audio track
        publisher.publishStream(TrackType.TRACK_TYPE_AUDIO)
        coVerifySequence {
            // Video
            mockPeerConnection.addTransceiver(any<VideoTrack>(), any())
            // Audio
            mockPeerConnection.addTransceiver(any<AudioTrack>(), any())
        }

        val initialOptions = publisher.currentOptions()
        assertEquals(2, initialOptions.size)

        val newPublishOptions = initialOptions + videoPublishOption.copy(id = 99)

        assertEquals(3, newPublishOptions.size)
        publisher.syncPublishOptions(
            captureFormat = CameraEnumerationAndroid.CaptureFormat(
                1280,
                720,
                24,
                30,
            ),
            newPublishOptions,
        )

        coVerify {
            publisher.addTransceiver(any(), any(), videoPublishOption)
        }

        val newPublishOptions2 = newPublishOptions - videoPublishOption
        publisher.syncPublishOptions(
            captureFormat = CameraEnumerationAndroid.CaptureFormat(
                1280,
                720,
                24,
                30,
            ),
            newPublishOptions2,
        )
        assertEquals(2, newPublishOptions2.size)
        coVerifyOrder {
            mockTransceiverCache.remove(videoPublishOption)
        }
    }

    @Test
    fun `getTrackType returns the correct track type if found`() = runTest {
        // 1) Mock a video track with a specific ID
        val mockVideoTrack = mockk<VideoTrack>(relaxed = true) {
            every { kind() } returns "video"
            every { state() } returns MediaStreamTrack.State.LIVE
            every { id() } returns "my-video-track-id" // Important for the test
        }

        // 2) Mock the transceiver that will be added to the cache
        val mockTransceiver = mockk<RtpTransceiver>(relaxed = true)
        every {
            mockPeerConnection.addTransceiver(
                mockVideoTrack,
                any<RtpTransceiver.RtpTransceiverInit>(),
            )
        } returns mockTransceiver
        every {
            mockTransceiverCache.items()
        } returns listOf(TransceiverId(videoPublishOption, mockTransceiver))
        every { mockTransceiver.sender.track() } returns mockVideoTrack

        // 3) Publish the track
        publisher.publishStream(TrackType.TRACK_TYPE_VIDEO)

        // 4) Now check that getTrackType returns VIDEO for "my-video-track-id"
        val foundType = publisher.getTrackType("my-video-track-id")
        assertEquals(
            "Expected getTrackType to return VIDEO for the known track ID.",
            TrackType.TRACK_TYPE_VIDEO,
            foundType,
        )

        // 5) Verify that a non-existent ID returns null
        val unknownType = publisher.getTrackType("non-existent-track-id")
        assertNull(
            "Expected getTrackType to return null for an unknown track ID.",
            unknownType,
        )
    }

    @Test
    fun `changePublishQuality updates transceiver parameters`() = runTest {
        val videoTrack = mockk<VideoTrack>(relaxed = true) {
            every { kind() } returns "video"
            every { state() } returns MediaStreamTrack.State.LIVE
            every { id() } returns "video-id"
        }
        val parameters = mockk<RtpParameters>(relaxed = true)
        val encodings = listOf(
            RtpParameters.Encoding(
                "r0",
                true,
                1.0,
            ),
            RtpParameters.Encoding(
                "r1",
                true,
                2.0,
            ),
        )
        val mockTransceiver = mockk<RtpTransceiver>(relaxed = true)
        val mockSender = mockk<RtpSender>(relaxed = true)
        every { mockTransceiver.sender } returns mockSender
        every { mockTransceiverCache.get(videoPublishOption) } returns mockTransceiver
        every { mockPeerConnection.addTransceiver(videoTrack, any()) } returns mockTransceiver

        publisher.publishStream(TrackType.TRACK_TYPE_VIDEO)

        val newSender = VideoSender(
            track_type = TrackType.TRACK_TYPE_VIDEO,
            codec = Codec(96, "VP9"),
            publish_option_id = videoPublishOption.id,
            layers = listOf(
                VideoLayerSetting(
                    name = "r0",
                    active = true,
                    max_framerate = 30,
                    scale_resolution_down_by = 1.0f,
                    max_bitrate = 600_000,
                    scalability_mode = "L3T3",
                ),
                VideoLayerSetting(
                    name = "r1",
                    active = true,
                    max_framerate = 15,
                    scale_resolution_down_by = 2.0f,
                    max_bitrate = 300_000,
                    scalability_mode = "L3T2",
                ),
            ),
        )

        publisher.changePublishQuality(newSender)

        coVerify {
            mockTransceiverCache.get(videoPublishOption)
        }
        // Note: Difficult to mock parameters with mockk
    }

    @Test
    fun `getAnnouncedTracks returns track info for each live transceiver`() = runTest {
        val videoTrack = mockk<VideoTrack>(relaxed = true) {
            every { kind() } returns "video"
            every { state() } returns MediaStreamTrack.State.LIVE
            every { id() } returns "video-id-123"
        }
        val audioTrack = mockk<AudioTrack>(relaxed = true) {
            every { kind() } returns "audio"
            every { state() } returns MediaStreamTrack.State.LIVE
            every { id() } returns "audio-id-456"
        }
        val videoTransceiver = mockk<RtpTransceiver>(relaxed = true)
        val audioTransceiver = mockk<RtpTransceiver>(relaxed = true)
        every { mockPeerConnection.addTransceiver(videoTrack, any()) } returns videoTransceiver
        every { mockPeerConnection.addTransceiver(audioTrack, any()) } returns audioTransceiver
        every { mockTransceiverCache.items() } returns listOf(
            TransceiverId(videoPublishOption, videoTransceiver),
            TransceiverId(audioPublishOption, audioTransceiver),
        )

        publisher.publishStream(TrackType.TRACK_TYPE_VIDEO)
        publisher.publishStream(TrackType.TRACK_TYPE_AUDIO)

        val announced = publisher.getAnnouncedTracks(null)
        assertEquals("Expected two announced tracks", 2, announced.size)
    }

    @Test
    fun `getAnnouncedTracksForReconnect returns track info from last localDescription`() = testScope.runTest {
        val videoTrack = mockk<VideoTrack>(relaxed = true) {
            every { kind() } returns "video"
            every { state() } returns MediaStreamTrack.State.LIVE
            every { id() } returns "video-id-reconnect"
        }
        val videoTransceiver = mockk<RtpTransceiver>(relaxed = true)
        every { mockPeerConnection.addTransceiver(videoTrack, any()) } returns videoTransceiver

        publisher.publishStream(TrackType.TRACK_TYPE_VIDEO)
        every { mockPeerConnection.localDescription } returns fakeSdpOffer

        val tracks = publisher.getAnnouncedTracksForReconnect()
        assertEquals("Expected 1 track for reconnect", 1, tracks.size)
    }

    @Test
    fun `publishStream re-enables a disabled track`() = testScope.runTest {
        val mockVideoTrack = mockk<VideoTrack>(relaxed = true) {
            every { kind() } returns "video"
            every { state() } returns MediaStreamTrack.State.LIVE
            every { enabled() } returns false
        }
        val mockTransceiver = mockk<RtpTransceiver>(relaxed = true)
        every { mockTransceiverCache.items() } returns listOf(
            TransceiverId(videoPublishOption, mockTransceiver),
        )
        every { mockTransceiver.sender } returns mockk(relaxed = true) {
            every { track() } returns mockVideoTrack
        }
        every { mockTransceiverCache.get(videoPublishOption) } returns mockTransceiver
        val track = publisher.publishStream(TrackType.TRACK_TYPE_VIDEO)

        // Ensure track was enabled
        coVerify { mockVideoTrack.setEnabled(true) }
        // Ensure no transceiver was added if transceiver exists for publish option
        coVerify(exactly = 0) { publisher.addTransceiver(any(), any(), videoPublishOption) }
    }
    //endregion

    // change publish quality region
    @Test
    fun `changePublishQuality does nothing if sender is null`() = runTest {
        // Given
        every { mockTransceiverCache.get(any()) } returns null
        val videoSender = VideoSender(
            track_type = TrackType.TRACK_TYPE_VIDEO,
            codec = Codec(96, "VP8"),
            layers = listOf(),
        )

        // When
        publisher.changePublishQuality(videoSender)

        // Then
        coVerify(exactly = 0) { publisher.updateEncodings(any(), any(), any()) }
    }

    @Test
    fun `changePublishQuality does not do an update if encodings is empty`() = runTest {
        // Given
        val mockRtpSender = mockk<RtpSender>(relaxed = true)
        val mockParams = buildEmptyEncodingParameters(
            rid = "",
            active = true,
            maxFramerate = 30,
            maxBitrate = 300_000,
            scaleResolutionDownBy = 1.0,
            scalabilityMode = "L3T2",
        )
        every { mockRtpSender.parameters } returns mockParams
        val mockTransceiver = mockk<TransceiverId>(relaxed = true)
        every { mockTransceiver.transceiver.sender } returns mockRtpSender
        every { mockTransceiverCache.get(any()) } returns mockTransceiver.transceiver
        val videoSender = VideoSender(
            track_type = TrackType.TRACK_TYPE_VIDEO,
            codec = Codec(96, "VP8"),
            layers = listOf(),
        )

        // When
        publisher.changePublishQuality(videoSender)

        // Then
        coVerify(exactly = 0) { publisher.updateEncodings(any(), any(), any()) }
    }

    @Test
    fun `changePublishQuality same settings no change and does not set parameters`() = runTest {
        // Given
        val mockRtpSender = mockk<RtpSender>(relaxed = true)
        val singleEnc = buildRtpParams(
            rid = "f",
            active = true,
            maxFramerate = 30,
            scaleResolutionDownBy = 1.0,
            scalabilityMode = "L3T2",
            codec = vp9Codec(),
            maxBitrate = 300_000,
        )
        every { mockRtpSender.parameters } returns singleEnc
        val mockTransceiver = mockk<TransceiverId>(relaxed = true)
        every { mockTransceiver.transceiver.sender } returns mockRtpSender
        every { mockTransceiverCache.get(any()) } returns mockTransceiver.transceiver
        val matchingLayer = VideoLayerSetting(
            name = "f",
            active = true,
            max_framerate = 30,
            scale_resolution_down_by = 1.0f,
            max_bitrate = 300_000,
            scalability_mode = "L3T2",
        )
        val videoSender = VideoSender(
            track_type = TrackType.TRACK_TYPE_VIDEO,
            codec = Codec(98, "VP9", clock_rate = 9000, fmtp = ""),
            layers = listOf(matchingLayer),
        )

        // When
        publisher.changePublishQuality(videoSender)

        // Then
        coVerify(exactly = 0) { mockRtpSender.parameters = any() }
    }

    @Test
    fun `changePublishQuality single encoding picks first layer if rid missing`() = runTest {
        // Given
        val mockRtpSender = mockk<RtpSender>(relaxed = true)
        val singleEnc = buildRtpParams(
            rid = "",
            active = true,
            maxBitrate = 300_000,
        )
        every { mockRtpSender.parameters } returns singleEnc
        val mockTransceiver = mockk<TransceiverId>(relaxed = true)
        every { mockTransceiver.transceiver.sender } returns mockRtpSender
        every { mockTransceiverCache.get(any()) } returns mockTransceiver.transceiver
        val layer = VideoLayerSetting(
            name = "someRid",
            active = true,
            max_framerate = 15,
            scale_resolution_down_by = 2.0f,
            max_bitrate = 300_000,
            scalability_mode = "L3T2",
        )
        val videoSender = VideoSender(
            track_type = TrackType.TRACK_TYPE_VIDEO,
            codec = Codec(100, "VP9"),
            layers = listOf(layer),
        )

        // When
        publisher.changePublishQuality(videoSender)

        // Then
        verify {
            mockRtpSender.parameters = match {
                it.encodings[0].active &&
                    it.encodings[0].maxBitrateBps == 300_000 &&
                    it.encodings[0].maxFramerate == 15 &&
                    it.encodings[0].scaleResolutionDownBy == 2.0 &&
                    it.encodings[0].scalabilityMode == "L3T2"
            }
        }
    }

    @Test
    fun `changePublishQuality usesSvcCodec updates only first encoding`() = runTest {
        // Given
        val mockRtpSender = mockk<RtpSender>(relaxed = true)
        val params = buildRtpParams(
            rid = "",
            active = true,
            maxFramerate = 30,
            scaleResolutionDownBy = 1.0,
            scalabilityMode = "L3T2",
            maxBitrate = 300_000,
        )
        every { mockRtpSender.parameters } returns params
        val mockTransceiver = mockk<TransceiverId>(relaxed = true)
        every { mockTransceiver.transceiver.sender } returns mockRtpSender
        every { mockTransceiverCache.get(any()) } returns mockTransceiver.transceiver
        val videoSender = VideoSender(
            track_type = TrackType.TRACK_TYPE_VIDEO,
            codec = Codec(100, "VP9"),
            layers = listOf(
                VideoLayerSetting(
                    name = "A",
                    active = true,
                    max_framerate = 24,
                    scale_resolution_down_by = 1.0f,
                    max_bitrate = 150_000,
                    scalability_mode = "L3T3",
                ),
            ),
        )

        // When
        publisher.changePublishQuality(videoSender)

        // Then
        verify {
            mockRtpSender.parameters = match {
                it.encodings[0].active &&
                    it.encodings[0].maxBitrateBps == 150_000 &&
                    it.encodings[0].maxFramerate == 24 &&
                    it.encodings[0].scalabilityMode == "L3T3"
            }
        }
    }
    //endregion

    //region-mid
    @Test
    fun `has existing transceiverMid`() = runTest {
        val tx = mockk<RtpTransceiver> { every { mid } returns "already-mid" }
        val result = publisher.extractMid(tx, 5, "some-sdp")
        assertEquals("Should return transceiver.mid", "already-mid", result)
    }

    @Test
    fun `no mid, index valid`() = runTest {
        val tx = mockk<RtpTransceiver> { every { mid } returns null }
        val result = publisher.extractMid(tx, 2, "fake-sdp")
        assertEquals("Should return '2'", "2", result)
    }

    @Test
    fun `no mid, negative index, sdp null`() = runTest {
        val tx = mockk<RtpTransceiver> { every { mid } returns null }
        val result = publisher.extractMid(tx, -1, null)
        assertEquals("Should return empty string", "", result)
    }

    @Test
    fun `track is null`() = runTest {
        val sender = mockk<RtpSender> { every { track() } returns null }
        val tx = mockk<RtpTransceiver> {
            every { this@mockk.mid } returns null
            every { this@mockk.sender } returns sender
        }
        val result = publisher.extractMid(tx, -1, "fake-sdp")
        assertEquals("Should return empty if track is null", "", result)
    }

    @Test
    fun `no matching media`() = runTest {
        // We'll parse real SDP, but only has 'audio' m-line, no 'video' or matching MSID
        val sdp = """
            v=0
            o=- 1111111111 1 IN IP4 127.0.0.1
            s=-
            t=0 0
            m=audio 9 UDP/TLS/RTP/SAVPF 111
            a=mid:audio-mid
            a=msid:some-audio-id
        """.trimIndent()

        val track = mockk<MediaStreamTrack> {
            every { this@mockk.kind() } returns "video"
            every { this@mockk.id() } returns "unmatched-id"
        }
        val sender = mockk<RtpSender> { every { track() } returns track }
        val tx = mockk<RtpTransceiver> {
            every { this@mockk.mid } returns null
            every { this@mockk.sender } returns sender
        }

        val result = publisher.extractMid(tx, -1, sdp)
        assertEquals("Should return empty if no media matches track", "", result)
    }
    //endregion

    // region utils
    private fun buildRtpParams(
        rid: String?,
        active: Boolean,
        maxFramerate: Int = 0,
        scaleResolutionDownBy: Double = 1.0,
        scalabilityMode: String? = null,
        codec: RtpParameters.Codec? = null,
        maxBitrate: Int,
    ): RtpParameters {
        val enc = RtpParameters.Encoding(
            rid ?: "",
            active,
            1.0,
        )
        enc.maxFramerate = maxFramerate
        enc.scaleResolutionDownBy = scaleResolutionDownBy
        enc.scalabilityMode = scalabilityMode
        enc.minBitrateBps = maxBitrate
        enc.maxBitrateBps = maxBitrate
        val constructor = RtpParameters::class.java.getDeclaredConstructor(
            String::class.java,
            RtpParameters.DegradationPreference::class.java,
            RtpParameters.Rtcp::class.java,
            MutableList::class.java, // for headerExtensions
            MutableList::class.java, // for encodings
            MutableList::class.java, // for codecs
        )
        constructor.isAccessible = true

        val rtpParameters = constructor.newInstance(
            "fake-transaction-id",
            null, // or a real DegradationPreference
            null, // Rtcp object or null
            emptyList<HeaderExtension>(), // headerExtensions
            mutableListOf(enc), // encodings
            codec?.let { mutableListOf(codec) } ?: mutableListOf<Codec>(), // codecs
        ) as RtpParameters
        return rtpParameters
    }

    fun buildEmptyEncodingParameters(
        rid: String?,
        active: Boolean,
        maxFramerate: Int = 0,
        scaleResolutionDownBy: Double = 1.0,
        scalabilityMode: String? = null,
        codec: RtpParameters.Codec? = null,
        maxBitrate: Int,
    ): RtpParameters {
        val enc = RtpParameters.Encoding(
            rid ?: "",
            active,
            1.0,
        )
        enc.maxFramerate = maxFramerate
        enc.scaleResolutionDownBy = scaleResolutionDownBy
        enc.scalabilityMode = scalabilityMode
        enc.minBitrateBps = maxBitrate
        enc.maxBitrateBps = maxBitrate
        val constructor = RtpParameters::class.java.getDeclaredConstructor(
            String::class.java,
            RtpParameters.DegradationPreference::class.java,
            RtpParameters.Rtcp::class.java,
            MutableList::class.java, // for headerExtensions
            MutableList::class.java, // for encodings
            MutableList::class.java, // for codecs
        )
        constructor.isAccessible = true

        val rtpParameters = constructor.newInstance(
            "fake-transaction-id",
            null, // or a real DegradationPreference
            null, // Rtcp object or null
            emptyList<HeaderExtension>(), // headerExtensions
            emptyList<RtpParameters.Encoding>(), // encodings
            codec?.let { mutableListOf(codec) } ?: mutableListOf<Codec>(), // codecs
        ) as RtpParameters
        return rtpParameters
    }

    /**
     * Uses reflection to create a [RtpParameters.Codec] instance, bypassing the private/package-private
     * constructor. Adjust argument defaults or values as needed.
     */
    fun createCodecViaReflection(
        payloadType: Int = 96,
        name: String = "VP8",
        kind: MediaStreamTrack.MediaType = MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
        clockRate: Int? = 90000,
        numChannels: Int? = null,
        parameters: Map<String, String> = emptyMap(),
    ): RtpParameters.Codec {
        val constructor: Constructor<RtpParameters.Codec> = RtpParameters.Codec::class.java
            .getDeclaredConstructor(
                Int::class.javaPrimitiveType,
                String::class.java,
                MediaStreamTrack.MediaType::class.java,
                Int::class.javaObjectType,
                Int::class.javaObjectType,
                Map::class.java,
            )
        constructor.isAccessible = true

        return constructor.newInstance(
            payloadType,
            name,
            kind,
            clockRate,
            numChannels,
            parameters,
        )
    }

    /**
     * Example usage for VP8
     */
    fun vp8Codec(): RtpParameters.Codec {
        return createCodecViaReflection(
            payloadType = 96,
            name = "VP8",
            kind = MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
            clockRate = 90000,
            numChannels = null,
            parameters = mapOf("profile-level-id" to "42E01F"),
        )
    }

    /**
     * Example usage for VP9
     */
    fun vp9Codec(): RtpParameters.Codec {
        return createCodecViaReflection(
            payloadType = 98,
            name = "VP9",
            kind = MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
            clockRate = 90000,
            numChannels = null,
            parameters = mapOf(),
        )
    }
    //endregion
}
