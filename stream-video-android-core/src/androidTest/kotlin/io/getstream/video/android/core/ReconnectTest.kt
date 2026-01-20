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

package io.getstream.video.android.core

import app.cash.turbine.testIn
import com.google.common.truth.Truth.assertThat
import io.getstream.log.taggedLogger
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Test
import org.webrtc.PeerConnection
import java.util.UUID

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
 * Note that the SFU doesnt set up the subscriber ice connection if nobody is publishing
 *
 */
class ReconnectTest : IntegrationTestBase(connectCoordinatorWS = false) {

    private val logger by taggedLogger("Test:ReconnectTest")

    @Test
    fun networkDown() = runTest {
        val call = client.call("default", UUID.randomUUID().toString())
        // join a call
        val result = call.join(create = true)
        // create a turbine connection state
        val connectionState = call.state.connection.testIn(backgroundScope)
        // asset that the connection state is connected
        val connectionStateItem = connectionState.awaitItem()
        assertThat(connectionStateItem).isAnyOf(
            RealtimeConnection.Connected,
            RealtimeConnection.Joined(result.getOrThrow()),
        )
        if (connectionStateItem is RealtimeConnection.Joined) {
            connectionState.awaitItem()
        }
        // assert the connection state is connected
        // disconnect the network
        // call.monitor.networkStateListener.onDisconnected()
        // verify that the connection state is reconnecting
        assertThat(connectionState.awaitItem()).isEqualTo(RealtimeConnection.Reconnecting)
        // go online and verify we're reconnected
        // call.monitor.networkStateListener.onConnected()
        // asset that the connection state is connected
        assertThat(connectionState.awaitItem()).isEqualTo(RealtimeConnection.Connected)
        // leave and clean up a call
        call.leave()
        call.cleanup()
        // await until disconnect a call
        assertThat(connectionState.awaitItem()).isEqualTo(RealtimeConnection.Disconnected)
    }

    @Test
    @Ignore("need more mocking for this test")
    fun peerConnectionBad() = runTest {
        val states = mutableListOf<RealtimeConnection>()
        backgroundScope.launch {
            call.state.connection.collect { states.add(it) }
        }

        // join a call
        call.join()
        Thread.sleep(2000L)
        // disconnect a peer connection
        call.session?.publisher?.connection?.dispose()
        // if we wait a bit we should recover
        Thread.sleep(4000L)
        println(states)
    }

    /**
     * If the peer connection breaks we should retry
     */
    @Test
    fun restartIce() = runTest {
        val call = client.call("default", UUID.randomUUID().toString())
        // join a call
        val result = call.join(create = true)
        // create a turbine of the publisher's peer connection state
        val publisher = call.session?.publisher?.state?.testIn(backgroundScope)

        // create a turbine connection state
        val connectionState = call.state.connection.testIn(backgroundScope)
        // asset that the connection state is connected
        val connectionStateItem = connectionState.awaitItem()
        assertThat(connectionStateItem).isAnyOf(
            RealtimeConnection.Connected,
            RealtimeConnection.Joined(result.getOrThrow()),
        )
        if (connectionStateItem is RealtimeConnection.Joined) {
            connectionState.awaitItem()
        }

        // assert peer connection state flows
        assertThat(publisher?.awaitItem()).isEqualTo(PeerConnection.PeerConnectionState.NEW)
        assertThat(publisher?.awaitItem()).isEqualTo(PeerConnection.PeerConnectionState.CONNECTING)
        assertThat(publisher?.awaitItem()).isEqualTo(PeerConnection.PeerConnectionState.CONNECTED)

        // the socket and rtc connection disconnect...,
        // or ice candidate don't arrive due to temporary network failure
        call.session?.reconnect(forceRestart = true)

        // leave and clean up a call
        call.leave()
        call.cleanup()
        // await until disconnect a call
        assertThat(connectionState.awaitItem()).isEqualTo(RealtimeConnection.Disconnected)
    }
}
