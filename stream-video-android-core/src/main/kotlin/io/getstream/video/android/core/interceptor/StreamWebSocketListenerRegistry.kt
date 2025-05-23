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

package io.getstream.video.android.core.interceptor

import io.getstream.video.android.core.internal.ExperimentalStreamVideoApi
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

object StreamWebSocketListenerRegistry {
    private val listeners = mutableListOf<WebSocketListener>()

    @ExperimentalStreamVideoApi
    fun register(listener: WebSocketListener) {
        listeners += listener
    }

    internal fun wrap(baseListener: WebSocketListener): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                listeners.forEach { it.onOpen(webSocket, response) }
                baseListener.onOpen(webSocket, response)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                listeners.forEach { it.onMessage(webSocket, text) }
                baseListener.onMessage(webSocket, text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                listeners.forEach { it.onMessage(webSocket, bytes) }
                baseListener.onMessage(webSocket, bytes)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                listeners.forEach { it.onClosing(webSocket, code, reason) }
                baseListener.onClosing(webSocket, code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                listeners.forEach { it.onClosed(webSocket, code, reason) }
                baseListener.onClosed(webSocket, code, reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                listeners.forEach { it.onFailure(webSocket, t, response) }
                baseListener.onFailure(webSocket, t, response)
            }
        }
    }
}
