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

package io.getstream.video.android.app

import android.app.Application
import io.getstream.video.android.StreamCalls
import io.getstream.video.android.StreamCallsImpl
import io.getstream.video.android.app.user.UserPreferences
import io.getstream.video.android.app.user.UserPreferencesImpl
import io.getstream.video.android.logging.LoggingLevel
import io.getstream.video.android.token.CredentialsProvider

class VideoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: VideoApp

        val userPreferences: UserPreferences by lazy {
            UserPreferencesImpl(
                instance.getSharedPreferences(KEY_PREFERENCES, MODE_PRIVATE)
            )
        }

        lateinit var credentialsProvider: CredentialsProvider
            private set

        lateinit var streamCalls: StreamCalls
            private set

        /**
         * Sets up and returns the [streamCalls] required to connect to the API.
         */
        fun initializeController(
            credentialsProvider: CredentialsProvider,
            loggingLevel: LoggingLevel
        ): StreamCalls {
            this.credentialsProvider = credentialsProvider

            this.streamCalls = StreamCallsImpl.Builder(
                context = instance,
                credentialsProvider = credentialsProvider,
                loggingLevel = loggingLevel
            ).build()

            return streamCalls
        }

        /**
         * Preferences file name.
         */
        private const val KEY_PREFERENCES = "video-prefs"
    }
}
