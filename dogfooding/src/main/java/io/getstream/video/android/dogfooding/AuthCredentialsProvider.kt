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

package io.getstream.video.android.dogfooding

import io.getstream.video.android.model.User
import io.getstream.video.android.token.CredentialsProvider

class AuthCredentialsProvider(
    private val apiKey: String,
    private val userToken: String,
    private val user: User
) : CredentialsProvider {

    private var sfuToken: String? = null

    override fun loadToken(): String {
        return userToken
    }

    override fun getCachedToken(): String {
        return userToken
    }

    override fun loadApiKey(): String {
        return apiKey
    }

    override fun getCachedApiKey(): String {
        return apiKey
    }

    override fun setSfuToken(token: String?) {
        this.sfuToken = token
    }

    override fun getSfuToken(): String {
        return sfuToken ?: ""
    }

    override fun getUserCredentials(): User {
        return user
    }
}
