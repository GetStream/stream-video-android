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

package io.getstream.video.android.core

import app.cash.turbine.testIn
import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.base.SocketTestBase
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.socket.SfuSocket
import io.getstream.video.android.core.socket.coordinator.CoordinatorSocketConnection
import io.getstream.video.android.core.socket.sfu.state.SfuSocketState
import io.getstream.video.android.core.socket.sfu.state.SfuSocketState.Connected
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.openapitools.client.models.VideoEvent
import org.robolectric.RobolectricTestRunner

/**
 * Test coverage for the sockets
 *
 * @see CoordinatorSocketConnection
 * @see CoordinatorSocketConnection
 * @see SfuSocket
 *
 * The socket does a few things
 * - health check/ping every 30 seconds
 * - monitors network state
 * - in case of temporary errors retry and for permanent ones fail
 *
 * The Sfu and coordinator have slightly different implements
 * Auth receives different events
 *
 * @see JoinCallEvent (for the sfu)
 * @see ConnectedEvent (for the coordinator)
 *
 * The sfu uses binary messages and the coordinator text
 * Other than it's mostly the same logic
 *
 */

@RunWith(RobolectricTestRunner::class)
class CoordinatorSocketConnectionTest : SocketTestBase() {

    @Test
    fun `coordinator - connect the socket`() = runTest {
        val mockNetworkStateProvider = mockk<NetworkStateProvider>()
        every { mockNetworkStateProvider.isConnected() } returns true
        val socket = CoordinatorSocketConnection(
            coordinatorUrl,
            testData.users["thierry"]!!,
            authData?.token!!,
            scope,
            buildOkHttp(),
            networkStateProvider,
        )
        socket.connect()

        val connectionState = socket.connectionState.testIn(backgroundScope)
        val connectionStateItem = connectionState.awaitItem()

        assertThat(connectionStateItem).isInstanceOf(Connected::class.java)
        assertThat(socket.connectionId.value).isNotEmpty()
    }

    @Test
    @Ignore
    fun `coordinator - an expired socket should be refreshed using the token provider`() = runTest {
        val socket = CoordinatorSocketConnection(
            coordinatorUrl,
            testData.users["thierry"]!!,
            testData.expiredToken,
            scope,
            buildOkHttp(),
            networkStateProvider,
        )
        // token refresh should be handled at the StreamVideoImpl level
        try {
            socket.connect()
        } catch (e: Throwable) {
            // ignore
        }
    }

    @Test
    fun `coordinator - a permanent error shouldn't be retried`() = runTest {
        val socket = CoordinatorSocketConnection(
            coordinatorUrl,
            testData.users["thierry"]!!,
            "invalid token",
            scope,
            buildOkHttp(),
            networkStateProvider,
        )
        try {
            socket.connect { it.cancel() }
        } catch (e: Throwable) {
            // ignore
        }

        val connectionState = socket.connectionState.testIn(backgroundScope)
        val connectionStateItem = connectionState.awaitItem()

        assertThat(socket.reconnectionAttempts).isEqualTo(0)
        assertThat(connectionStateItem).isInstanceOf(SfuSocketState.Connecting::class.java)
    }

    @Test
    @Ignore
    fun `coordinator - a temporary error should be retried`() = runTest {
        // mock the actual socket connection
        val mockedNetworkStateProvider = mockk<NetworkStateProvider>(relaxed = true)
        every { mockedNetworkStateProvider.isConnected() } returns true
        val socket = CoordinatorSocketConnection(
            coordinatorUrl,
            testData.users["thierry"]!!,
            authData?.token!!,
            scope,
            buildOkHttp(),
            mockedNetworkStateProvider,
        )
        socket.mockSocket = mockedWebSocket
        socket.reconnectTimeout = 0
        val job1 = scope.launch { socket.connect() }
        val job2 = scope.launch {
            // trigger an unknown host exception from onFailure
            val error = java.net.UnknownHostException("internet is down")
            socket.onFailure(mockedWebSocket, error, null)
        }
        // trigger the error
        job2.join()
        // complete the connection (which should fail)
        delay(10) // timeout is 500ms by default
        socket.disconnect(CoordinatorSocketConnection.DisconnectReason.ByRequest)

        assertThat(socket.reconnectionAttempts).isEqualTo(1)
    }

    @Test
    fun `going offline should temporarily disconnect`() = runTest {
        // mock the actual socket connection
        val socket = CoordinatorSocketConnection(
            coordinatorUrl,
            testData.users["thierry"]!!,
            testData.tokens["thierry"]!!,
            scope,
            buildOkHttp(),
            networkStateProvider,
        )
        socket.mockSocket = mockedWebSocket
        socket.reconnectTimeout = 0
        val job = scope.launch { socket.connect() }
        delay(100)
        socket.onInternetDisconnected()
        // assertThat(socket.connectionState.value).isInstanceOf(SocketState.NetworkDisconnected::class.java)
        // go back online
        val job2 = scope.launch { socket.onInternetConnected() }
        delay(100)
        job.cancel()
        job2.cancel()
        // TODO: this could be easier to test
        // assertThat(socket.connectionState.value).isInstanceOf(SocketState.Connecting::class.java)
    }

    @Test
    fun `wrong formatted VideoEventType is ignored`() = runTest {
        // mock the actual socket connection
        val socket = CoordinatorSocketConnection(
            coordinatorUrl,
            testData.users["thierry"]!!,
            authData?.token!!,
            // make sure to use the TestScope because the exceptions will be swallowed by regular CoroutineScope
            scope = this,
            buildOkHttp(),
            networkStateProvider,
        )

        socket.connect()
        socket.connectContinuation.cancel()

        // create a VideoEvent type that resembles a real one, but doesn't contain the necessary fields
        val testJson = "{\"type\":\"health.check\"}"
        socket.onMessage(mockedWebSocket, testJson)
        // no exception is thrown
    }

    @Test
    fun `wrong formatted error in VideoEventType is ignored`() = runTest {
        // mock the actual socket connection
        val socket = CoordinatorSocketConnection(
            coordinatorUrl,
            testData.users["thierry"]!!,
            authData?.token!!,
            // make sure to use the TestScope because the exceptions will be swallowed by regular CoroutineScope
            scope = this,
            buildOkHttp(),
            networkStateProvider,
        )
        socket.connect()

        // create a socket message that doesn't even have the error message (so it's neither
        // a valid VideoEventType nor it is a valid Error)
        val testJson = "{\"someRandomField\":\"randomValue\"}"
        socket.onMessage(mockedWebSocket, testJson)
        // no exception is thrown
    }
}

@RunWith(RobolectricTestRunner::class)
class SfuSocketTest : SocketTestBase() {
    fun collectEvents(socket: SfuSocket): Pair<List<VideoEvent>, List<Throwable>> {
        val events = mutableListOf<VideoEvent>()
        val errors = mutableListOf<Throwable>()

        runBlocking {
            val job = launch {
                socket.events.collect() {
                    events.add(it)
                }
            }

            val job2 = launch {
                socket.errors.collect() {
                    errors.add(it)
                }
            }

            delay(1000)
            socket.disconnect(CoordinatorSocketConnection.DisconnectReason.ByRequest)
            job.cancel()
            job2.cancel()
        }

        return Pair(events, errors)
    }

    @Test
    @Ignore
    fun `sfu socket should connect and stay connected`() = runTest {
        val sessionId = randomUUID().toString()
        val updateSdp: () -> String = {
            "hello"
        }
        val socket = SfuSocket(
            sfuUrl,
            sessionId,
            sfuToken,
            updateSdp,
            scope,
            buildOkHttp(),
            networkStateProvider,
            {},
        )
        socket.connect()
        socket.sendHealthCheck()

        scope.launch {
            socket.events.collect {
                println("event: $it")
            }
        }
        scope.launch {
            socket.errors.collect {
                println("errors: $it")
            }
        }

        while (true) {
            Thread.sleep(5_000)
            println("socket state: ${socket.connectionState.value}")
        }
    }

    @Test
    @Ignore
    fun `sfu - a permanent error shouldn't be retried a`() = runTest {
        val sessionId = randomUUID().toString()
        val updateSdp: () -> String = {
            "hello"
        }
        val socket = SfuSocket(
            sfuUrl,
            sessionId,
            "invalid",
            updateSdp,
            scope,
            buildOkHttp(),
            networkStateProvider,
            {},
        )
        try {
            socket.connect()
        } catch (e: Throwable) {
            // ignore
        }

        val connectionState = socket.connectionState.testIn(backgroundScope)
        val connectionStateItem = connectionState.awaitItem()

        assertThat(socket.reconnectionAttempts).isEqualTo(0)
        assertThat(
            connectionStateItem,
        ).isInstanceOf(SfuSocketState.DisconnectedPermanently::class.java)
    }
}
