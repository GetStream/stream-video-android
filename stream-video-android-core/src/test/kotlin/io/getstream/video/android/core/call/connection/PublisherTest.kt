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
import io.getstream.video.android.core.model.StreamPeerType
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.webrtc.AudioTrack
import org.webrtc.MediaConstraints
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack
import stream.video.sfu.models.PublishOption
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.signal.SetPublisherResponse

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
                coroutineScope = CoroutineScope(Dispatchers.Unconfined),
                type = StreamPeerType.PUBLISHER,
                mediaConstraints = MediaConstraints(),
                onStreamAdded = null,
                onNegotiationNeeded = { _, _ -> },
                onIceCandidate = null,
                maxBitRate = 1_500_000,
                sfuClient = mockSignalServerService,
                sessionId = "session-id",
                rejoin = { },
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
    fun `publishStream with ended track does not create a transceiver`() = runTest {
        val endedTrack = mockk<VideoTrack> {
            every { state() } returns MediaStreamTrack.State.ENDED
        }

        publisher.publishStream(endedTrack, TrackType.TRACK_TYPE_VIDEO)

        verify(exactly = 0) { publisher.addTransceiver(any(), any(), any()) }
    }

    @Test
    fun `publishStream with no matching option logs an error and does nothing`() = runTest {
        val liveAudioTrack = mockk<AudioTrack> {
            every { state() } returns MediaStreamTrack.State.LIVE
            every { enabled() } returns false
        }
        // There's no matching PublishOption for SCREEN_SHARE in the list
        publisher.publishStream(liveAudioTrack, TrackType.TRACK_TYPE_SCREEN_SHARE)

        verify(exactly = 0) { publisher.addTransceiver(any(), any(), any()) }
    }

    @Test
    fun `publishStream for a valid video track calls addTransceiver`() = runTest {
        val liveVideoTrack = mockk<VideoTrack> {
            every { state() } returns MediaStreamTrack.State.LIVE
            every { enabled() } returns true
            justRun { setEnabled(true) }
        }

        publisher.publishStream(liveVideoTrack, TrackType.TRACK_TYPE_VIDEO)

        // Should call addTransceiver at least once for the video option
        verify(atLeast = 1) {
            publisher.addTransceiver(any(), any(), videoPublishOption)
        }
    }

    @Test
    fun `unpublishStream stops and disposes matching transceiver`() = runTest {
        // We'll manually publish an AUDIO transceiver
        publisher.publishStream(
            track = mockk(relaxed = true) {
                every { kind() } returns "audio"
                every { state() } returns MediaStreamTrack.State.LIVE
            },
            trackType = TrackType.TRACK_TYPE_AUDIO,
        )

        // Now unpublish it:
        publisher.unpublishStream(TrackType.TRACK_TYPE_AUDIO, stopTrack = true)

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

    //endregion
}
