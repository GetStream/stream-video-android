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

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import io.getstream.logging.StreamLog
import io.getstream.logging.android.AndroidStreamLogger
import io.getstream.video.android.StreamCalls
import io.getstream.video.android.StreamCallsBuilder
import io.getstream.video.android.app.lifecycle.StreamActivityLifecycleCallbacks
import io.getstream.video.android.app.ui.incoming.IncomingCallActivity
import io.getstream.video.android.app.user.UserPreferences
import io.getstream.video.android.app.user.UserPreferencesImpl
import io.getstream.video.android.events.CallCreatedEvent
import io.getstream.video.android.logging.LoggingLevel
import io.getstream.video.android.model.state.StreamCallState
import io.getstream.video.android.token.CredentialsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VideoApp : Application() {

    private val appScope = CoroutineScope(Dispatchers.Main)

    val userPreferences: UserPreferences by lazy {
        UserPreferencesImpl(
            getSharedPreferences(KEY_PREFERENCES, MODE_PRIVATE)
        )
    }

    lateinit var credentialsProvider: CredentialsProvider
        private set

    lateinit var streamCalls: StreamCalls
        private set

    private var currentActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StreamLog.setValidator { _, _ -> true }
            StreamLog.setLogger(AndroidStreamLogger())
        }
        StreamLog.i(TAG) { "[onCreate] no args" }
        registerActivityLifecycleCallbacks(
            StreamActivityLifecycleCallbacks(
                onActivityCreated = { currentActivity = it },
                onActivityStarted = { currentActivity = it },
                onLastActivityStopped = { currentActivity = null },
            )
        )
    }

    /**
     * Sets up and returns the [streamCalls] required to connect to the API.
     */
    fun initializeStreamCalls(
        credentialsProvider: CredentialsProvider,
        loggingLevel: LoggingLevel
    ): StreamCalls {
        StreamLog.d(TAG) { "[initializeStreamCalls] loggingLevel: $loggingLevel" }
        this.credentialsProvider = credentialsProvider

        return StreamCallsBuilder(
            context = this,
            credentialsProvider = credentialsProvider,
            loggingLevel = loggingLevel
        ).build().also {
            // TODO can we initialize streamCalls directly in VideoApp?
            streamCalls = it
            observeState()
            StreamLog.v(TAG) { "[initializeStreamCalls] completed" }
        }
    }

    private fun observeState() {
        appScope.launch {
            streamCalls.callState.collect { state ->
                when (state) {
                    is StreamCallState.Incoming -> {

                        val input = state.run {
                            /**
                             * [IncomingCallVieModel] will be listening to [streamCalls.callState] as well.
                             * So [IncomingCallActivity] won't need to any input except [callId]
                             */
                            CallCreatedEvent(
                                callId = callId,
                                users = users,
                                info = info,
                                details = details
                            )
                        }
                        val intent = IncomingCallActivity.getLaunchIntent(applicationContext, input).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        startActivity(intent)
                    }
                    else -> {}
                }
            }
        }
    }

    private companion object {
        /**
         * Preferences file name.
         */
        private const val KEY_PREFERENCES = "video-prefs"
        private const val TAG = "Call:App"
    }
}

internal val Context.videoApp get() = applicationContext as VideoApp
