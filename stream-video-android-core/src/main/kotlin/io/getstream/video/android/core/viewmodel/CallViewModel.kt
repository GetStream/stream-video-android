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

@file:OptIn(ExperimentalCoroutinesApi::class)

package io.getstream.video.android.core.viewmodel

import android.hardware.camera2.CameraMetadata
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ConnectionState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoImpl
import io.getstream.video.android.core.call.SFUSession
import io.getstream.video.android.core.call.state.AcceptCall
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CallMediaState
import io.getstream.video.android.core.call.state.CancelCall
import io.getstream.video.android.core.call.state.CustomAction
import io.getstream.video.android.core.call.state.DeclineCall
import io.getstream.video.android.core.call.state.FlipCamera
import io.getstream.video.android.core.call.state.InviteUsersToCall
import io.getstream.video.android.core.call.state.LeaveCall
import io.getstream.video.android.core.call.state.SelectAudioDevice
import io.getstream.video.android.core.call.state.ShowCallInfo
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.core.call.state.ToggleScreenConfiguration
import io.getstream.video.android.core.call.state.ToggleSpeakerphone
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.permission.PermissionManager
import io.getstream.video.android.core.user.UsersProvider
import io.getstream.video.android.core.utils.mapState
import io.getstream.webrtc.android.ui.VideoTextureViewRenderer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.webrtc.RendererCommon
import stream.video.sfu.models.TrackType
import java.util.UUID

private const val CONNECT_TIMEOUT = 30_000L

public class CallViewModel(
    public val client: StreamVideo,
    public val call: Call,
    private val permissions: PermissionManager,
    private val usersProvider: UsersProvider,
) : ViewModel() {

    private val logger by taggedLogger("Call:ViewModel")

    // shortcut to the call settings
    private val settings = call.state.settings

    private val clientImpl = client as StreamVideoImpl

    // call state needs to be improve
    // - started/ some people joined/ everyone joined
    // - all other people rejected
    // -

    private var _isVideoInitialized: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isVideoInitialized: StateFlow<Boolean> = _isVideoInitialized

    /** if we are in picture in picture mode */
    private val _isInPictureInPicture: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isInPictureInPicture: StateFlow<Boolean> = _isInPictureInPicture

    /** if the call is fullscreen */
    private val _isFullscreen: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isFullscreen: StateFlow<Boolean> = _isFullscreen

    /**
     * Determines whether the video should be enabled/disabled before [Call] and [SFUSession] get initialised.
     */
    // TODO: figure out where in the settings default enables/disabled is stored
    private val isVideoEnabled: MutableStateFlow<Boolean> =
        MutableStateFlow(true && permissions.hasCameraPermission.value)

    /**
     * Determines whether the video should be on or not. If [SFUSession] is not initialised reflects the UI state
     * stored inside [isVideoEnabled], otherwise reflects the state of the [SFUSession.isVideoEnabled].
     */
    private val isVideoOn =
        call.state.connection.mapState { it == ConnectionState.Connected && isVideoEnabled.value }

    /**
     * Determines whether the audio should be enabled/disabled before [Call] and [SFUSession] get initialised.
     */
    private val isAudioEnabled: MutableStateFlow<Boolean> =
        MutableStateFlow(true && permissions.hasRecordAudioPermission.value)

    /**
     * Determines whether the audio should be on or not. If [SFUSession] is not initialised reflects the UI state
     * stored inside [isAudioEnabled], otherwise reflects the state of the [SFUSession.isAudioEnabled].
     */
    private val isAudioOn =
        call.state.connection.mapState { it == ConnectionState.Connected && isAudioEnabled.value }

    /**
     * Determines whether the speaker phone should be enabled/disabled before [Call] and [SFUSession] get initialised.
     */
    private val isSpeakerPhoneEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /**
     * Determines whether the speaker phone should be on or not. If [SFUSession] is not initialised reflects the UI
     * state stored inside [isSpeakerPhoneEnabled], otherwise reflects the state of the
     * [SFUSession.isSpeakerPhoneEnabled].
     */
    private val isSpeakerPhoneOn =
        call.state.connection.mapState { it == ConnectionState.Connected && isSpeakerPhoneEnabled.value }

    /**
     * The state of the call media. Combines [isAudioOn], [isVideoOn], [isSpeakerPhoneOn].
     */
    public val callMediaState: StateFlow<CallMediaState> =
        combine(isAudioOn, isVideoOn, isSpeakerPhoneOn) { isAudioOn, isVideoOn, isSpeakerPhoneOn ->
            CallMediaState(
                isMicrophoneEnabled = isAudioOn,
                isSpeakerphoneEnabled = isSpeakerPhoneOn,
                isCameraEnabled = isVideoOn
            )
        }.onEach {
            logger.d { "[callMediaState] callMediaState: $it" }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = CallMediaState()
        )

    // what does this do?
    private val _isShowingCallInfo = MutableStateFlow(false)
    public val isShowingCallInfo: StateFlow<Boolean> = _isShowingCallInfo

    public fun connectToCall() {
        viewModelScope.launch {
            withTimeout(CONNECT_TIMEOUT) {
                // TODO connection is already running, how do we await it?

                _isVideoInitialized.value = true
                initializeCall(true)
            }
        }
    }

    private suspend fun initializeCall(autoPublish: Boolean) {
        call.activeSession?.let { session ->
            val callResult =
                session.connectToCall(UUID.randomUUID().toString(), autoPublish)

            // TODO raise an error if it failed
            call.mediaManager.startCapturingLocalVideo(CameraMetadata.LENS_FACING_FRONT)
        }
    }

    /**
     * Flips the camera for the current participant if possible.
     */
    private fun flipCamera() {
        // TODO: session is required
        call.mediaManager?.camera?.flip()
    }

    override fun onCleared() {
        super.onCleared()
        // TODO: properly clean up
    }

    public fun onCallAction(callAction: CallAction) {
        when (callAction) {
            is ToggleSpeakerphone -> onSpeakerphoneChanged(callAction.isEnabled)
            is ToggleCamera -> onVideoChanged(callAction.isEnabled)
            is ToggleMicrophone -> onMicrophoneChanged(callAction.isEnabled)
            is SelectAudioDevice -> call.activeSession?.selectAudioDevice(callAction.audioDevice)
            FlipCamera -> flipCamera()
            CancelCall, LeaveCall -> call.leave()
            AcceptCall -> acceptCall()
            DeclineCall -> rejectCall()
            is InviteUsersToCall -> inviteUsersToCall(callAction.users)
            is ToggleScreenConfiguration -> {
                _isFullscreen.value = callAction.isFullscreen && callAction.isLandscape
            }

            is ShowCallInfo -> {
                this._isShowingCallInfo.value = true
            }

            is CustomAction -> {
                // custom actions
            }
        }
    }

    private fun acceptCall() {
//        val state = clientCallState.value
//        if (state !is State.Incoming || state.acceptedByMe) {
//            logger.w { "[acceptCall] rejected (state is not unaccepted Incoming): $state" }
//            return
//        }
//        logger.d { "[acceptCall] state: $state" }
//        viewModelScope.launch {
//            streamVideo.acceptCall(state.callGuid.type, state.callGuid.id)
//                .onSuccess {
//                    logger.v { "[acceptCall] completed: $it" }
//                }
//                .onError {
//                    logger.e { "[acceptCall] failed: $it" }
//                    rejectCall()
//                }
//        }
    }

    private fun rejectCall() {
//        val state = clientCallState.value
//        if (state !is State.Incoming || state.acceptedByMe) {
//            logger.w { "[declineCall] rejected (state is not unaccepted Incoming): $state" }
//            return
//        }
//        logger.d { "[declineCall] state: $state" }
//        viewModelScope.launch {
//            val result = streamVideo.rejectCall(state.callGuid.type, state.callGuid.id)
//            logger.d { "[declineCall] result: $result" }
//        }
    }

    private fun onMicrophoneChanged(microphoneEnabled: Boolean) {
        logger.d { "[onMicrophoneChanged] microphoneEnabled: $microphoneEnabled" }
        if (!permissions.hasRecordAudioPermission.value) {
            logger.w { "[onMicrophoneChanged] the [Manifest.permissions.RECORD_AUDIO] has to be granted for audio to be sent" }
        }
        call.activeSession?.setMicrophoneEnabled(microphoneEnabled)
        isAudioEnabled.value = microphoneEnabled
    }

    private fun onVideoChanged(videoEnabled: Boolean) {
        logger.d { "[onVideoChanged] videoEnabled: $videoEnabled" }
        if (!permissions.hasCameraPermission.value) {
            logger.w { "[onVideoChanged] the [Manifest.permissions.CAMERA] has to be granted for video to be sent" }
        }
        call.activeSession?.setCameraEnabled(videoEnabled)
        isVideoEnabled.value = videoEnabled
    }

    private fun onSpeakerphoneChanged(speakerPhoneEnabled: Boolean) {
        logger.d { "[onSpeakerphoneChanged] speakerPhoneEnabled: $speakerPhoneEnabled" }
        call.activeSession?.setSpeakerphoneEnabled(speakerPhoneEnabled)
        isSpeakerPhoneEnabled.value = speakerPhoneEnabled
    }

    /**
     * Exposes a list of users you can plug in to the UI, such as user invites.
     */
    public fun getUsers(): List<User> = usersProvider.provideUsers()

    /**
     * Exposes a [StateFlow] of a list of users, that can be updated over time, based on your custom
     * logic, and plugged into the UI, similar to [getUsers].
     */
    public fun getUsersState(): StateFlow<List<User>> = usersProvider.userState

    /**
     * Attempts to invite people to an ongoing call.
     *
     * @param users The list of users to add to the call.
     */
    private fun inviteUsersToCall(users: List<User>) {
//        logger.d { "[inviteUsersToCall] Inviting users to call, users: $users" }
//        val callState = clientCallState.value
//
//        if (callState !is State.Connected) {
//            logger.d { "[inviteUsersToCall] Invalid state, not in State.Connected!" }
//            return
//        }
//        viewModelScope.launch {
//            streamVideo.inviteUsers(callState.callGuid.type, callState.callGuid.id, users)
//                .onSuccess {
//                    logger.d { "[inviteUsersToCall] Success!" }
//                }
//                .onError {
//                    logger.d { "[inviteUsersToCall] Error, ${it}." }
//                }
//        }
    }

    val session by lazy { call.activeSession ?: throw IllegalStateException("Session is null") }

    public fun initRenderer(
        videoRenderer: VideoTextureViewRenderer,
        sessionId: String,
        trackType: TrackType,
        onRender: (View) -> Unit = {}
    ) {
        logger.d { "[initRenderer] #sfu; sessionId: $sessionId" }

        // Note this comes from peerConnectionFactory.eglBase
        videoRenderer.init(
            clientImpl.peerConnectionFactory.eglBase.eglBaseContext,
            object : RendererCommon.RendererEvents {
                override fun onFirstFrameRendered() {
                    logger.v { "[initRenderer.onFirstFrameRendered] #sfu; sessionId: $sessionId" }
                    if (trackType != TrackType.TRACK_TYPE_SCREEN_SHARE) {
                        call.state.updateParticipantTrackSize(
                            sessionId,
                            videoRenderer.measuredWidth,
                            videoRenderer.measuredHeight
                        )
                    }
                    onRender(videoRenderer)
                }

                override fun onFrameResolutionChanged(p0: Int, p1: Int, p2: Int) {
                    logger.v { "[initRenderer.onFrameResolutionChanged] #sfu; sessionId: $sessionId" }

                    if (trackType != TrackType.TRACK_TYPE_SCREEN_SHARE) {
                        call.state.updateParticipantTrackSize(
                            sessionId,
                            videoRenderer.measuredWidth,
                            videoRenderer.measuredHeight
                        )
                    }
                }
            }
        )
    }

    public fun onPictureInPictureModeChanged(inPictureInPictureMode: Boolean) {
        this._isInPictureInPicture.value = inPictureInPictureMode
    }
}
