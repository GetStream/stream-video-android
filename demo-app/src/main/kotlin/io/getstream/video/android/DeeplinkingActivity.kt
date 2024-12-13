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

package io.getstream.video.android

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.Global
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.getstream.android.push.permissions.NotificationPermissionManager
import io.getstream.android.push.permissions.NotificationPermissionStatus
import io.getstream.log.taggedLogger
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.data.state.GlobalCodecChoiceState
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.ui.common.StreamCallActivity
import io.getstream.video.android.ui.common.StreamCallActivityConfiguration
import io.getstream.video.android.util.InitializedState
import io.getstream.video.android.util.StreamVideoInitHelper
import io.getstream.video.android.util.config.AppConfig
import io.getstream.video.android.util.config.AppConfig.fromUri
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
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
                        .background(VideoTheme.colors.baseSheetPrimary),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = VideoTheme.colors.brandPrimary,
                    )
                }
            }
        }

        val callIdFromExtra = intent?.getStringExtra(CALL_ID)
        val data: Uri? = intent?.data

        val callId = callIdFromExtra ?: extractCallId(data)
        if (callId == null) {
            logger.e { "Can't open the call from deeplink because call ID is null" }
            finish()
            return
        }

        logger.d { "Action: ${intent?.action}" }
        logger.d { "Data: ${intent?.data}" }

        val requestMultiplePermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            // Handle the permissions result here
            if (permissions.all { it.value }) {
                logger.d { "All permissions granted, joining call." }
                // All permissions were granted
                // The demo app can start a meeting automatically on first application launch - this
                // means that we haven't yet asked for notification permissions - we should first ask for
                // these permissions and then proceed with the call (to prevent the video screen from
                // asking video&audio permissions at the same time)
                if (ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // ensure that audio & video permissions are granted
                    joinCall(data, callId)
                } else {
                    // first ask for push notification permission
                    val manager =
                        NotificationPermissionManager.createNotificationPermissionsManager(
                            application = app,
                            requestPermissionOnAppLaunch = { true },
                            onPermissionStatus = {
                                // we don't care about the result for demo purposes
                                if (it != NotificationPermissionStatus.REQUESTED) {
                                    joinCall(data, callId)
                                }
                            },
                        )
                    manager.start()
                }
            } else {
                logger.d { "Not all permissions were granted!" }
                // At least one permission was denied
                finish()
            }
        }

        val permissions = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            // Add any other permissions you need here
        )

        requestMultiplePermissionsLauncher.launch(permissions)
    }

    private fun extractCallId(data: Uri?): String? {
        if (data == null) {
            // No data, return null
            return null
        }

        var callId: String? = null

        // Get call id from path
        val pathSegments = data.pathSegments
        pathSegments?.forEachIndexed { index, segment ->
            if (segment == "join") {
                // Next segment is the callId
                callId = pathSegments[index + 1]
            }
        }

        // Try to take from query string
        return callId ?: data.getQueryParameter("id")
    }

    private fun joinCall(data: Uri?, cid: String) {
        lifecycleScope.launch {
            data?.let {
                val determinedEnv = AppConfig.availableEnvironments.fromUri(it)
                determinedEnv?.let {
                    AppConfig.selectEnv(determinedEnv)
                }
            }
            // Deep link can be opened without the app after install - there is no user yet
            // But in this case the StreamVideoInitHelper will use a random account
            StreamVideoInitHelper.reloadSdk(
                dataStore = dataStore,
                useRandomUserAsFallback = true,
            )

            logger.d { "SDK loaded." }
            StreamVideoInitHelper.initializedState.collectLatest {
                if (it == InitializedState.FINISHED || it == InitializedState.FAILED) {
                    if (StreamVideo.isInstalled) {
                        val callId = StreamCallId(type = "default", id = cid)
                        val intent = StreamCallActivity.callIntent(
                            context = this@DeeplinkingActivity,
                            cid = callId,
                            clazz = CallActivity::class.java,
                            configuration = StreamCallActivityConfiguration().copy(custom = Bundle().apply {
                                logger.d { "Starting StreamCallActivity with extra publish / subscribe data: ${GlobalCodecChoiceState.preferredPublishCodec}, ${GlobalCodecChoiceState.preferredSubscribeCodec}" }
                                GlobalCodecChoiceState.preferredPublishCodec?.let { publishCodec ->
                                    putString(
                                        "preferredPublishCodec",
                                        publishCodec,
                                    )
                                }
                                GlobalCodecChoiceState.preferredSubscribeCodec?.let { subscribeCodec ->
                                    putString(
                                        "preferredSubscribeCodec",
                                        subscribeCodec,
                                    )
                                }
                            }),
                        ).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        // We can not go into the call.
                        finish()
                    }
                }
            }
        }
    }

    companion object {

        private const val EXTRA_DISABLE_MIC_OVERRIDE = "disableMic"
        private const val CALL_ID = "cid-deeplink"

        /**
         * @param url the URL containing the call ID
         * @param disableMicOverride optional parameter if you want to override the users setting
         * and disable the microphone.
         */
        @JvmStatic
        fun createIntent(
            context: Context,
            url: Uri,
            disableMicOverride: Boolean = false,
        ): Intent {
            return Intent(context, DeeplinkingActivity::class.java).apply {
                data = url
                putExtra(EXTRA_DISABLE_MIC_OVERRIDE, disableMicOverride)
            }
        }

        /**
         * @param url the URL containing the call ID
         * @param disableMicOverride optional parameter if you want to override the users setting
         * and disable the microphone.
         */
        @JvmStatic
        fun createIntent(
            context: Context,
            callID: String,
            disableMicOverride: Boolean = false,
        ): Intent {
            return Intent(context, DeeplinkingActivity::class.java).apply {
                putExtra(CALL_ID, callID)
                putExtra(EXTRA_DISABLE_MIC_OVERRIDE, disableMicOverride)
            }
        }
    }
}
