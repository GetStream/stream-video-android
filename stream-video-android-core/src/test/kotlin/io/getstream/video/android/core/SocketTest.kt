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

import android.content.Context
import android.net.ConnectivityManager
import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.socket.CoordinatorSocket
import io.getstream.video.android.core.socket.PersistentSocket
import io.getstream.video.android.core.socket.SfuSocket
import io.getstream.video.android.core.socket.SocketState
import io.getstream.video.android.core.socket.SocketState.Connected
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.WebSocket
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.openapitools.client.models.VideoEvent
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

/**
 * Test coverage for the sockets
 *
 * @see PersistentSocket
 * @see CoordinatorSocket
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
open class SocketTestBase : TestBase() {
    val coordinatorUrl =
        "https://video.stream-io-api.com/video/connect?api_key=hd8szvscpxvd&stream-auth-type=jwt&X-Stream-Client=stream-video-android"
    val sfuUrl = "wss://sfu-9c0dc03.ovh-lim1.stream-io-video.com/ws"

    val sfuToken =
        "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI4ZDBlYjU0NDg4ZDFiYTUxOTk3Y2Y1NWRmYTY0Y2NiMCIsInN1YiI6InVzZXIvdGhpZXJyeSIsImF1ZCI6WyJzZnUtOWMwZGMwMy5vdmgtbGltMS5zdHJlYW0taW8tdmlkZW8uY29tIl0sImV4cCI6MTY4MjIwMDYxMiwibmJmIjoxNjgyMTc5MDEyLCJpYXQiOjE2ODIxNzkwMTIsImFwcF9pZCI6MTEyOTUyOCwiY2FsbF9pZCI6ImRlZmF1bHQ6ajhCOGhNbTJ3U0FqIiwidXNlciI6eyJpZCI6InRoaWVycnkiLCJuYW1lIjoiVGhpZXJyeSIsImltYWdlIjoiaGVsbG8iLCJ0cCI6IjByYlhXV2trRy8zRWF6OFl3OFdnak5JdlNSZlNIQWszIn0sInJvbGVzIjpbInVzZXIiXSwib3duZXIiOmZhbHNlfQ.Xb1ysM0s7qXfOPvAHYRxnXfgfVScEMQyk_GKMucmwWbgdkd8-_Vl2xps38FL4uXidFzRuVRIcVTtJSupn2ePNA"

    @RelaxedMockK
    lateinit var mockedWebSocket: WebSocket

    /**
     * Mocks
     * - network state, so we can fake going offline/online
     * - socket, so we can pretend the network is unavailable or we get an error
     */

    val networkStateProvider = NetworkStateProvider(
        connectivityManager = context
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    )
    val scope = CoroutineScope(DispatcherProvider.IO)

    fun buildOkHttp(): OkHttpClient {
        val connectionTimeoutInMs = 10000L
        return OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .connectTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .writeTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .readTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .callTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .build()
    }

    fun collectEvents(socket: CoordinatorSocket): Pair<List<VideoEvent>, List<Throwable>> {
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
            socket.disconnect()
            job.cancel()
            job2.cancel()
        }

        return Pair(events, errors)
    }
}

@RunWith(RobolectricTestRunner::class)
class CoordinatorSocketTest : SocketTestBase() {

    @Test
    fun `coordinator - connect the socket`() = runTest {
        val socket = CoordinatorSocket(coordinatorUrl, testData.users["thierry"]!!, testData.tokens["thierry"]!!, scope, buildOkHttp(), networkStateProvider)
        socket.connect()

        assertThat(socket.connectionState.value).isInstanceOf(Connected::class.java)
        assertThat(socket.connectionId).isNotEmpty()

        val (events, errors) = collectEvents(socket)
    }

    @Test
    fun `coordinator - an expired socket should be refreshed using the token provider`() = runTest {
        val socket = CoordinatorSocket(coordinatorUrl, testData.users["thierry"]!!, testData.expiredToken, scope, buildOkHttp(), networkStateProvider)
        // token refresh should be handled at the StreamVideoImpl level
        try {
            socket.connect()
        } catch (e: Throwable) {
            // ignore
        }
    }

    @Test
    fun `coordinator - a permanent error shouldn't be retried`() = runTest {
        val socket = CoordinatorSocket(coordinatorUrl, testData.users["thierry"]!!, "invalid token", scope, buildOkHttp(), networkStateProvider)
        try {
            socket.connect()
        } catch (e: Throwable) {
            // ignore
        }
        assertThat(socket.reconnectionAttempts).isEqualTo(0)
        assertThat(socket.connectionState.value).isInstanceOf(SocketState.DisconnectedPermanently::class.java)
    }

    @Test
    fun `coordinator - a temporary error should be retried`() = runTest {
        // mock the actual socket connection
        val socket = CoordinatorSocket(coordinatorUrl, testData.users["thierry"]!!, testData.tokens["thierry"]!!, scope, buildOkHttp(), networkStateProvider)
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
        socket.disconnect()

        assertThat(socket.reconnectionAttempts).isEqualTo(1)
    }

    @Test
    fun `going offline should temporarily disconnect`() = runTest {
        // mock the actual socket connection
        val socket = CoordinatorSocket(coordinatorUrl, testData.users["thierry"]!!, testData.tokens["thierry"]!!, scope, buildOkHttp(), networkStateProvider)
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
            socket.disconnect()
            job.cancel()
            job2.cancel()
        }

        return Pair(events, errors)
    }

    @Test
    @Ignore("token is expired")
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
            networkStateProvider
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
            networkStateProvider
        )
        try {
            socket.connect()
        } catch (e: Throwable) {
            // ignore
        }
        assertThat(socket.reconnectionAttempts).isEqualTo(0)
        assertThat(socket.connectionState.value).isInstanceOf(SocketState.DisconnectedPermanently::class.java)
    }
}
