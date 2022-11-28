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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID
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

    private val hasVideoPermission: MutableStateFlow<Boolean> =
        MutableStateFlow(permissionManager.hasCameraPermission)
    private val isVideoEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)

    public val isVideoOn: Flow<Boolean> =
        hasVideoPermission.combine(isVideoEnabled) { hasPermission, videoEnabled ->
            hasPermission && videoEnabled
        }

    private val hasAudioPermission: MutableStateFlow<Boolean> =
        MutableStateFlow(permissionManager.hasRecordAudioPermission)
    private val isAudioEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)

    public val isAudioOn: Flow<Boolean> =
        hasAudioPermission.combine(isAudioEnabled) { hasPermission, audioEnabled ->
            hasPermission && audioEnabled
        }

    private val _callMediaState: MutableStateFlow<CallMediaState> =
        MutableStateFlow(
            CallMediaState(
                isMicrophoneEnabled = isAudioEnabled.value && hasAudioPermission.value,
                isCameraEnabled = isVideoEnabled.value && hasVideoPermission.value,
                isSpeakerphoneEnabled = false
            )
        )

    public val callMediaState: StateFlow<CallMediaState> = _callMediaState

    public val participantList: Flow<List<CallParticipantState>> =
        callState.filterNotNull().flatMapLatest { it.callParticipants }

    public val activeSpeakers: Flow<List<CallParticipantState>> =
        callState.filterNotNull().flatMapLatest { it.activeSpeakers }

    public val localParticipant: Flow<CallParticipantState> =
        callState.filterNotNull().flatMapLatest { it.localParticipant }

    private val _isShowingCallInfo = MutableStateFlow(false)
    public val isShowingCallInfo: StateFlow<Boolean> = _isShowingCallInfo

    private val _isShowingSettings = MutableStateFlow(false)
    public val isShowingAudioDevicePicker: StateFlow<Boolean> = _isShowingSettings

    public val streamCallState: StateFlow<State> get() = streamVideo.callState

    private val _callType: MutableStateFlow<CallType> = MutableStateFlow(CallType.VIDEO)
    public val callType: StateFlow<CallType> = _callType

    private val _callId: MutableStateFlow<String> = MutableStateFlow(value = "")
    public val callId: StateFlow<String> = _callId

    private val _participants: MutableStateFlow<List<CallUser>> = MutableStateFlow(emptyList())
    public val participants: StateFlow<List<CallUser>> = _participants

    private val _isInPictureInPicture: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isInPictureInPicture: StateFlow<Boolean> = _isInPictureInPicture

    private val callSettings: CallSettings = CallSettings(
        autoPublish = true,
        microphoneOn = false,
        cameraOn = true,
        speakerOn = true
    )

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
        logger.d { "[connectToCall] input: $callSettings" }
        viewModelScope.launch {
            logger.d { "[connectToCall] state: ${streamCallState.value}" }
            withTimeout(CONNECT_TIMEOUT) {
                logger.v { "[connectToCall] received: ${streamCallState.value}" }
                client = streamVideo.awaitCallClient()
                _isVideoInitialized.value = true
                initializeCall(callSettings = callSettings)
            }
        }
    }

    private suspend fun initializeCall(callSettings: CallSettings) {
        val callResult = client.connectToCall(
            UUID.randomUUID().toString(),
            callSettings
        )

        when (callResult) {
            is Success -> {
                val call = callResult.data
                _callState.value = call
                isVideoEnabled.value = callSettings.cameraOn
                isAudioEnabled.value = callSettings.microphoneOn
                _callMediaState.value = CallMediaState(
                    isMicrophoneEnabled = callSettings.microphoneOn,
                    isCameraEnabled = callSettings.cameraOn,
                    isSpeakerphoneEnabled = callSettings.speakerOn
                )

                val isVideoOn = isVideoOn.firstOrNull() ?: false

                if (callSettings.autoPublish && isVideoOn) {
                    client.startCapturingLocalVideo(CameraMetadata.LENS_FACING_FRONT)
                }
            }
            is Failure -> {
                // TODO - show error to user
            }
        }
    }

    public fun toggleSpeakerphone(enabled: Boolean) {
        if (::client.isInitialized) {
            client.setSpeakerphoneEnabled(enabled)
            onSpeakerphoneChanged(enabled)
        } else {
            callSettings.speakerOn = enabled
        }
    }

    public fun toggleCamera(enabled: Boolean) {
        if (::client.isInitialized) {
            client.setCameraEnabled(enabled)
            onVideoChanged(enabled)
        } else {
            callSettings.cameraOn = enabled
            callSettings.autoPublish = (callSettings.cameraOn || callSettings.microphoneOn)
        }
    }

    public fun toggleMicrophone(enabled: Boolean) {
        if (::client.isInitialized) {
            client.setMicrophoneEnabled(enabled)
            onMicrophoneChanged(enabled)
        } else {
            callSettings.microphoneOn = enabled
            callSettings.autoPublish = (callSettings.cameraOn || callSettings.microphoneOn)
        }
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
    public fun showCallInfo() {
        this._isShowingCallInfo.value = true
    }

    public fun onCallAction(callAction: CallAction) {
        when (callAction) {
            is ToggleSpeakerphone -> toggleSpeakerphone(callAction.isEnabled)
            is ToggleCamera -> toggleCamera(callAction.isEnabled)
            is ToggleMicrophone -> toggleMicrophone(callAction.isEnabled)
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
        _callMediaState.value = CallMediaState()
        hasAudioPermission.value = false
        hasVideoPermission.value = false
        _isVideoInitialized.value = false
        dismissOptions()
    }

    /**
     * Resets the state of two popup UI flags.
     */
    public fun dismissOptions() {
        this._isShowingSettings.value = false
        this._isShowingCallInfo.value = false
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
        this.isAudioEnabled.value = microphoneEnabled
        val mediaState = _callMediaState.value

        _callMediaState.value = mediaState.copy(isMicrophoneEnabled = microphoneEnabled)
    }

    private fun onVideoChanged(videoEnabled: Boolean) {
        logger.d { "[onVideoChanged] videoEnabled: $videoEnabled" }
        this.isVideoEnabled.value = videoEnabled
        val mediaState = _callMediaState.value

        _callMediaState.value = mediaState.copy(isCameraEnabled = videoEnabled)
    }

    private fun onSpeakerphoneChanged(speakerPhoneEnabled: Boolean) {
        logger.d { "[onSpeakerphoneChanged] speakerPhoneEnabled: $speakerPhoneEnabled" }
        val mediaState = _callMediaState.value

        _callMediaState.value = mediaState.copy(isSpeakerphoneEnabled = speakerPhoneEnabled)
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

    public fun onPictureInPictureModeChanged(inPictureInPictureMode: Boolean) {
        this._isInPictureInPicture.value = inPictureInPictureMode
    }
}
