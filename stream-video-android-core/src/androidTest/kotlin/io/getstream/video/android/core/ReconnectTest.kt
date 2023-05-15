/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core

import com.google.common.truth.Truth.assertThat
import io.getstream.log.taggedLogger
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Connection state shows if we've established a connection with the SFU
 * - join state (did we try to setup the RtcSession yes or no?). API calls can fail during the join and require us to repeat it
 * - socket connection health. the socket can disconnect.
 * - peer connection subscriber
 * - peer connection publisher
 * - network detection might be faster than the peer connection
 * The call connection state is the health of these 5 components
 *
 * Note that the video connection can still be working even if:
 * * the socket is disconnected
 * * publisher is disconnected
 *
 * When the connection breaks the subscriber peer connection will usually indicate the issue first (since it has constant traffic)
 * The subscriber can break because of 2 reasons:
 * * Something is wrong with your network (90% of the time)
 * * Something is wrong with the SFU (should be rare)
 *
 * We want the reconnect to be as fast as possible.
 * * If you can reach our edge network your connection is fine. So the optimal flow here is
 * * When there is an error try to connect to the same SFU immediately
 * * Meanwhile ask the API if we need to switch to a different
 * * If the API says we need to switch, swap to the new SFU
 *
 * TODO: Which API endpoint should we call to check if we need to switch SFU?
 *
 */
class ReconnectTest : IntegrationTestBase(connectCoordinatorWS = false) {

    private val logger by taggedLogger("Test:AndroidDeviceTest")
    @Test
    fun switchSfuTest() = runTest {
        call.join()

        // TODO: exclude the SFU that failed...
        // TODO: can we remove any API calls here or resuse latency measurements
        // TODO: add loading/status indicators

        call.switchSfu()
    }

    @Test
    fun reconnectPeers() = runTest {
        call.join()
        Thread.sleep(2000)
        val a = call.session?.subscriber?.connection?.connectionState()
        val b = call.session?.publisher?.connection?.connectionState()
        println("yyyzzz $a and $b ${call.session?.subscriber}")

        // the socket and rtc connection disconnect...,
        // or ice candidate don't arrive due to temporary network failure
        call.session?.reconnect()
        Thread.sleep(2000)
        // reconnect recreates the peer connections
        val sub = call.session?.subscriber?.connection?.connectionState()
        val pub = call.session?.publisher?.connection?.connectionState()

        println("yyyzzz $sub and $pub ${call.session?.subscriber?.state?.value}")
    }

    @Test
    fun switchSfuQuickly() = runTest {
        call.join()
        Thread.sleep(2000)

        // connect to the new socket
        // do an ice restart
        call.session?.let {
            it.switchSfu(it.sfuUrl, it.sfuToken)
        }

    }

    @Test
    fun showErrors() = runTest {
        call.join()
        Thread.sleep(2000)

        // Show an error if we are reconnecting
        assertThat(call.state._connection.value).isEqualTo(RtcConnectionState.Reconnecting)

        // connect to the new socket
        // do an ice restart
        call.session?.let {
            it.switchSfu(it.sfuUrl, it.sfuToken)
        }

    }

}
