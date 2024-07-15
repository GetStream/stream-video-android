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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import okhttp3.OkHttpClient
import org.openapitools.client.models.ConnectedEvent
import org.openapitools.client.models.VideoEvent
import java.util.concurrent.Flow

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
) : SocketListener() {
    // Private state
    private val parser: VideoParser = MoshiVideoParser()
    private val tokenManager = TokenManagerImpl()

    // Internal state
    internal open val logger by taggedLogger("Video:Socket")
    internal val errors: Flow<StreamWebSocketEvent.Error>
    internal val events: Flow<VideoEvent> = MutableSharedFlow<VideoEvent>()
    internal val internalSocket = VideoSocket(
        apiKey,
        url,
        tokenManager,
        SocketFactory(
            parser,
            tokenManager,
            httpClient,
        ),
        scope as? UserScope ?: UserScope(ClientScope()),
        StreamLifecycleObserver(scope, lifecycle),
        networkStateProvider,
    )

    // Init
    init {
        tokenManager.setTokenProvider(CacheableTokenProvider(tokenProvider))
        internalSocket.addListener(this)
    }

    // Events
    override fun onConnecting() {
        super.onConnecting()
        logger.d { "[onConnecting] Socket is connecting" }
    }

    override fun onConnected(event: ConnectedEvent) {
        super.onConnected(event)
        logger.d { "[onConnected] Socket connected with event: $event" }
    }

    override fun onEvent(event: VideoEvent) {
        super.onEvent(event)
        logger.d { "[onEvent] Received event: $event" }
    }

    override fun onError(error: Error) {
        super.onError(error)
        logger.e { "[onError] Socket error: $error" }
    }

    override fun onDisconnected(cause: DisconnectCause) {
        super.onDisconnected(cause)
        logger.d { "[onDisconnected] Socket disconnected. Cause: $cause" }
    }

    /**
     * Check if the socket is in a state that can connect.
     */
    internal fun canConnect(): Boolean = safeCall(false) {
        val connectionState = connectionState.value
        logger.d { "[canConnect] Current state: $connectionState" }
        val result = when (connectionState) {
            // Can't connect if we are already connected.
            is SocketState.Connected -> false
            is SocketState.Connecting -> false
            // We can connect if we are disconnected.
            is SocketState.DisconnectedPermanently -> true
            is SocketState.DisconnectedTemporarily -> true
            is SocketState.NetworkDisconnected -> true
            is SocketState.DisconnectedByRequest -> true
            is SocketState.NotConnected -> true
        }
        logger.d { "[canConnect] Decision: $result" }
        result
    }

    /**
     * Check if the socket is in a state that can reconnect.
     */
    internal fun canReconnect(): Boolean = safeCall(false) {
        val connectionState = connectionState.value
        logger.d { "[canReconnect] Current state: $connectionState" }
        val result = when (connectionState) {
            // Can't reconnect if we are already connected.
            is SocketState.Connected -> false
            is SocketState.Connecting -> false
            is SocketState.DisconnectedPermanently -> false
            is SocketState.DisconnectedTemporarily -> true
            is SocketState.NetworkDisconnected -> false
            is SocketState.DisconnectedByRequest -> false
            is SocketState.NotConnected -> false
        }
        logger.d { "[canReconnect] Decision: $result" }
        result
    }

    /**
     * Check if the socket is in a state that can disconnect.
     */
    internal fun canDisconnect(): Boolean = safeCall(true) {
        val connectionState = connectionState.value
        logger.d { "[canDisconnect] Current state: $connectionState" }
        val result = when (connectionState) {
            is SocketState.Connected -> true
            is SocketState.Connecting -> true
            is SocketState.DisconnectedPermanently -> false
            is SocketState.DisconnectedByRequest -> false
            is SocketState.DisconnectedTemporarily -> true
            is SocketState.NetworkDisconnected -> false
            else -> true
        }
        logger.d { "[canDisconnect] Decision: $result" }
        result
    }
}
