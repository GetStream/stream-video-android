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
import io.getstream.video.android.errors.VideoErrorCode
import io.getstream.video.android.errors.VideoNetworkError
import io.getstream.video.android.events.ConnectedEvent
import io.getstream.video.android.events.VideoEvent
import io.getstream.video.android.parser.VideoParser
import io.getstream.video.android.utils.Failure
import io.getstream.video.android.utils.Success
import okhttp3.Response
import okhttp3.WebSocket

@Suppress("TooManyFunctions")
internal class EventsParser(
    private val parser: VideoParser,
    private val chatSocket: VideoSocket,
) : okhttp3.WebSocketListener() {

    private var connectionEventReceived = false
    private var closedByClient = true

    override fun onOpen(webSocket: WebSocket, response: Response) {
        connectionEventReceived = false
        closedByClient = false
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onMessage(webSocket: WebSocket, text: String) {
        try {
            val errorMessage = parser.fromJsonOrError(text, SocketErrorMessage::class.java)
            if (errorMessage is Success && errorMessage.data.error != null) {
                handleErrorEvent(errorMessage.data.error)
            } else {
                handleEvent(text)
            }
        } catch (t: Throwable) {
            onSocketError(VideoNetworkError.create(VideoErrorCode.UNABLE_TO_PARSE_SOCKET_EVENT))
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        // no-op
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        if (code == CODE_CLOSE_SOCKET_FROM_CLIENT) {
            closedByClient = true
        } else {
            // Treat as failure and reconnect, socket shouldn't be closed by server
            onFailure(VideoNetworkError.create(VideoErrorCode.SOCKET_CLOSED))
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        // Called when socket is disconnected by client also (client.disconnect())
        onSocketError(VideoNetworkError.create(VideoErrorCode.SOCKET_FAILURE, t))
    }

    private fun onFailure(videoError: VideoError) {
        // Called when socket is disconnected by client also (client.disconnect())
        onSocketError(VideoNetworkError.create(VideoErrorCode.SOCKET_FAILURE, videoError.cause))
    }

    internal fun closeByClient() {
        closedByClient = true
    }

    private fun handleEvent(text: String) {
        val eventResult = parser.fromJsonOrError(text, VideoEvent::class.java)
        if (eventResult is Success) {
            val event = eventResult.data
            if (!connectionEventReceived) {
                if (event is ConnectedEvent) {
                    connectionEventReceived = true
                    onConnectionResolved(event)
                } else {
                    onSocketError(VideoNetworkError.create(VideoErrorCode.CANT_PARSE_CONNECTION_EVENT))
                }
            } else {
                onEvent(event)
            }
        } else if (eventResult is Failure) {
            onSocketError(
                VideoNetworkError.create(
                    VideoErrorCode.CANT_PARSE_EVENT,
                    eventResult.error.cause
                )
            )
        }
    }

    private fun handleErrorEvent(error: ErrorResponse) {
        onSocketError(VideoNetworkError.create(error.code, error.message, error.statusCode))
    }

    private fun onSocketError(error: VideoError) {
        if (!closedByClient) {
            chatSocket.onSocketError(error)
        }
    }

    private fun onConnectionResolved(event: ConnectedEvent) {
        if (!closedByClient) {
            chatSocket.onConnectionResolved(event)
        }
    }

    private fun onEvent(event: VideoEvent) {
        if (!closedByClient) {
            chatSocket.onEvent(event)
        }
    }

    internal companion object {
        internal const val CODE_CLOSE_SOCKET_FROM_CLIENT = 1000
    }
}
