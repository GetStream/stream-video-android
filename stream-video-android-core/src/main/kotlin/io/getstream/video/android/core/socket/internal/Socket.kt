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

package io.getstream.video.android.core.socket.internal

import com.squareup.moshi.JsonAdapter
import com.squareup.wire.Message
import okhttp3.WebSocket
import org.openapitools.client.infrastructure.Serializer
import org.openapitools.client.models.VideoWSAuthMessageRequest
import stream.video.coordinator.client_v1_rpc.WebsocketHealthcheck
import stream.video.sfu.event.HealthCheckRequest
import stream.video.sfu.event.JoinRequest
import stream.video.sfu.event.SfuRequest

/**
 * Wrapper around the [WebSocket] that lets you send different types of events.
 *
 * @property socket The real WS that we send data to and receive data from.
 */
internal class Socket(private val socket: WebSocket) {

    /**
     * Sends the [authRequest] as a message to the socket, attempting to authenticate the
     * currently logged in user.
     */
    fun authenticate(authRequest: VideoWSAuthMessageRequest) {
        val adapter: JsonAdapter<VideoWSAuthMessageRequest> =
            Serializer.moshi.adapter(VideoWSAuthMessageRequest::class.java)

        val message = adapter.toJson(authRequest)

        socket.send(message)
    }

    fun joinCall(joinRequest: JoinRequest) {
        socket.send(SfuRequest(join_request = joinRequest).encodeByteString())
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
    fun ping(state: WebsocketHealthcheck) {
        socket.send(state.encodeByteString())
    }

    fun pingCall(healthCheckRequest: HealthCheckRequest) {
        socket.send(SfuRequest(health_check_request = healthCheckRequest).encodeByteString())
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
