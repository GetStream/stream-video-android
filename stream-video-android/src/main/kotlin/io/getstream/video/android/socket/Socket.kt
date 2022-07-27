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

import com.squareup.wire.Message
import okhttp3.WebSocket
import stream.video.AuthPayload
import stream.video.Healthcheck

/**
 * Wrapper around the [WebSocket] that lets you send different types of events.
 *
 * @property socket The real WS that we send data to and receive data from.
 */
internal class Socket(private val socket: WebSocket) {

    /**
     * Sends the [authPayload] as a Binary message to the socket, attempting to authenticate the
     * currently logged in user.
     */
    fun authenticate(authPayload: AuthPayload) {
        socket.send(authPayload.encodeByteString())
    }

    /**
     * Sends an event which can be encoded into a ByteString using proto.
     *
     * @param event The event to send to the socket.
     */
    fun send(event: Message<*, *>) {
        socket.send(event.encodeByteString())
    }

    /**
     * Pings the server with a state update.
     *
     * @param state The state of the connection.
     */
    fun ping(state: Healthcheck) {
        socket.send(state.encodeByteString())
    }

    /**
     * Closes the connection of the WebSocket with a given code and reason.
     *
     * @param code Code that describes the type of close.
     * @param reason The explanation of the close.
     */
    fun close(code: Int, reason: String) {
        socket.close(code, reason)
    }
}
