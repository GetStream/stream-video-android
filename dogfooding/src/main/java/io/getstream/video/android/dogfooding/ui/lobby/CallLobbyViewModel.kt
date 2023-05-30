/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.dogfooding.ui.lobby

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.getstream.result.Result
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CreateCallOptions
import io.getstream.video.android.core.DeviceStatus
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.state.CallDeviceState
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CallLobbyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    dataStore: StreamUserDataStore
) : ViewModel() {

    private val cid: String = checkNotNull(savedStateHandle["cid"])
    val callId: StreamCallId = StreamCallId.fromCallCid(cid)

    val call: Call by lazy {
        val streamVideo = StreamVideo.instance()
        streamVideo.call(type = callId.type, id = callId.id)
    }

    val user: User? = dataStore.user.value

    val deviceState: StateFlow<CallDeviceState> =
        combine(
            call.camera.status,
            call.microphone.status,
            call.speaker.status
        ) { cameraEnabled, microphoneEnabled, speakerphoneEnabled ->
            CallDeviceState(
                isCameraEnabled = cameraEnabled is DeviceStatus.Enabled,
                isMicrophoneEnabled = microphoneEnabled is DeviceStatus.Enabled,
                isSpeakerphoneEnabled = speakerphoneEnabled is DeviceStatus.Enabled
            )
        }.stateIn(viewModelScope, SharingStarted.Lazily, CallDeviceState())

    private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    internal val isLoading: StateFlow<Boolean> = _isLoading

    private val event: MutableStateFlow<CallLobbyEvent> = MutableStateFlow(CallLobbyEvent.Nothing)
    internal val uiState: StateFlow<CallLobbyUiState> = event
        .flatMapLatest { event ->
            when (event) {
                is CallLobbyEvent.JoinCall -> {
                    _isLoading.value = true
                    val result = joinCall()

                    if (result.isSuccess) {
                        flowOf(CallLobbyUiState.JoinCompleted)
                    } else {
                        flowOf(CallLobbyUiState.JoinFailed(result.errorOrNull()?.message.orEmpty()))
                    }
                }

                else -> flowOf(CallLobbyUiState.Nothing)
            }
        }
        .onCompletion { _isLoading.value = false }
        .stateIn(viewModelScope, SharingStarted.Lazily, CallLobbyUiState.Nothing)

    fun handleUiEvent(event: CallLobbyEvent) {
        this.event.value = event
    }

    fun enableCamera(enabled: Boolean) {
        call.camera.setEnabled(enabled)
    }

    fun enableMicrophone(enabled: Boolean) {
        call.microphone.setEnabled(enabled)
    }

    fun enableSpeakerphone(enabled: Boolean) {
        call.speaker.setEnabled(enabled)
    }

    private suspend fun joinCall(): Result<RtcSession> {
        val streamVideo = StreamVideo.instance()
        return call.join(
            create = true,
            createOptions = CreateCallOptions(memberIds = listOf(streamVideo.userId))
        )
    }

    fun signOut() {
        StreamVideo.instance().logOut()
    }
}

sealed interface CallLobbyUiState {
    object Nothing : CallLobbyUiState

    object JoinCompleted : CallLobbyUiState

    data class JoinFailed(val reason: String?) : CallLobbyUiState
}

sealed interface CallLobbyEvent {
    object Nothing : CallLobbyEvent

    object JoinCall : CallLobbyEvent
}
