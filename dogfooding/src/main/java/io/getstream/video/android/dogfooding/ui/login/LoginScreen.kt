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

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.firebase.ui.auth.AuthUI
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.dogfooding.R
import io.getstream.video.android.dogfooding.ui.theme.Colors
import io.getstream.video.android.dogfooding.ui.theme.StreamButton

@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = hiltViewModel(),
    navigateToCallJoin: () -> Unit
) {
    val uiState by loginViewModel.uiState.collectAsState()
    val isLoading by remember(uiState) { mutableStateOf(uiState !is LoginUiState.Nothing) }

    HandleLoginUiStates(loginUiState = uiState, navigateToCallJoin = navigateToCallJoin)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.Background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier.size(102.dp),
            painter = painterResource(id = R.drawable.ic_stream_video_meeting),
            contentDescription = null
        )

        Spacer(modifier = Modifier.height(27.dp))

        Text(
            text = stringResource(id = R.string.stream_meetings),
            color = Color.White,
            fontSize = 38.sp
        )

        Spacer(modifier = Modifier.height(17.dp))

        Text(
            text = stringResource(id = R.string.stream_sign_in_description),
            color = Colors.Description,
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
        )

        Spacer(modifier = Modifier.height(50.dp))

        StreamButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 55.dp),
            enabled = !isLoading,
            text = stringResource(id = R.string.stream_sign_in_google),
            onClick = { loginViewModel.handleUiEvent(LoginEvent.GoogleSignIn()) }
        )

        StreamButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 55.dp),
            enabled = !isLoading,
            text = stringResource(id = R.string.stream_sign_in_email),
            onClick = {}
        )

        Spacer(modifier = Modifier.height(47.dp))

        Text(
            text = stringResource(id = R.string.stream_sign_in_contact),
            color = Colors.Description,
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
        )
    }
}

@Composable
private fun HandleLoginUiStates(
    loginUiState: LoginUiState,
    navigateToCallJoin: () -> Unit,
    loginViewModel: LoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val signInLauncher = rememberRegisterForActivityResult(
        onSignInSuccess = { email ->
            loginViewModel.handleUiEvent(LoginEvent.SignInInSuccess(email = email))
        },
        onSignInFailed = {
            Toast.makeText(context, "Verification failed!", Toast.LENGTH_SHORT).show()
        }
    )

    LaunchedEffect(key1 = Unit) {
        loginViewModel.sigInInIfValidUserExist(context)
    }

    LaunchedEffect(key1 = loginUiState) {
        when (loginUiState) {
            is LoginUiState.GoogleSignIn -> {
                val providers = arrayListOf(
                    AuthUI.IdpConfig.GoogleBuilder().build()
                )

                val signInIntent = AuthUI.getInstance().createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .build()
                signInLauncher.launch(signInIntent)
            }

            is LoginUiState.SignInComplete -> {
                loginViewModel.initializeStreamVideo(
                    context = context,
                    tokenResponse = loginUiState.tokenResponse
                )

                navigateToCallJoin.invoke()
            }

            else -> Unit
        }
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    VideoTheme {
        LoginScreen {}
    }
}
