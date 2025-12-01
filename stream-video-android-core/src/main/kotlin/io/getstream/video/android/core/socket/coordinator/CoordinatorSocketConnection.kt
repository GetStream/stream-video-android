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

package io.getstream.video.android.core.socket.coordinator

import androidx.lifecycle.Lifecycle
import com.squareup.moshi.JsonAdapter
import io.getstream.android.video.generated.infrastructure.Serializer
import io.getstream.android.video.generated.models.ConnectUserDetailsRequest
import io.getstream.android.video.generated.models.ConnectedEvent
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.android.video.generated.models.WSAuthMessageRequest
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.errors.DisconnectCause
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.lifecycle.StreamLifecycleObserver
import io.getstream.video.android.core.socket.common.SocketActions
import io.getstream.video.android.core.socket.common.SocketFactory
import io.getstream.video.android.core.socket.common.SocketListener
import io.getstream.video.android.core.socket.common.StreamWebSocketEvent
import io.getstream.video.android.core.socket.common.VideoParser
import io.getstream.video.android.core.socket.common.parser2.MoshiVideoParser
import io.getstream.video.android.core.socket.common.scope.ClientScope
import io.getstream.video.android.core.socket.common.scope.UserScope
import io.getstream.video.android.core.socket.common.token.CacheableTokenProvider
import io.getstream.video.android.core.socket.common.token.TokenManagerImpl
import io.getstream.video.android.core.socket.common.token.TokenProvider
import io.getstream.video.android.core.socket.common.token.TokenRepository
import io.getstream.video.android.core.socket.coordinator.state.VideoSocketState
import io.getstream.video.android.core.utils.isWhitespaceOnly
import io.getstream.video.android.core.utils.mapState
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.User
import io.getstream.video.android.model.User.Companion.isAnonymous
import io.getstream.video.android.model.UserToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

/**
 * PersistentSocket architecture
 *
 * - Health monitor that sends a ping every 30 seconds
 * - Automatically reconnects if it encounters a temp failure
 * - Raises the error if there is a permanent failure
 * - Flow to avoid concurrency related bugs
 * - Ability to wait till the socket is connected (important to prevent race conditions)
 */
public open class CoordinatorSocketConnection(
    private val apiKey: ApiKey,
    /** The URL to connect to */
    private val url: String,
    /** The  user to connect. */
    private val user: User,
    /** The initial token. */
    private val token: String,
    /** Inject your http client */
    private val httpClient: OkHttpClient,
    /** Inject your network state provider */
    private val networkStateProvider: NetworkStateProvider,
    /** Set the scope everything should run in */
    private val scope: CoroutineScope = UserScope(ClientScope()),
    /** Lifecycle */
    private val lifecycle: Lifecycle,
    /** Token provider */
    private val tokenProvider: TokenProvider,
    private val tokenRepository: TokenRepository,
) : SocketListener<VideoEvent, ConnectedEvent>(),
    SocketActions<VideoEvent, VideoEvent, StreamWebSocketEvent.Error, VideoSocketState, UserToken, User> {

    // Private state
    private val parser: VideoParser = MoshiVideoParser()
    private val tokenManager = TokenManagerImpl(tokenRepository)

    // Internal state
    private val logger by taggedLogger("Video:Socket")
    private val errors: MutableSharedFlow<StreamWebSocketEvent.Error> = MutableSharedFlow(
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        replay = 1,
        extraBufferCapacity = 100,
    )
    private val events: MutableSharedFlow<VideoEvent> = MutableSharedFlow(
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        replay = 1,
        extraBufferCapacity = 100,
    )
    private val connectionId: MutableStateFlow<String?> = MutableStateFlow(null)
    private val internalSocket = CoordinatorSocket(
        apiKey,
        url,
        tokenManager,
        SocketFactory(
            parser,
            httpClient,
        ),
        scope as? UserScope ?: UserScope(ClientScope(), Dispatchers.IO.limitedParallelism(1)),
        StreamLifecycleObserver(scope, lifecycle),
        networkStateProvider,
    ).also {
        it.addListener(this)
    }

    private val state: StateFlow<VideoSocketState> = internalSocket.state()

    // Init
    init {
        tokenManager.setTokenProvider(CacheableTokenProvider(tokenProvider, tokenRepository))
    }

    // Extension opportunity for subclasses
    override fun onCreated() {
        super.onCreated()
        logger.d {
            "[onCreated] Socket is created, initial token: $token, tokenManager.getToken() = ${tokenManager.getToken()}"
        }
        scope.launch {
            logger.d { "[onConnected] Video socket created, user: $user" }
            if (token.isEmpty()) {
                logger.e { "[onConnected] Token is empty. Disconnecting." }
                disconnect()
            } else {
                val authRequest = WSAuthMessageRequest(
                    token = tokenManager.getToken().ifEmpty { token },
                    userDetails = ConnectUserDetailsRequest(
                        id = user.id,
                        name = user.name.takeUnless { it.isWhitespaceOnly() },
                        image = user.image.takeUnless { it.isWhitespaceOnly() },
                        custom = user.custom,
                    ),
                )

                val adapter: JsonAdapter<WSAuthMessageRequest> =
                    Serializer.moshi.adapter(WSAuthMessageRequest::class.java)
                val data = adapter.toJson(authRequest)
                logger.d { "[onConnected] Sending auth request: $authRequest" }
                logger.d { "[onConnected#data] Data: $data" }
                sendData(data)
            }
        }
    }

    override fun onConnecting() {
        super.onConnecting()
        logger.d { "[onConnecting] Socket is connecting" }
    }

    override fun onConnected(event: ConnectedEvent) {
        super.onConnected(event)
        whenConnected {
            connectionId.value = it
        }
        logger.d { "[onConnected] Socket connected with event: $event" }
    }

    override fun onEvent(event: VideoEvent) {
        super.onEvent(event)
        logger.d { "[onEvent] Received event: $event" }
        val emit = events.tryEmit(event)
        if (!emit) {
            logger.e { "[onEvent] Failed to emit event: $event" }
        }
    }

    override fun onError(error: StreamWebSocketEvent.Error) {
        super.onError(error)
        logger.e { "[onError] Socket error: $error" }
        val emit = errors.tryEmit(error)
        if (!emit) {
            logger.e { "[onError] Failed to emit error: $error" }
        }
    }

    override fun onDisconnected(cause: DisconnectCause) {
        super.onDisconnected(cause)
        logger.d { "[onDisconnected] Socket disconnected. Cause: $cause" }
    }

    // API
    override fun connectionId(): StateFlow<String?> = connectionId.mapState {
        it
    }
    override fun whenConnected(
        connectionTimeout: Long,
        connected: suspend (connectionId: String) -> Unit,
    ) {
        logger.d { "[whenConnected]" }
        scope.launch {
            internalSocket.awaitConnection(connectionTimeout)
            internalSocket.connectionIdOrError().also {
                connected(it)
            }
        }
    }
    override fun state(): StateFlow<VideoSocketState> = state

    override fun events(): Flow<VideoEvent> = events

    override fun errors(): Flow<StreamWebSocketEvent.Error> = errors

    override fun sendData(data: String) = internalSocket.senRawData(data)

    override suspend fun sendEvent(event: VideoEvent): Boolean = internalSocket.sendEvent(event)

    override suspend fun connect(connectData: User) {
        logger.d { "[connect]" }
        internalSocket.connectUser(connectData, connectData.isAnonymous())
    }

    override suspend fun reconnect(data: User, force: Boolean) {
        logger.d { "[reconnect]" }
        internalSocket.reconnectUser(data, data.isAnonymous(), force)
    }

    override suspend fun disconnect() = internalSocket.disconnect()

    override fun updateToken(token: UserToken) {
        logger.d { "[updateToken]" }
        tokenManager.updateToken(token)
    }
}
