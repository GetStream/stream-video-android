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

package io.getstream.video.android.core.user

import android.content.SharedPreferences
import androidx.core.content.edit
import io.getstream.video.android.core.model.ApiKey
import io.getstream.video.android.core.model.Device
import io.getstream.video.android.core.model.SfuToken
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.model.UserDevices
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class UserPreferencesImpl(
    private val sharedPreferences: SharedPreferences
) : UserPreferences {

    /**
     * @see UserPreferences.getUserCredentials
     */
    override fun getUserCredentials(): User? {
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
     * @see UserPreferences.getApiKey
     */
    override fun getApiKey(): ApiKey =
        sharedPreferences.getString(KEY_APIKEY, null) ?: ""

    /**
     * @see UserPreferences.storeApiKey
     */
    override fun storeApiKey(apiKey: ApiKey) {
        sharedPreferences.edit {
            putString(KEY_APIKEY, apiKey)
        }
    }

    // TODO: SFU tokens aren't reusable, we should probably not store these

    override fun getSfuToken(): SfuToken =
        sharedPreferences.getString(KEY_SFU_TOKEN, "") ?: ""

    override fun storeSfuToken(sfuToken: SfuToken?) {
        sharedPreferences.edit {
            putString(KEY_SFU_TOKEN, sfuToken)
        }
    }

    override fun getUserToken(): String =
        sharedPreferences.getString(KEY_USER_TOKEN, "") ?: ""

    override fun storeUserToken(userToken: String) {
        sharedPreferences.edit {
            putString(KEY_USER_TOKEN, userToken)
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

    private fun parseDevices(json: String?): List<Device> {
        json ?: return emptyList()

        return try {
            val devices: UserDevices = Json.decodeFromString(json)

            devices.devices
        } catch (error: Throwable) {
            emptyList()
        }
    }

    override fun getDevices(): List<Device> {
        return parseDevices(sharedPreferences.getString(KEY_DEVICES, ""))
    }

    override fun removeDevices() {
    }

    override fun storeDevice(device: Device) {
        val devices = getDevices() + device
        val json = Json.encodeToString(UserDevices(devices))

        sharedPreferences.edit {
            putString(KEY_DEVICES, json)
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
        private const val KEY_DEVICES = "devices"
        private const val KEY_SFU_TOKEN = "sfu_token"
        private const val KEY_USER_TOKEN = "user_token"

    }
}
