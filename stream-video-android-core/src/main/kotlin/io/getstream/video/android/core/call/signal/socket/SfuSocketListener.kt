/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.call.signal.socket

import io.getstream.result.StreamError
import io.getstream.video.android.core.errors.DisconnectCause
import io.getstream.video.android.core.events.ConnectedEvent
import io.getstream.video.android.core.events.SfuDataEvent

/**
 * Used to listen to socket events on the SFU/Signaling level, inside an active call.
 */
public interface SfuSocketListener {

    /**
     * Triggered when we begin the connection process. Useful to indicate the progress to your
     * users.
     */
    public fun onConnecting() {
    }

    /**
     * Triggered when a socket connection is successfully established.
     *
     * @param event The [ConnectedEvent] that contains the ID of the client socket connection.
     */
    public fun onConnected(event: ConnectedEvent) {
    }

    /**
     * Triggered when the socket gets disconnected for any reason.
     * @see DisconnectCause For possible reasons the socket might be disconnected.
     *
     * @param cause The cause of the disconnection.
     */
    public fun onDisconnected(cause: DisconnectCause) {
    }

    /**
     * Triggered when there's an error in the connecting logic or after the socket was connected.
     *
     * @param error The issue that occurred with the socket.
     */
    public fun onError(error: StreamError) {
    }

    /**
     * Used for passing down all [SfuDataEvent]s coming from the WebSocket connection, after parse.
     *
     * @param event The event which holds concrete information based on its type.
     */
    public fun onEvent(event: SfuDataEvent) {
    }
}
