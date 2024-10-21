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

package io.getstream.video.android.core.socket.sfu

import androidx.lifecycle.Lifecycle
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.errors.DisconnectCause
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.events.SfuDataEvent
import io.getstream.video.android.core.events.SfuDataRequest
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.lifecycle.StreamLifecycleObserver
import io.getstream.video.android.core.socket.common.SfuParser
import io.getstream.video.android.core.socket.common.SocketActions
import io.getstream.video.android.core.socket.common.SocketFactory
import io.getstream.video.android.core.socket.common.SocketListener
import io.getstream.video.android.core.socket.common.StreamWebSocketEvent
import io.getstream.video.android.core.socket.common.scope.ClientScope
import io.getstream.video.android.core.socket.common.scope.UserScope
import io.getstream.video.android.core.socket.common.token.CacheableTokenProvider
import io.getstream.video.android.core.socket.common.token.TokenManager
import io.getstream.video.android.core.socket.common.token.TokenManagerImpl
import io.getstream.video.android.core.socket.common.token.TokenProvider
import io.getstream.video.android.core.socket.sfu.state.SfuSocketState
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.SfuToken
import io.getstream.video.android.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.openapitools.client.models.ConnectedEvent
import org.openapitools.client.models.VideoEvent
import stream.video.sfu.event.JoinRequest
import stream.video.sfu.event.SfuRequest

class SfuSocketConnection(
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
) : SocketListener<SfuDataEvent, JoinCallResponseEvent>(),
    SocketActions<SfuDataRequest, SfuDataEvent, StreamWebSocketEvent.Error, SfuSocketState, SfuToken> {

    companion object {
        internal const val DEFAULT_SFU_SOCKET_TIMEOUT: Long = 10000L
    }

    private val logger by taggedLogger("Video:SfuSocket")
    private val tokenManager = TokenManagerImpl()
    private val internalSocket: SfuSocket = SfuSocket(
        wssUrl = url,
        apiKey = apiKey,
        tokenManager = tokenManager,
        socketFactory = SocketFactory(
            parser = object : SfuParser {},
            httpClient = httpClient,
        ),
        lifecycleObserver = StreamLifecycleObserver(scope, lifecycle),
        networkStateProvider = networkStateProvider,
        userScope = scope as? UserScope ?: UserScope(ClientScope()),
    ).also {
        it.addListener(this)
    }
    private val state: StateFlow<SfuSocketState> = internalSocket.state()
    private val events: MutableSharedFlow<SfuDataEvent> = MutableSharedFlow(
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        replay = 1,
        extraBufferCapacity = 100,
    )
    private val errors: MutableSharedFlow<StreamWebSocketEvent.Error> = MutableSharedFlow(
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        replay = 1,
        extraBufferCapacity = 100,
    )
    private val connectionId: MutableStateFlow<String?> = MutableStateFlow(null)

    // Initialization
    init {
        tokenManager.setTokenProvider(CacheableTokenProvider(tokenProvider))
    }

    override fun onCreated() {
        super.onCreated()
        logger.d { "[onCreated] Socket is created" }
    }

    override fun onConnecting() {
        super.onConnecting()
        logger.d { "[onConnecting] Socket is connecting" }
    }

    override fun onConnected(event: JoinCallResponseEvent) {
        super.onConnected(event)
        logger.d { "[onConnected] Socket connected with event: $event" }
    }

    override fun onEvent(event: SfuDataEvent) {
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

    override fun whenConnected(
        connectionTimeout: Long,
        connected: suspend (connectionId: String) -> Unit,
    ) {
        scope.launch {
            internalSocket.awaitConnection(connectionTimeout)
            internalSocket.connectionIdOrError().also {
                connected(it)
            }
        }
    }

    override suspend fun connect(connectData: Any) {
        val join = connectData as JoinRequest
        logger.d { "[connect] request: $join" }
        internalSocket.connect(join)
    }

    override suspend fun disconnect() {
        internalSocket.disconnect()
    }

    override fun updateToken(token: SfuToken) {
        throw UnsupportedOperationException("Update token is not supported for SFU. Create a new socket instead.")
    }

    override suspend fun reconnect(user: User, force: Boolean) {
        throw UnsupportedOperationException("Reconnect user is not supported for SFU, it has different reconnect strategy")
    }

    override fun state(): StateFlow<SfuSocketState> = state

    override fun events(): MutableSharedFlow<SfuDataEvent> = events

    override fun errors(): MutableSharedFlow<StreamWebSocketEvent.Error> = errors

    override fun sendData(data: String) = internalSocket.sendRawData(data)

    override suspend fun sendEvent(event: SfuDataRequest): Boolean = internalSocket.sendEvent(event)

    override fun connectionId(): Flow<String> = connectionId.mapNotNull {
        it
    }
}
