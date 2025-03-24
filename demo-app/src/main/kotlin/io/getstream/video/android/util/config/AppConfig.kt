/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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
import android.net.Uri
import androidx.core.content.edit
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.getstream.log.taggedLogger
import io.getstream.video.android.BuildConfig
import io.getstream.video.android.tooling.util.StreamFlavors
import io.getstream.video.android.util.config.types.StreamEnvironment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Main entry point for remote / local configuration
 */
@OptIn(ExperimentalStdlibApi::class)
object AppConfig {
    // Constants
    private val logger by taggedLogger("RemoteConfig")
    private const val SHARED_PREF_NAME = "stream_demo_app"
    private const val SELECTED_ENV = "selected_env_v2"

    // Data
    private lateinit var environment: StreamEnvironment
    private lateinit var prefs: SharedPreferences

    // State of config values
    val availableEnvironments = listOf(
        StreamEnvironment(
            env = "pronto",
            aliases = listOf("stream-calls-dogfood"),
            displayName = "Pronto",
            sharelink = "https://pronto.getstream.io/join/",
        ),
        StreamEnvironment(
            env = "demo",
            aliases = listOf(""),
            displayName = "Demo",
            sharelink = "https://getstream.io/video/demos/join/",
        ),
        StreamEnvironment(
            env = "staging",
            aliases = emptyList(),
            displayName = "Staging",
            sharelink = "https://staging.getstream.io/join/",
        ),
        StreamEnvironment(
            env = "pronto-staging",
            aliases = emptyList(),
            displayName = "Pronto Staging",
            sharelink = "https://pronto-staging.getstream.io/join/",
        ),
    )
    val currentEnvironment = MutableStateFlow(availableEnvironments.default(BuildConfig.FLAVOR))


    // Utilities
    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    // API
    /**
     * Setup the remote configuration.
     * Will automatically put config into [AppConfig.config]
     *
     * @param context an android context.
     * @param coroutineScope the scope used to run [onLoaded]
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun load(
        context: Context,
        coroutineScope: CoroutineScope = GlobalScope,
        onLoaded: suspend () -> Unit = {},
    ) {
        // Load prefs
        prefs = context.getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        try {
            val jsonAdapter: JsonAdapter<StreamEnvironment> = moshi.adapter()
            val selectedEnvData = prefs.getString(SELECTED_ENV, null)
            val selectedEnvironment = selectedEnvData?.let {
                jsonAdapter.fromJson(it)
            }
            val which = selectedEnvironment ?: availableEnvironments.default(BuildConfig.FLAVOR)
            selectEnv(which)
            onLoaded()
        } catch (e: Exception) {
            logger.e(e) { "Failed to parse  remote config. Deeplinks not working!" }
        }
    }

    /**
     * Select environment. Must be one of [StreamRemoteConfig.environments].
     *
     * @param which environment to select
     */
    fun selectEnv(which: StreamEnvironment) {
        val jsonAdapter: JsonAdapter<StreamEnvironment> = moshi.adapter()
        // Select default environment from config if none is in prefs
        environment = which
        // Update selected env
        prefs.edit(commit = true) {
            putString(SELECTED_ENV, jsonAdapter.toJson(environment))
        }
        currentEnvironment.value = environment
    }

    fun List<StreamEnvironment>.fromUri(env: Uri): StreamEnvironment? {
        val environmentName = env.extractEnvironment()
        return environmentName?.let { name ->
            firstOrNull { streamEnv ->
                streamEnv.env == name || streamEnv.aliases.contains(name)
            }
        }
    }

    private fun List<StreamEnvironment>.default(currentFlavor: String): StreamEnvironment {
        return if (currentFlavor == StreamFlavors.development) {
           first { it.env == "pronto" }
        } else {
            first { it.env == "demo" }
        }
    }

    private fun Uri?.extractEnvironment(): String? {
        // Extract the host from the Uri
        val host = this?.host ?: return null
        // Split the host by "." and return the first part
        val parts = host.split(".")
        // 0                        |  1                |  2
        //                          | getstream         | io
        // pronto                   | getstream         | io
        // stream-call-dogfood      | vercel            | app
        return if (parts.size > 2) parts[0] else ""
    }
}
