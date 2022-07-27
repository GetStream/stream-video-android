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

package io.getstream.video.android.socket

import io.getstream.video.android.errors.VideoError
import io.getstream.video.android.events.ConnectedEvent
import stream.video.Call

public interface VideoSocket {

    /**
     * Initializes the socket connection.
     */
    public fun connectSocket()

    /**
     * Attempts to authenticate the user after the initial connection event.
     */
    public fun authenticateUser()

    /**
     * Attempts to reconnect the socket.
     */
    public fun reconnect()

    /**
     * Triggered when the user joins a call.
     *
     * @param call The information about the call.
     */
    public fun onCallJoined(call: Call)

    /**
     * Triggered when the user leaves a call.
     */
    public fun onCallClosed()

    /**
     * Triggered when an error happens with the socket connection or events parsing.
     *
     * @param error The issue that caused the socket to fail.
     */
    public fun onSocketError(error: VideoError)

    /**
     * Triggered when an event is received from the socket.
     *
     * @param event The event received.
     */
    public fun onEvent(event: Any)

    /**
     * Triggered when the connection is fully resolved and validated.
     *
     * @param event The event that holds connection data.
     */
    public fun onConnectionResolved(event: ConnectedEvent)

    /**
     * Releases the socket connection when required in the app lifecycle.
     */
    public fun releaseConnection()

    /**
     * Attaches a listener to the socket that receives events.
     */
    public fun addListener(socketListener: SocketListener)

    /**
     * Detaches a listener from the socket to stop receiving events.
     */
    public fun removeListener(socketListener: SocketListener)
}
