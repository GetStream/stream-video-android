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

package io.getstream.video.android.call.signal.socket

import io.getstream.video.android.errors.VideoError
import io.getstream.video.android.events.ConnectedEvent
import io.getstream.video.android.events.SfuDataEvent
import stream.video.sfu.event.JoinRequest

internal interface SfuSocket {

    /**
     * Initializes the socket connection.
     */
    fun connectSocket()

    /**
     * Sends a request to join the call.
     */
    fun sendJoinRequest(request: JoinRequest)

    /**
     * Attempts to reconnect the socket.
     */
    fun reconnect()

    /**
     * Triggered when an error happens with the socket connection or events parsing.
     *
     * @param error The issue that caused the socket to fail.
     */
    fun onSocketError(error: VideoError)

    /**
     * Triggered when an event is received from the socket.
     *
     * @param event The event received.
     */
    fun onEvent(event: SfuDataEvent)

    /**
     * Triggered when the connection is fully resolved and validated.
     *
     * @param event The event that holds connection data.
     */
    fun onConnectionResolved(event: ConnectedEvent)

    /**
     * Releases the socket connection when required in the app lifecycle.
     */
    fun releaseConnection()

    /**
     * Attaches a listener to the socket that receives events.
     */
    fun addListener(sfuSocketListener: SfuSocketListener)
    /**
     * Detaches a listener from the socket to stop receiving events.
     */
    fun removeListener(sfuSocketListener: SfuSocketListener)
}
