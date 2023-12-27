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

package io.getstream.video.android.util.config

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.getstream.log.taggedLogger
import io.getstream.video.android.BuildConfig
import io.getstream.video.android.R
import io.getstream.video.android.util.config.types.Flavor
import io.getstream.video.android.util.config.types.StreamEnvironment
import io.getstream.video.android.util.config.types.StreamRemoteConfig
import java.util.concurrent.Executors

/**
 * Main entry point for remote / local configuration
 */
@OptIn(ExperimentalStdlibApi::class)
object AppConfig {
    // Constants
    private val logger by taggedLogger("RemoteConfig")
    private const val APP_CONFIG_KEY = "appconfig"
    private const val SHARED_PREF_NAME = "stream_demo_app"
    private const val SELECTED_ENV = "selected_env"

    // Data
    private lateinit var config: StreamRemoteConfig
    private lateinit var environment: StreamEnvironment
    private lateinit var prefs: SharedPreferences

    // State
    public val currentEnvironment = mutableStateOf<StreamEnvironment?>(null)
    public val availableEnvironments = mutableStateOf<List<StreamEnvironment>>(arrayListOf())
    public val availableLogins = mutableStateOf<List<String>>(arrayListOf())

    // Utils
    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    // API

    /**
     * Setup the remote configuration.
     * Will automatically put config into [AppConfig.config]
     *
     * @param context an android context.
     */
    fun load(context: Context, onLoaded: () -> Unit = {}) {
        // Load prefs
        prefs = context.getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)

        // Initialize local and default values
        val remoteConfig = initializeRemoteConfig()

        // Fetch remote
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(Executors.newSingleThreadExecutor()) { task ->
                if (task.isSuccessful) {
                    logger.v { "Updated remote config values" }
                } else {
                    logger.e { "Update of remote config failed." }
                }
                try {
                    // Parse config
                    val parsed = parseConfig(remoteConfig)
                    config = parsed!!

                    // Update available logins
                    availableLogins.value = config.supportedLogins.firstOrNull {
                        it.flavor.contains(BuildConfig.FLAVOR)
                    }?.logins ?: arrayListOf("email")

                    // Select environment
                    val jsonAdapter: JsonAdapter<StreamEnvironment> = moshi.adapter()
                    val selectedEnvData = prefs.getString(SELECTED_ENV, null)
                    var selectedEnvironment = selectedEnvData?.let {
                        jsonAdapter.fromJson(it)
                    }
                    if (selectedEnvironment?.isForFlavor(BuildConfig.FLAVOR) != true) {
                        // We may have selected environment previously which is no longer available
                        selectedEnvironment = null
                    }
                    val which = selectedEnvironment ?: config.environments.default(BuildConfig.FLAVOR)
                    selectEnv(which)
                    availableEnvironments.value = config.environments.filter {
                        it.isForFlavor(BuildConfig.FLAVOR)
                    }
                    currentEnvironment.value = which
                    onLoaded()
                } catch (e: Exception) {
                    logger.e(e) { "Failed to parse  remote config. Deeplinks not working!" }
                }
            }
    }

    /**
     * Select environment. Must be one of [StreamRemoteConfig.environments].
     *
     * @param which environment to select
     */
    fun selectEnv(which: StreamEnvironment) {
        val currentFlavor = BuildConfig.FLAVOR
        val jsonAdapter: JsonAdapter<StreamEnvironment> = moshi.adapter()

        val selectedEnvironment = which.takeIf {
            config.environments.containsForFlavor(it.env!!, currentFlavor)
        }

        // Select default environment from config if none is in prefs
        environment = selectedEnvironment ?: config.environments.default(currentFlavor)
        // Update selected env
        prefs.edit(commit = true) {
            putString(SELECTED_ENV, jsonAdapter.toJson(environment))
        }
        currentEnvironment.value = environment
    }

    // Internal logic
    private fun initializeRemoteConfig(): FirebaseRemoteConfig {
        val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        return remoteConfig
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun parseConfig(remoteConfig: FirebaseRemoteConfig): StreamRemoteConfig? {
        val value = remoteConfig.getString(APP_CONFIG_KEY)
        val jsonAdapter: JsonAdapter<StreamRemoteConfig> = moshi.adapter()
        return jsonAdapter.fromJson(value)
    }

    private fun List<StreamEnvironment>.containsForFlavor(name: String, flavor: String): Boolean {
        val found = this.find {
            it.env == name && it.flavors.containsFlavorName(flavor)
        }
        return found != null
    }

    private fun List<Flavor>.containsFlavorName(name: String): Boolean {
        val found = this.find {
            it.flavor!! == name
        }
        return found != null
    }

    private fun StreamEnvironment.isForFlavor(flavor: String): Boolean {
        return flavors.find { it.flavor == flavor } != null
    }

    private fun StreamEnvironment.isDefaultForFlavor(flavor: String): Boolean {
        return flavors.find { it.flavor == flavor }?.default == true
    }

    private fun List<StreamEnvironment>.default(currentFlavor: String): StreamEnvironment {
        return findLast { env ->
            env.isDefaultForFlavor(currentFlavor)
        } ?: config.environments.find {
            it.isForFlavor(currentFlavor)
        } ?: config.environments.first()
    }
}
