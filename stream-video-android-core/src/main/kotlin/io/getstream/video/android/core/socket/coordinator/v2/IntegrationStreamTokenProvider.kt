/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.socket.coordinator.v2

import io.getstream.android.core.api.authentication.StreamTokenProvider
import io.getstream.android.core.api.model.value.StreamToken
import io.getstream.android.core.api.model.value.StreamUserId
import io.getstream.video.android.core.socket.common.token.TokenProvider

/**
 * Adapts the integration-supplied video [TokenProvider] to core's [StreamTokenProvider].
 *
 * Selected at builder time for authenticated users; guest users are served by
 * `GuestStreamTokenProvider` instead. See D-05 in the phase context.
 */
internal class IntegrationStreamTokenProvider(
    private val delegate: TokenProvider,
) : StreamTokenProvider {

    /**
     * Loads a token from the wrapped integration [TokenProvider].
     *
     * The [userId] parameter is intentionally unused — the integration's
     * `TokenProvider.loadToken()` already binds the user identity through the SDK's builder
     * configuration (D-05). Core passes the identifier for parity with alternate providers
     * that mint per-user tokens.
     */
    override suspend fun loadToken(userId: StreamUserId): StreamToken =
        StreamToken.fromString(delegate.loadToken())
}
