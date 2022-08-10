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

package io.getstream.video.android.token

internal class CredentialsManagerImpl : CredentialsManager {
    @Volatile
    private var token: String = EMPTY_TOKEN
    private var apiKey: String = EMPTY_TOKEN
    private lateinit var provider: CredentialsProvider

    override fun ensureTokenLoaded() {
        if (!hasToken()) {
            loadTokenSync()
        }
    }

    override fun loadTokenSync(): String {
        return provider.getCachedToken().also {
            this.token = it
        }
    }

    override fun setCredentialsProvider(provider: CredentialsProvider) {
        this.provider = provider
        this.token = provider.getCachedToken()
        this.apiKey = provider.getCachedApiKey()
    }

    override fun hasTokenProvider(): Boolean {
        return this::provider.isInitialized
    }

    override fun getToken(): String = token

    override fun hasToken(): Boolean {
        return token != EMPTY_TOKEN
    }

    override fun expireToken() {
        token = EMPTY_TOKEN
    }

    override fun getApiKey(): String {
        return apiKey
    }

    companion object {
        const val EMPTY_TOKEN = ""
    }
}
