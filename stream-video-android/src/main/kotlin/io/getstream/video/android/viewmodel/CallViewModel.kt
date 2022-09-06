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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.getstream.video.android.client.VideoClient
import io.getstream.video.android.model.CallParticipant
import io.getstream.video.android.model.CallParticipantState
import io.getstream.video.android.model.Room
import io.getstream.video.android.webrtc.WebRTCClient
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import stream.video.Call
import stream.video.UserEventType
import java.util.*

public class CallViewModel(private val videoClient: VideoClient) : ViewModel() {

    private val webRTCClient: WebRTCClient
        get() = videoClient.webRTCClient

    private val _roomState: MutableStateFlow<Room?> = MutableStateFlow(null)
    public val roomState: StateFlow<Room?> = _roomState

    private val _urlState: MutableStateFlow<String> = MutableStateFlow("")
    public val urlState: StateFlow<String> = _urlState

    private val _tokenState: MutableStateFlow<String> = MutableStateFlow("")
    public val tokenState: StateFlow<String> = _tokenState

    private val _callState: MutableStateFlow<Call?> = MutableStateFlow(null)
    public val callState: StateFlow<Call?> = _callState

    private var _localParticipantState: MutableStateFlow<CallParticipant?> = MutableStateFlow(null)
    public val localParticipant: Flow<CallParticipant> = _localParticipantState.filterNotNull()

    private var _isVideoInitialized: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isVideoInitialized: StateFlow<Boolean> = _isVideoInitialized

    private val _isCameraEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isCameraEnabled: Flow<Boolean> = _isCameraEnabled

    private val _isMicrophoneEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isMicrophoneEnabled: Flow<Boolean> = _isMicrophoneEnabled

    public val participantList: Flow<List<CallParticipant>> =
        roomState.filterNotNull().flatMapLatest { it.callParticipants }

    public val participantsState: Flow<List<CallParticipantState>> =
        roomState.filterNotNull().flatMapLatest { it.callParticipantState }

    public val activeSpeakers: Flow<List<CallParticipant>> = participantList.map { list ->
        list.filter { participant -> participant.hasAudio }
    }

    private val _primarySpeaker = MutableStateFlow<CallParticipant?>(null)
    public val primarySpeaker: StateFlow<CallParticipant?> = _primarySpeaker

    private val _isShowingParticipantsInfo = MutableStateFlow(false)
    public val isShowingParticipantsInfo: StateFlow<Boolean> = _isShowingParticipantsInfo

    public fun init(
        call: Call,
        url: String,
        token: String
    ) {
        this._urlState.value = url
        this._tokenState.value = token
        this._callState.value = call

        viewModelScope.launch {
            _isVideoInitialized.value = true

            combine(
                participantList,
                activeSpeakers,
            ) { participants, speakers -> participants to speakers }
                .collect { (participants, speakers) ->
                    handlePrimarySpeaker(
                        participants,
                        speakers,
                    )
                }
        }

        // TODO - session ID
        _roomState.value = webRTCClient.joinCall(UUID.randomUUID().toString(), true)
    }

    private fun setupLocalParticipant(localParticipant: CallParticipant) {
        viewModelScope.launch {
//            localParticipant.setMicrophoneEnabled(true)
            _isMicrophoneEnabled.value = true
            videoClient.sendUserEvent(UserEventType.USER_EVENT_TYPE_AUDIO_UNMUTED)

//            localParticipant.setCameraEnabled(true)
            _isCameraEnabled.value = true
            videoClient.sendUserEvent(UserEventType.USER_EVENT_TYPE_VIDEO_STARTED)
        }
    }

    private fun handlePrimarySpeaker(
        participantsList: List<CallParticipant>,
        speakers: List<CallParticipant>,
    ) {

        var speaker = _primarySpeaker.value
        val localParticipant = participantsList.firstOrNull { it.isLocal }

        if (speaker?.isLocal == true) {
            val remoteSpeaker = // Try not to display local participant as speaker.
                participantsList.firstOrNull { !it.isLocal }

            if (remoteSpeaker != null) {
                speaker = remoteSpeaker
            }
        }

        // If previous primary speaker leaves
        if (!participantsList.contains(speaker)) {
            // Default to another person in room, or local participant.
            speaker = participantsList.firstOrNull { !it.isLocal }
                ?: localParticipant
        }

        if (speakers.isNotEmpty() && !speakers.contains(speaker)) {
            val remoteSpeaker = // Try not to display local participant as speaker.
                speakers.firstOrNull { !it.isLocal }

            if (remoteSpeaker != null) {
                speaker = remoteSpeaker
            }
        }

        _primarySpeaker.value = speaker
    }

    public fun toggleCamera(enabled: Boolean) {
        val participant = _localParticipantState.value ?: return

        viewModelScope.launch {

            _isCameraEnabled.value = enabled
            val event =
                if (enabled) UserEventType.USER_EVENT_TYPE_VIDEO_STARTED
                else UserEventType.USER_EVENT_TYPE_VIDEO_STOPPED
            videoClient.sendUserEvent(event)
        }
    }

    public fun toggleMicrophone(enabled: Boolean) {
        val participant = _localParticipantState.value ?: return

        viewModelScope.launch {
//            participant.setMicrophoneEnabled(enabled)
            _isMicrophoneEnabled.value = enabled

            val event =
                if (enabled) UserEventType.USER_EVENT_TYPE_AUDIO_UNMUTED
                else UserEventType.USER_EVENT_TYPE_AUDIO_MUTED_UNSPECIFIED
            videoClient.sendUserEvent(event)
        }
    }

    /**
     * Flips the camera for the current participant if possible.
     */
    public fun flipCamera() {
//        val room = _roomState.value ?: return
//
//        room.flipCamera()
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
//        val room = _roomState.value ?: return
//
//        room.disconnect()
    }

    public fun showParticipants() {
        this._isShowingParticipantsInfo.value = true
    }

    public fun hideParticipants() {
        this._isShowingParticipantsInfo.value = false
    }

    public fun leaveCall() {
//        val currentRoom = _roomState.value

//        currentRoom?.disconnect()
        this.videoClient.leaveCall()
        viewModelScope.cancel()
    }

    public fun startCapturingLocalVideo() {
        // videoClient.webRTCClient.startCapturingLocalVideo(0) TODO - call this on the room
    }
}
