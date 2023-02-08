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

import io.getstream.video.android.core.model.ApiKey
import io.getstream.video.android.core.model.UserToken
import io.getstream.video.android.core.token.CredentialsProvider

internal class CredentialsManagerImpl : CredentialsManager {
    @Volatile
    private var token: UserToken =
        EMPTY
    private var apiKey: ApiKey =
        EMPTY
    private lateinit var provider: CredentialsProvider

    override fun ensureTokenLoaded() {
        if (!hasToken()) {
            loadTokenSync()
        }
    }

    override fun loadTokenSync(): String {
        return provider.getCachedUserToken().also {
            this.token = it
        }
    }

    override fun setCredentialsProvider(provider: CredentialsProvider) {
        this.provider = provider
        this.token = provider.getCachedUserToken()
        this.apiKey = provider.getCachedApiKey()
    }

    override fun hasTokenProvider(): Boolean {
        return this::provider.isInitialized
    }

    override fun getToken(): UserToken = token

    override fun hasToken(): Boolean {
        return token != EMPTY
    }

    override fun expireToken() {
        token = EMPTY
    }

    override fun getApiKey(): String {
        return apiKey
    }

    companion object {
        const val EMPTY = ""
    }
}
