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
import io.getstream.logging.StreamLog
import io.getstream.video.android.StreamVideo
import io.getstream.video.android.audio.AudioDevice
import io.getstream.video.android.call.CallClient
import io.getstream.video.android.call.state.CallAction
import io.getstream.video.android.call.state.CallMediaState
import io.getstream.video.android.call.state.CustomAction
import io.getstream.video.android.call.state.FlipCamera
import io.getstream.video.android.call.state.LeaveCall
import io.getstream.video.android.call.state.ToggleCamera
import io.getstream.video.android.call.state.ToggleMicrophone
import io.getstream.video.android.call.state.ToggleSpeakerphone
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.CallParticipantState
import io.getstream.video.android.model.CallSettings
import io.getstream.video.android.model.CallType
import io.getstream.video.android.model.CallUser
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
    private val usersProvider: UsersProvider
) : ViewModel() {

    private val logger = StreamLog.getLogger("Call:ViewModel")

    private val _callState: MutableStateFlow<Call?> = MutableStateFlow(null)
    public val callState: StateFlow<Call?> = _callState

    private var _isVideoInitialized: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isVideoInitialized: StateFlow<Boolean> = _isVideoInitialized

    private val isVideoEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val isVideoOn: StateFlow<Boolean> = permissionManager.hasCameraPermission
        .combine(isVideoEnabled) { hasPermission, videoEnabled ->
            hasPermission && videoEnabled
        }.onEach {
            if (::client.isInitialized && _callState.value != null) {
                logger.d { "[isVideoEnabled] setCameraEnabled: $it" }
                client.setCameraEnabled(it)
            }
        }.stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, false)

    private val isAudioEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val isAudioOn: StateFlow<Boolean> = permissionManager.hasRecordAudioPermission
        .combine(isAudioEnabled) { hasPermission, audioEnabled ->
            hasPermission && audioEnabled
        }.onEach {
            if (::client.isInitialized && _callState.value != null) {
                logger.d { "[isAudioEnabled] setMicrophoneEnabled: $it" }
                client.setMicrophoneEnabled(it)
            }
        }.stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, false)

    private val isSpeakerPhoneOn: MutableStateFlow<Boolean> = MutableStateFlow(false)

    public val callMediaState: StateFlow<CallMediaState> =
        combine(isAudioOn, isVideoOn, isSpeakerPhoneOn) { isAudioOn, isVideoOn, isSpeakerPhoneEnabled ->
            CallMediaState(
                isMicrophoneEnabled = isAudioOn,
                isSpeakerphoneEnabled = isSpeakerPhoneEnabled,
                isCameraEnabled = isVideoOn
            )
        }.onEach {
            logger.d { "[callMediaState] callMediaState: $it" }
        }.stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = CallMediaState())

    public val participantList: Flow<List<CallParticipantState>> =
        callState.filterNotNull().flatMapLatest { it.callParticipants }

    public val activeSpeakers: Flow<List<CallParticipantState>> =
        callState.filterNotNull().flatMapLatest { it.activeSpeakers }

    public val localParticipant: Flow<CallParticipantState> =
        callState.filterNotNull().flatMapLatest { it.localParticipant }

    private val _isShowingParticipantsInfo = MutableStateFlow(false)
    public val isShowingParticipantsInfo: StateFlow<Boolean> = _isShowingParticipantsInfo

    private val _isShowingSettings = MutableStateFlow(false)
    public val isShowingAudioDevicePicker: StateFlow<Boolean> = _isShowingSettings

    public val streamCallState: StateFlow<State> get() = streamVideo.callState

    private val _callType: MutableStateFlow<CallType> = MutableStateFlow(CallType.VIDEO)
    public val callType: StateFlow<CallType> = _callType

    private val _callId: MutableStateFlow<String> = MutableStateFlow(value = "")
    public val callId: StateFlow<String> = _callId

    private val _participants: MutableStateFlow<List<CallUser>> = MutableStateFlow(emptyList())
    public val participants: StateFlow<List<CallUser>> = _participants

    private lateinit var client: CallClient

    private var prevState: State = State.Idle

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
                        _participants.value = state.users.values.toList()
                    }
                    is State.Starting -> {
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
                    is State.Joining -> {
                    }
                    is State.InCall -> {
                    }
                    is State.Drop -> {
                    }
                }
                prevState = state
            }
        }
    }

    public fun connectToCall(initialCallSettings: CallSettings) {
        logger.d { "[connectToCall] input: $initialCallSettings" }

        isAudioEnabled.value = initialCallSettings.audioOn && permissionManager.hasRecordAudioPermission.value
        isVideoEnabled.value = initialCallSettings.videoOn && permissionManager.hasCameraPermission.value

        viewModelScope.launch {
            logger.d { "[connectToCall] state: ${streamCallState.value}" }
            withTimeout(CONNECT_TIMEOUT) {
                logger.v { "[connectToCall] received: ${streamCallState.value}" }
                client = streamVideo.awaitCallClient()
                _isVideoInitialized.value = true
                initializeCall(initialCallSettings)
            }
        }
    }

    private suspend fun initializeCall(initialCallSettings: CallSettings) {
        val callResult = client.connectToCall(
            UUID.randomUUID().toString(),
        ) {
            val callSettings = CallSettings(
                autoPublish = initialCallSettings.autoPublish,
                audioOn = isAudioOn.value,
                videoOn = isVideoOn.value,
                speakerOn = isSpeakerPhoneOn.value
            )
            logger.d { "[initializeCall] call settings: $callSettings" }
            callSettings
        }

        when (callResult) {
            is Success -> {
                val call = callResult.data
                _callState.value = call

                val isVideoOn = isVideoOn.firstOrNull() ?: false

                if (initialCallSettings.autoPublish && isVideoOn) {
                    client.startCapturingLocalVideo(CameraMetadata.LENS_FACING_FRONT)
                }
            }
            is Failure -> {
                // TODO - show error to user
            }
        }
    }

    private fun toggleSpeakerphone(enabled: Boolean) {
        if (::client.isInitialized) client.setSpeakerphoneEnabled(enabled)
        onSpeakerphoneChanged(enabled)
    }

    /**
     * Flips the camera for the current participant if possible.
     */
    public fun flipCamera() {
        client.flipCamera()
    }

    /**
     * Sets the flag used to display the settings menu in the UI to true.
     */
    public fun showSettings() {
        _isShowingSettings.value = true
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

    /**
     * Sets the flag used to display participants info menu in the UI to true.
     */
    public fun showParticipants() {
        this._isShowingParticipantsInfo.value = true
    }

    public fun onCallAction(callAction: CallAction) {
        when (callAction) {
            is ToggleSpeakerphone -> toggleSpeakerphone(callAction.isEnabled)
            is ToggleCamera -> onVideoChanged(callAction.isEnabled)
            is ToggleMicrophone -> onMicrophoneChanged(callAction.isEnabled)
            is FlipCamera -> flipCamera()
            is LeaveCall -> cancelCall()
            is CustomAction -> {
                // custom actions
            }
        }
    }

    /**
     * Drops the call by sending a cancel event, which informs other users.
     */
    public fun cancelCall() {
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
        return client.getAudioDevices()
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
        this._isShowingSettings.value = false
        this._isShowingParticipantsInfo.value = false
    }

    /**
     * Selects an audio device to be used for playback.
     *
     * @param device The device to use.
     */
    public fun selectAudioDevice(device: AudioDevice) {
        client.selectAudioDevice(device)
    }

    public fun acceptCall() {
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

    public fun rejectCall() {
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

    public fun hangUpCall() {
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
        isAudioEnabled.value = microphoneEnabled
    }

    private fun onVideoChanged(videoEnabled: Boolean) {
        logger.d { "[onVideoChanged] videoEnabled: $videoEnabled" }
        if (!permissionManager.hasCameraPermission.value) {
            logger.w { "[onMicrophoneChanged] the [Manifest.permissions.CAMERA] has to be granted for video to be sent" }
        }
        isVideoEnabled.value = videoEnabled
    }

    private fun onSpeakerphoneChanged(speakerPhoneEnabled: Boolean) {
        logger.d { "[onSpeakerphoneChanged] speakerPhoneEnabled: $speakerPhoneEnabled" }
        isSpeakerPhoneOn.value = speakerPhoneEnabled
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
    public fun inviteUsersToCall(users: List<User>) {
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
}
