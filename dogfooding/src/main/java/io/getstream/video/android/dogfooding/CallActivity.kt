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

package io.getstream.video.android.dogfooding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import io.getstream.video.android.call.state.LeaveCall
import io.getstream.video.android.call.state.ToggleCamera
import io.getstream.video.android.call.state.ToggleMicrophone
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.activecall.ActiveCallContent
import io.getstream.video.android.model.CallSettings
import io.getstream.video.android.viewmodel.CallViewModel
import io.getstream.video.android.viewmodel.CallViewModelFactory
import io.getstream.video.android.viewmodel.PermissionManager
import io.getstream.video.android.viewmodel.PermissionManagerImpl

class CallActivity : AppCompatActivity() {

    private lateinit var permissionManager: PermissionManager
    private val factory by lazy {
        CallViewModelFactory(dogfoodingApp.streamVideo, permissionManager)
    }

    private val callViewModel by viewModels<CallViewModel>(factoryProducer = { factory })

    override fun onCreate(savedInstanceState: Bundle?) {
        initPermissionManager()
        super.onCreate(savedInstanceState)

        setContent {
            VideoTheme {
                ActiveCallContent(
                    callViewModel,
                    onCallAction = { action ->
                        when (action) {
                            is ToggleMicrophone -> toggleMicrophone(action)
                            is ToggleCamera -> toggleCamera(action)
                            else -> callViewModel.onCallAction(action)
                        }

                        if (action is LeaveCall) {
                            finish()
                        }
                    }
                )
            }
        }
    }

    private fun initPermissionManager() {
        permissionManager = PermissionManagerImpl(this, onPermissionResult = { permission, isGranted ->
            when (permission) {
                Manifest.permission.CAMERA -> callViewModel.onCallAction(ToggleCamera(isGranted))
                Manifest.permission.RECORD_AUDIO -> callViewModel.onCallAction(ToggleMicrophone(isGranted))
            }
        }, onShowSettings = {
                showPermissionsDialog()
            })
    }

    override fun onResume() {
        super.onResume()
        startVideoFlow()
    }

    private fun startSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                val uri = Uri.fromParts("package", packageName, null)
                data = uri
            }
        )
    }

    private fun startVideoFlow() {
        val isInitialized = callViewModel.isVideoInitialized.value
        if (isInitialized) return
        callViewModel.connectToCall(
            CallSettings(
                audioOn = false,
                videoOn = permissionManager.hasCameraPermission.value,
                speakerOn = false
            )
        )
    }

    private fun toggleMicrophone(action: ToggleMicrophone) {
        if (!permissionManager.hasRecordAudioPermission.value && action.isEnabled) {
            permissionManager.requestPermission(Manifest.permission.RECORD_AUDIO)
        } else {
            callViewModel.onCallAction(action)
        }
    }

    private fun toggleCamera(action: ToggleCamera) {
        if (!permissionManager.hasCameraPermission.value && action.isEnabled) {
            permissionManager.requestPermission(Manifest.permission.CAMERA)
        } else {
            callViewModel.onCallAction(action)
        }
    }

    private fun showPermissionsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissions required to launch the app")
            .setMessage("Open settings to allow camera and microphone permissions.")
            .setPositiveButton("Launch settings") { dialog, _ ->
                startSettings()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                finish()
                dialog.dismiss()
            }
            .create()
            .show()
    }

    companion object {
        internal fun getIntent(
            context: Context,
        ): Intent {
            return Intent(context, CallActivity::class.java)
        }
    }
}
