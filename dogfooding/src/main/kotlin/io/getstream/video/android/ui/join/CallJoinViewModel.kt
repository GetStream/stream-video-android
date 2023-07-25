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

package io.getstream.video.android.ui.join

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import io.getstream.chat.android.client.ChatClient
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.model.User
import io.getstream.video.android.model.mapper.isValidCallId
import io.getstream.video.android.model.mapper.toTypeAndId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CallJoinViewModel @Inject constructor(
    dataStore: StreamUserDataStore,
) : ViewModel() {
    val user: Flow<User?> = dataStore.user
    val isLoggedOut = dataStore.user.map { it == null }

    private val event: MutableSharedFlow<CallJoinEvent> = MutableSharedFlow()
    internal val uiState: SharedFlow<CallJoinUiState> = event
        .flatMapLatest { event ->
            when (event) {
                is CallJoinEvent.GoBackToLogin -> {
                    flowOf(CallJoinUiState.GoBackToLogin)
                }

                is CallJoinEvent.JoinCall -> {
                    val call = joinCall(event.callId)
                    flowOf(CallJoinUiState.JoinCompleted(callId = call.cid))
                }

                is CallJoinEvent.JoinCompleted -> flowOf(CallJoinUiState.JoinCompleted(event.callId))
                else -> flowOf(CallJoinUiState.Nothing)
            }
        }
        .shareIn(viewModelScope, SharingStarted.Lazily, 0)

    init {
        viewModelScope.launch {
            // We need to check whether the StreamVideo instance is initialised and go back to Login
            // if not. In the current implementation we only initialise after Login and if the
            // Android process is restored then the Login is skipped Stream Video is not initialised.
            if (!StreamVideo.isInstalled) {
                event.emit(CallJoinEvent.GoBackToLogin)
            }
        }
    }

    fun handleUiEvent(event: CallJoinEvent) {
        viewModelScope.launch { this@CallJoinViewModel.event.emit(event) }
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
        viewModelScope.launch {
            FirebaseAuth.getInstance().signOut()
            StreamVideo.instance().logOut()
            ChatClient.instance().disconnect(true).enqueue()
            delay(200)
            StreamVideo.removeClient()
        }
    }
}

sealed interface CallJoinUiState {
    object Nothing : CallJoinUiState

    data class JoinCompleted(val callId: String) : CallJoinUiState

    object GoBackToLogin : CallJoinUiState
}

sealed interface CallJoinEvent {
    object Nothing : CallJoinEvent

    data class JoinCall(val callId: String? = null) : CallJoinEvent

    data class JoinCompleted(val callId: String) : CallJoinEvent

    object GoBackToLogin : CallJoinEvent
}
