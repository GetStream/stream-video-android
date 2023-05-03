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
import io.getstream.result.Result
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.model.mapper.toTypeAndId
import io.getstream.video.android.core.user.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import org.openapitools.client.models.GetOrCreateCallResponse
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class CallJoinViewModel @Inject constructor(
    userPreferences: UserPreferences
) : ViewModel() {

    val user: User? = userPreferences.getUserCredentials()

    private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    internal val isLoading: StateFlow<Boolean> = _isLoading

    private val event: MutableStateFlow<CallJoinEvent> = MutableStateFlow(CallJoinEvent.Nothing)
    internal val uiState: StateFlow<CallJoinUiState> = event
        .flatMapLatest { event ->
            when (event) {
                is CallJoinEvent.CreateCall -> {
                    val result = startNewCall()

                    if (result.isSuccess) {
                        val cid = result.getOrThrow().call.cid
                        flowOf(CallJoinUiState.JoinCompletedUi(callId = cid))
                    } else {
                        flowOf(CallJoinUiState.JoiningFailed(result.errorOrNull()?.message.orEmpty()))
                    }
                }

                is CallJoinEvent.JoinCall -> {
                    val result = joinCall(event.callId)

                    if (result.isSuccess) {
                        flowOf(CallJoinUiState.JoinCompletedUi(callId = event.callId))
                    } else {
                        flowOf(CallJoinUiState.JoiningFailed(result.errorOrNull()?.message.orEmpty()))
                    }
                }

                is CallJoinEvent.JoinCompleted -> flowOf(CallJoinUiState.JoinCompletedUi(event.callId))
                else -> flowOf(CallJoinUiState.Nothing)
            }
        }.onStart { _isLoading.value = true }
        .onCompletion { _isLoading.value = false }
        .stateIn(viewModelScope, SharingStarted.Lazily, CallJoinUiState.Nothing)

    fun handleUiEvent(event: CallJoinEvent) {
        this.event.value = event
    }

    private suspend fun startNewCall(): Result<GetOrCreateCallResponse> {
        val streamVideo = StreamVideo.instance()
        val callId = "default:NnXAIvBKE4Hy" + Random.nextInt(10000)
        val (type, id) = callId.toTypeAndId()
        val call = streamVideo.call(type = type, id = id)
        return call.create(memberIds = listOf(streamVideo.userId))
    }

    private suspend fun joinCall(callId: String): Result<RtcSession> {
        val streamVideo = StreamVideo.instance()
        val (type, id) = callId.toTypeAndId()
        val call = streamVideo.call(type = type, id = id)
        return call.join()
    }

    fun signOut() {
        StreamVideo.instance().logOut()
    }
}

sealed interface CallJoinUiState {
    object Nothing : CallJoinUiState

    data class JoiningFailed(val reason: String) : CallJoinUiState

    data class JoinCompletedUi(val callId: String) : CallJoinUiState
}

sealed interface CallJoinEvent {
    object Nothing : CallJoinEvent

    object CreateCall : CallJoinEvent

    data class JoinCall(val callId: String) : CallJoinEvent

    data class JoinCompleted(val callId: String) : CallJoinEvent
}
