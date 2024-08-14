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
import io.getstream.result.Error
import io.getstream.video.android.core.errors.DisconnectCause
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.lifecycle.StreamLifecycleObserver
import io.getstream.video.android.core.socket.common.SocketFactory
import io.getstream.video.android.core.socket.common.SocketListener
import io.getstream.video.android.core.socket.common.StreamWebSocketEvent
import io.getstream.video.android.core.socket.common.VideoParser
import io.getstream.video.android.core.socket.common.VideoSocket
import io.getstream.video.android.core.socket.common.parser2.MoshiVideoParser
import io.getstream.video.android.core.socket.common.scope.ClientScope
import io.getstream.video.android.core.socket.common.scope.UserScope
import io.getstream.video.android.core.socket.common.token.CacheableTokenProvider
import io.getstream.video.android.core.socket.common.token.TokenManagerImpl
import io.getstream.video.android.core.socket.common.token.TokenProvider
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.User
import io.getstream.video.android.model.User.Companion.isAnonymous
import io.getstream.video.android.model.UserToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.openapitools.client.models.ConnectedEvent
import org.openapitools.client.models.VideoEvent

/**
 * PersistentSocket architecture
 *
 * - Healthmonitor that sends a ping every 30 seconds
 * - Automatically reconnects if it encounters a temp failure
 * - Raises the error if there is a permanent failure
 * - Flow to avoid concurrency related bugs
 * - Ability to wait till the socket is connected (important to prevent race conditions)
 */
public open class PersistentSocket(
    private val apiKey: ApiKey,
    /** The URL to connect to */
    private val url: String,
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
) : SocketListener<VideoEvent>() {
    companion object {
        internal const val DEFAULT_COORDINATOR_SOCKET_TIMEOUT: Long = 10000L
    }

    // Private state
    private val parser: VideoParser = MoshiVideoParser()
    private val tokenManager = TokenManagerImpl()

    // Internal state
    internal open val logger by taggedLogger("Video:Socket")
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
    private val state: MutableStateFlow<SocketState> = MutableStateFlow(SocketState.NotConnected)
    private val internalSocket = VideoSocket(
        apiKey,
        url,
        tokenManager,
        SocketFactory(
            parser,
            httpClient,
        ),
        scope as? UserScope ?: UserScope(ClientScope()),
        StreamLifecycleObserver(scope, lifecycle),
        networkStateProvider,
    ).also {
        it.addListener(this)
    }

    // Init
    init {
        tokenManager.setTokenProvider(CacheableTokenProvider(tokenProvider))
    }

    // Extension opportunity for subclasses
    override fun onCreated() {
        super.onCreated()
        logger.d { "[onCreated] Socket is created" }
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

    override fun onError(error: Error) {
        super.onError(error)
        logger.e { "[onError] Socket error: $error" }
        val emit = errors.tryEmit(StreamWebSocketEvent.Error(error))
        if (!emit) {
            logger.e { "[onError] Failed to emit error: $error" }
        }
    }

    override fun onDisconnected(cause: DisconnectCause) {
        super.onDisconnected(cause)
        logger.d { "[onDisconnected] Socket disconnected. Cause: $cause" }
    }

    // API
    /**
     * Connection ID as [Flow]
     */
    public fun connectionId(): Flow<String> = connectionId.mapNotNull {
        it
    }

    /**
     * Ensure that the token is connected before sending events.
     */
    public fun whenConnected(
        connectionTimeout: Long = DEFAULT_COORDINATOR_SOCKET_TIMEOUT,
        connected: suspend (connectionId: String) -> Unit,
    ) {
        scope.launch {
            internalSocket.awaitConnection(connectionTimeout)
            internalSocket.connectionIdOrError().also {
                connected(it)
            }
        }
    }

    /**
     * State of the socket as [StateFlow]
     */
    public fun state(): StateFlow<SocketState> = state

    /**
     * Socket events as [Flow]
     */
    public fun events(): Flow<VideoEvent> = events

    /**
     * Socket errors as [Flow]
     */
    public fun errors(): Flow<Error> = errors.map { it.streamError }

    /**
     * Send event to the socket.
     */
    public suspend fun sendEvent(event: VideoEvent): Boolean = internalSocket.sendEvent(event)

    public suspend fun connect(user: User) {
        internalSocket.connectUser(user, user.isAnonymous())
    }

    public suspend fun reconnect(user: User, force: Boolean = false) {
        internalSocket.reconnectUser(user, user.isAnonymous(), force)
    }

    /**
     * Disconnect the socket.
     */
    public suspend fun disconnect() = internalSocket.disconnect()

    /**
     * Update the token from the outside.
     */
    public fun updateToken(token: UserToken) {
        tokenManager.updateToken(token)
    }
}
