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
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.StreamVideo
import io.getstream.video.android.StreamVideoProvider
import io.getstream.video.android.call.state.CancelCall
import io.getstream.video.android.call.state.ToggleCamera
import io.getstream.video.android.call.state.ToggleMicrophone
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.CallContent
import io.getstream.video.android.compose.ui.components.call.activecall.DefaultPictureInPictureContent
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.state.StreamCallState
import io.getstream.video.android.permission.PermissionManager
import io.getstream.video.android.permission.PermissionManagerProvider
import io.getstream.video.android.permission.StreamPermissionManagerImpl
import io.getstream.video.android.viewmodel.CallViewModel
import io.getstream.video.android.viewmodel.CallViewModelFactory
import io.getstream.video.android.viewmodel.CallViewModelFactoryProvider

public abstract class AbstractComposeCallActivity :
    AppCompatActivity(),
    StreamVideoProvider,
    CallViewModelFactoryProvider,
    PermissionManagerProvider {

    private val streamVideo: StreamVideo by lazy { getStreamVideo(this) }

    private lateinit var callPermissionManager: PermissionManager
    private val factory by lazy {
        getCallViewModelFactory() ?: defaultViewModelFactory()
    }

    /**
     * Provides the default ViewModel factory.
     */
    public fun defaultViewModelFactory(): CallViewModelFactory {
        return CallViewModelFactory(
            streamVideo = streamVideo,
            permissionManager = callPermissionManager,
        )
    }

    /**
     * Provides the default [PermissionManager] implementation.
     */
    override fun initPermissionManager(): PermissionManager {
        return StreamPermissionManagerImpl(
            fragmentActivity = this,
            onPermissionResult = { permission, isGranted ->
                when (permission) {
                    Manifest.permission.CAMERA -> callViewModel.onCallAction(ToggleCamera(isGranted))
                    Manifest.permission.RECORD_AUDIO -> callViewModel.onCallAction(
                        ToggleMicrophone(
                            isGranted
                        )
                    )
                }
            },
            onShowSettings = {
                showPermissionsDialog()
            }
        )
    }

    protected val callViewModel: CallViewModel by viewModels(factoryProducer = { factory })

    override fun onCreate(savedInstanceState: Bundle?) {
        callPermissionManager = initPermissionManager()
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

        startVideoFlow()
    }

    override fun getPermissionManager(): PermissionManager = callPermissionManager

    protected open fun buildContent(): (@Composable () -> Unit) = {
        VideoTheme {
            CallContent(
                viewModel = callViewModel,
                onCallAction = { action ->
                    when (action) {
                        is ToggleMicrophone -> toggleMicrophone(action)
                        is ToggleCamera -> toggleCamera(action)
                        else -> callViewModel.onCallAction(action)
                    }
                },
                onBackPressed = ::handleBackPressed,
                pictureInPictureContent = { PictureInPictureContent(call = it) }
            )
        }
    }

    @Composable
    protected open fun PictureInPictureContent(call: Call) {
        DefaultPictureInPictureContent(roomState = call)
    }

    private fun toggleMicrophone(action: ToggleMicrophone) {
        if (!callPermissionManager.hasRecordAudioPermission.value && action.isEnabled) {
            callPermissionManager.requestPermission(Manifest.permission.RECORD_AUDIO)
        } else {
            callViewModel.onCallAction(action)
        }
    }

    private fun toggleCamera(action: ToggleCamera) {
        if (!callPermissionManager.hasCameraPermission.value && action.isEnabled) {
            callPermissionManager.requestPermission(Manifest.permission.CAMERA)
        } else {
            callViewModel.onCallAction(action)
        }
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
        callViewModel.connectToCall()
    }

    private fun showPermissionsDialog() {
        AlertDialog.Builder(this).setTitle("Permissions required to launch the app")
            .setMessage("Open settings to allow camera and microphone permissions.")
            .setPositiveButton("Launch settings") { dialog, _ ->
                startSettings()
                dialog.dismiss()
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.create().show()
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

    /**
     * Triggers when the user taps on the system or header back button.
     *
     * Attempts to show Picture in Picture mode, if the user allows it and your Application supports
     * the feature.
     */
    protected open fun handleBackPressed() {
        val callState = callViewModel.streamCallState.value

        if (callState !is StreamCallState.Connected) {
            closeCall()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                callViewModel.dismissOptions()

                enterPictureInPictureMode(
                    PictureInPictureParams.Builder().setAspectRatio(Rational(9, 16)).apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            this.setAutoEnterEnabled(true)
                        }
                    }.build()
                )
            } else {
                enterPictureInPictureMode()
            }
        } else {
            closeCall()
        }
    }

    private fun closeCall() {
        callViewModel.onCallAction(CancelCall)
        callViewModel.clearState()
        finish()
    }

    override fun onStop() {
        super.onStop()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val isInPiP = isInPictureInPictureMode

            if (isInPiP) {
                callViewModel.onCallAction(CancelCall)
                callViewModel.clearState()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            callViewModel.onPictureInPictureModeChanged(isInPictureInPictureMode)
        }
    }
}
