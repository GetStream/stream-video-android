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

package io.getstream.video.android.xml

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Rational
import android.view.Menu
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.core.StreamVideoProvider
import io.getstream.video.android.core.call.state.CancelCall
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.core.model.state.StreamCallState
import io.getstream.video.android.core.permission.PermissionManager
import io.getstream.video.android.core.permission.PermissionManagerProvider
import io.getstream.video.android.core.permission.StreamPermissionManagerImpl
import io.getstream.video.android.core.viewmodel.CallViewModel
import io.getstream.video.android.core.viewmodel.CallViewModelFactory
import io.getstream.video.android.core.viewmodel.CallViewModelFactoryProvider
import io.getstream.video.android.xml.binding.bindView
import io.getstream.video.android.xml.databinding.ActivityCallBinding
import io.getstream.video.android.xml.utils.extensions.streamThemeInflater
import io.getstream.video.android.xml.widget.active.ActiveCallView
import io.getstream.video.android.xml.widget.incoming.IncomingCallView
import io.getstream.video.android.xml.widget.outgoing.OutgoingCallView
import io.getstream.video.android.xml.widget.participant.CallParticipantView
import io.getstream.video.android.xml.widget.participant.PictureInPictureView
import io.getstream.video.android.xml.widget.participant.RendererInitializer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull

public abstract class AbstractXmlCallActivity :
    AppCompatActivity(),
    StreamVideoProvider,
    CallViewModelFactoryProvider,
    PermissionManagerProvider {

    private lateinit var callPermissionManager: PermissionManager

    private var pipJob: Job? = null

    private val binding by lazy { ActivityCallBinding.inflate(streamThemeInflater) }

    private val streamVideo by lazy { getStreamVideo(this) }

    private val factory by lazy {
        getCallViewModelFactory() ?: defaultViewModelFactory()
    }

    private val callViewModel by viewModels<CallViewModel>(factoryProducer = { factory })

    /**
     * Provides the default ViewModel factory.
     */
    public fun defaultViewModelFactory(): CallViewModelFactory {
        return CallViewModelFactory(
            streamVideo = streamVideo,
            permissionManager = getPermissionManager(),
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
                    Manifest.permission.RECORD_AUDIO -> callViewModel.onCallAction(ToggleMicrophone(isGranted))
                }
            },
            onShowSettings = {
                showPermissionsDialog()
            }
        )
    }

    /**
     * Returns the [PermissionManager] initialized in [initPermissionManager].
     */
    override fun getPermissionManager(): PermissionManager = callPermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        callPermissionManager = initPermissionManager()
        showWhenLockedAndTurnScreenOn()
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupToolbar()
        observeStreamCallState()
    }

    override fun onResume() {
        super.onResume()
        startVideoFlow()
    }

    /**
     * Observes the current call state and sets the toolbar title accordingly.
     */
    private fun observeStreamCallState() {
        lifecycleScope.launchWhenCreated {
            callViewModel.streamCallState.combine(callViewModel.isInPictureInPicture) { state, isPictureInPicture ->
                updateToolbar(state, isPictureInPicture)
                when {
                    state is StreamCallState.Incoming && !state.acceptedByMe -> showIncomingScreen()
                    state is StreamCallState.Outgoing && !state.acceptedByCallee -> showOutgoingScreen()
                    state is StreamCallState.Connected && isPictureInPicture -> showPipLayout()
                    state is StreamCallState.Idle -> finish()
                    else -> showActiveCallScreen()
                }
            }.collect()
        }
    }

    /**
     * Updates the toolbar title depending on the call state.
     *
     * @param streamCallState The state of the call we are observing.
     * @param isPictureInPicture Whether the app is in picture in picture mode. If true will hide the toolbar or hide it
     * if it is false.
     */
    private fun updateToolbar(streamCallState: StreamCallState, isPictureInPicture: Boolean) {
        binding.callToolbar.isVisible = !isPictureInPicture

        val callId = when (streamCallState) {
            is StreamCallState.Active -> streamCallState.callGuid.id
            else -> ""
        }
        val status = streamCallState.formatAsTitle()

        val title = when (callId.isBlank()) {
            true -> status
            else -> "$status: $callId"
        }
        binding.callToolbar.title = title
    }

    /**
     * Sets up the toolbar.
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.callToolbar)
        supportActionBar?.let {
            it.setDisplayShowTitleEnabled(false)
            it.setDisplayShowHomeEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
        binding.callToolbar.setNavigationOnClickListener { handleBackPressed() }
    }

    /**
     * Shows the outgoing call screen and initialises the state observers required to populate the screen.
     */
    private fun showOutgoingScreen() {
        if (isViewInsideContainer<OutgoingCallView>()) return
        val outgoingCallView = OutgoingCallView(this)
        addContentView(outgoingCallView)
        outgoingCallView.bindView(callViewModel, this)
    }

    /**
     * Shows the incoming call screen and initialises the state observers required to populate the screen.
     */
    private fun showIncomingScreen() {
        if (isViewInsideContainer<IncomingCallView>()) return
        val incomingCallView = IncomingCallView(this)
        addContentView(incomingCallView)
        incomingCallView.bindView(callViewModel, this)
    }

    /**
     * Shows the active call screen and initialises the state observers required to populate the screen.
     */
    private fun showActiveCallScreen() {
        if (isViewInsideContainer<ActiveCallView>()) return
        val activeCallView = ActiveCallView(this)
        addContentView(activeCallView)
        activeCallView.bindView(callViewModel, this)
    }

    /**
     * Shows the picture in picture layout which consists of the primary call participants feed.
     */
    private fun showPipLayout() {
        if (isViewInsideContainer<CallParticipantView>()) return
        val callParticipant = PictureInPictureView(this)
        callParticipant.rendererInitializer = RendererInitializer { videoRenderer, streamId, trackType, onRender ->
            callViewModel.callState.value?.initRenderer(videoRenderer, streamId, trackType, onRender)
        }
        addContentView(callParticipant)
        pipJob?.cancel()
        pipJob = lifecycleScope.launchWhenCreated {
            callViewModel.primarySpeaker.filterNotNull().collect {
                callParticipant.participant = it
            }
        }
    }

    private fun addContentView(view: View) {
        view.layoutParams =
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        binding.contentHolder.removeAllViews()
        binding.contentHolder.addView(view)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.call_menu, menu)
        return true
    }

    /**
     * If the user denied the permission and clicked don't ask again, will open settings so the user can enable the
     * permissions.
     */
    private fun startSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                val uri = Uri.fromParts("package", packageName, null)
                data = uri
            }
        )
    }

    /**
     * Starts the flow to connect to a call.
     */
    private fun startVideoFlow() {
        val isInitialized = callViewModel.isVideoInitialized.value
        if (isInitialized) return
        callViewModel.connectToCall()
    }

    /**
     * Shows a dialog explaining why the permissions are needed.
     */
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

    /**
     * Keeps the app visible if the device enters the locked state.
     */
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

    /**
     * Clears state when the user closes the call.
     */
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
            if (!isInPictureInPictureMode) {
                pipJob?.cancel()
                getChildInstanceOf<PictureInPictureView>()?.let {
                    it.rendererInitializer = null
                    it.participant = null
                }
            }
        }
    }

    /**
     * Formats the current call state so that we can show it in the toolbar.
     */
    private fun StreamCallState.formatAsTitle() = when (this) {
        is StreamCallState.Drop -> "Drop"
        is StreamCallState.Joined -> "Joined"
        is StreamCallState.Connecting -> "Connecting"
        is StreamCallState.Connected -> "Connected"
        is StreamCallState.Incoming -> "Incoming"
        is StreamCallState.Joining -> "Joining"
        is StreamCallState.Outgoing -> "Outgoing"
        StreamCallState.Idle -> "Idle"
    }

    /**
     * Returns the view of type [T] if it is inside the content holder.
     *
     * @return The instance of [T] if one is inside the content holder, otherwise null.
     */
    private inline fun <reified T : View> getChildInstanceOf(): T? {
        return binding.contentHolder.children.firstOrNull { it is T } as? T
    }

    /**
     * Checks if the view inside the content is of type [T].
     *
     * @return Whether the instance of [T] is inside the content holder.
     */
    private inline fun <reified T : View> isViewInsideContainer(): Boolean {
        return getChildInstanceOf<T>() != null
    }
}
