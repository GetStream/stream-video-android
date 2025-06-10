package io.getstream.video.android.core.call.connection

import io.getstream.result.Result
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.call.utils.TrackOverridesHandler
import io.getstream.video.android.core.model.AudioTrack
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.core.trySetEnabled
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.webrtc.AudioTrack as RtcAudioTrack
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import stream.video.sfu.models.Participant
import stream.video.sfu.models.PeerType
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.signal.ICERestartRequest
import stream.video.sfu.signal.SendAnswerRequest
import stream.video.sfu.signal.UpdateSubscriptionsResponse

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriberTest {

    //region Mocked dependencies
    @RelaxedMockK
    lateinit var mockSignalServer: SignalServerService

    @RelaxedMockK
    lateinit var mockPeerConnection: PeerConnection

    @RelaxedMockK
    internal lateinit var mockTrackOverridesHandler: TrackOverridesHandler

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
                onIceCandidateRequest = null,
            ),
            recordPrivateCalls = true,
        ) {
            every { this@spyk.connection } returns mockPeerConnection
        }

        // Stub out super‑class SDP related suspend functions
        justRun { subscriber["setRemoteDescription"](any<SessionDescription>()) }
        justRun { subscriber["setLocalDescription"](any<SessionDescription>()) }
        //coEvery { subscriber["createAnswer"](any()) } returns Result.Success(fakeAnswer)

        // Stub SFU calls
        coEvery { mockSignalServer.sendAnswer(any()) } returns mockk(relaxed = true)
        coEvery { mockSignalServer.iceRestart(any()) } returns mockk(relaxed = true)
        coEvery { mockSignalServer.updateSubscriptions(any()) } returns UpdateSubscriptionsResponse() // default instance
    }

    //region Negotiation & ICE

    @Test
    fun `restartIce sends request to SFU`() = testScope.runTest {
        subscriber.restartIce()

        coVerify {
            mockSignalServer.iceRestart(match<ICERestartRequest> {
                it.session_id == "session-id" && it.peer_type == PeerType.PEER_TYPE_SUBSCRIBER
            })
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
                any()
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
                MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO, any()
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
    @Test
    fun `viewportDimensions keeps highest resolution per session per trackType`() = runTest {
        val sessionId = "remote-session"
        val viewportId = "viewport-1"
        val viewportId2 = "viewport-2"

        subscriber.setTrackDimension(
            viewportId = viewportId,
            sessionId = sessionId,
            trackType = TrackType.TRACK_TYPE_VIDEO,
            visible = true,
            dimensions = VideoDimension(640, 480),
        )
        subscriber.setTrackDimension(
            viewportId = viewportId2,
            sessionId = sessionId,
            trackType = TrackType.TRACK_TYPE_VIDEO,
            visible = true,
            dimensions = VideoDimension(1280, 720), // larger
        )

        val dims = subscriber.viewportDimensions()
        val videoDims = dims[sessionId]?.get(TrackType.TRACK_TYPE_VIDEO)
        assertNotNull(videoDims)
        assertEquals(1280, videoDims!!.dimensions.width)
        assertEquals(720, videoDims.dimensions.height)
    }
    //endregion

    //region Video subscriptions
    @Test
    fun `setVideoSubscriptions stores subscriptions and calls SFU`() = testScope.runTest {
        // Participants list
        val localParticipant = mockParticipant("local", "session-id")
        val remoteP1 = mockParticipant("remote1", "s1")
        val remoteP2 = mockParticipant("remote2", "s2")
        val participants = listOf(localParticipant, remoteP1, remoteP2)

        val response = UpdateSubscriptionsResponse()
        coEvery { mockSignalServer.updateSubscriptions(any()) } returns response

        val result = subscriber.setVideoSubscriptions(
            trackOverridesHandler = mockTrackOverridesHandler,
            participants = participants,
            remoteParticipants = listOf(remoteP1, remoteP2),
            useDefaults = true,
        )

        assertEquals(Result.Success(response), result)
        coVerify { mockSignalServer.updateSubscriptions(any()) }
    }
    //endregion

    //region Utils
    private fun mockParticipant(userId: String, sessionId: String): ParticipantState {
        return mockk(relaxed = true) {
            every { this@mockk.userId } returns MutableStateFlow(
                userId
            )
            every { this@mockk.sessionId } returns sessionId
            every { this@mockk.videoEnabled } returns MutableStateFlow(
                true
            )
            every { this@mockk.screenSharingEnabled } returns MutableStateFlow(
                false
            )
        }
    }
    //endregion

}