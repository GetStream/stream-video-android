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

internal interface TokenManager {
    /**
     * Update the current token.
     */
    fun updateToken(token: String)

    /**
     * Ensure a token has been loaded.
     */
    suspend fun ensureTokenLoaded()

    /**
     * Load a new token.
     */
    suspend fun loadSync(): String

    /**
     * Expire the current token.
     */
    fun expireToken()

    /**
     * Check if a [TokenProvider] has been provided.
     *
     * @return true if a token provider has been provided, false on another case.
     */
    fun hasTokenProvider(): Boolean

    /**
     * Inject a new [CacheableTokenProvider]
     *
     * @param provider A [CacheableTokenProvider]
     */
    fun setTokenProvider(provider: CacheableTokenProvider)

    /**
     * Obtain last token loaded.
     *
     * @return the last token loaded. If the token was expired an empty [String] will be returned.
     */
    fun getToken(): String

    /**
     * Check if a token was loaded.
     *
     * @return true if a token was loaded and it is not expired, false on another case.
     */
    fun hasToken(): Boolean
}
