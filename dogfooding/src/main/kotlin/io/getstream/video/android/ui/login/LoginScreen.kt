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

@file:OptIn(ExperimentalComposeUiApi::class)

package io.getstream.video.android.ui.login

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat.startActivity
import androidx.hilt.navigation.compose.hiltViewModel
import io.getstream.video.android.BuildConfig
import io.getstream.video.android.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.ui.theme.Colors
import io.getstream.video.android.ui.theme.LinkText
import io.getstream.video.android.ui.theme.LinkTextData
import io.getstream.video.android.ui.theme.StreamButton
import io.getstream.video.android.util.GoogleSignInHelper
import io.getstream.video.android.util.UserIdHelper

@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = hiltViewModel(),
    navigateToCallJoin: () -> Unit,
) {
    val uiState by loginViewModel.uiState.collectAsState(initial = LoginUiState.Nothing)
    val isLoading by remember(uiState) {
        mutableStateOf(uiState !is LoginUiState.Nothing && uiState !is LoginUiState.SignInFailure)
    }
    var isShowingEmailLoginDialog by remember { mutableStateOf(false) }

    HandleLoginUiStates(loginUiState = uiState, navigateToCallJoin = navigateToCallJoin)

    LoginContent(
        isLoading = isLoading,
        showEmailLoginDialog = { isShowingEmailLoginDialog = true },
    )

    if (isShowingEmailLoginDialog) {
        EmailLoginDialog(
            onDismissRequest = { isShowingEmailLoginDialog = false },
        )
    }
}

@Composable
private fun LoginContent(
    isLoading: Boolean,
    showEmailLoginDialog: () -> Unit,
    loginViewModel: LoginViewModel = hiltViewModel(),
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Colors.background)
                .semantics { testTagsAsResourceId = true },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                modifier = Modifier.size(102.dp),
                painter = painterResource(id = R.drawable.ic_stream_video_meeting_logo),
                contentDescription = null,
            )

            Spacer(modifier = Modifier.height(27.dp))

            Text(
                text = stringResource(id = R.string.app_name),
                color = Color.White,
                fontSize = 38.sp,
            )

            if (BuildConfig.FLAVOR != "production") {
                Spacer(modifier = Modifier.height(17.dp))

                Text(
                    text = stringResource(id = R.string.sign_in_description),
                    color = Colors.description,
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                )

                Spacer(modifier = Modifier.height(50.dp))

                StreamButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 55.dp),
                    enabled = !isLoading,
                    text = stringResource(id = R.string.sign_in_google),
                    onClick = { loginViewModel.handleUiEvent(LoginEvent.GoogleSignIn()) },
                )

                StreamButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 55.dp),
                    enabled = !isLoading,
                    text = stringResource(id = R.string.sign_in_email),
                    onClick = { showEmailLoginDialog.invoke() },
                )
            }

            Spacer(modifier = Modifier.height(47.dp))

            val context = LocalContext.current
            LinkText(
                linkTextData = listOf(
                    LinkTextData(text = stringResource(id = R.string.sign_in_contact)),
                    LinkTextData(
                        text = stringResource(
                            id = R.string.sign_in_contact_us,
                        ),
                        tag = "contact us",
                        annotation = "https://getstream.io/video/docs/",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse(it.item)
                            startActivity(context, intent, null)
                        },
                    ),
                ),
            )

            if (BuildConfig.BENCHMARK.toBoolean()) {
                StreamButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 55.dp)
                        .testTag("authenticate"),
                    text = "Login for Benchmark",
                    onClick = {
                        loginViewModel.handleUiEvent(
                            LoginEvent.SignInSuccess("benchmark.test@getstream.io"),
                        )
                    },
                )
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = VideoTheme.colors.primaryAccent,
            )
        }
    }
}

@Composable
private fun EmailLoginDialog(
    onDismissRequest: () -> Unit,
    loginViewModel: LoginViewModel = hiltViewModel(),
) {
    var email by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = { onDismissRequest.invoke() },
        content = {
            Surface(
                modifier = Modifier.width(300.dp),
            ) {
                Column(modifier = Modifier.background(Colors.background)) {
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(text = "Enter your e-mail address") },
                        colors = TextFieldDefaults.textFieldColors(
                            textColor = VideoTheme.colors.textLowEmphasis,
                            focusedLabelColor = VideoTheme.colors.primaryAccent,
                            unfocusedIndicatorColor = VideoTheme.colors.primaryAccent,
                            focusedIndicatorColor = VideoTheme.colors.primaryAccent,
                            placeholderColor = Color.White,
                        ),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Email,
                        ),
                    )

                    StreamButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        onClick = {
                            val userId = UserIdHelper.getUserIdFromEmail(email)
                            loginViewModel.handleUiEvent(LoginEvent.SignInSuccess(userId))
                        },
                        text = "Log in",
                    )
                }
            }
        },
    )
}

@Composable
private fun HandleLoginUiStates(
    loginUiState: LoginUiState,
    navigateToCallJoin: () -> Unit,
    loginViewModel: LoginViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val signInLauncher = rememberLauncherForGoogleSignInActivityResult(
        onSignInSuccess = { email ->
            val userId = UserIdHelper.getUserIdFromEmail(email)
            loginViewModel.handleUiEvent(LoginEvent.SignInSuccess(userId = userId))
        },
        onSignInFailed = {
            loginViewModel.handleUiEvent(LoginEvent.Nothing)
            Toast.makeText(context, "Verification failed!", Toast.LENGTH_SHORT).show()
        },
    )

    LaunchedEffect(key1 = Unit) {
        loginViewModel.signInIfValidUserExist()
    }

    LaunchedEffect(key1 = loginUiState) {
        when (loginUiState) {
            is LoginUiState.GoogleSignIn -> {
                signInLauncher.launch(
                    GoogleSignInHelper.getGoogleSignInClient(context).signInIntent,
                )
            }

            is LoginUiState.SignInComplete -> {
                navigateToCallJoin.invoke()
            }

            is LoginUiState.AlreadyLoggedIn -> {
                navigateToCallJoin.invoke()
            }

            is LoginUiState.SignInFailure -> {
                Toast.makeText(
                    context,
                    "Login failed! (${loginUiState.errorMessage})",
                    Toast.LENGTH_SHORT,
                ).show()
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
