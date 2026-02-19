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

package io.getstream.video.android.core.notifications.internal.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import io.getstream.video.android.model.Device
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DeviceTokenStorage(val context: Context) {

    private val Context.dataStore: DataStore<DevicePreferences?> by dataStore(
        fileName = "stream-device-token-storage.pb",
        serializer = DevicePreferencesSerializer,
    )

    /** A state that contains a persisted [Device] data. */
    val userDevice: Flow<Device?> =
        context.dataStore.data.map { it?.userDevice }

    suspend fun updateUserDevice(device: Device?) {
        context.dataStore.updateData { preferences ->
            (preferences ?: DevicePreferences()).copy(userDevice = device)
        }
    }

    /** Clear the persisted all user data. */
    public suspend fun clear(): DevicePreferences? = context.dataStore.updateData { null }
}
