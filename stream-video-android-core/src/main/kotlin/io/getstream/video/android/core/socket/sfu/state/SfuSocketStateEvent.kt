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

package io.getstream.video.android.core.socket.sfu.state

import io.getstream.result.Error
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.socket.common.ConnectionConf
import stream.video.sfu.models.WebsocketReconnectStrategy

public sealed class SfuSocketStateEvent {

    /**
     * Event to start a new connection.
     */
    data class Connect(
        val connectionConf: ConnectionConf.SfuConnectionConf,
        val connectionType: WebsocketReconnectStrategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_UNSPECIFIED,
    ) : SfuSocketStateEvent()

    /**
     * Event to notify the connection was established.
     */
    data class ConnectionEstablished(
        val connectedEvent: JoinCallResponseEvent,
    ) : SfuSocketStateEvent()

    /**
     * Event to notify some WebSocket event has been lost.
     */
    object WebSocketEventLost : SfuSocketStateEvent() { override fun toString() = "WebSocketEventLost" }

    /**
     * Event to notify Network is not available.
     */
    object NetworkNotAvailable : SfuSocketStateEvent() { override fun toString() = "NetworkNotAvailable" }

    /**
     * Event to notify Network is available.
     */
    object NetworkAvailable : SfuSocketStateEvent() { override fun toString() = "NetworkAvailable" }

    /**
     * Event to notify an Unrecoverable Error happened on the WebSocket connection.
     */
    data class UnrecoverableError(val error: Error.NetworkError) : SfuSocketStateEvent()

    /**
     * Event to notify a network Error happened on the WebSocket connection.
     */
    data class NetworkError(
        val error: Error.NetworkError,
        val reconnectStrategy: WebsocketReconnectStrategy,
    ) : SfuSocketStateEvent()

    /**
     * Event to stop WebSocket connection required by user.
     */
    object RequiredDisconnection : SfuSocketStateEvent() { override fun toString() = "RequiredDisconnection" }

    /**
     * Event to stop WebSocket connection.
     */
    object Stop : SfuSocketStateEvent() { override fun toString() = "Stop" }

    /**
     * Event to resume WebSocket connection.
     */
    object Resume : SfuSocketStateEvent() { override fun toString() = "Resume" }
}
