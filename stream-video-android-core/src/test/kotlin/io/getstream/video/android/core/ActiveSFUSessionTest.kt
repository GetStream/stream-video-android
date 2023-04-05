package io.getstream.video.android.core

import com.google.common.truth.Truth
import io.getstream.video.android.core.call.connection.StreamPeerConnectionFactory
import io.getstream.video.android.core.events.ICETrickleEvent
import io.getstream.video.android.core.events.SFUConnectedEvent
import io.getstream.video.android.core.events.SubscriberOfferEvent
import io.getstream.video.android.core.utils.buildAudioConstraints
import io.getstream.video.android.core.utils.buildMediaConstraints
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import stream.video.sfu.models.PeerType


@RunWith(RobolectricTestRunner::class)
class ActiveSFUSessionTest : IntegrationTestBase() {
    /**
     * Creating the publisher and subscriber peer connection
     * - If we are not allowed to publish, we don't need to establish that peer connection
     *
     * TODO: This test could use a mock of the client, it doesn't need the real client and could run faster
     * Integration testing is already handled in other parts of the codebase
     */
    @get:Rule
    val mockkRule = MockKRule(this)

    @MockK(relaxUnitFun = true, relaxed = true)
    lateinit var mockedPCFactory: StreamPeerConnectionFactory

    @Before
    fun setup() {
        // setup the mock
        clientImpl.peerConnectionFactory = mockedPCFactory
    }

    @Test
    fun `Constraints`() = runTest {
        val mediaConstraints = buildMediaConstraints()
        println(mediaConstraints)
        val audioConstraints = buildAudioConstraints()
        println(audioConstraints)
    }

    @Test
    fun `Create a subscriber peer connection`() = runTest {
        val joinResult = call.join()
        assertSuccess(joinResult)
        val subscriber = call.activeSession!!.createSubscriber()
        println(subscriber)
    }

    @Test
    fun `Create a publisher peer connection`() = runTest {
        val joinResult = call.join()
        assertSuccess(joinResult)
        val publisher = call.activeSession!!.createPublisher()
        println(publisher)
    }

    @Test
    fun `Offer and Answer cycle`() = runTest {
        // Join the call
        val joinResult = call.join()
        assertSuccess(joinResult)

        val offerEvent = SubscriberOfferEvent(sdp=testData.fakeSDP)
        call.activeSession?.handleSubscriberOffer(offerEvent)
    }

    @Test
    fun `ICE Trickle`() = runTest {
        // Join the call
        val joinResult = call.join()
        assertSuccess(joinResult)

        // publisher trickle
        val candidate = """{"sdpMid": "test", "sdpMLineIndex": 0, "candidate": "test", "usernameFragment": "test"}"""
        val publisherTrickle = ICETrickleEvent(candidate, PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED)
        call.activeSession?.handleTrickle(publisherTrickle)
        // subscriber trickle
        val subscriberTrickle = ICETrickleEvent(candidate, PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED)
        call.activeSession?.handleTrickle(subscriberTrickle)
    }

    @Test
    fun `onNegotiationNeeded`() = runTest {

    }

    @Test
    fun `Dynascale events`() = runTest {
        TODO()
    }

    @Test
    fun `Publish the video track`() = runTest {
        // Join the call
        val joinResult = call.join()
        assertSuccess(joinResult)
        waitForNextEvent<SFUConnectedEvent>()
        Truth.assertThat(call.state.connection.value).isEqualTo(ConnectionState.Connected)
        call.activeSession?.publishVideo()
    }

    @Test
    fun `Send audio track`() = runTest {

    }


    @Test
    fun `Switch video track`() = runTest {

    }


}