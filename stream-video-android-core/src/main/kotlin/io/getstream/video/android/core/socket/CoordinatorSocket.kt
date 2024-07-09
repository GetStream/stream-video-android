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
import com.squareup.moshi.JsonAdapter
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.socket.common.VideoParser
import io.getstream.video.android.core.socket.common.parser2.MoshiVideoParser
import io.getstream.video.android.core.socket.common.scope.ClientScope
import io.getstream.video.android.core.socket.common.scope.UserScope
import io.getstream.video.android.core.socket.common.token.TokenProvider
import io.getstream.video.android.core.utils.isWhitespaceOnly
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.WebSocket
import org.openapitools.client.infrastructure.Serializer
import org.openapitools.client.models.ConnectUserDetailsRequest
import org.openapitools.client.models.ConnectedEvent
import org.openapitools.client.models.ConnectionErrorEvent
import org.openapitools.client.models.UnsupportedVideoEventException
import org.openapitools.client.models.VideoEvent
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
    internal var token: String,
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
    networkStateProvider = networkStateProvider
) {
    override val logger by taggedLogger("Video:CoordinatorWS")

    override suspend fun authenticate() {
        logger.d { "[authenticateUser] user: $user" }
        if (token.isEmpty()) {
            logger.e { "[authenticateUser] Token is empty. Disconnecting." }
            internalSocket.disconnect()
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
            logger.d { "[authenticateUser] Sending auth request: $authRequest" }
            internalSocket.sendEvent(authRequest)
        }
    }

    /** Invoked when a text (type `0x1`) message has been received. */
    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        if (text.isEmpty() || text == "null") {
            logger.w { "[onMessage] Received empty socket message" }
            return
        }

        scope.launch(singleThreadDispatcher) {
            try {
                Serializer.moshi.adapter(VideoEvent::class.java).let { eventAdapter ->
                    eventAdapter.fromJson(text)?.let { parsedEvent -> processEvent(parsedEvent) }
                }
            } catch (e: Throwable) {
                if (e.cause is UnsupportedVideoEventException) {
                    val ex = e.cause as UnsupportedVideoEventException
                    logger.w { "[onMessage] Received unsupported VideoEvent type: ${ex.type}. Ignoring." }
                } else {
                    logger.w { "[onMessage] VideoEvent parsing error ${e.message}." }
                    handleError(e)
                }
            }
        }
    }

    private suspend fun processEvent(parsedEvent: VideoEvent) {
        if (parsedEvent is ConnectionErrorEvent) {
            handleError(
                ErrorResponse(
                    code = parsedEvent.error?.code ?: -1,
                    message = parsedEvent.error?.message ?: "",
                    statusCode = parsedEvent.error?.statusCode ?: -1,
                    exceptionFields = parsedEvent.error?.exceptionFields ?: emptyMap(),
                    moreInfo = parsedEvent.error?.moreInfo ?: "",
                ),
            )
            return
        }

        if (parsedEvent is ConnectedEvent) {
            _connectionId.value = parsedEvent.connectionId
            setConnectedStateAndContinue(parsedEvent)
        }

        ackHealthMonitor()
        events.emit(parsedEvent)
    }
}
