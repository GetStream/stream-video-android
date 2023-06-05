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

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import io.getstream.log.StreamLog
import io.getstream.video.android.datastore.flow.asStateFlow
import io.getstream.video.android.datastore.model.StreamUserPreferences
import io.getstream.video.android.datastore.serializer.UserSerializer
import io.getstream.video.android.datastore.serializer.encrypted
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserDevices
import io.getstream.video.android.model.UserToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

public class StreamUserDataStore constructor(
    dataStore: DataStore<StreamUserPreferences?>,
    private val scope: CoroutineScope
) :
    DataStore<StreamUserPreferences?> by dataStore {

    public suspend fun updateUserPreferences(streamUserPreferences: StreamUserPreferences) {
        updateData { streamUserPreferences }
    }

    public suspend fun updateUser(user: User?) {
        updateData { preferences ->
            (preferences ?: StreamUserPreferences()).copy(user = user)
        }
    }

    public suspend fun updateApiKey(apiKey: ApiKey) {
        updateData { preferences ->
            (preferences ?: StreamUserPreferences()).copy(apiKey = apiKey)
        }
    }

    public suspend fun updateUserToken(userToken: UserToken) {
        updateData { preferences ->
            (preferences ?: StreamUserPreferences()).copy(userToken = userToken)
        }
    }

    public suspend fun updateUserDevices(userDevices: UserDevices) {
        updateData { preferences ->
            (preferences ?: StreamUserPreferences()).copy(userDevices = userDevices)
        }
    }

    public fun cancelJobs() {
        scope.cancel()
    }

    public suspend fun clear(): StreamUserPreferences? = updateData { null }

    public val user: StateFlow<User?> = data.map { it?.user }.asStateFlow(null, scope)

    public val apiKey: StateFlow<ApiKey> =
        data.map { it?.apiKey.orEmpty() }.asStateFlow("", scope)

    public val userToken: StateFlow<UserToken> =
        data.map { it?.userToken.orEmpty() }.asStateFlow("", scope)

    public val userDevices: StateFlow<UserDevices?> =
        data.map { it?.userDevices ?: UserDevices() }
            .asStateFlow(UserDevices(), scope)

    public companion object {
        /**
         * Represents if [StreamUserDataStore] is already installed or not.
         * Lets you know if the internal [StreamUserDataStore] instance is being used as the
         * uncaught exception handler when true or if it is using the default one if false.
         */
        public var isInstalled: Boolean = false
            get() = internalStreamUserDataStore != null
            private set

        /**
         * [StreamUserDataStore] instance to be used.
         */
        @Volatile
        private var internalStreamUserDataStore: StreamUserDataStore? = null

        /**
         * Returns an installed [StreamUserDataStore] instance or throw an exception if its not installed.
         */
        public fun instance(): StreamUserDataStore {
            return internalStreamUserDataStore
                ?: throw IllegalStateException(
                    "StreamUserDataStore.install() must be called before obtaining StreamUserDataStore instance."
                )
        }

        /**
         * Returns an installed [StreamUserDataStore] instance lazy or throw an exception if its not installed.
         */
        public fun lazyInstance(): Lazy<StreamUserDataStore> {
            return lazy(LazyThreadSafetyMode.NONE) { instance() }
        }

        /**
         * Installs a new [StreamUserDataStore] instance to be used.
         */
        public fun install(
            context: Context,
            isEncrypted: Boolean = true,
            scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        ): StreamUserDataStore {
            synchronized(this) {
                if (isInstalled) {
                    StreamLog.e("StreamVideo") {
                        "The $internalStreamUserDataStore is already installed but you've tried to " +
                            "install a new exception handler."
                    }
                    return internalStreamUserDataStore!!
                }

                val dataStore = if (isEncrypted) {
                    AeadConfig.register()
                    val aead = AndroidKeysetManager.Builder()
                        .withSharedPref(context, "master_keyset", "master_key_preference")
                        .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                        .withMasterKeyUri("android-keystore://master_key")
                        .build()
                        .keysetHandle
                        .getPrimitive(Aead::class.java)

                    DataStoreFactory.create(serializer = UserSerializer().encrypted(aead)) {
                        context.dataStoreFile("proto_stream_video_user.pb")
                    }
                } else {
                    DataStoreFactory.create(serializer = UserSerializer()) {
                        context.dataStoreFile("proto_stream_video_user.pb")
                    }
                }

                val userDataStore = StreamUserDataStore(dataStore, scope)

                internalStreamUserDataStore = userDataStore
                return userDataStore
            }
        }

        /**
         * Uninstall a previous [StreamUserDataStore] instance.
         */
        public fun unInstall() {
            internalStreamUserDataStore = null
        }
    }
}
