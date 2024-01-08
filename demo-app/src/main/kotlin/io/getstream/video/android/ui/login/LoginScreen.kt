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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
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
import androidx.core.content.ContextCompat.getString
import androidx.core.content.ContextCompat.startActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.BuildConfig
import io.getstream.video.android.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.ui.theme.Colors
import io.getstream.video.android.ui.theme.LinkText
import io.getstream.video.android.ui.theme.LinkTextData
import io.getstream.video.android.ui.theme.StreamButton
import io.getstream.video.android.util.UserHelper
import io.getstream.video.android.util.config.AppConfig
import io.getstream.video.android.util.config.types.StreamEnvironment

/**
 * @param autoLogIn Flag that controls auto log-in with a random user.
 * - True in places where original login behavior is required (first start, StreamVideo instance not initialized, etc).
 * - False where showing login options (Google or random user) on the Login screen is required (avatar long press on Call Join screen, etc)
 */

@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = hiltViewModel(),
    autoLogIn: Boolean = true,
    navigateToCallJoin: () -> Unit,
) {
    val uiState by loginViewModel.uiState.collectAsState(initial = LoginUiState.Nothing)
    val isLoading by remember(uiState) {
        mutableStateOf(uiState !is LoginUiState.Nothing && uiState !is LoginUiState.SignInFailure)
    }
    var isShowingEmailLoginDialog by remember { mutableStateOf(false) }

    HandleLoginUiStates(
        autoLogIn = autoLogIn,
        loginUiState = uiState,
        navigateToCallJoin = navigateToCallJoin,
    )

    LoginContent(
        autoLogIn = autoLogIn,
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
    autoLogIn: Boolean,
    isLoading: Boolean,
    showEmailLoginDialog: () -> Unit,
    loginViewModel: LoginViewModel = hiltViewModel(),
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val selectedEnv by AppConfig.currentEnvironment.collectAsStateWithLifecycle()
        val availableEnvs by AppConfig.availableEnvironments.collectAsStateWithLifecycle()

        selectedEnv?.let {
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                SelectableDialog(
                    items = availableEnvs,
                    selectedItem = it,
                    onItemSelected = { env ->
                        AppConfig.selectEnv(env)
                        loginViewModel.reloadSdk()
                    },
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .wrapContentHeight()
                .fillMaxWidth()
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
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = stringResource(id = R.string.app_name),
                color = Color.White,
                fontSize = 38.sp,
            )
            Spacer(modifier = Modifier.height(30.dp))

            if (!isLoading) {
                val availableLogins by AppConfig.availableLogins.collectAsStateWithLifecycle()

                availableLogins.forEach {
                    when (it) {
                        "google" -> {
                            StreamButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .padding(horizontal = 55.dp),
                                enabled = !isLoading,
                                text = stringResource(id = R.string.sign_in_google),
                                onClick = {
                                    loginViewModel.autoLogIn = false
                                    loginViewModel.handleUiEvent(LoginEvent.GoogleSignIn())
                                },
                            )
                        }

                        "email" -> {
                            StreamButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .padding(horizontal = 55.dp),
                                enabled = !isLoading,
                                text = stringResource(id = R.string.sign_in_email),
                                onClick = {
                                    loginViewModel.autoLogIn = true
                                    showEmailLoginDialog.invoke()
                                },
                            )
                        }

                        "guest" -> {
                            StreamButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .padding(horizontal = 55.dp),
                                enabled = !isLoading,
                                text = stringResource(R.string.random_user_sign_in),
                                onClick = {
                                    loginViewModel.autoLogIn = true
                                    loginViewModel.signInIfValidUserExist()
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(15.dp))
                }

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
            }

            if (BuildConfig.BUILD_TYPE == "benchmark") {
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
                        label = { Text(text = stringResource(R.string.enter_your_email_address)) },
                        colors = TextFieldDefaults.textFieldColors(
                            textColor = Colors.description,
                            focusedLabelColor = VideoTheme.colors.primaryAccent,
                            unfocusedLabelColor = VideoTheme.colors.textLowEmphasis,
                            unfocusedIndicatorColor = VideoTheme.colors.primaryAccent,
                            focusedIndicatorColor = VideoTheme.colors.primaryAccent,
                            cursorColor = Colors.description,
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
                            val userId = UserHelper.getUserIdFromEmail(email)
                            loginViewModel.handleUiEvent(LoginEvent.SignInSuccess(userId))
                        },
                        text = "Log in",
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectableDialog(
    items: List<StreamEnvironment>,
    selectedItem: StreamEnvironment?,
    onItemSelected: (StreamEnvironment) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf(selectedItem?.displayName ?: "") }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = "Current environment: $selectedText",
            color = Color.White,
        )
        if (items.size > 1) {
            StreamButton(
                text = "Change",
                onClick = { showDialog = true },
                modifier = Modifier.padding(16.dp),
            )
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = {
                        Text("Available environments")
                    },
                    text = {
                        FlowRow {
                            items.forEach { item ->
                                StreamButton(
                                    text = item.displayName,
                                    onClick = {
                                        onItemSelected(item)
                                        selectedText = item.displayName
                                        showDialog = false
                                    },
                                    modifier = Modifier.padding(8.dp),
                                )
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {},
                )
            }
        }
    }
}

@Composable
private fun HandleLoginUiStates(
    loginViewModel: LoginViewModel = hiltViewModel(),
    loginUiState: LoginUiState,
    autoLogIn: Boolean,
    navigateToCallJoin: () -> Unit,
) {
    val context = LocalContext.current
    val signInLauncher = rememberLauncherForGoogleSignInActivityResult(
        onSignInSuccess = { email ->
            val userId = UserHelper.getUserIdFromEmail(email)
            loginViewModel.handleUiEvent(LoginEvent.SignInSuccess(userId = userId))
        },
        onSignInFailed = {
            loginViewModel.handleUiEvent(
                LoginEvent.SignInFailure(
                    errorMessage = getString(context, R.string.google_sign_in_not_finalized),
                ),
            )
        },
    )

    LaunchedEffect(key1 = autoLogIn) {
        loginViewModel.autoLogIn = autoLogIn
    }

    LaunchedEffect(key1 = Unit) {
        loginViewModel.signInIfValidUserExist()
    }

    LaunchedEffect(key1 = loginUiState) {
        when (loginUiState) {
            is LoginUiState.GoogleSignIn -> {
                signInLauncher.launch(loginUiState.signInIntent)
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
                    loginUiState.errorMessage,
                    Toast.LENGTH_LONG,
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
