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

package io.getstream.video.android.viewmodel

import android.hardware.camera2.CameraMetadata
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.getstream.log.taggedLogger
import io.getstream.video.android.StreamVideo
import io.getstream.video.android.audio.AudioDevice
import io.getstream.video.android.call.CallClient
import io.getstream.video.android.call.state.AcceptCall
import io.getstream.video.android.call.state.CallAction
import io.getstream.video.android.call.state.CallMediaState
import io.getstream.video.android.call.state.CancelCall
import io.getstream.video.android.call.state.CustomAction
import io.getstream.video.android.call.state.DeclineCall
import io.getstream.video.android.call.state.FlipCamera
import io.getstream.video.android.call.state.InviteUsersToCall
import io.getstream.video.android.call.state.LeaveCall
import io.getstream.video.android.call.state.SelectAudioDevice
import io.getstream.video.android.call.state.ShowCallInfo
import io.getstream.video.android.call.state.ToggleCamera
import io.getstream.video.android.call.state.ToggleMicrophone
import io.getstream.video.android.call.state.ToggleScreenConfiguration
import io.getstream.video.android.call.state.ToggleSpeakerphone
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.CallParticipantState
import io.getstream.video.android.model.CallSettings
import io.getstream.video.android.model.CallType
import io.getstream.video.android.model.CallUser
import io.getstream.video.android.model.ScreenSharingSession
import io.getstream.video.android.model.User
import io.getstream.video.android.permission.PermissionManager
import io.getstream.video.android.user.UsersProvider
import io.getstream.video.android.utils.Failure
import io.getstream.video.android.utils.Success
import io.getstream.video.android.utils.onError
import io.getstream.video.android.utils.onSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.*
import io.getstream.video.android.model.state.StreamCallState as State

private const val CONNECT_TIMEOUT = 30_000L

public class CallViewModel(
    private val streamVideo: StreamVideo,
    private val permissionManager: PermissionManager,
    private val usersProvider: UsersProvider,
) : ViewModel() {

    private val logger by taggedLogger("Call:ViewModel")

    private val _callState: MutableStateFlow<Call?> = MutableStateFlow(null)
    public val callState: StateFlow<Call?> = _callState

    private val clientState: MutableStateFlow<CallClient?> = MutableStateFlow(null)
    private val client: CallClient?
        get() = clientState.value

    private var _isVideoInitialized: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isVideoInitialized: StateFlow<Boolean> = _isVideoInitialized

    /**
     * Determines whether the video should be enabled/disabled before [Call] and [CallClient] get initialised.
     */
    private val isVideoEnabled: MutableStateFlow<Boolean> =
        MutableStateFlow(streamVideo.config.defaultVideoOn && permissionManager.hasCameraPermission.value)

    /**
     * Determines whether the video should be on or not. If [CallClient] is not initialised reflects the UI state
     * stored inside [isVideoEnabled], otherwise reflects the state of the [CallClient.isVideoEnabled].
     */
    private val isVideoOn: StateFlow<Boolean> =
        clientState.flatMapLatest { it?.isVideoEnabled ?: isVideoEnabled }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                streamVideo.config.defaultVideoOn
            )

    /**
     * Determines whether the audio should be enabled/disabled before [Call] and [CallClient] get initialised.
     */
    private val isAudioEnabled: MutableStateFlow<Boolean> =
        MutableStateFlow(streamVideo.config.defaultAudioOn && permissionManager.hasRecordAudioPermission.value)

    /**
     * Determines whether the audio should be on or not. If [CallClient] is not initialised reflects the UI state
     * stored inside [isAudioEnabled], otherwise reflects the state of the [CallClient.isAudioEnabled].
     */
    private val isAudioOn: StateFlow<Boolean> =
        clientState.flatMapLatest { it?.isAudioEnabled ?: isAudioEnabled }
            .onEach {
                logger.d { "[isAudioOn] isAudioOn: $it" }
            }
            .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, false)

    /**
     * Determines whether the speaker phone should be enabled/disabled before [Call] and [CallClient] get initialised.
     */
    private val isSpeakerPhoneEnabled: MutableStateFlow<Boolean> =
        MutableStateFlow(streamVideo.config.defaultSpeakerPhoneOn)

    /**
     * Determines whether the speaker phone should be on or not. If [CallClient] is not initialised reflects the UI
     * state stored inside [isSpeakerPhoneEnabled], otherwise reflects the state of the
     * [CallClient.isSpeakerPhoneEnabled].
     */
    private val isSpeakerPhoneOn: StateFlow<Boolean> = clientState
        .flatMapLatest { it?.isSpeakerPhoneEnabled ?: isSpeakerPhoneEnabled }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = streamVideo.config.defaultSpeakerPhoneOn
        )

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

    public val participantList: Flow<List<CallParticipantState>> =
        callState.filterNotNull().flatMapLatest { it.callParticipants }

    public val activeSpeakers: Flow<List<CallParticipantState>> =
        callState.filterNotNull().flatMapLatest { it.activeSpeakers }

    public val localParticipant: Flow<CallParticipantState> =
        callState.filterNotNull().flatMapLatest { it.localParticipant }

    public val primarySpeaker: Flow<CallParticipantState?> =
        callState.filterNotNull().flatMapLatest { it.primarySpeaker }

    private val _isShowingCallInfo = MutableStateFlow(false)
    public val isShowingCallInfo: StateFlow<Boolean> = _isShowingCallInfo

    public val streamCallState: StateFlow<State> get() = streamVideo.callState

    private val _callType: MutableStateFlow<CallType> = MutableStateFlow(CallType.VIDEO)
    public val callType: StateFlow<CallType> = _callType

    private val _callId: MutableStateFlow<String> = MutableStateFlow(value = "")
    public val callId: StateFlow<String> = _callId

    private val _participants: MutableStateFlow<List<CallUser>> = MutableStateFlow(emptyList())
    public val participants: StateFlow<List<CallUser>> = _participants

    public val screenSharingSessions: Flow<List<ScreenSharingSession>> =
        callState.flatMapLatest { it?.screenSharingSessions ?: emptyFlow() }

    private var prevState: State = State.Idle

    private val _isInPictureInPicture: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isInPictureInPicture: StateFlow<Boolean> = _isInPictureInPicture

    private val _isFullscreen: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isFullscreen: StateFlow<Boolean> = _isFullscreen

    init {
        viewModelScope.launch {
            streamVideo.callState.collect { state ->
                logger.i { "[observeState] state: $state" }
                when (state) {
                    is State.Idle -> {
                        clearState()
                    }
                    is State.Incoming -> {
                        _callType.value = CallType.fromType(state.callGuid.type)
                        _participants.value = state.users.values
                            .filter { it.id != streamVideo.getUser().id }
                            .toList().filter { it.id != streamVideo.getUser().id }
                    }
                    is State.Joining -> {
                        _callType.value = CallType.fromType(state.callGuid.type)
                        _callId.value = state.callGuid.id
                    }
                    is State.Outgoing -> {
                        _callType.value = CallType.fromType(state.callGuid.type)
                        _callId.value = state.callGuid.id
                        _participants.value = state.users.values
                            .filter { it.id != streamVideo.getUser().id }
                            .toList()
                    }
                    else -> Unit
                }
                prevState = state
            }
        }
    }

    public fun connectToCall() {
        viewModelScope.launch {
            logger.d { "[connectToCall] state: ${streamCallState.value}" }
            withTimeout(CONNECT_TIMEOUT) {
                logger.v { "[connectToCall] received: ${streamCallState.value}" }
                clientState.value = streamVideo.awaitCallClient()
                client?.setInitialCallSettings(
                    CallSettings(
                        autoPublish = streamVideo.config.autoPublish,
                        microphoneOn = isAudioEnabled.value,
                        cameraOn = isVideoEnabled.value,
                        speakerOn = isSpeakerPhoneEnabled.value
                    )
                )
                _isVideoInitialized.value = true
                initializeCall(streamVideo.config.autoPublish)
            }
        }
    }

    private suspend fun initializeCall(autoPublish: Boolean) {
        client?.let { client ->
            when (
                val callResult =
                    client.connectToCall(UUID.randomUUID().toString(), autoPublish)
            ) {
                is Success -> {
                    val call = callResult.data
                    _callState.value = call

                    val isVideoOn = isVideoOn.firstOrNull() ?: false

                    if (autoPublish && isVideoOn) {
                        client.startCapturingLocalVideo(CameraMetadata.LENS_FACING_FRONT)
                    }
                }
                is Failure -> {
                    // TODO - show error to user
                }
            }
        } ?: logger.e { "[initializeCall] CallClient was not initialised." }
    }

    /**
     * Flips the camera for the current participant if possible.
     */
    private fun flipCamera() {
        client?.flipCamera()
    }

    /**
     * Attempts to reconnect to the video room, by cleaning the state, disconnecting, canceling any
     * jobs and finally reinitializing.
     */
    public fun reconnect() {
//        val room = _roomState.value ?: return
//
//        _primarySpeaker.value = null
//        room.disconnect()
//        viewModelScope.coroutineContext.cancelChildren()
//
//        viewModelScope.launch {
//            val call = _callState.value
//            val url = _urlState.value
//            val token = _tokenState.value
//
//            init(room, call!!, url, token)
//        }
    }

    override fun onCleared() {
        super.onCleared()
        clearState()
    }

    public fun onCallAction(callAction: CallAction) {
        when (callAction) {
            is ToggleSpeakerphone -> onSpeakerphoneChanged(callAction.isEnabled)
            is ToggleCamera -> onVideoChanged(callAction.isEnabled)
            is ToggleMicrophone -> onMicrophoneChanged(callAction.isEnabled)
            is SelectAudioDevice -> selectAudioDevice(callAction.audioDevice)
            FlipCamera -> flipCamera()
            CancelCall -> cancelCall()
            AcceptCall -> acceptCall()
            DeclineCall -> hangUpCall()
            LeaveCall -> cancelCall()
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

    /**
     * Drops the call by sending a cancel event, which informs other users.
     */
    private fun cancelCall() {
        val state = streamVideo.callState.value
        if (state !is State.Active) {
            logger.w { "[cancelCall] rejected (state is not Active): $state" }
            return
        }
        viewModelScope.launch {
            logger.d { "[cancelCall] state: $state" }
            streamVideo.cancelCall(state.callGuid.cid)
        }
    }

    /**
     * @return A [List] of [AudioDevice] that can be used for playback.
     */
    public fun getAudioDevices(): List<AudioDevice> {
        return client?.getAudioDevices() ?: listOf()
    }

    /**
     * Clears the state of the call and disposes of the CallClient and Call instances.
     */
    public fun clearState() {
        logger.i { "[clearState] no args" }
        _callState.value = null
        isVideoEnabled.value = false
        isAudioEnabled.value = false
        _isVideoInitialized.value = false
        dismissOptions()
    }

    /**
     * Resets the state of two popup UI flags.
     */
    public fun dismissOptions() {
        this._isShowingCallInfo.value = false
    }

    /**
     * Selects an audio device to be used for playback.
     *
     * @param device The device to use.
     */
    private fun selectAudioDevice(device: AudioDevice) {
        client?.selectAudioDevice(device)
    }

    private fun acceptCall() {
        val state = streamVideo.callState.value
        if (state !is State.Incoming || state.acceptedByMe) {
            logger.w { "[acceptCall] rejected (state is not unaccepted Incoming): $state" }
            return
        }
        logger.d { "[acceptCall] state: $state" }
        viewModelScope.launch {
            streamVideo.acceptCall(state.callGuid.cid)
                .onSuccess {
                    logger.v { "[acceptCall] completed: $it" }
                }
                .onError {
                    logger.e { "[acceptCall] failed: $it" }
                    rejectCall()
                }
        }
    }

    private fun rejectCall() {
        val state = streamVideo.callState.value
        if (state !is State.Incoming || state.acceptedByMe) {
            logger.w { "[declineCall] rejected (state is not unaccepted Incoming): $state" }
            return
        }
        logger.d { "[declineCall] state: $state" }
        viewModelScope.launch {
            val result = streamVideo.rejectCall(state.callGuid.cid)
            logger.d { "[declineCall] result: $result" }
        }
    }

    private fun hangUpCall() {
        val state = streamVideo.callState.value
        if (state !is State.Active) {
            logger.w { "[hangUpCall] rejected (state is not Active): $state" }
            return
        }
        logger.d { "[hangUpCall] state: $state" }
        viewModelScope.launch {
            streamVideo.cancelCall(state.callGuid.cid)
        }
    }

    private fun onMicrophoneChanged(microphoneEnabled: Boolean) {
        logger.d { "[onMicrophoneChanged] microphoneEnabled: $microphoneEnabled" }
        if (!permissionManager.hasRecordAudioPermission.value) {
            logger.w { "[onMicrophoneChanged] the [Manifest.permissions.RECORD_AUDIO] has to be granted for audio to be sent" }
        }
        client?.setMicrophoneEnabled(microphoneEnabled)
        isAudioEnabled.value = microphoneEnabled
    }

    private fun onVideoChanged(videoEnabled: Boolean) {
        logger.d { "[onVideoChanged] videoEnabled: $videoEnabled" }
        if (!permissionManager.hasCameraPermission.value) {
            logger.w { "[onVideoChanged] the [Manifest.permissions.CAMERA] has to be granted for video to be sent" }
        }
        client?.setCameraEnabled(videoEnabled)
        isVideoEnabled.value = videoEnabled
    }

    private fun onSpeakerphoneChanged(speakerPhoneEnabled: Boolean) {
        logger.d { "[onSpeakerphoneChanged] speakerPhoneEnabled: $speakerPhoneEnabled" }
        client?.setSpeakerphoneEnabled(speakerPhoneEnabled)
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
        logger.d { "[inviteUsersToCall] Inviting users to call, users: $users" }
        val callState = streamCallState.value

        if (callState !is State.Connected) {
            logger.d { "[inviteUsersToCall] Invalid state, not in State.Connected!" }
            return
        }
        viewModelScope.launch {
            streamVideo.inviteUsers(users, callState.callGuid.cid)
                .onSuccess {
                    logger.d { "[inviteUsersToCall] Success!" }
                }
                .onError {
                    logger.d { "[inviteUsersToCall] Error, ${it.message}." }
                }
        }
    }

    public fun onPictureInPictureModeChanged(inPictureInPictureMode: Boolean) {
        this._isInPictureInPicture.value = inPictureInPictureMode
    }
}
