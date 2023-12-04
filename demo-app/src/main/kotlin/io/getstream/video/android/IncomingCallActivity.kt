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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.getstream.result.Result
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
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
        val streamVideo = StreamVideo.instance()

        lifecycleScope.launch {
            // Not necessary if you initialise the SDK in Application.onCreate()
            StreamVideoInitHelper.loadSdk(dataStore = dataStore)
            val call = streamVideo.call(callId.type, callId.id)

            // We also check if savedInstanceState is null to prevent duplicate calls when activity
            // is recreated (e.g. when entering PiP mode)
            // TODO: AAP This is the same logic as in CallActivity, should be merged
            if (NotificationHandler.ACTION_ACCEPT_CALL == intent.action && savedInstanceState == null) {
                call.accept()
                val activeCall = streamVideo.state.activeCall.value
                val activeOrNewCall = if (activeCall != null) {
                    if (activeCall.id != callId.id) {
                        Log.w("CallActivity", "A call with id: ${callId.cid} existed. Leaving.")
                        // If the call id is different leave the previous call
                        activeCall.leave()
                        // Return a new call
                        streamVideo.call(type = callId.type, id = callId.id)
                    } else {
                        // Call ID is the same, use the active call
                        activeCall
                    }
                } else {
                    // There is no active call, create new call
                    streamVideo.call(type = callId.type, id = callId.id)
                }
                if (activeCall != activeOrNewCall) {
                    val joinResult = call.join(create = true)

                    // Unable to join. Device is offline or other usually connection issue.
                    if (joinResult is Result.Failure) {
                        Log.e("CallActivity", "Call.join failed ${joinResult.value}")
                        finish()
                    }
                }
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
                                onCallAction = onCallAction,
                            )
                        },
                        onRejectedContent = {
                            call.leave()
                            finish()
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
