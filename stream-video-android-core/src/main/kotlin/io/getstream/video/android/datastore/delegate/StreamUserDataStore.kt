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
import io.getstream.log.taggedLogger
import io.getstream.video.android.datastore.model.StreamUserPreferences
import io.getstream.video.android.datastore.serializer.UserSerializer
import io.getstream.video.android.datastore.serializer.encrypted
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.Device
import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.KeyStore

/**
 * A DataStore managers to persist Stream user login data safely, consistently, and transactionally.
 *
 * @param dataStore A [DataStore] that contains data type of [StreamUserPreferences].
 */
public class StreamUserDataStore constructor(
    dataStore: DataStore<StreamUserPreferences?>,
) :
    DataStore<StreamUserPreferences?> by dataStore {

    /**
     * Update user preferences with the give [StreamUserPreferences].
     *
     * @param streamUserPreferences A new [StreamUserPreferences] to replace previous persisted data.
     */
    public suspend fun updateUserPreferences(streamUserPreferences: StreamUserPreferences) {
        updateData { streamUserPreferences }
    }

    /**
     * Update [User] information that is used to build a `StreamVideo` instance for logging in.
     *
     * @param user A user instance to be used logged in.
     */
    public suspend fun updateUser(user: User?) {
        updateData { preferences ->
            (preferences ?: StreamUserPreferences()).copy(user = user)
        }
    }

    /**
     * Update [ApiKey] information that is used to build a `StreamVideo` instance for logging in.
     *
     * @param apiKey An API key instance to be used logged in.
     */
    public suspend fun updateApiKey(apiKey: ApiKey) {
        updateData { preferences ->
            (preferences ?: StreamUserPreferences()).copy(apiKey = apiKey)
        }
    }

    /**
     * Update [UserToken] information that is used to build a `StreamVideo` instance for logging in.
     *
     * @param userToken An user token instance to be used logged in.
     */
    public suspend fun updateUserToken(userToken: UserToken) {
        updateData { preferences ->
            (preferences ?: StreamUserPreferences()).copy(userToken = userToken)
        }
    }

    /**
     * Update [Device] information that is used to be get push notifications from the Stream server.
     *
     * @param userDevice A user device used to be get push notifications from the Stream server.
     */
    public suspend fun updateUserDevice(userDevice: Device?) {
        updateData { preferences ->
            (preferences ?: StreamUserPreferences()).copy(userDevice = userDevice)
        }
    }

    /** Clear the persisted all user data. */
    public suspend fun clear(): StreamUserPreferences? = updateData { null }

    /** A state that contains a persisted [User] data. */
    public val user: Flow<User?> = data.map { it?.user }

    /** A state that contains a persisted [ApiKey] data. */
    public val apiKey: Flow<ApiKey> =
        data.map { it?.apiKey.orEmpty() }

    /** A state that contains a persisted [UserToken] data. */
    public val userToken: Flow<UserToken> =
        data.map { it?.userToken.orEmpty() }

    /** A state that contains a persisted [Device] data. */
    public val userDevice: Flow<Device?> =
        data.map { it?.userDevice }

    public companion object {

        private val logger by taggedLogger("StreamUserDataStore")
        private const val MASTER_KEY_PREFERENCE_FILE = "master_key_preference"
        private const val USER_PROTO_FILE = "proto_stream_video_user.pb"
        private const val MASTER_KEY_NAME = "master_key"

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
                    "StreamUserDataStore.install() must be called before obtaining StreamUserDataStore instance.",
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

                    val aead = try {
                        createAead(context)
                    } catch (e: Exception) {
                        // The client application is most likely using allowBack="true" in manifest
                        // and our encrypted datastore can't be restored. We need to clear it.
                        logger.e(e) { "Failed to decrypt the datastore - clearing data" }
                        deleteEncryptionKeysAndData(context)
                        createAead(context)
                    }
                    DataStoreFactory.create(serializer = UserSerializer().encrypted(aead)) {
                        context.dataStoreFile(USER_PROTO_FILE)
                    }
                } else {
                    DataStoreFactory.create(serializer = UserSerializer()) {
                        context.dataStoreFile(USER_PROTO_FILE)
                    }
                }

                val userDataStore = StreamUserDataStore(dataStore)

                internalStreamUserDataStore = userDataStore
                return userDataStore
            }
        }

        private fun createAead(context: Context) =
            AndroidKeysetManager.Builder()
                .withSharedPref(context, "master_keyset", MASTER_KEY_PREFERENCE_FILE)
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri("android-keystore://${MASTER_KEY_NAME}")
                .build()
                .keysetHandle
                .getPrimitive(Aead::class.java)

        private fun deleteEncryptionKeysAndData(context: Context) {
            // Remove the preference file
            context.getSharedPreferences(MASTER_KEY_PREFERENCE_FILE, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()

            // Delete the master key
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            try {
                keyStore.load(null)
                keyStore.deleteEntry(MASTER_KEY_NAME)
            } catch (e: Exception) {
                logger.e(e) { "Failed to delete the master key" }
                // Let's try to proceed anyway
            }

            // Delete stored data
            context.dataStoreFile(USER_PROTO_FILE).delete()
        }

        /**
         * Uninstall a previous [StreamUserDataStore] instance.
         */
        public fun unInstall() {
            internalStreamUserDataStore = null
        }
    }
}
