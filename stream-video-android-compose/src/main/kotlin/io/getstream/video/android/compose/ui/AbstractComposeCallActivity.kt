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

package io.getstream.video.android.compose.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.StreamVideo
import io.getstream.video.android.StreamVideoProvider
import io.getstream.video.android.call.state.ToggleCamera
import io.getstream.video.android.call.state.ToggleMicrophone
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.CallContent
import io.getstream.video.android.model.CallSettings
import io.getstream.video.android.model.state.StreamCallState
import io.getstream.video.android.viewmodel.CallViewModel
import io.getstream.video.android.viewmodel.CallViewModelFactory
import io.getstream.video.android.viewmodel.PermissionManagerImpl

public abstract class AbstractComposeCallActivity : AppCompatActivity(), StreamVideoProvider {

    private val streamVideo: StreamVideo by lazy { getStreamVideo(this) }

    private val permissionManager by lazy {
        PermissionManagerImpl(applicationContext)
    }
    private val factory by lazy {
        CallViewModelFactory(streamVideo, permissionManager)
    }

    private val callViewModel by viewModels<CallViewModel>(factoryProducer = { factory })

    private val permissionsContract = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        permissions[Manifest.permission.RECORD_AUDIO]?.let {
            callViewModel.onAudioPermissionChanged(it)
            callViewModel.toggleMicrophone(it)
        }

        permissions[Manifest.permission.CAMERA]?.let {
            callViewModel.onVideoPermissionChanged(it)
            callViewModel.toggleCamera(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        showWhenLockedAndTurnScreenOn()
        super.onCreate(savedInstanceState)
        setContent(content = buildContent())

        lifecycleScope.launchWhenCreated {
            callViewModel.streamCallState.collect {
                if (it is StreamCallState.Idle) {
                    finish()
                }
            }
        }
    }

    protected fun buildContent(): (@Composable () -> Unit) = {
        VideoTheme {
            CallContent(
                viewModel = callViewModel,
                onRejectCall = callViewModel::rejectCall,
                onAcceptCall = callViewModel::acceptCall,
                onCancelCall = callViewModel::cancelCall,
                onCallAction = { callAction ->
                    when (callAction) {
                        is ToggleCamera -> toggleVideo(callAction.isEnabled)
                        is ToggleMicrophone -> toggleMicrophone(callAction.isEnabled)
                        else -> callViewModel.onCallAction(callAction)
                    }
                }
            )
        }
    }

    @SuppressLint("NewApi")
    private fun toggleVideo(isEnabled: Boolean) {
        when {
            isEnabled && !permissionManager.hasVideoPermission -> {
                checkPermission(Manifest.permission.CAMERA)
            }
            else -> callViewModel.onCallAction(ToggleCamera(isEnabled))
        }
    }

    @SuppressLint("NewApi")
    private fun toggleMicrophone(isEnabled: Boolean) {
        when {
            isEnabled && !permissionManager.hasAudioPermission -> {
                checkPermission(Manifest.permission.RECORD_AUDIO)
            }
            else -> callViewModel.onCallAction(ToggleMicrophone(isEnabled))
        }
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
        callViewModel.connectToCall(getDefaultCallSettings())
    }

    protected fun getDefaultCallSettings(): CallSettings = CallSettings(
        audioOn = false,
        videoOn = permissionManager.hasVideoPermission,
        speakerOn = false
    )

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkPermission(permission: String) {
        if (!shouldShowRequestPermissionRationale(permission)) {
            permissionsContract.launch(arrayOf(permission))
        } else {
            showPermissionsDialog()
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
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }
}
