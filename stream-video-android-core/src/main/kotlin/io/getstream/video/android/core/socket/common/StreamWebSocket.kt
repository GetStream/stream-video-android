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

import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.video.android.core.errors.VideoErrorCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

private const val EVENTS_BUFFER_SIZE = 100
private const val CLOSE_SOCKET_CODE = 1000
internal const val DISPOSE_SOCKET_RECONNECT = 4002
internal const val DISPOSE_SOCKET_REASON = "Connection closed to reconnect"
private const val CLOSE_SOCKET_REASON = "Connection close by client"

internal class StreamWebSocket<V, T : GenericParser<V>>(
    private val tag: String = "",
    private val parser: T,
    socketCreator: (WebSocketListener) -> WebSocket,
) {
    private val logger by taggedLogger("Video:Events$tag")
    private val eventFlow =
        MutableSharedFlow<StreamWebSocketEvent>(extraBufferCapacity = EVENTS_BUFFER_SIZE)

    private val webSocket = socketCreator(object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            val event = parser.decodeOrError(bytes.toByteArray()).onSuccess {
                eventFlow.tryEmit(it)
            }.onError {
                eventFlow.tryEmit(StreamWebSocketEvent.Error(it))
            }
            logger.v { "[onMessage#ByteString] event: `$event`" }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val event = parser.decodeOrError(text.toByteArray()).onSuccess {
                eventFlow.tryEmit(it)
            }.onError {
                eventFlow.tryEmit(StreamWebSocketEvent.Error(it))
            }
            logger.v { "[onMessage#string] event: `$event`" }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            eventFlow.tryEmit(
                StreamWebSocketEvent.Error(
                    Error.NetworkError.fromVideoErrorCode(
                        videoErrorCode = VideoErrorCode.SOCKET_FAILURE,
                        cause = t,
                    ),
                ),
            )
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (code != CLOSE_SOCKET_CODE) {
                // Treat as failure and reconnect, socket shouldn't be closed by server
                eventFlow.tryEmit(
                    StreamWebSocketEvent.Error(
                        Error.NetworkError.fromVideoErrorCode(
                            videoErrorCode = VideoErrorCode.SOCKET_CLOSED,
                        ),
                    ),
                )
            }
        }
    })

    fun send(event: V): Boolean {
        logger.d { "[send] event: `$event`" }
        val parsedEvent = parser.encode(event)
        return webSocket.send(parsedEvent)
    }
    fun close(
        source: String = "N/A",
        code: Int = CLOSE_SOCKET_CODE,
        reason: String = CLOSE_SOCKET_REASON,
    ): Boolean {
        logger.d { "[close], source:$source, code: $code, reason: $reason" }
        return webSocket.close(code, reason)
    }
    fun listen(): Flow<StreamWebSocketEvent> = eventFlow.asSharedFlow()

    fun sendRaw(data: String) {
        logger.d { "[send#raw] event: `$data`" }
        webSocket.send(data)
    }
}
