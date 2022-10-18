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

package io.getstream.video.android.app.user

import android.content.SharedPreferences
import androidx.core.content.edit
import io.getstream.video.android.model.UserCredentials

class UserPreferencesImpl(
    private val sharedPreferences: SharedPreferences
) : UserPreferences {

    override fun getCachedCredentials(): UserCredentials {
        return UserCredentials(
            id = sharedPreferences.getString(KEY_ID, "") ?: "",
            token = sharedPreferences.getString(KEY_TOKEN, "") ?: "",
            image = sharedPreferences.getString(KEY_IMAGE, "") ?: "",
            name = sharedPreferences.getString(KEY_NAME, "") ?: "",
            role = sharedPreferences.getString(KEY_ROLE, "") ?: ""
        )
    }

    override fun storeUserCredentials(userCredentials: UserCredentials) {
        sharedPreferences.edit {
            putString(KEY_ID, userCredentials.id)
            putString(KEY_TOKEN, userCredentials.token)
            putString(KEY_IMAGE, userCredentials.image)
            putString(KEY_NAME, userCredentials.name)
            putString(KEY_ROLE, userCredentials.role)
        }
    }

    override fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    internal companion object {
        private const val KEY_ID = "id"
        private const val KEY_TOKEN = "token"
        private const val KEY_IMAGE = "image"
        private const val KEY_NAME = "name"
        private const val KEY_ROLE = "role"
    }
}
