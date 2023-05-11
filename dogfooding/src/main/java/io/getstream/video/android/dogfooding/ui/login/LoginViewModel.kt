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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.user.UserPreferences
import io.getstream.video.android.dogfooding.API_KEY
import io.getstream.video.android.dogfooding.BuildConfig
import io.getstream.video.android.dogfooding.dogfoodingApp
import io.getstream.video.android.dogfooding.token.StreamVideoNetwork
import io.getstream.video.android.dogfooding.token.TokenResponse
import io.getstream.video.android.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val event: MutableStateFlow<LoginEvent> = MutableStateFlow(LoginEvent.Nothing)
    internal val uiState: StateFlow<LoginUiState> = event
        .flatMapLatest { event ->
            when (event) {
                is LoginEvent.Loading -> flowOf(LoginUiState.Loading)
                is LoginEvent.GoogleSignIn -> flowOf(LoginUiState.GoogleSignIn)
                is LoginEvent.SignInInSuccess -> signInInSuccess(event.email)
                else -> flowOf(LoginUiState.Nothing)
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, LoginUiState.Nothing)

    fun handleUiEvent(event: LoginEvent) {
        this.event.value = event
    }

    private fun signInInSuccess(email: String) = flow<LoginUiState> {
        val response = StreamVideoNetwork.tokenService.fetchToken(
            userId = email,
            apiKey = API_KEY
        )
        emit(LoginUiState.SignInComplete(response))
    }.flowOn(Dispatchers.IO)

    init {
        val user = userPreferences.getUserCredentials()

        if (user != null) {
            handleUiEvent(LoginEvent.Loading)
            sigInInIfValidUserExist()
        }
    }

    fun sigInInIfValidUserExist() {
        val user = userPreferences.getUserCredentials()

        if (user != null && user.isValid() && !BuildConfig.BENCHMARK) {
            handleUiEvent(LoginEvent.SignInInSuccess(email = user.id))
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

        userPreferences.storeUserCredentials(user)
        userPreferences.storeUserToken(token)

        context.dogfoodingApp.initializeStreamVideo(
            apiKey = API_KEY,
            user = user,
            loggingLevel = LoggingLevel.BODY,
            token = token
        )
    }
}

sealed interface LoginUiState {
    object Nothing : LoginUiState

    object Loading : LoginUiState

    object GoogleSignIn : LoginUiState

    data class SignInComplete(val tokenResponse: TokenResponse) : LoginUiState
}

sealed interface LoginEvent {
    object Nothing : LoginEvent

    object Loading : LoginEvent

    data class GoogleSignIn(val id: String = UUID.randomUUID().toString()) : LoginEvent

    data class SignInInSuccess(val email: String) : LoginEvent
}
