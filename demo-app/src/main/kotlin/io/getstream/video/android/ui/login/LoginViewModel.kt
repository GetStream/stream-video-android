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

package io.getstream.video.android.ui.login

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import dagger.hilt.android.lifecycle.HiltViewModel
import io.getstream.log.streamLog
import io.getstream.video.android.BuildConfig
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.data.repositories.GoogleAccountRepository
import io.getstream.video.android.data.services.stream.GetAuthDataResponse
import io.getstream.video.android.data.services.stream.StreamService
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.model.User
import io.getstream.video.android.util.StreamVideoInitHelper
import io.getstream.video.android.util.UserHelper
import io.getstream.video.android.util.config.AppConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val dataStore: StreamUserDataStore,
    private val googleSignInClient: GoogleSignInClient,
    private val googleAccountRepository: GoogleAccountRepository,
) : ViewModel() {
    var autoLogIn: Boolean = true

    private val event: MutableSharedFlow<LoginEvent> = MutableSharedFlow()
    internal val uiState: SharedFlow<LoginUiState> = event
        .flatMapLatest { event ->
            when (event) {
                is LoginEvent.Loading -> flowOf(LoginUiState.Loading)
                is LoginEvent.GoogleSignIn -> flowOf(
                    LoginUiState.GoogleSignIn(
                        signInIntent = googleSignInClient.signInIntent,
                    ),
                )

                is LoginEvent.SignInSuccess -> signInSuccess(event.userId)
                is LoginEvent.SignInFailure -> flowOf(
                    LoginUiState.SignInFailure(event.errorMessage),
                )
                is LoginEvent.SignIn -> signIn(event.user)
                else -> flowOf(LoginUiState.Nothing)
            }
        }.shareIn(viewModelScope, SharingStarted.Lazily, 0)

    fun handleUiEvent(event: LoginEvent) {
        viewModelScope.launch { this@LoginViewModel.event.emit(event) }
    }

    public fun reloadSdk() {
        viewModelScope.launch {
            StreamVideoInitHelper.reloadSdk(dataStore)
        }
    }

    private fun signInSuccess(userId: String): Flow<LoginUiState> = AppConfig.currentEnvironment.flatMapLatest {
        if (it != null) {
            if (StreamVideo.isInstalled) {
                flowOf(LoginUiState.AlreadyLoggedIn)
            } else {
                try {
                    val authData = StreamService.instance.getAuthData(
                        environment = it.env,
                        userId = userId,
                        StreamService.TOKEN_EXPIRY_TIME,
                    )
                    val loggedInGoogleUser =
                        if (autoLogIn) null else googleAccountRepository.getCurrentUser()
                    val user = User(
                        id = authData.userId,
                        // if autoLogIn is true it means we have a random user
                        name = if (autoLogIn) userId else loggedInGoogleUser?.name ?: "",
                        image = if (autoLogIn) "" else loggedInGoogleUser?.photoUrl ?: "",
                        role = "admin",
                        custom = mapOf("email" to authData.userId),
                    )
                    // Store the data in the demo app
                    dataStore.updateUser(user)
                    // Init the Video SDK with the data
                    StreamVideoInitHelper.loadSdk(dataStore)
                    flowOf(LoginUiState.SignInComplete(authData))
                } catch (exception: Throwable) {
                    val message = "Sign in failed: ${exception.message ?: "Generic error"}"
                    streamLog { "Failed to fetch token - cause: $exception" }
                    flowOf(LoginUiState.SignInFailure(message))
                }
            }
        } else {
            flowOf(LoginUiState.Loading)
        }
    }

    private fun signIn(user: User): Flow<LoginUiState> = AppConfig.currentEnvironment.flatMapLatest {
        if (it != null) {
            if (StreamVideo.isInstalled) {
                flowOf(LoginUiState.AlreadyLoggedIn)
            } else {
                try {
                    val authData = StreamService.instance.getAuthData(
                        environment = it.env,
                        userId = user.id,
                        StreamService.TOKEN_EXPIRY_TIME,
                    )
                    // Store the data in the demo app
                    dataStore.updateUser(user)
                    // Init the Video SDK with the data
                    StreamVideoInitHelper.loadSdk(dataStore)
                    flowOf(LoginUiState.SignInComplete(authData))
                } catch (exception: Throwable) {
                    val message = "Sign in failed: ${exception.message ?: "Generic error"}"
                    streamLog { "Failed to fetch token - cause: $exception" }
                    flowOf(LoginUiState.SignInFailure(message))
                }
            }
        } else {
            flowOf(LoginUiState.Loading)
        }
    }

    fun signInIfValidUserExist() {
        viewModelScope.launch {
            val user = dataStore.user.firstOrNull()
            if (user != null) {
                handleUiEvent(LoginEvent.Loading)
                if (BuildConfig.BUILD_TYPE != "benchmark") {
                    delay(10)
                    handleUiEvent(LoginEvent.SignInSuccess(userId = user.id))
                }
            } else {
                if (autoLogIn) {
                    handleUiEvent(LoginEvent.Loading)
                    handleUiEvent(
                        LoginEvent.SignInSuccess(
                            UserHelper.generateRandomString(upperCaseOnly = true),
                        ),
                    )
                }
            }
        }
    }
}

sealed interface LoginUiState {
    object Nothing : LoginUiState

    object Loading : LoginUiState

    object AlreadyLoggedIn : LoginUiState

    data class GoogleSignIn(val signInIntent: Intent) : LoginUiState

    data class SignInComplete(

        val authData: GetAuthDataResponse,
    ) : LoginUiState

    data class SignInFailure(val errorMessage: String) : LoginUiState
}

sealed interface LoginEvent {
    data object Nothing : LoginEvent

    data object Loading : LoginEvent

    data class GoogleSignIn(val id: String = UUID.randomUUID().toString()) : LoginEvent

    data class SignInSuccess(val userId: String) : LoginEvent

    data class SignInFailure(val errorMessage: String) : LoginEvent

    data class SignIn(val user: User) : LoginEvent
}
