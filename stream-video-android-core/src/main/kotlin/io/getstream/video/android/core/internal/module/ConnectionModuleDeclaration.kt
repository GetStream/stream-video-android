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

package io.getstream.video.android.core.internal.module

import androidx.lifecycle.Lifecycle
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.socket.common.scope.ClientScope
import io.getstream.video.android.core.socket.common.scope.UserScope
import io.getstream.video.android.core.trace.Tracer
import io.getstream.video.android.model.ApiKey
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient

/**
 * Api definition for any connection module.
 */
internal interface ConnectionModuleDeclaration<Api, SocketConnection, Http : OkHttpClient, Token> {

    /**
     * The tracer for this connection module.
     */
    val tracer: Tracer

    /**
     * The application  key.
     */
    val apiKey: ApiKey

    /**
     * The URL of the API.
     */
    val apiUrl: String

    /**
     * The URL of the web-socket
     */
    val wssUrl: String

    /**
     * The API that is accessed.
     */
    val api: Api

    /**
     * The HTTP client.
     */
    val http: Http

    /**
     * The coroutine scope.
     */
    val scope: CoroutineScope get() = UserScope(ClientScope())

    /**
     * Connection timeout.
     */
    val connectionTimeoutInMs: Long

    /**
     * Logging levels.
     */
    val loggingLevel: LoggingLevel get() = LoggingLevel()

    /**
     * The user token.
     */
    val userToken: Token

    /**
     * The lifecycle of the application.
     */
    val lifecycle: Lifecycle

    /**
     * The network state provider.
     */
    val networkStateProvider: NetworkStateProvider

    /**
     * The actual socket connection.
     */
    val socketConnection: SocketConnection

    /**
     * API to update the token.
     */
    fun updateToken(token: Token)

    /**
     * API to update the authentication type.
     */
    fun updateAuthType(authType: String)
}
