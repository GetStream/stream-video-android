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

package io.getstream.video.android.core.internal.module

import android.content.Context
import android.net.ConnectivityManager
import io.getstream.video.android.core.coordinator.state.UserState
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.socket.SocketStateService
import io.getstream.video.android.core.socket.VideoSocket
import io.getstream.video.android.core.socket.internal.SocketFactory
import io.getstream.video.android.core.socket.internal.VideoSocketImpl
import io.getstream.video.android.core.user.UserPreferences
import kotlinx.coroutines.CoroutineScope

internal class VideoModule(
    private val appContext: Context,
    private val preferences: UserPreferences,
    private val videoDomain: String
) {
    /**
     * The [CoroutineScope] used for all business logic related operations.
     */
    private val scope = CoroutineScope(io.getstream.video.android.core.dispatchers.DispatcherProvider.IO)

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
            setUser(preferences.getUserCredentials())
        }
    }

    /**
     * @return The WebSocket handler that is used to connect to different calls.
     */
    internal fun socket(): VideoSocket {
        val wssURL = "wss://$videoDomain/video/connect"

        return VideoSocketImpl(
            wssUrl = wssURL,
            preferences = preferences,
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

    internal fun networkStateProvider(): NetworkStateProvider = networkStateProvider

}
