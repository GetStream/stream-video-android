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

package io.getstream.video.android.core.token

import io.getstream.video.android.core.socket.common.token.CacheableTokenProvider
import io.getstream.video.android.core.socket.common.token.PersistingTokenProvider
import io.getstream.video.android.core.socket.common.token.TokenManager

internal class FakeTokenManager(
    private val token: String,
    private val loadSyncToken: String = token,
) : TokenManager {
    override suspend fun loadSync(): String = loadSyncToken
    override fun getToken(): String = token

    override suspend fun ensureTokenLoaded() {
        // empty
    }

    override fun setTokenProvider(provider: CacheableTokenProvider) {
        // empty
    }

    override fun setTokenProvider(provider: PersistingTokenProvider) {
        // empty
    }

    override fun hasTokenProvider(): Boolean {
        return true
    }

    override fun hasToken(): Boolean {
        return true
    }

    override fun updateToken(token: String) {
        TODO("Not yet implemented")
    }

    override fun expireToken() {
        // empty
    }
}
