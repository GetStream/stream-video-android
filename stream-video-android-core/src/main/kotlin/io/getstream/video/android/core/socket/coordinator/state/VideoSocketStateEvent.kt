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

package io.getstream.video.android.core.socket.coordinator.state

import io.getstream.android.video.generated.models.ConnectedEvent
import io.getstream.result.Error
import io.getstream.video.android.core.socket.common.ConnectionConf

public sealed class VideoSocketStateEvent {

    /**
     * Event to start a new connection.
     */
    data class Connect(
        val connectionConf: ConnectionConf,
        val connectionType: VideoSocketConnectionType,
    ) : VideoSocketStateEvent()

    /**
     * Event to notify the connection was established.
     */
    data class ConnectionEstablished(val connectedEvent: ConnectedEvent) : VideoSocketStateEvent()

    /**
     * Event to notify some WebSocket event has been lost.
     */
    object WebSocketEventLost : VideoSocketStateEvent() { override fun toString() = "WebSocketEventLost" }

    /**
     * Event to notify Network is not available.
     */
    object NetworkNotAvailable : VideoSocketStateEvent() { override fun toString() = "NetworkNotAvailable" }

    /**
     * Event to notify Network is available.
     */
    object NetworkAvailable : VideoSocketStateEvent() { override fun toString() = "NetworkAvailable" }

    /**
     * Event to notify an Unrecoverable Error happened on the WebSocket connection.
     */
    data class UnrecoverableError(val error: Error.NetworkError) : VideoSocketStateEvent()

    /**
     * Event to notify a network Error happened on the WebSocket connection.
     */
    data class NetworkError(val error: Error.NetworkError) : VideoSocketStateEvent()

    /**
     * Event to stop WebSocket connection required by user.
     */
    object RequiredDisconnection : VideoSocketStateEvent() { override fun toString() = "RequiredDisconnection" }

    /**
     * Event to stop WebSocket connection.
     */
    object Stop : VideoSocketStateEvent() { override fun toString() = "Stop" }

    /**
     * Event to resume WebSocket connection.
     */
    object Resume : VideoSocketStateEvent() { override fun toString() = "Resume" }
}
