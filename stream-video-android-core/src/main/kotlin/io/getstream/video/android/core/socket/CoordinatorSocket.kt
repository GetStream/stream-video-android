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
import com.squareup.moshi.JsonDataException
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.utils.isWhitespaceOnly
import io.getstream.video.android.core.utils.safeCall
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
                name = user.name.takeUnless { it.isWhitespaceOnly() },
                image = user.image.takeUnless { it.isWhitespaceOnly() },
                custom = user.custom,
            ),
        )
        val message = adapter.toJson(authRequest)

        super.socket?.send(message)
    }

    /** Invoked when a text (type `0x1`) message has been received. */
    override fun onMessage(webSocket: WebSocket, text: String) {
        logger.d { "[onMessage] text: $text " }

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
                    val eventType = extractEventType(text)
                    val errorMessage = "Error when parsing VideoEvent with type: $eventType. Cause: ${e.message}."

                    logger.e { "[onMessage] $errorMessage" }
                    handleError(JsonDataException(errorMessage))
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

    private fun extractEventType(json: String): String = safeCall("Unknown") {
        val regex = """"type":"(.*?)"""".toRegex()
        val matchResult = regex.find(json)
        return matchResult?.groups?.get(1)?.value ?: "Unknown"
    }
}
