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
import io.getstream.video.android.core.StreamVideo.Companion.buildSdkTrackingHeaders
import io.getstream.video.android.model.User
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.UnsupportedEncodingException

internal class SocketFactory(
    private val parser: VideoParser,
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val logger by taggedLogger("Video:SocketFactory")

    @Throws(UnsupportedEncodingException::class)
    fun createSocket(connectionConf: ConnectionConf): StreamWebSocket {
        val request = buildRequest(connectionConf)
        logger.i { "[createSocket] new web socket: ${request.url}" }
        return StreamWebSocket(parser) { httpClient.newWebSocket(request, it) }
    }

    @Throws(UnsupportedEncodingException::class)
    private fun buildRequest(connectionConf: ConnectionConf): Request =
        Request.Builder()
            .url(connectionConf.endpoint)
            .addHeader("Connection", "Upgrade")
            .addHeader("Upgrade", "websocket")
            .addHeader("X-Stream-Client", buildSdkTrackingHeaders())
            .build()

    internal sealed class ConnectionConf {
        var isReconnection: Boolean = false
            private set
        abstract val endpoint: String
        abstract val apiKey: String
        abstract val user: User

        data class AnonymousConnectionConf(
            override val endpoint: String,
            override val apiKey: String,
            override val user: User,
        ) : ConnectionConf()

        data class UserConnectionConf(
            override val endpoint: String,
            override val apiKey: String,
            override val user: User,
        ) : ConnectionConf()

        internal fun asReconnectionConf(): ConnectionConf = this.also { isReconnection = true }

        internal val id: String
            get() = when (this) {
                is AnonymousConnectionConf -> "!anon"
                is UserConnectionConf -> user.id
            }
    }
}
