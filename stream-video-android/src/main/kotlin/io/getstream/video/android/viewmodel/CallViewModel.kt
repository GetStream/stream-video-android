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
import io.getstream.video.android.StreamCalls
import io.getstream.video.android.audio.AudioDevice
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.CallInput
import io.getstream.video.android.model.CallParticipant
import io.getstream.video.android.model.CallParticipantState
import io.getstream.video.android.model.CallSettings
import io.getstream.video.android.model.state.StreamCallState
import io.getstream.video.android.token.CredentialsProvider
import io.getstream.video.android.utils.Failure
import io.getstream.video.android.utils.Success
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.*

public class CallViewModel(
    private val input: CallInput,
    private val streamCalls: StreamCalls,
    private val credentialsProvider: CredentialsProvider
) : ViewModel() {

    private val logger = StreamLog.getLogger("Call:ViewModel")

    private val _callState: MutableStateFlow<Call?> =
        MutableStateFlow(null)
    public val callState: StateFlow<Call?> = _callState

    private var _isVideoInitialized: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isVideoInitialized: StateFlow<Boolean> = _isVideoInitialized

    private val _isCameraEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isCameraEnabled: Flow<Boolean> = _isCameraEnabled

    private val _isMicrophoneEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isMicrophoneEnabled: Flow<Boolean> = _isMicrophoneEnabled

    public val participantList: Flow<List<CallParticipant>> =
        callState.filterNotNull().flatMapLatest { it.callParticipants }

    public val participantsState: Flow<List<CallParticipantState>> =
        callState.filterNotNull().flatMapLatest { it.callParticipantState }

    public val activeSpeakers: Flow<List<CallParticipant>> =
        callState.filterNotNull().flatMapLatest { it.activeSpeakers }

    public val localParticipant: Flow<CallParticipant> =
        callState.filterNotNull().flatMapLatest { it.localParticipant }

    private val _isShowingParticipantsInfo = MutableStateFlow(false)
    public val isShowingParticipantsInfo: StateFlow<Boolean> = _isShowingParticipantsInfo

    private val _isShowingSettings = MutableStateFlow(false)
    public val isShowingSettings: StateFlow<Boolean> = _isShowingSettings

    public val streamCallState: StateFlow<StreamCallState> get() = streamCalls.callState

    init {
        viewModelScope.launch {
            streamCalls.callState.collect {
                when (it) {
                    is StreamCallState.Idle -> {
                        logger.i { "[observeState] state: Idle" }
                        clearState()
                    }
                    else -> { /* no-op */ }
                }
            }
        }
    }

    public fun connectToCall() {
        logger.d { "[createCall] input: $input" }
        // this._callState.value = videoClient.getCall(callId) TODO - load details

        streamCalls.createCallClient(
            input.callUrl.removeSuffix("/twirp"),
            input.userToken,
            input.iceServers,
            credentialsProvider
        )
        _isVideoInitialized.value = true

        connectToCall(
            callSettings = CallSettings(
                audioOn = true,
                videoOn = false,
                speakerOn = true
            )
        )
    }

    private fun connectToCall(callSettings: CallSettings) {
        viewModelScope.launch {
            val callResult = streamCalls.connectToCall(
                UUID.randomUUID().toString(),
                true/*false*/,
                callSettings
            )

            when (callResult) {
                is Success -> {
                    val call = callResult.data
                    _callState.value = call

                    if (callSettings.videoOn) {
                        streamCalls.startCapturingLocalVideo(CameraMetadata.LENS_FACING_FRONT)
                    }
                }
                is Failure -> {
                    // TODO - show error to user
                }
            }
        }
    }

    public fun toggleCamera(enabled: Boolean) {
        streamCalls.setCameraEnabled(enabled)
    }

    public fun toggleMicrophone(enabled: Boolean) {
        streamCalls.setMicrophoneEnabled(enabled)
    }

    /**
     * Flips the camera for the current participant if possible.
     */
    public fun flipCamera() {
        streamCalls.flipCamera()
    }

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

    public fun showParticipants() {
        this._isShowingParticipantsInfo.value = true
    }

    public fun leaveCall() {
        viewModelScope.launch {
            logger.d { "[leaveCall] no args" }
            streamCalls.cancelCall(input.callCid)
        }
    }

    public fun getAudioDevices(): List<AudioDevice> {
        return streamCalls.getAudioDevices()
    }

    private fun clearState() {
        logger.i { "[leaveCall] no args" }
        streamCalls.clearCallState()
        viewModelScope.cancel()
        val room = _callState.value ?: return

        room.disconnect()
    }

    public fun dismissOptions() {
        this._isShowingSettings.value = false
        this._isShowingParticipantsInfo.value = false
    }

    public fun selectAudioDevice(device: AudioDevice) {
        streamCalls.selectAudioDevice(device)
    }
}
