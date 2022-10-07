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

import io.getstream.video.android.model.User

/**
 * Exposes a way to build a token provided that connects to custom implementation for
 * authentication.
 */
public interface CredentialsProvider {

    /**
     * @return The user token backed by authentication services.
     */
    public fun loadToken(): String

    /**
     * @return The user token that's cached.
     */
    public fun getCachedToken(): String

    public fun loadApiKey(): String

    public fun getCachedApiKey(): String

    public fun setSfuToken(token: String?)

    public fun getSfuToken(): String

    public fun getUserCredentials(): User
}
