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

package io.getstream.video.android.core.socket.common.token

/**
 * An implementation of [TokenProvider] that keeps previous values of the loaded token.
 * This implementation delegate the process to obtain a new token to another tokenProvider.
 *
 * @property tokenProvider The [TokenProvider] used to obtain new tokens.
 */
internal class CacheableTokenProvider(private val tokenProvider: TokenProvider) : TokenProvider {
    private var cachedToken = ""
    override suspend fun loadToken(): String = tokenProvider.loadToken().also { cachedToken = it }

    /**
     * Obtain the cached token.
     *
     * @return The cached token.
     */
    fun getCachedToken(): String = cachedToken
}
