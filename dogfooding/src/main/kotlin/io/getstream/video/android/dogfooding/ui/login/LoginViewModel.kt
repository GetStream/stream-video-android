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

package io.getstream.video.android.dogfooding.ui.login

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import io.getstream.log.Priority
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.dogfooding.API_KEY
import io.getstream.video.android.dogfooding.BuildConfig
import io.getstream.video.android.dogfooding.dogfoodingApp
import io.getstream.video.android.dogfooding.token.StreamVideoNetwork
import io.getstream.video.android.dogfooding.token.TokenResponse
import io.getstream.video.android.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val dataStore: StreamUserDataStore
) : ViewModel() {

    private val event: MutableSharedFlow<LoginEvent> = MutableSharedFlow()
    internal val uiState: SharedFlow<LoginUiState> = event
        .flatMapLatest { event ->
            when (event) {
                is LoginEvent.Loading -> flowOf(LoginUiState.Loading)
                is LoginEvent.GoogleSignIn -> flowOf(LoginUiState.GoogleSignIn)
                is LoginEvent.SignInInSuccess -> signInInSuccess(event.email)
                else -> flowOf(LoginUiState.Nothing)
            }
        }.shareIn(viewModelScope, SharingStarted.Lazily, 0)

    fun handleUiEvent(event: LoginEvent) {
        viewModelScope.launch { this@LoginViewModel.event.emit(event) }
    }

    private fun signInInSuccess(email: String) = flow<LoginUiState> {

        try {
            val response = StreamVideoNetwork.tokenService.fetchToken(
                userId = email,
                apiKey = API_KEY
            )
            emit(LoginUiState.SignInComplete(response))
        } catch (exception: Throwable) {
            emit(LoginUiState.SignInFailure(exception.message ?: "General error"))
            Log.e("LoginViewModel", "Failed to fetch token - cause: $exception")
        }
    }.flowOn(Dispatchers.IO)

    init {
        sigInInIfValidUserExist()
    }

    fun sigInInIfValidUserExist() {
        viewModelScope.launch {
            dataStore.user.collectLatest { user ->
                if (user != null && user.isValid() && !BuildConfig.BENCHMARK) {
                    handleUiEvent(LoginEvent.Loading)
                    delay(10)
                    handleUiEvent(LoginEvent.SignInInSuccess(email = user.id))
                }
            }
        }
    }

    fun initializeStreamVideo(
        context: Context,
        tokenResponse: TokenResponse,
    ) {
        val authUser = FirebaseAuth.getInstance().currentUser
        val userId = tokenResponse.userId
        val token = tokenResponse.token
        val user = User(
            id = authUser?.email ?: userId,
            name = authUser?.displayName ?: "",
            image = authUser?.photoUrl?.toString() ?: "",
            role = "admin",
            custom = mapOf("email" to userId)
        )

        context.dogfoodingApp.initializeStreamVideo(
            apiKey = API_KEY,
            user = user,
            loggingLevel = LoggingLevel(priority = Priority.DEBUG),
            token = token
        )
    }
}

sealed interface LoginUiState {
    object Nothing : LoginUiState

    object Loading : LoginUiState

    object GoogleSignIn : LoginUiState

    data class SignInComplete(val tokenResponse: TokenResponse) : LoginUiState

    data class SignInFailure(val errorMessage: String) : LoginUiState
}

sealed interface LoginEvent {
    object Nothing : LoginEvent

    object Loading : LoginEvent

    data class GoogleSignIn(val id: String = UUID.randomUUID().toString()) : LoginEvent

    data class SignInInSuccess(val email: String) : LoginEvent
}
