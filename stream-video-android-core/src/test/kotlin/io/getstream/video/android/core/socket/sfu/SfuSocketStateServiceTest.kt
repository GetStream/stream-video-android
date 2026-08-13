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

package io.getstream.video.android.core.socket.sfu

import com.google.common.truth.Truth.assertThat
import io.getstream.result.Error
import io.getstream.video.android.core.errors.VideoErrorCode
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.socket.common.ConnectionConf
import io.getstream.video.android.core.socket.common.fromVideoErrorCode
import io.getstream.video.android.core.socket.sfu.state.SfuSocketState
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import stream.video.sfu.models.WebsocketReconnectStrategy

class SfuSocketStateServiceTest {

    private val conf = mockk<ConnectionConf.SfuConnectionConf>(relaxed = true)

    @Test
    fun `connecting then websocket open then join response reaches Connected`() = runTest {
        val service = SfuSocketStateService()

        service.onConnect(conf)
        assertThat(service.currentState).isInstanceOf(SfuSocketState.Connecting::class.java)

        // Transport WebSocket opened, but no JoinResponse yet.
        service.onWebSocketEstablished()
        assertThat(service.currentState).isEqualTo(SfuSocketState.WebSocketConnected)

        // JoinResponse delivered by the SFU.
        service.onConnectionEstablished(mockk<JoinCallResponseEvent>(relaxed = true))
        assertThat(service.currentState).isInstanceOf(SfuSocketState.Connected::class.java)
    }

    @Test
    fun `transport failure while Connecting moves to DisconnectedTemporarily carrying its code`() =
        runTest {
            val service = SfuSocketStateService()
            service.onConnect(conf)
            assertThat(service.currentState).isInstanceOf(SfuSocketState.Connecting::class.java)

            // OkHttp failed the WebSocket upgrade before it opened — the error is passed
            // through as-is, carrying its own code/reason.
            service.onNetworkError(
                Error.NetworkError.fromVideoErrorCode(VideoErrorCode.SOCKET_FAILURE),
                WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
            )

            val state = service.currentState
            assertThat(
                state,
            ).isInstanceOf(SfuSocketState.Disconnected.DisconnectedTemporarily::class.java)
            assertThat(
                (state as SfuSocketState.Disconnected.DisconnectedTemporarily).error.serverErrorCode,
            )
                .isEqualTo(VideoErrorCode.SOCKET_FAILURE.code)
        }

    @Test
    fun `join-response timeout while WebSocketConnected moves to DisconnectedTemporarily with join-response code`() =
        runTest {
            val service = SfuSocketStateService()
            service.onConnect(conf)
            service.onWebSocketEstablished()
            assertThat(service.currentState).isEqualTo(SfuSocketState.WebSocketConnected)

            // The join-response timer fires after the WebSocket opened.
            service.onNetworkError(
                Error.NetworkError.fromVideoErrorCode(VideoErrorCode.SFU_JOIN_RESPONSE_TIMEOUT),
                WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
            )

            val state = service.currentState
            assertThat(
                state,
            ).isInstanceOf(SfuSocketState.Disconnected.DisconnectedTemporarily::class.java)
            assertThat(
                (state as SfuSocketState.Disconnected.DisconnectedTemporarily).error.serverErrorCode,
            )
                .isEqualTo(VideoErrorCode.SFU_JOIN_RESPONSE_TIMEOUT.code)
        }

    @Test
    fun `timeout disconnect can reconnect via Connect`() = runTest {
        val service = SfuSocketStateService()
        service.onConnect(conf)
        service.onWebSocketEstablished()
        service.onNetworkError(
            Error.NetworkError.fromVideoErrorCode(VideoErrorCode.SFU_JOIN_RESPONSE_TIMEOUT),
            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
        )
        assertThat(service.currentState)
            .isInstanceOf(SfuSocketState.Disconnected.DisconnectedTemporarily::class.java)

        service.onConnect(conf)
        assertThat(service.currentState).isInstanceOf(SfuSocketState.Connecting::class.java)
    }

    @Test
    fun `join response can still arrive directly from Connecting`() = runTest {
        val service = SfuSocketStateService()
        service.onConnect(conf)

        service.onConnectionEstablished(mockk<JoinCallResponseEvent>(relaxed = true))
        assertThat(service.currentState).isInstanceOf(SfuSocketState.Connected::class.java)
    }
}
