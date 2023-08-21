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

package io.getstream.video.android

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import io.getstream.android.push.permissions.NotificationPermissionManager
import io.getstream.android.push.permissions.NotificationPermissionStatus
import io.getstream.log.taggedLogger
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.ui.call.CallActivity
import io.getstream.video.android.ui.theme.Colors
import io.getstream.video.android.util.StreamVideoInitHelper
import kotlinx.coroutines.launch
import javax.inject.Inject

class DeeplinkingActivity : ComponentActivity() {

    private val logger by taggedLogger("Call:DeeplinkView")

    @Inject
    lateinit var dataStore: StreamUserDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        logger.d { "[onCreate] savedInstanceState: $savedInstanceState" }
        super.onCreate(savedInstanceState)

        setContent {
            VideoTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Colors.background),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = VideoTheme.colors.primaryAccent,
                    )
                }
            }
        }

        val data: Uri = intent?.data ?: return
        val callId = data.getQueryParameter("id") ?: return

        logger.d { "Action: ${intent?.action}" }
        logger.d { "Data: ${intent?.data}" }

        // The demo app can start a meeting automatically on first application launch - this
        // means that we haven't yet asked for notification permissions - we should first ask for
        // these permissions and then proceed with the call (to prevent the video screen from
        // asking video&audio permissions at the same time)
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            // join call directly
            joinCall(callId)
        } else {
            // first ask for push notification permission
            val manager = NotificationPermissionManager.createNotificationPermissionsManager(
                application = app,
                requestPermissionOnAppLaunch = { true },
                onPermissionStatus = {
                    // we don't care about the result for demo purposes
                    if (it != NotificationPermissionStatus.REQUESTED) {
                        joinCall(callId)
                    }
                },
            )
            manager.start()
        }
    }

    private fun joinCall(cid: String) {
        lifecycleScope.launch {
            // Deep link can be opened without the app after install - there is no user yet
            // But in this case the StreamVideoInitHelper will use a random account
            StreamVideoInitHelper.loadSdk(
                dataStore = dataStore,
                useRandomUserAsFallback = true,
            )
            if (StreamVideo.isInstalled) {
                val callId = StreamCallId(type = "default", id = cid)
                val intent = CallActivity.createIntent(
                    context = this@DeeplinkingActivity,
                    callId = callId,
                    disableMicOverride = intent.getBooleanExtra(EXTRA_DISABLE_MIC_OVERRIDE, false),
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        }
    }

    companion object {

        private const val EXTRA_DISABLE_MIC_OVERRIDE = "disableMic"

        /**
         * @param callId the Call ID you want to join
         * @param disableMicOverride optional parameter if you want to override the users setting
         * and disable the microphone.
         */
        @JvmStatic
        fun createIntent(
            context: Context,
            callId: String,
            disableMicOverride: Boolean = false,
        ): Intent {
            return Intent(context, DeeplinkingActivity::class.java).apply {
                data = Uri.Builder()
                    .appendQueryParameter("id", callId)
                    .build()
                putExtra(EXTRA_DISABLE_MIC_OVERRIDE, disableMicOverride)
            }
        }
    }
}
