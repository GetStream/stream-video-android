/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import dagger.hilt.android.lifecycle.HiltViewModel
import io.getstream.android.push.PushProvider
import io.getstream.chat.android.client.ChatClient
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.data.datasource.local.InMemoryStore
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.model.Device
import io.getstream.video.android.model.User
import io.getstream.video.android.model.mapper.isValidCallCid
import io.getstream.video.android.model.mapper.toTypeAndId
import io.getstream.video.android.tooling.util.StreamBuildFlavorUtil
import io.getstream.video.android.util.InitializedState
import io.getstream.video.android.util.NetworkMonitor
import io.getstream.video.android.util.StreamVideoInitHelper
import io.getstream.video.android.util.fcmToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CallJoinViewModel @Inject constructor(
    private val dataStore: StreamUserDataStore,
    private val googleSignInClient: GoogleSignInClient,
    private val inMemoryStore: InMemoryStore,
    networkMonitor: NetworkMonitor,
) : ViewModel() {
    val user: Flow<User?> = dataStore.user
    val isLoggedOut = MutableStateFlow(false)
    var autoLogInAfterLogOut = true
    val isNetworkAvailable = networkMonitor.isNetworkAvailable

    private val event: MutableSharedFlow<CallJoinEvent> = MutableSharedFlow()
    internal val uiState: SharedFlow<CallJoinUiState> = event
        .flatMapLatest { event ->
            when (event) {
                is CallJoinEvent.GoBackToLogin -> {
                    flowOf(CallJoinUiState.GoBackToLogin)
                }
                is CallJoinEvent.JoinCall -> {
                    val call = joinCall(event.callId)
                    if (call != null) {
                        flowOf(CallJoinUiState.JoinCompleted(callId = call.cid))
                    } else {
                        flowOf(CallJoinUiState.GoBackToLogin)
                    }
                }
                is CallJoinEvent.JoinCompleted -> flowOf(
                    CallJoinUiState.JoinCompleted(event.callId),
                )
                else -> flowOf(CallJoinUiState.Nothing)
            }
        }
        .shareIn(viewModelScope, SharingStarted.Lazily, 0)

    init {
        viewModelScope.launch {
            isNetworkAvailable.collect { isNetworkAvailable ->
                if (isNetworkAvailable && !StreamVideo.isInstalled) {
                    StreamVideoInitHelper.loadSdk(
                        dataStore = dataStore,
                    )
                }
            }
        }

        /**
         * For E2E Testing Only
         */
        if (StreamBuildFlavorUtil.isE2eTesting) {
            viewModelScope.launch {
                user
                    .filterNotNull()
                    .collectLatest { newUser ->
                        if (inMemoryStore.getUser() == null) {
                            inMemoryStore.saveUser(newUser)
                        }
                    }
            }
        }
    }

    fun handleUiEvent(event: CallJoinEvent) {
        viewModelScope.launch { this@CallJoinViewModel.event.emit(event) }
    }

    private suspend fun joinCall(callId: String? = null): Call? {
        // A fast re-login lands on this screen while StreamVideoInitHelper.loadSdk, started in
        // init, is still rebuilding the SDK, so the instance may not be installed yet at tap
        // time. loadSdk returns early when another initialization is already in flight, so the
        // helper's terminal state must be awaited before reading the instance.
        if (!StreamVideo.isInstalled) {
            StreamVideoInitHelper.loadSdk(dataStore = dataStore)
            StreamVideoInitHelper.initializedState.first {
                it == InitializedState.FINISHED || it == InitializedState.FAILED
            }
        }
        val streamVideo = StreamVideo.instanceOrNull() ?: return null
        val newCallId = callId ?: "default:${UUID.randomUUID()}"
        val (type, id) = if (newCallId.isValidCallCid()) {
            newCallId.toTypeAndId()
        } else {
            "default" to newCallId
        }
        return streamVideo.call(type = type, id = id)
    }

    fun logOut() {
        viewModelScope.launch {
            ChatClient.instance().disconnect(true).enqueue()
            dataStore.clear() // Demo App DataStore
            googleSignInClient.signOut()

            StreamVideo.instanceOrNull()?.let { streamVideo ->
                fcmToken?.let { fcmToken ->
                    streamVideo.deleteDevice(
                        Device(
                            id = fcmToken,
                            pushProvider = PushProvider.FIREBASE.key,
                            pushProviderName = "firebase",
                        ),
                    )
                }
                streamVideo.logOut()
            }

            StreamVideo.removeClient()

            isLoggedOut.value = true
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
