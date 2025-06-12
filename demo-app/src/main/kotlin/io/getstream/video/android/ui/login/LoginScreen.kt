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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Adb
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.ripple
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
import androidx.compose.ui.platform.LocalConfiguration
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
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.compose.ui.components.base.StreamButton
import io.getstream.video.android.compose.ui.components.base.StreamDialogPositiveNegative
import io.getstream.video.android.compose.ui.components.base.StreamIconToggleButton
import io.getstream.video.android.compose.ui.components.base.StreamTextField
import io.getstream.video.android.compose.ui.components.base.styling.ButtonStyles
import io.getstream.video.android.compose.ui.components.base.styling.IconStyles
import io.getstream.video.android.compose.ui.components.base.styling.StreamDialogStyles
import io.getstream.video.android.model.User
import io.getstream.video.android.models.builtInUsers
import io.getstream.video.android.tooling.extensions.toPx
import io.getstream.video.android.tooling.util.StreamFlavors
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
    VideoTheme {
//        LockScreenOrientation(orientation = Configuration.ORIENTATION_UNDEFINED)
        val uiState by loginViewModel.uiState.collectAsState(initial = LoginUiState.Nothing)
        val isLoading by remember(uiState) {
            mutableStateOf(
                uiState !is LoginUiState.Nothing && uiState !is LoginUiState.SignInFailure,
            )
        }
        val selectedEnv by AppConfig.currentEnvironment.collectAsStateWithLifecycle()
        val availableEnvs by remember { mutableStateOf(AppConfig.availableEnvironments) }
        val availableLogins = when (BuildConfig.FLAVOR == StreamFlavors.development) {
            true -> listOf("built-in", "google", "email", "guest")
            else -> listOf("google", "email", "guest")
        }

        var isShowingEmailLoginDialog by remember { mutableStateOf(false) }
        var isShowingBuiltInUsersDialog by remember { mutableStateOf(false) }

        HandleLoginUiStates(
            autoLogIn = autoLogIn,
            loginUiState = uiState,
            navigateToCallJoin = navigateToCallJoin,
        )

        LoginContentResponsive(
            availableEnvs = availableEnvs,
            selectedEnv = selectedEnv,
            availableLogins = availableLogins,
            autoLogIn = autoLogIn,
            isLoading = isLoading,
            showEmailLoginDialog = { isShowingEmailLoginDialog = true },
            showBuiltInUserDialog = { isShowingBuiltInUsersDialog = true },
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
        if (isShowingBuiltInUsersDialog) {
            BuiltInUsersLoginDialog(
                login = { autoLoginBoolean, event ->
                    autoLoginBoolean?.let {
                        loginViewModel.autoLogIn = it
                    }
                    event?.let {
                        loginViewModel.handleUiEvent(event)
                    }
                },
                onDismissRequest = { isShowingBuiltInUsersDialog = false },
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LoginContentResponsive(
    autoLogIn: Boolean,
    isLoading: Boolean,
    showEmailLoginDialog: () -> Unit = {},
    showBuiltInUserDialog: () -> Unit = {},
    reloadSdk: () -> Unit = {},
    login: (Boolean?, LoginEvent?) -> Unit = { _, _ -> },
    availableEnvs: List<StreamEnvironment>,
    selectedEnv: StreamEnvironment?,
    availableLogins: List<String>,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        LoginContentLandscape(
            autoLogIn,
            isLoading,
            showEmailLoginDialog,
            showBuiltInUserDialog,
            reloadSdk,
            login,
            availableEnvs,
            selectedEnv,
            availableLogins
        )
    } else {
        LoginContentPortrait(
            autoLogIn,
            isLoading,
            showEmailLoginDialog,
            showBuiltInUserDialog,
            reloadSdk,
            login,
            availableEnvs,
            selectedEnv,
            availableLogins
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LoginContentPortrait(
    autoLogIn: Boolean,
    isLoading: Boolean,
    showEmailLoginDialog: () -> Unit = {},
    showBuiltInUserDialog: () -> Unit = {},
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

        EnvironmentSelectionDialog(
            modifier = Modifier.align(Alignment.End),
            reloadSdk = reloadSdk,
            availableEnvs = availableEnvs,
            selectedEnv = selectedEnv
        )
        Logo(Modifier.fillMaxWidth(), selectedEnv)
        Spacer(modifier = Modifier.weight(1f)) // Pushes the following views to the bottom
        LoginButtons(
            Modifier.wrapContentHeight(),
            isLoading,
            showEmailLoginDialog,
            showBuiltInUserDialog,
            login,
            availableLogins,
            1
        )
        LoadingIndicator(Modifier.align(Alignment.CenterHorizontally), isLoading)
    }
}

@Composable
private fun LoadingIndicator(modifier: Modifier, isLoading: Boolean) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = modifier,
            color = VideoTheme.colors.brandPrimary,
        )
    }
}

@Composable
private fun EnvironmentSelectionDialog(
    modifier: Modifier,
    reloadSdk: () -> Unit = {},
    availableEnvs: List<StreamEnvironment>,
    selectedEnv: StreamEnvironment?,
) {
    selectedEnv?.let {
        Box(
            modifier = modifier
        ) {
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
}

@Composable
private fun Logo(modifier: Modifier, selectedEnv: StreamEnvironment?) {
    Column(
        modifier = modifier
//            .wrapContentHeight()
//            .fillMaxWidth()
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
//            modifier = Modifier.fillMaxWidth(),
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
}

@Composable
private fun LoginButtons(
    modifier: Modifier,
    isLoading: Boolean,
    showEmailLoginDialog: () -> Unit = {},
    showBuiltInUserDialog: () -> Unit = {},
    login: (Boolean?, LoginEvent?) -> Unit = { _, _ -> },
    availableLogins: List<String>,
    columnSize: Int,
) {
    Column(
        modifier = modifier
//            .align(Alignment.CenterHorizontally)
            .background(
                color = VideoTheme.colors.baseSheetSecondary,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            )
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        if (!isLoading) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnSize), // This defines your 2 columns
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth(),
//                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(VideoTheme.dimens.spacingM), // Spacing between columns
                verticalArrangement = Arrangement.spacedBy(VideoTheme.dimens.spacingM), // Spacing between rows
            ) {
                items(availableLogins.size) { index ->

                    when (availableLogins[index]) {
                        "built-in" -> {
                            StreamButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("Stream_BuiltInUserSignIn"),
                                icon = Icons.Outlined.Adb,
                                enabled = !isLoading,
                                text = stringResource(id = R.string.builtin_user_sign_in),
                                style = ButtonStyles.secondaryButtonStyle(),
                                onClick = showBuiltInUserDialog,
                            )
                        }

                        "google" -> {
                            StreamButton(
                                icon = ImageVector.vectorResource(R.drawable.google_button_logo),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("Stream_GoogleSignIn"),
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("Stream_EmailSignIn"),
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("Stream_RandomUserSignIn"),
                                icon = Icons.Outlined.GroupAdd,
                                enabled = !isLoading,
                                text = stringResource(id = R.string.random_user_sign_in),
                                style = ButtonStyles.tertiaryButtonStyle(),
                                onClick = {
                                    login(true, null)
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(VideoTheme.dimens.spacingM))
                }
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
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LoginContentLandscape(
    autoLogIn: Boolean,
    isLoading: Boolean,
    showEmailLoginDialog: () -> Unit = {},
    showBuiltInUserDialog: () -> Unit = {},
    reloadSdk: () -> Unit = {},
    login: (Boolean?, LoginEvent?) -> Unit = { _, _ -> },
    availableEnvs: List<StreamEnvironment>,
    selectedEnv: StreamEnvironment?,
    availableLogins: List<String>,
) {

    Box {

        Row {
            Logo(
                Modifier
                    .weight(1f), selectedEnv
            )
//            Box(modifier = Modifier.background(Color.Red)
//                .size(100.dp))
//            Column {
            LoginButtons(
                Modifier
                    .weight(1f)
                    .padding(top = 80.dp),
                isLoading,
                showEmailLoginDialog,
                showBuiltInUserDialog,
                login,
                availableLogins,
                2
            )
//            }
        }
        EnvironmentSelectionDialog(
            modifier = Modifier.align(Alignment.TopEnd),
            reloadSdk, availableEnvs, selectedEnv
        )

        LoadingIndicator(
            Modifier.align(Alignment.Center),
            isLoading
        )
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

@Composable
private fun BuiltInUsersLoginDialog(
    onDismissRequest: () -> Unit = {},
    login: (Boolean?, LoginEvent?) -> Unit = { _, _ -> },
) {
    val users = User.builtInUsers()
    StreamDialogPositiveNegative(
        style = StreamDialogStyles.defaultDialogStyle(),
        onDismiss = onDismissRequest,
        icon = Icons.Default.Email,
        title = stringResource(R.string.select_user),
        content = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                items(users) { user ->
                    Row(
                        modifier = Modifier
                            .height(64.dp)
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = {
                                    login(true, LoginEvent.SignIn(user))
                                    onDismissRequest()
                                },
                            ),
                    ) {
                        Spacer(modifier = Modifier.width(16.dp))
                        UserAvatar(
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.CenterVertically),
                            userImage = user.image,
                            userName = user.userNameOrId,
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            text = user.name.orEmpty(),
                            color = VideoTheme.colors.basePrimary,
                            style = VideoTheme.typography.subtitleS,
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }
            }
        },
        negativeButton = Triple("Cancel", ButtonStyles.secondaryButtonStyle(), onDismissRequest),
    )
}

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
                        Modifier
                            .background(
                                color = VideoTheme.colors.baseSheetTertiary,
                                shape = VideoTheme.shapes.dialog,
                            )
                            .width(180.dp),
                    ) {
                        items.forEach { item ->
                            StreamButton(
                                text = item.displayName,
                                onClick = {
                                    onItemSelected(item)
                                    selectedText = item.displayName
                                    showDialog = !showDialog
                                },
                                style = ButtonStyles.tertiaryButtonStyle(),
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .fillMaxWidth(),
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
        val env = StreamEnvironment(env = "demo", displayName = "Demo")
        LoginContentPortrait(
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
private fun LoginScreenLandscapePreview() {
    VideoTheme {
        val env = StreamEnvironment(env = "demo", displayName = "Demo")
        LoginContentLandscape(
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
        val env = StreamEnvironment(env = "demo", displayName = "Demo")
        EmailLoginDialog()
    }
}

@Preview
@Composable
private fun SelectEnvOption() {
    VideoTheme {
        val env = StreamEnvironment(env = "demo", displayName = "Demo")
        val env2 = StreamEnvironment(env = "pronto", displayName = "Pronto")

        SelectableDialog(
            items = listOf(env, env2),
            selectedItem = env,
            onItemSelected = {},
        )
    }
}
