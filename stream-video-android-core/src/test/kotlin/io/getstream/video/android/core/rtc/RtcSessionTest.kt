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

package io.getstream.video.android.core.rtc

import com.google.common.truth.Truth
import io.getstream.video.android.core.ConnectionState
import io.getstream.video.android.core.base.IntegrationTestBase
import io.getstream.video.android.core.events.ICETrickleEvent
import io.getstream.video.android.core.events.SFUConnectedEvent
import io.getstream.video.android.core.events.SubscriberOfferEvent
import io.getstream.video.android.core.utils.buildAudioConstraints
import io.getstream.video.android.core.utils.buildMediaConstraints
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import stream.video.sfu.models.PeerType

@RunWith(RobolectricTestRunner::class)
@Ignore
class RtcSessionTest : IntegrationTestBase() {
    /**
     * Creating the publisher and subscriber peer connection
     * - If we are not allowed to publish, we don't need to establish that peer connection
     *
     * TODO: This test could use a mock of the client, it doesn't need the real client and could run faster
     * Integration testing is already handled in other parts of the codebase
     */

    @Before
    fun setup() {
        // setup the mock
        call.peerConnectionFactory = mockedPCFactory
    }

    @Test
    fun `Constraints`() = runTest {
        val mediaConstraints = buildMediaConstraints()

        val audioConstraints = buildAudioConstraints()
    }

    @Test
    fun `Create a subscriber peer connection`() = runTest {
        val joinResult = call.join()
        assertSuccess(joinResult)
        val subscriber = call.session!!.createSubscriber()
    }

    @Test
    fun `Offer and Answer cycle`() = runTest {
        // Join the call
        val joinResult = call.join()
        assertSuccess(joinResult)

        val offerEvent = SubscriberOfferEvent(sdp = testData.fakeSDP)
        call.session?.handleSubscriberOffer(offerEvent)
    }

    @Test
    fun `ICE Trickle`() = runTest {
        // Join the call
        val joinResult = call.join()
        assertSuccess(joinResult)

        // publisher trickle
        val candidate =
            """{"sdpMid": "test", "sdpMLineIndex": 0, "candidate": "test", "usernameFragment": "test"}"""
        val publisherTrickle = ICETrickleEvent(candidate, PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED)
        call.session?.handleIceTrickle(publisherTrickle)
        // subscriber trickle
        val subscriberTrickle = ICETrickleEvent(candidate, PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED)
        call.session?.handleIceTrickle(subscriberTrickle)
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
        // call.session?.publishVideo()
    }

    @Test
    fun `Send audio track`() = runTest {
    }

    @Test
    fun `Switch video track`() = runTest {
    }
}
