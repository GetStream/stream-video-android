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

import android.app.Application
import android.content.Context

public object UserCredentialsManager {

    private lateinit var userPreferences: UserPreferences

    /**
     * Used to initialize the internal preferences at the start of your app.
     *
     * @param context The context used to create the preferences.
     * @return [UserPreferences] instance used to store and expose credentials.
     */
    public fun initialize(context: Context): UserPreferences {
        if (::userPreferences.isInitialized) {
            return getPreferences()
        }

        userPreferences = UserPreferencesImpl(
            context.getSharedPreferences(KEY_PREFERENCES, Application.MODE_PRIVATE)
        )

        return getPreferences()
    }

    public fun getPreferences(): UserPreferences {
        return userPreferences
    }

    private const val KEY_PREFERENCES = "stream-video-preferences"
}
