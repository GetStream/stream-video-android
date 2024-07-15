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

package io.getstream.video.android.core.socket

import androidx.lifecycle.Lifecycle
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.socket.common.scope.ClientScope
import io.getstream.video.android.core.socket.common.scope.UserScope
import io.getstream.video.android.core.socket.common.token.TokenProvider
import io.getstream.video.android.core.utils.isWhitespaceOnly
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.openapitools.client.models.ConnectUserDetailsRequest
import org.openapitools.client.models.ConnectedEvent
import org.openapitools.client.models.WSAuthMessageRequest

/**
 * The Coordinator sockets send a user authentication request
 *  @see WSAuthMessageRequest
 *
 */
public class CoordinatorSocket(
    private val apiKey: ApiKey,
    private val url: String,
    private val user: User,
    private val token: String,
    private val scope: CoroutineScope = UserScope(ClientScope()),
    private val httpClient: OkHttpClient,
    private val lifecycle: Lifecycle,
    private val tokenProvider: TokenProvider,
    private val networkStateProvider: NetworkStateProvider,
) : PersistentSocket(
    apiKey = apiKey,
    url = url,
    httpClient = httpClient,
    scope = scope,
    lifecycle = lifecycle,
    tokenProvider = tokenProvider,
    networkStateProvider = networkStateProvider,
) {

    override val logger by taggedLogger("Video:CoordinatorWS")

    override fun onCreated() {
        scope.launch {
            logger.d { "[onConnected] Video socket connected, user: $user" }
            if (token.isEmpty()) {
                logger.e { "[onConnected] Token is empty. Disconnecting." }
                disconnect()
            } else {
                val authRequest = WSAuthMessageRequest(
                    token = token,
                    userDetails = ConnectUserDetailsRequest(
                        id = user.id,
                        name = user.name.takeUnless { it.isWhitespaceOnly() },
                        image = user.image.takeUnless { it.isWhitespaceOnly() },
                        custom = user.custom,
                    ),
                )
                logger.d { "[onConnected] Sending auth request: $authRequest" }
                sendEvent(authRequest)
            }
        }
    }
}
