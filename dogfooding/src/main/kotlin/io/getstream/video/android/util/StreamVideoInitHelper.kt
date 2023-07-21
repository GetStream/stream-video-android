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

package io.getstream.video.android.util

import android.content.Context
import android.util.Log
import io.getstream.log.Priority
import io.getstream.video.android.App
import io.getstream.video.android.BuildConfig
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.model.User
import io.getstream.video.android.token.StreamVideoNetwork
import kotlinx.coroutines.flow.first

object StreamVideoInitHelper {

    /**
     * This function will initialise [StreamVideo] if we are logged in or do nothing if not.
     * Set [useGuestAsFallback] to true if you want to use a guest fallback if the user is not
     * logged in.
     */
    suspend fun init(context: Context, useGuestAsFallback: Boolean = true) {
        val app = context.applicationContext as App

        if (StreamVideo.isInstalled) {
            Log.w("StreamVideoInitHelper", "[initStreamVideo] StreamVideo is already initialised.")
            return
        }
        val dataStore = StreamUserDataStore.install(context)
        val preferences = dataStore.data.first()
        if (preferences != null) {
            app.initializeStreamChat(
                user = preferences.user!!,
                token = preferences.userToken
            )

            app.initializeStreamVideo(
                user = preferences.user!!,
                token = preferences.userToken,
                apiKey = preferences.apiKey,
                loggingLevel = LoggingLevel(priority = Priority.VERBOSE)
            )
        } else if (useGuestAsFallback) {
            val guest = User(id = "guest", name = "Guest", role = "guest")
            val result = StreamVideoNetwork.tokenService.fetchToken(
                userId = guest.id,
                apiKey = BuildConfig.DOGFOODING_API_KEY
            )
            app.initializeStreamChat(user = guest, token = result.token)
            app.initializeStreamVideo(
                user = guest,
                token = result.token,
                apiKey = BuildConfig.DOGFOODING_API_KEY,
                loggingLevel = LoggingLevel(priority = Priority.VERBOSE)
            )
        }
    }
}
