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

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import io.getstream.result.Result
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.compose.ui.components.call.ringing.RingingCallContent
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.state.AcceptCall
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.DeclineCall
import io.getstream.video.android.core.call.state.LeaveCall
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.core.call.state.ToggleSpeakerphone
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.model.streamCallId
import io.getstream.video.android.util.StreamVideoInitHelper
import kotlinx.coroutines.launch

class IncomingCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // release the lock, turn on screen, and keep the device awake.
        showWhenLockedAndTurnScreenOn()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val callId = intent.streamCallId(NotificationHandler.INTENT_EXTRA_CALL_CID)!!

        lifecycleScope.launch {

            StreamVideoInitHelper.init(this@IncomingCallActivity)
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
                    Toast.LENGTH_SHORT
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
                        modifier = Modifier.background(color = VideoTheme.colors.appBackground),
                        call = call,
                        onBackPressed = {
                            call.leave()
                            finish()
                        },
                        onAcceptedContent = {
                            CallContent(
                                modifier = Modifier.fillMaxSize(),
                                call = call,
                                onCallAction = onCallAction
                            )
                        },
                        onRejectedContent = {
                            call.leave()
                            finish()
                        },
                        onCallAction = onCallAction
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
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }
}
