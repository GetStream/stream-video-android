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

package io.getstream.video.android.core.token.internal

import io.getstream.video.android.core.token.CredentialsProvider

internal interface CredentialsManager {
    /**
     * Ensure a token has been loaded.
     */
    fun ensureTokenLoaded()

    /**
     * Load a new token.
     */
    fun loadTokenSync(): String

    /**
     * Expire the current token.
     */
    fun expireToken()

    /**
     * Check if a [CredentialsProvider] has been provided.
     *
     * @return true if a token provider has been provided, false on another case.
     */
    fun hasTokenProvider(): Boolean

    /**
     * Inject a new [CredentialsProvider]
     *
     * @param provider A [CredentialsProvider] which serves the token based on custom implementation.
     */
    fun setCredentialsProvider(provider: CredentialsProvider)

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

    /**
     * @return The API key for this environment.
     */
    fun getApiKey(): String
}
