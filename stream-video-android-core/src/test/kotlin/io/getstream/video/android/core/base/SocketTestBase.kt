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

package io.getstream.video.android.core.base

import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.Lifecycle
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.socket.common.StreamWebSocketEvent
import io.getstream.video.android.core.socket.coordinator.CoordinatorSocketConnection
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.WebSocket
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

open class SocketTestBase : TestBase() {
    val coordinatorUrl =
        "https://video.stream-io-api.com/video/connect?api_key=${authData?.apiKey!!}&stream-auth-type=jwt&X-Stream-Client=stream-video-android"
    val sfuUrl = "wss://sfu-f079b1a.dpk-den1.stream-io-video.com/ws"
    val sfuToken =
        "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI4ZDBlYjU0NDg4ZDFiYTUxOTk3Y2Y1NWRmYTY0Y2NiMCIsInN1YiI6InVzZXIvdGhpZXJyeSIsImF1ZCI6WyJzZnUtZjA3OWIxYS5kcGstZGVuMS5zdHJlYW0taW8tdmlkZW8uY29tIl0sImV4cCI6MTY4NDI5NTQyMiwibmJmIjoxNjg0MjczODIyLCJpYXQiOjE2ODQyNzM4MjIsImFwcF9pZCI6MTEyOTUyOCwiY2FsbF9pZCI6ImRlZmF1bHQ6ZTJjMDRkZjYtYTNiMy00YTcyLWIxMjctOTJiNjkyZmMxMDA2IiwidXNlciI6eyJpZCI6InRoaWVycnkiLCJuYW1lIjoiVGhpZXJyeSIsImltYWdlIjoiaGVsbG8iLCJ0cCI6IkdvNGhUc3R4MEhoUDFXRnBDa0NlT0w0ZmpxVDRCQWx1In0sInJvbGVzIjpbInVzZXIiXSwib3duZXIiOnRydWV9.Fp5z70vshi6UoGMMNV1aZd1AIAS4fvm46jJ_O3YGfdyReUXl7gDHeonxwrbT0OJ-rd2tyGVHCCrbvkqGEznwKg"

    @RelaxedMockK
    lateinit var mockedWebSocket: WebSocket

    val scope = CoroutineScope(DispatcherProvider.IO)

    /**
     * Mocks
     * - network state, so we can fake going offline/online
     * - socket, so we can pretend the network is unavailable or we get an error
     */
    val networkStateProvider = NetworkStateProvider(
        scope = scope,
        connectivityManager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE,
        ) as ConnectivityManager,
    )

    val lifecycle = mockk<Lifecycle>(relaxed = true)

    fun buildOkHttp(): OkHttpClient {
        val connectionTimeoutInMs = 10000L
        return OkHttpClient.Builder().addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            },
        ).connectTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .writeTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .readTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .callTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS).build()
    }

    fun collectEvents(socket: CoordinatorSocketConnection): Pair<List<VideoEvent>, List<StreamWebSocketEvent.Error>> {
        val events = mutableListOf<VideoEvent>()
        val errors = mutableListOf<StreamWebSocketEvent.Error>()

        runBlocking {
            val job = launch {
                socket.events().collect {
                    events.add(it)
                }
            }

            val job2 = launch {
                socket.errors().collect { err ->
                    errors.add(err)
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
