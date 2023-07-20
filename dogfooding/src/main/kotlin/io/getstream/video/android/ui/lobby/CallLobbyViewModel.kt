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

package io.getstream.video.android.ui.lobby

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import io.getstream.chat.android.client.ChatClient
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
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
        val call = streamVideo.call(type = callId.type, id = callId.id)
        // start listening to the call events to get the participant count
        viewModelScope.launch {
            call.get()
        }
        call
    }

    val user: User? = dataStore.user.value
    val isLoggedOut = dataStore.user.map { it == null }

    private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    internal val isLoading: StateFlow<Boolean> = _isLoading

    private val event: MutableSharedFlow<CallLobbyEvent> = MutableSharedFlow()
    internal val uiState: SharedFlow<CallLobbyUiState> = event
        .flatMapLatest { event ->
            when (event) {
                is CallLobbyEvent.JoinCall -> flowOf(CallLobbyUiState.JoinCompleted)

                else -> flowOf(CallLobbyUiState.Nothing)
            }
        }
        .onCompletion { _isLoading.value = false }
        .shareIn(viewModelScope, SharingStarted.Lazily, 0)

    fun handleUiEvent(event: CallLobbyEvent) {
        viewModelScope.launch { this@CallLobbyViewModel.event.emit(event) }
    }

    fun enableCamera(enabled: Boolean) {
        call.camera.setEnabled(enabled)
    }

    fun enableMicrophone(enabled: Boolean) {
        call.microphone.setEnabled(enabled)
    }

    fun signOut() {
        viewModelScope.launch {
            FirebaseAuth.getInstance().signOut()
            StreamVideo.instance().logOut()
            ChatClient.instance().disconnect(true).enqueue()
            delay(200)
            StreamVideo.removeClient()
        }
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
