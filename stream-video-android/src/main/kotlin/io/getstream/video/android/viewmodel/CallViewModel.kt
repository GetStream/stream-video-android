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
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.CallInput
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.*

public class CallViewModel(
    private val input: CallInput,
    private val streamVideo: StreamVideo,
    private val credentialsProvider: CredentialsProvider
) : ViewModel() {

    private val logger = StreamLog.getLogger("Call:ViewModel")

    private val _callState: MutableStateFlow<Call?> =
        MutableStateFlow(null)
    public val callState: StateFlow<Call?> = _callState

    private var _isVideoInitialized: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isVideoInitialized: StateFlow<Boolean> = _isVideoInitialized

    private val hasVideoPermission: MutableStateFlow<Boolean> =
        MutableStateFlow(input.hasVideoPermission)
    private val isVideoEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)

    public val isVideoOn: Flow<Boolean> =
        hasVideoPermission.combine(isVideoEnabled) { hasPermission, videoEnabled ->
            hasPermission && videoEnabled
        }

    private val hasAudioPermission: MutableStateFlow<Boolean> =
        MutableStateFlow(input.hasAudioPermission)
    private val isAudioEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)

    public val isAudioOn: Flow<Boolean> =
        hasAudioPermission.combine(isAudioEnabled) { hasPermission, audioEnabled ->
            hasPermission && audioEnabled
        }

    public val participantList: Flow<List<CallParticipantState>> =
        callState.filterNotNull().flatMapLatest { it.callParticipants }

    public val activeSpeakers: Flow<List<CallParticipantState>> =
        callState.filterNotNull().flatMapLatest { it.activeSpeakers }

    public val localParticipant: Flow<CallParticipantState> =
        callState.filterNotNull().flatMapLatest { it.localParticipant }

    private val _isShowingParticipantsInfo = MutableStateFlow(false)
    public val isShowingParticipantsInfo: StateFlow<Boolean> = _isShowingParticipantsInfo

    private val _isShowingSettings = MutableStateFlow(false)
    public val isShowingSettings: StateFlow<Boolean> = _isShowingSettings

    public val streamCallState: StateFlow<StreamCallState> get() = streamVideo.callState

    private lateinit var client: CallClient

    init {
        viewModelScope.launch {
            streamVideo.callState.collect {
                when (it) {
                    is StreamCallState.Idle -> {
                        logger.i { "[observeState] state: Idle" }
                        clearState()
                    }
                    else -> { /* no-op */
                    }
                }
            }
        }
    }

    public fun connectToCall(callSettings: CallSettings) {
        logger.d { "[createCall] input: $input" }
        // this._callState.value = videoClient.getCall(callId) TODO - load details

        // TODO CallClient is supposed to live longer than VM
        //  VM can be destroyed while the call is still running in the BG
        client = streamVideo.createCallClient(
            input.callUrl.removeSuffix("/twirp"),
            input.userToken,
            input.iceServers,
            credentialsProvider
        )
        _isVideoInitialized.value = true

        initializeCall(callSettings = callSettings)
    }

    private fun initializeCall(callSettings: CallSettings) {
        viewModelScope.launch {
            val callResult = client.connectToCall(
                UUID.randomUUID().toString(),
                callSettings
            )

            when (callResult) {
                is Success -> {
                    val call = callResult.data
                    _callState.value = call
                    isVideoEnabled.value = callSettings.videoOn
                    isAudioEnabled.value = callSettings.audioOn

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
    }

    public fun toggleCamera(enabled: Boolean) {
        client.setCameraEnabled(enabled)
    }

    public fun toggleMicrophone(enabled: Boolean) {
        client.setMicrophoneEnabled(enabled)
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

    /**
     * Drops the call by sending a cancel event, which informs other users.
     */
    public fun cancelCall() {
        viewModelScope.launch {
            logger.d { "[leaveCall] no args" }
            streamVideo.cancelCall(input.callCid)
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
        logger.i { "[leaveCall] no args" }
        streamVideo.clearCallState()
        viewModelScope.cancel()
        val room = _callState.value ?: return

        room.disconnect()
        _callState.value = null
        isVideoEnabled.value = false
        isAudioEnabled.value = false
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
}
