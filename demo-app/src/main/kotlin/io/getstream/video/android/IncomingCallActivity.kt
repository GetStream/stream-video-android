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

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.getstream.result.Result
import io.getstream.video.android.compose.theme.base.VideoTheme
import io.getstream.video.android.compose.ui.components.call.ringing.RingingCallContent
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.state.AcceptCall
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.DeclineCall
import io.getstream.video.android.core.call.state.FlipCamera
import io.getstream.video.android.core.call.state.LeaveCall
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.core.call.state.ToggleSpeakerphone
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.model.streamCallId
import io.getstream.video.android.ui.call.CallScreen
import io.getstream.video.android.util.StreamVideoInitHelper
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class IncomingCallActivity : ComponentActivity() {

    @Inject
    lateinit var dataStore: StreamUserDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // release the lock, turn on screen, and keep the device awake.
        showWhenLockedAndTurnScreenOn()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val callId = intent.streamCallId(NotificationHandler.INTENT_EXTRA_CALL_CID)!!

        lifecycleScope.launch {
            // Not necessary if you initialise the SDK in Application.onCreate()
            StreamVideoInitHelper.loadSdk(dataStore = dataStore)
            val call = StreamVideo.instance().call(callId.type, callId.id)

            // Update the call state. This activity could have been started from a push notification.
            // Doing a call.get() will also internally update the Call state object with the latest
            // state from the backend.
            val result = call.get()

            if (result is Result.Failure) {
                // Failed to recover the current state of the call
                // TODO: Automaticly call this in the SDK?
                Log.e("IncomingCallActivity", "Call.join failed ${result.value}")
                Toast.makeText(
                    this@IncomingCallActivity,
                    "Failed get call status (${result.value.message})",
                    Toast.LENGTH_SHORT,
                ).show()
                finish()
            }

            // We also check if savedInstanceState is null to prevent duplicate calls when activity
            // is recreated (e.g. when entering PiP mode)
            if (NotificationHandler.ACTION_ACCEPT_CALL == intent.action && savedInstanceState == null) {
                call.accept()
                call.join()
            }

            setContent {
                VideoTheme {
                    val onCallAction: (CallAction) -> Unit = { callAction ->
                        when (callAction) {
                            is ToggleCamera -> call.camera.setEnabled(callAction.isEnabled)
                            is ToggleMicrophone -> call.microphone.setEnabled(callAction.isEnabled)
                            is ToggleSpeakerphone -> call.speaker.setEnabled(callAction.isEnabled)
                            is FlipCamera -> call.camera.flip()
                            is LeaveCall -> {
                                call.leave()
                                finish()
                            }
                            is DeclineCall -> {
                                lifecycleScope.launch {
                                    call.reject()
                                    call.leave()
                                    finish()
                                }
                            }
                            is AcceptCall -> {
                                lifecycleScope.launch {
                                    call.accept()
                                    call.join()
                                }
                            }
                            else -> Unit
                        }
                    }
                    RingingCallContent(
                        modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
                        call = call,
                        onBackPressed = {
                            call.leave()
                            finish()
                        },
                        onAcceptedContent = {
                            CallScreen(
                                call = call,
                                showDebugOptions = BuildConfig.DEBUG,
                                onCallDisconnected = {
                                    finish()
                                },
                                onUserLeaveCall = {
                                    call.leave()
                                    finish()
                                },
                            )
                        },
                        onRejectedContent = {
                            LaunchedEffect(key1 = call) {
                                call.reject()
                                finish()
                            }
                        },
                        onCallAction = onCallAction,
                    )
                }
            }
        }
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
    }
}
