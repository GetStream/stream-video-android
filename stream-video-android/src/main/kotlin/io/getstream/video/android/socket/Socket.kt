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

import io.getstream.video.android.parser.VideoParser
import okhttp3.WebSocket
import stream.video.AuthPayload

internal class Socket(private val socket: WebSocket, private val parser: VideoParser) {

    /**
     * Sends the [authPayload] as a Binary message to the socket, attempting to authenticate the
     * currently logged in user.
     */
    fun authenticate(authPayload: AuthPayload) {
        socket.send(authPayload.encodeByteString())
    }

    fun send(event: Any) {
        socket.send(parser.toJson(event))
    }

    fun ping(message: String) {
        socket.send(message)
    }

    fun close(code: Int, reason: String) {
        socket.close(code, reason)
    }
}
