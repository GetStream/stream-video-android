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

package io.getstream.video.android.module

import android.content.Context
import android.net.ConnectivityManager
import io.getstream.video.android.client.user.UserState
import io.getstream.video.android.dispatchers.DispatcherProvider
import io.getstream.video.android.network.NetworkStateProvider
import io.getstream.video.android.socket.SocketFactory
import io.getstream.video.android.socket.SocketStateService
import io.getstream.video.android.socket.VideoSocket
import io.getstream.video.android.socket.VideoSocketImpl
import io.getstream.video.android.token.CredentialsManager
import io.getstream.video.android.token.CredentialsManagerImpl
import io.getstream.video.android.token.CredentialsProvider
import kotlinx.coroutines.CoroutineScope

internal class CallsModule(
    private val appContext: Context,
    private val credentialsProvider: CredentialsProvider
) {
    /**
     * The [CoroutineScope] used for all business logic related operations.
     */
    private val scope = CoroutineScope(DispatcherProvider.IO)

    /**
     * Factory for providing sockets based on the connected user.
     */
    private val socketFactory: SocketFactory by lazy {
        SocketFactory()
    }

    private val socketStateService: SocketStateService by lazy {
        SocketStateService()
    }

    /**
     * Cached user token manager.
     */
    private val credentialsManager: CredentialsManager by lazy {
        CredentialsManagerImpl().apply {
            setCredentialsProvider(credentialsProvider)
        }
    }

    /**
     * Provider that handles connectivity and listens to state changes, exposing them to listeners.
     */
    private val networkStateProvider: NetworkStateProvider by lazy {
        NetworkStateProvider(
            connectivityManager = appContext
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        )
    }

    /**
     * User state that provides the information about the current user.
     */
    private val userState: UserState by lazy {
        UserState().apply {
            setUser(credentialsProvider.getUserCredentials())
        }
    }

    /**
     * @return The WebSocket handler that is used to connect to different calls.
     */
    internal fun socket(): VideoSocket {
        return VideoSocketImpl(
            wssUrl = REDIRECT_WS_BASE_URL ?: WS_BASE_URL,
            credentialsManager = credentialsManager,
            socketFactory = socketFactory,
            networkStateProvider = networkStateProvider,
            userState = userState,
            coroutineScope = scope
        )
    }

    /**
     * @return The [UserState] that serves us information about the currently logged in user.
     */
    internal fun userState(): UserState {
        return userState
    }

    internal fun socketStateService(): SocketStateService = socketStateService

    internal companion object {

        /**
         * Used for testing on devices and redirecting from a public realm to localhost.
         *
         * Will only be used if the value is non-null, so if you're able to test locally, just
         * leave it as-is.
         */
        @Suppress("RedundantNullableReturnType")
        internal val REDIRECT_WS_BASE_URL: String? = null // e.g. "ws://4.tcp.eu.ngrok.io:12265/rpc/stream.video.coordinator.client_v1_rpc.Websocket/Connect"
        internal const val WS_BASE_URL =
            "wss://wss-video-coordinator.oregon-v1.stream-io-video.com/rpc/stream.video.coordinator.client_v1_rpc.Websocket/Connect"
    }
}
