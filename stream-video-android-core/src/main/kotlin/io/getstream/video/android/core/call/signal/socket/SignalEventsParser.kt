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

import io.getstream.log.taggedLogger
import io.getstream.video.android.core.errors.VideoError
import io.getstream.video.android.core.errors.VideoErrorCode
import io.getstream.video.android.core.errors.VideoNetworkError
import io.getstream.video.android.core.events.ConnectedEvent
import okhttp3.Response
import okhttp3.WebSocket
import okio.ByteString
import stream.video.sfu.event.SfuEvent

internal class SignalEventsParser(
    private val sfuSocket: SfuSocket,
) : okhttp3.WebSocketListener() {

    private val logger by taggedLogger("Call:SFU-WS")

    private var connectionEventReceived = false
    private var closedByClient = true

    override fun onOpen(webSocket: WebSocket, response: Response) {
        logger.i { "[onOpen] response: $response" }
        connectionEventReceived = false
        closedByClient = false

        sfuSocket.onConnectionResolved(ConnectedEvent(""))
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        super.onMessage(webSocket, bytes)
        val byteBuffer = bytes.asByteBuffer()
        val byteArray = ByteArray(byteBuffer.capacity())
        byteBuffer.get(byteArray)

        try {
            val rawEvent = SfuEvent.ADAPTER.decode(byteArray)
            logger.v { "[onMessage] rawEvent: $rawEvent" }
            val message = RTCEventMapper.mapEvent(rawEvent)
            sfuSocket.onEvent(message)
        } catch (error: Throwable) {
            logger.e { "[onMessage] failed: $error" }
            error.printStackTrace()
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        logger.i { "[onClosing] code: $code, reason: $reason" }
        // no-op
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        logger.i { "[onClosed] code: $code, reason: $reason" }
        if (code == CODE_CLOSE_SOCKET_FROM_CLIENT) {
            closedByClient = true
        } else {
            // Treat as failure and reconnect, socket shouldn't be closed by server
            onFailure(VideoNetworkError.create(VideoErrorCode.SOCKET_CLOSED))
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        logger.i { "[onFailure] failure: $t, response: $response" }
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

    private fun onSocketError(error: VideoError) {
        if (!closedByClient) {
            sfuSocket.onSocketError(error)
        }
    }

    internal companion object {
        internal const val CODE_CLOSE_SOCKET_FROM_CLIENT = 1000
    }
}
