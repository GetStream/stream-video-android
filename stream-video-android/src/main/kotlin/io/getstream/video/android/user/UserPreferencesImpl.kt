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

package io.getstream.video.android.user

import android.content.SharedPreferences
import androidx.core.content.edit
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.User
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class UserPreferencesImpl(
    private val sharedPreferences: SharedPreferences
) : UserPreferences {

    /**
     * @see UserPreferences.getCachedCredentials
     */
    override fun getCachedCredentials(): User? {
        return parseUser(sharedPreferences.getString(KEY_USER, ""))
    }

    /**
     * @see UserPreferences.storeUserCredentials
     */
    override fun storeUserCredentials(user: User) {
        sharedPreferences.edit {
            putString(KEY_USER, Json.encodeToString(user))
        }
    }

    /**
     * @see UserPreferences.getCachedApiKey
     */
    override fun getCachedApiKey(): ApiKey? =
        sharedPreferences.getString(KEY_APIKEY, null)

    /**
     * @see UserPreferences.storeApiKey
     */
    override fun storeApiKey(apiKey: ApiKey) {
        sharedPreferences.edit {
            putString(KEY_APIKEY, apiKey)
        }
    }

    /**
     * Parses the User JSON from the local preferences if able or a null user if nothing is stored.
     *
     * @return The user value that's parsed.
     */
    private fun parseUser(json: String?): User? {
        json ?: return null

        return try {
            Json.decodeFromString(json)
        } catch (error: Throwable) {
            null
        }
    }

    /**
     * @see UserPreferences.clear
     */
    override fun clear() {
        sharedPreferences.edit { clear() }
    }

    internal companion object {
        private const val KEY_USER = "user_data"
        private const val KEY_APIKEY = "apikey"
    }
}
