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

package io.getstream.video.android.dogfooding.ui.call

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.CallContainer
import io.getstream.video.android.compose.ui.components.call.controls.CallControls
import io.getstream.video.android.compose.ui.components.call.controls.actions.FlipCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.LeaveCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.core.model.CallType
import io.getstream.video.android.core.permission.PermissionManager
import io.getstream.video.android.core.viewmodel.CallViewModel
import io.getstream.video.android.core.viewmodel.CallViewModelFactory
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.streamCallId

class CallActivity : ComponentActivity() {

    private val factory by lazy { callViewModelFactory() }
    private val vm by viewModels<CallViewModel> { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vm.setPermissionManager(getPermissionManager())
        vm.setOnLeaveCall { finish() }

        setContent {
            VideoTheme {
                CallContainer(
                    modifier = Modifier.background(color = VideoTheme.colors.appBackground),
                    callViewModel = vm,
                    callType = CallType.VIDEO,
                    onBackPressed = { finish() },
                    callControlsContent = {
                        CallControls(
                            callViewModel = vm,
                            actions = listOf(
                                {
                                    ToggleCameraAction(
                                        modifier = Modifier.size(52.dp),
                                        isCameraEnabled = vm.callDeviceState.value.isCameraEnabled,
                                        onCallAction = vm::onCallAction
                                    )
                                },
                                {
                                    ToggleMicrophoneAction(
                                        modifier = Modifier.size(52.dp),
                                        isMicrophoneEnabled = vm.callDeviceState.value.isMicrophoneEnabled,
                                        onCallAction = vm::onCallAction
                                    )
                                },
                                {
                                    FlipCameraAction(
                                        modifier = Modifier.size(52.dp),
                                        onCallAction = vm::onCallAction
                                    )
                                },
                                {
                                    LeaveCallAction(
                                        modifier = Modifier.size(52.dp),
                                        onCallAction = vm::onCallAction
                                    )
                                },
                            ),
                            onCallAction = vm::onCallAction
                        )
                    }
                )
            }
        }
    }

    val call by lazy {
        val (type, id) =
            intent.streamCallId(EXTRA_CALL_ID)
                ?: throw IllegalArgumentException("You must pass correct channel id.")
        StreamVideo.instance().call(type = type, id = id)
    }

    private fun callViewModelFactory(): CallViewModelFactory {


        return CallViewModelFactory(
            call = call
        )
    }

    override fun onPause() {
        super.onPause()
        call.camera.pause()
    }

    override fun onResume() {
        super.onResume()
        call.camera.resume()
    }

    private fun getPermissionManager(): PermissionManager {
        return PermissionManager.create(
            activity = this,
            onPermissionResult = { permission, isGranted ->
                when (permission) {
                    android.Manifest.permission.CAMERA -> vm.onCallAction(ToggleCamera(isGranted))
                    android.Manifest.permission.RECORD_AUDIO -> vm.onCallAction(
                        ToggleMicrophone(
                            isGranted
                        )
                    )
                }
            },
            onShowRequestPermissionRationale = {}
        )
    }

    companion object {
        internal const val EXTRA_CALL_ID = "EXTRA_CALL_ID"

        fun getIntent(
            context: Context,
            callId: StreamCallId
        ): Intent {
            return Intent(context, CallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_CALL_ID, callId)
            }
        }
    }
}
