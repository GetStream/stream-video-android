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

package io.getstream.video.android.dogfooding.ui.join

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.model.User
import io.getstream.video.android.model.mapper.isValidCallId
import io.getstream.video.android.model.mapper.toTypeAndId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CallJoinViewModel @Inject constructor(
    dataStore: StreamUserDataStore,
) : ViewModel() {

    val user: StateFlow<User?> = dataStore.user

    private val event: MutableStateFlow<CallJoinEvent> = MutableStateFlow(CallJoinEvent.Nothing)
    internal val uiState: StateFlow<CallJoinUiState> = event
        .flatMapLatest { event ->
            when (event) {
                is CallJoinEvent.JoinCall -> {
                    val call = joinCall(event.callId)
                    flowOf(CallJoinUiState.JoinCompleted(callId = call.cid))
                }

                is CallJoinEvent.JoinCompleted -> flowOf(CallJoinUiState.JoinCompleted(event.callId))
                else -> flowOf(CallJoinUiState.Nothing)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, CallJoinUiState.Nothing)

    fun handleUiEvent(event: CallJoinEvent) {
        this.event.value = event
    }

    private fun joinCall(callId: String? = null): Call {
        val streamVideo = StreamVideo.instance()
        val newCallId = callId ?: "default:${UUID.randomUUID()}"
        val (type, id) = if (newCallId.isValidCallId()) {
            newCallId.toTypeAndId()
        } else {
            "default" to newCallId
        }
        return streamVideo.call(type = type, id = id)
    }

    fun signOut() {
        StreamVideo.instance().logOut()
    }
}

sealed interface CallJoinUiState {
    object Nothing : CallJoinUiState

    data class JoinCompleted(val callId: String) : CallJoinUiState
}

sealed interface CallJoinEvent {
    object Nothing : CallJoinEvent

    data class JoinCall(val callId: String? = null) : CallJoinEvent

    data class JoinCompleted(val callId: String) : CallJoinEvent
}
