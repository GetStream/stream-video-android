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

import com.squareup.moshi.JsonAdapter
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.WebSocket
import org.openapitools.client.infrastructure.Serializer
import org.openapitools.client.models.ConnectUserDetailsRequest
import org.openapitools.client.models.ConnectedEvent
import org.openapitools.client.models.UnknownVideoEvent
import org.openapitools.client.models.VideoEvent
import org.openapitools.client.models.WSAuthMessageRequest

/**
 * The Coordinator sockets send a user authentication request
 *  @see WSAuthMessageRequest
 *
 */
public class CoordinatorSocket(
    private val url: String,
    private val user: User,
    internal var token: String,
    private val scope: CoroutineScope = CoroutineScope(DispatcherProvider.IO),
    private val httpClient: OkHttpClient,
    private val networkStateProvider: NetworkStateProvider,
) : PersistentSocket<ConnectedEvent>(
    url = url,
    httpClient = httpClient,
    scope = scope,
    networkStateProvider = networkStateProvider,
    onFastReconnected = { },
) {
    override val logger by taggedLogger("PersistentCoordinatorSocket")

    override fun authenticate() {
        logger.d { "[authenticateUser] user: $user" }

        if (token.isEmpty()) {
            throw IllegalStateException("User token is empty")
        }

        val adapter: JsonAdapter<WSAuthMessageRequest> =
            Serializer.moshi.adapter(WSAuthMessageRequest::class.java)

        val authRequest = WSAuthMessageRequest(
            token = token,
            userDetails = ConnectUserDetailsRequest(
                id = user.id,
                name = user.name.takeUnless { it.isBlank() },
                image = user.image.takeUnless { it.isBlank() },
                custom = user.custom.takeUnless { it.isEmpty() },
            ),
        )
        val message = adapter.toJson(authRequest)

        super.socket?.send(message)
    }

    /** Invoked when a text (type `0x1`) message has been received. */
    override fun onMessage(webSocket: WebSocket, text: String) {
        logger.d { "[onMessage] text: $text " }

        if (text.isEmpty()) {
            logger.w { "[onMessage] Received empty socket message" }
            return
        }

        scope.launch(singleThreadDispatcher) {
            // parse the message
            val jsonAdapter: JsonAdapter<VideoEvent> = Serializer.moshi.adapter(
                VideoEvent::class.java,
            )

            val parsedEvent = try {
                jsonAdapter.fromJson(text)
            } catch (e: Throwable) {
                logger.w { "[onMessage] VideoEvent parsing error ${e.message}" }
                null
            }

            when (parsedEvent) {
                is UnknownVideoEvent ->
                    logger.w { "[onMessage] Received unknown VideoEvent type: ${parsedEvent.expectedType}" }

                null -> tryParseApiError(text)

                else -> processEvent(parsedEvent)
            }
        }
    }

    private fun tryParseApiError(text: String) {
        try {
            val json = Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            }

            val parsedError = json.decodeFromString<SocketError>(text)
            parsedError.let {
                logger.w { "[onMessage] SocketError: ${parsedError.error}" }
                handleError(it.error)
            }
        } catch (e: Throwable) {
            logger.w { "[onMessage] Error when trying to parse SocketError: ${e.message}" }
            handleError(e)
        }
    }
    private suspend fun processEvent(parsedEvent: VideoEvent) {
        if (parsedEvent is ConnectedEvent) {
            _connectionId.value = parsedEvent.connectionId
            setConnectedStateAndContinue(parsedEvent)
        }

        ackHealthMonitor()
        events.emit(parsedEvent)
    }
}
