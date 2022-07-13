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
import io.getstream.video.android.token.TokenManager
import okhttp3.OkHttpClient
import okhttp3.Request
import stream.video.User
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal class SocketFactory(
    private val parser: VideoParser,
    private val tokenManager: TokenManager,
    private val httpClient: OkHttpClient = OkHttpClient(),
) {

    @Throws(UnsupportedEncodingException::class)
    fun createSocket(eventsParser: EventsParser, connectionConf: ConnectionConf): Socket {
        val url = buildUrl(connectionConf)
        val request = Request.Builder().url(url).build()
        val newWebSocket = httpClient.newWebSocket(request, eventsParser)

        return Socket(newWebSocket, parser)
    }

    @Throws(UnsupportedEncodingException::class)
    private fun buildUrl(connectionConf: ConnectionConf): String {
        var json = buildUserDetailJson(connectionConf)
        return try {
            json = URLEncoder.encode(json, StandardCharsets.UTF_8.name())
            val baseWsUrl =
                "${connectionConf.endpoint}connect?json=$json&api_key=${connectionConf.apiKey}"
            when (connectionConf) {
                is ConnectionConf.UserConnectionConf -> {
                    val token = tokenManager.getToken()
                    "$baseWsUrl&authorization=$token&stream-auth-type=jwt"
                }
            }
        } catch (_: Throwable) {
            throw UnsupportedEncodingException("Unable to encode user details json: $json")
        }
    }

    private fun buildUserDetailJson(connectionConf: ConnectionConf): String {
        val data = mapOf(
            "user_details" to connectionConf.reduceUserDetails(),
            "user_id" to connectionConf.user.id,
            "server_determines_connection_id" to true,
            // TODO - tracking headers
        )
        return parser.toJson(data)
    }

    /**
     * Converts the [User] object to a map of properties updated while connecting the user.
     *
     * @return A map of User's properties to update.
     */
    private fun ConnectionConf.reduceUserDetails(): Map<String, Any> =
        mutableMapOf<String, Any>("id" to user.id)
            .apply {
                if (!isReconnection) {
                    put("teams", user.teams)

                    // TODO - how do we define custom data?
//                if (user.image.isNotBlank()) put("image", user.image)
//                if (user.name.isNotBlank()) put("name", user.name)
//                putAll(user.extraData)
                }
            }

    internal sealed class ConnectionConf {
        var isReconnection: Boolean = false
            private set
        abstract val endpoint: String
        abstract val apiKey: String
        abstract val user: User

        data class UserConnectionConf(
            override val endpoint: String,
            override val apiKey: String,
            override val user: User,
        ) : ConnectionConf()

        internal fun asReconnectionConf(): ConnectionConf = this.also { isReconnection = true }
    }
}
