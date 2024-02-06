/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.core.content.ContextCompat.getString
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.BuildConfig
import io.getstream.video.android.R
import io.getstream.video.android.compose.theme.base.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamButton
import io.getstream.video.android.compose.ui.components.base.StreamDialogPositiveNegative
import io.getstream.video.android.compose.ui.components.base.StreamIconToggleButton
import io.getstream.video.android.compose.ui.components.base.StreamTextField
import io.getstream.video.android.compose.ui.components.base.styling.ButtonStyles
import io.getstream.video.android.compose.ui.components.base.styling.IconStyles
import io.getstream.video.android.compose.ui.components.base.styling.StreamDialogStyles
import io.getstream.video.android.tooling.extensions.toPx
import io.getstream.video.android.util.LockScreenOrientation
import io.getstream.video.android.util.UserHelper
import io.getstream.video.android.util.config.AppConfig
import io.getstream.video.android.util.config.types.Flavor
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
    VideoTheme {
        LockScreenOrientation(orientation = Configuration.ORIENTATION_PORTRAIT)
        val uiState by loginViewModel.uiState.collectAsState(initial = LoginUiState.Nothing)
        val isLoading by remember(uiState) {
            mutableStateOf(
                uiState !is LoginUiState.Nothing && uiState !is LoginUiState.SignInFailure,
            )
        }
        val selectedEnv by AppConfig.currentEnvironment.collectAsStateWithLifecycle()
        val availableEnvs by AppConfig.availableEnvironments.collectAsStateWithLifecycle()
        val availableLogins by AppConfig.availableLogins.collectAsStateWithLifecycle()

        var isShowingEmailLoginDialog by remember { mutableStateOf(false) }

        HandleLoginUiStates(
            autoLogIn = autoLogIn,
            loginUiState = uiState,
            navigateToCallJoin = navigateToCallJoin,
        )

        LoginContent(
            availableEnvs = availableEnvs,
            selectedEnv = selectedEnv,
            availableLogins = availableLogins,
            autoLogIn = autoLogIn,
            isLoading = isLoading,
            showEmailLoginDialog = { isShowingEmailLoginDialog = true },
            reloadSdk = {
                loginViewModel.reloadSdk()
            },
            login = { autoLoginBoolean, event ->
                autoLoginBoolean?.let {
                    loginViewModel.autoLogIn = it
                }
                if (event == null) {
                    loginViewModel.signInIfValidUserExist()
                } else {
                    loginViewModel.handleUiEvent(event)
                }
            },
        )

        if (isShowingEmailLoginDialog) {
            EmailLoginDialog(
                login = { autoLoginBoolean, event ->
                    autoLoginBoolean?.let {
                        loginViewModel.autoLogIn = it
                    }
                    event?.let {
                        loginViewModel.handleUiEvent(event)
                    }
                },
                onDismissRequest = { isShowingEmailLoginDialog = false },
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LoginContent(
    autoLogIn: Boolean,
    isLoading: Boolean,
    showEmailLoginDialog: () -> Unit = {},
    reloadSdk: () -> Unit = {},
    login: (Boolean?, LoginEvent?) -> Unit = { _, _ -> },
    availableEnvs: List<StreamEnvironment>,
    selectedEnv: StreamEnvironment?,
    availableLogins: List<String>,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = VideoTheme.colors.baseSheetPrimary),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        selectedEnv?.let {
            Box(modifier = Modifier.align(Alignment.End)) {
                SelectableDialog(
                    items = availableEnvs,
                    selectedItem = it,
                    onItemSelected = { env ->
                        AppConfig.selectEnv(env)
                        reloadSdk()
                    },
                )
            }
        }
        Column(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .semantics { testTagsAsResourceId = true },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                modifier = Modifier.size(width = 254.dp, height = 179.dp),
                painter = painterResource(id = R.drawable.stream_calls_logo),
                contentDescription = null,
            )

            Spacer(modifier = Modifier.height(27.dp))

            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = buildAnnotatedString {
                    append("Stream\n")
                    append(
                        AnnotatedString(
                            "[Video Calling]\n",
                            spanStyle = SpanStyle(VideoTheme.colors.brandGreen),
                        ),
                    )
                    append(selectedEnv?.displayName ?: "")
                },
                color = Color.White,
                fontSize = 24.sp,
            )
            Spacer(modifier = Modifier.height(30.dp))
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .background(
                    color = VideoTheme.colors.baseSheetSecondary,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                )
                .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            if (!isLoading) {
                availableLogins.forEach {
                    when (it) {
                        "google" -> {
                            StreamButton(
                                icon = ImageVector.vectorResource(R.drawable.google_button_logo),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading,
                                text = stringResource(id = R.string.sign_in_google),
                                style = ButtonStyles.primaryButtonStyle()
                                    .copy(
                                        iconStyle = IconStyles.customColorIconStyle(
                                            color = Color.Unspecified,
                                        ),
                                    ),
                                onClick = {
                                    login(false, LoginEvent.GoogleSignIn())
                                },
                            )
                        }

                        "email" -> {
                            StreamButton(
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Default.Email,
                                enabled = !isLoading,
                                text = stringResource(id = R.string.sign_in_email),
                                style = ButtonStyles.primaryButtonStyle(),
                                onClick = {
                                    showEmailLoginDialog.invoke()
                                },
                            )
                        }

                        "guest" -> {
                            StreamButton(
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Outlined.GroupAdd,
                                enabled = !isLoading,
                                text = stringResource(id = R.string.random_user_sign_in),
                                style = ButtonStyles.tetriaryButtonStyle(),
                                onClick = {
                                    login(true, null)
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(VideoTheme.dimens.spacingM))
                }
            }

            if (BuildConfig.BUILD_TYPE == "benchmark") {
                StreamButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 55.dp)
                        .testTag("authenticate"),
                    text = "Login for Benchmark",
                    style = ButtonStyles.secondaryButtonStyle(),
                    onClick = {
                        login(null, LoginEvent.SignInSuccess("benchmark.test@getstream.io"))
                    },
                )
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = VideoTheme.colors.brandPrimary,
            )
        }
    }
}

@Composable
private fun EmailLoginDialog(
    onDismissRequest: () -> Unit = {},
    login: (Boolean?, LoginEvent?) -> Unit = { _, _ -> },
) {
    var email by remember { mutableStateOf(TextFieldValue("")) }

    StreamDialogPositiveNegative(
        style = StreamDialogStyles.defaultDialogStyle(),
        onDismiss = { onDismissRequest.invoke() },
        icon = Icons.Default.Email,
        title = stringResource(R.string.enter_your_email_address),
        content = {
            StreamTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                value = email,
                onValueChange = { email = it },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Email,
                ),
            )
        },
        positiveButton = Triple("Login", ButtonStyles.secondaryButtonStyle()) {
            val userId = UserHelper.getUserIdFromEmail(email.text)
            login(true, LoginEvent.SignInSuccess(userId))
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
            StreamIconToggleButton(
                toggleState = rememberUpdatedState(newValue = ToggleableState(showDialog)),
                onClick = { showDialog = !showDialog },
                onIcon = Icons.Default.Settings,
                offIcon = Icons.Default.Settings,
                onStyle = ButtonStyles.secondaryIconButtonStyle(),
                offStyle = ButtonStyles.primaryIconButtonStyle(),
                modifier = Modifier.padding(16.dp),
            )
            if (showDialog) {
                Popup(
                    onDismissRequest = { showDialog = !showDialog },
                    alignment = Alignment.TopEnd,
                    offset = IntOffset(
                        0,
                        (VideoTheme.dimens.componentHeightL + VideoTheme.dimens.spacingL).toPx()
                            .toInt(),
                    ),
                ) {
                    Column(
                        Modifier.background(
                            color = VideoTheme.colors.baseSheetTertiary,
                            shape = VideoTheme.shapes.dialog,
                        ),
                    ) {
                        items.forEach { item ->
                            StreamButton(
                                text = item.displayName,
                                onClick = {
                                    onItemSelected(item)
                                    selectedText = item.displayName
                                    showDialog = !showDialog
                                },
                                style = ButtonStyles.tetriaryButtonStyle(),
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                    }
                }
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
        val env = StreamEnvironment("demo", "Demo", listOf(Flavor("development", true)))
        LoginContent(
            autoLogIn = false,
            isLoading = false,
            availableEnvs = listOf(env),
            selectedEnv = env,
            availableLogins = listOf("google", "email", "guest"),
        )
    }
}

@Preview
@Composable
private fun EmailDialogPreview() {
    VideoTheme {
        val env = StreamEnvironment("demo", "Demo", listOf(Flavor("development", true)))
        EmailLoginDialog()
    }
}

@Preview
@Composable
private fun SelectEnvOption() {
    VideoTheme {
        val env = StreamEnvironment("demo", "Demo", listOf(Flavor("development", true)))
        val env2 = StreamEnvironment("pronto", "Pronto", listOf(Flavor("development", true)))

        SelectableDialog(
            items = listOf(env, env2),
            selectedItem = env,
            onItemSelected = {},
        )
    }
}
