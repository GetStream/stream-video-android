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

package io.getstream.video.android.core.socket.common

import io.getstream.result.Error
import io.getstream.video.android.core.errors.DisconnectCause
import io.getstream.android.video.generated.models.VideoEvent

/**
 * Listener which is invoked for WebSocket events.
 */
public open class SocketListener<EventType, ConnectedEventType> {

    /**
     * The callbacks are by default delivered on the main thread. Changing this property to false will deliver
     * the callbacks on their originating threads.
     *
     * Set to false for faster callback delivery on the original thread (no unnecessary context switching).
     */
    public open val deliverOnMainThread: Boolean = true

    /**
     * Called when the socket is created.
     */
    public open fun onCreated() {
    }

    /**
     * Invoked when the connection begins to establish and socket state changes to Connecting.
     */
    public open fun onConnecting() {
    }

    /**
     * Invoked when we receive the first [ConnectedEventType] in this connection.
     *
     * Note: This is not invoked when the ws connection is opened but when the [ConnectedEventType] is received.
     *
     * @param event [ConnectedEventType] sent by server as first event once the connection is established.
     */
    public open fun onConnected(event: ConnectedEventType) {
    }

    /**
     * Invoked when the web socket connection is disconnected.
     *
     * @param cause [DisconnectCause] reason of disconnection.
     */
    public open fun onDisconnected(cause: DisconnectCause) {
    }

    /**
     * Invoked when there is any error in this web socket connection.
     *
     * @param error [Error] object with the error details.
     */
    public open fun onError(error: StreamWebSocketEvent.Error) {
    }

    /**
     * Invoked when we receive any successful event.
     *
     * @param event parsed [VideoEvent] received in this web socket connection.
     */
    public open fun onEvent(event: EventType) {
    }
}
