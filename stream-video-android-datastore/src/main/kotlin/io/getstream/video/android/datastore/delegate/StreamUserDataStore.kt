/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.datastore.delegate

import androidx.datastore.core.DataStore
import io.getstream.video.android.datastore.model.StreamUserPreferences
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserDevices
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

public class StreamUserDataStore(dataStore: DataStore<StreamUserPreferences?>) :
    DataStore<StreamUserPreferences?> by dataStore {

    public suspend fun updateUserPreferences(streamUserPreferences: StreamUserPreferences) {
        updateData { streamUserPreferences }
    }

    public suspend fun updateUser(user: User?) {
        updateData { preferences ->
            preferences?.copy(user = user)
        }
    }

    public suspend fun updateApiKey(apiKey: ApiKey) {
        updateData { preferences ->
            preferences?.copy(apiKey = apiKey)
        }
    }

    public suspend fun updateUserDevices(userDevices: UserDevices) {
        updateData { preferences ->
            preferences?.copy(userDevices = userDevices)
        }
    }

    public suspend fun clear(): StreamUserPreferences? = updateData { null }

    public val preferences: Flow<StreamUserPreferences?>
        inline get() = data

    public val user: Flow<User?>
        inline get() = preferences.map { it?.user }

    public val apiKey: Flow<ApiKey?>
        inline get() = preferences.map { it?.apiKey }

    public val userDevices: Flow<UserDevices?>
        inline get() = preferences.map { it?.userDevices }
}
