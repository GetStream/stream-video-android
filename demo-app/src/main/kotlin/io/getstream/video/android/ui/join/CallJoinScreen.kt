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

package io.getstream.video.android.ui.join

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowOverflow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoCall
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
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
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
import io.getstream.video.android.data.state.GlobalCodecChoiceState
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewUsers
import io.getstream.video.android.model.User
import io.getstream.video.android.tooling.util.StreamFlavors
import io.getstream.video.android.util.LockScreenOrientation
import io.getstream.video.android.util.config.AppConfig
import io.getstream.video.android.util.config.types.StreamEnvironment

@Composable
fun CallJoinScreen(
    callJoinViewModel: CallJoinViewModel = hiltViewModel(),
    navigateToCallLobby: (callId: String) -> Unit,
    navigateUpToLogin: (autoLogIn: Boolean) -> Unit,
    navigateToDirectCallJoin: () -> Unit,
    navigateToBarcodeScanner: () -> Unit = {},
) {
    LockScreenOrientation(orientation = Configuration.ORIENTATION_PORTRAIT)
    LaunchedEffect(callJoinViewModel) {
        GlobalCodecChoiceState.clear()
    }
    val uiState by callJoinViewModel.uiState.collectAsState(CallJoinUiState.Nothing)
    val user by callJoinViewModel.user.collectAsState(initial = null)

    var isSignOutDialogVisible by remember { mutableStateOf(false) }
    val isLoggedOut by callJoinViewModel.isLoggedOut.collectAsState(initial = false)
    val isNetworkAvailable by callJoinViewModel.isNetworkAvailable.collectAsStateWithLifecycle()

    HandleCallJoinUiState(
        callJoinUiState = uiState,
        navigateToCallLobby = navigateToCallLobby,
        navigateUpToLogin = { navigateUpToLogin(true) },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VideoTheme.colors.baseSheetPrimary),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CallJoinHeader(
            user = user,
            showDirectCall = BuildConfig.FLAVOR == StreamFlavors.development,
            onAvatarLongClick = { if (isNetworkAvailable) isSignOutDialogVisible = true },
            onDirectCallClick = navigateToDirectCallJoin,
            onSignOutClick = {
                callJoinViewModel.autoLogInAfterLogOut = false
                callJoinViewModel.logOut()
            },
        )
        Spacer(modifier = Modifier.size(VideoTheme.dimens.genericMax))
        CallJoinBody(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .verticalScroll(rememberScrollState())
                .weight(1f),
            callJoinViewModel = callJoinViewModel,
            openCamera = {
                navigateToBarcodeScanner()
            },
            isNetworkAvailable = isNetworkAvailable,
        )
    }

    if (isSignOutDialogVisible) {
        SignOutDialog(
            onConfirmation = {
                isSignOutDialogVisible = false
                callJoinViewModel.autoLogInAfterLogOut = false
                callJoinViewModel.logOut()
            },
            onDismissRequest = { isSignOutDialogVisible = false },
        )
    }

    LaunchedEffect(key1 = isLoggedOut) {
        if (isLoggedOut) {
            navigateUpToLogin.invoke(callJoinViewModel.autoLogInAfterLogOut)
        }
    }
}

@Composable
private fun HandleCallJoinUiState(
    callJoinUiState: CallJoinUiState,
    navigateToCallLobby: (callId: String) -> Unit,
    navigateUpToLogin: () -> Unit,
) {
    LaunchedEffect(key1 = callJoinUiState) {
        when (callJoinUiState) {
            is CallJoinUiState.JoinCompleted -> navigateToCallLobby.invoke(callJoinUiState.callId)

            is CallJoinUiState.GoBackToLogin -> navigateUpToLogin.invoke()

            else -> Unit
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CallJoinHeader(
    user: User?,
    isProduction: Boolean = BuildConfig.FLAVOR == StreamFlavors.production,
    showDirectCall: Boolean = user?.custom?.get("email")?.contains("getstreamio") == true,
    onAvatarLongClick: () -> Unit,
    onDirectCallClick: () -> Unit,
    onSignOutClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(VideoTheme.dimens.spacingM)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        user?.let {
            Box(
                modifier = if (isProduction) {
                    Modifier.combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                        onLongClick = onAvatarLongClick,
                    )
                } else {
                    Modifier
                },
            ) {
                UserAvatar(
                    modifier = Modifier.size(VideoTheme.dimens.componentHeightL),
                    userImage = it.image,
                    userName = it.userNameOrId,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            modifier = Modifier.weight(1f),
            color = Color.White,
            text = user?.userNameOrId.orEmpty(),
            maxLines = 1,
            fontSize = 16.sp,
        )

        if (!isProduction || showDirectCall) {
            var showMenu by remember {
                mutableStateOf(false)
            }
            var popupPosition by remember { mutableStateOf(IntOffset(0, 0)) }
            var buttonSize by remember { mutableStateOf(IntSize(0, 0)) }

            StreamIconToggleButton(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    val buttonBounds = coordinates.boundsInParent()
                    popupPosition = IntOffset(
                        x = buttonBounds.right.toInt() - buttonSize.width,
                        y = buttonBounds.bottom.toInt(),
                    )
                    buttonSize = coordinates.size
                },
                toggleState = rememberUpdatedState(newValue = ToggleableState(showMenu)),
                onIcon = Icons.Default.Settings,
                onStyle = VideoTheme.styles.buttonStyles.secondaryIconButtonStyle(),
                offStyle = VideoTheme.styles.buttonStyles.primaryIconButtonStyle(),
            ) {
                showMenu = when (it) {
                    ToggleableState.On -> false
                    ToggleableState.Off -> true
                    ToggleableState.Indeterminate -> false
                }
            }

            if (showMenu) {
                Popup(
                    onDismissRequest = {
                        showMenu = !showMenu
                    },
                    offset = popupPosition,
                ) {
                    JoinScreenSettingsMenu(
                        showDirectCall,
                        isProduction,
                        {
                            showMenu = false
                            onDirectCallClick.invoke()
                        },
                        {
                            showMenu = false
                            onSignOutClick.invoke()
                        },
                        {
                            GlobalCodecChoiceState.preferredPublishCodec = it
                        },
                        {
                            GlobalCodecChoiceState.preferredSubscribeCodec = it
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JoinScreenSettingsMenu(
    showDirectCall: Boolean = true,
    isProduction: Boolean = true,
    onDirectCallClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
    onPublishCodec: (String) -> Unit = {},
    onSubscribeCodec: (String) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .background(
                VideoTheme.colors.baseSheetTertiary,
                VideoTheme.shapes.dialog,
            )
            .padding(VideoTheme.dimens.spacingM),
    ) {
        if (showDirectCall) {
            StreamButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.direct_call),
                icon = Icons.Default.Call,
                style = VideoTheme.styles.buttonStyles.primaryButtonStyle(),
                onClick = {
                    onDirectCallClick.invoke()
                },
            )
        }
        Spacer(modifier = Modifier.width(5.dp))
        if (!isProduction) {
            StreamButton(
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.AutoMirrored.Filled.Logout,
                style = VideoTheme.styles.buttonStyles.tertiaryButtonStyle(),
                text = stringResource(id = R.string.sign_out),
                onClick = {
                    onSignOutClick()
                },
            )
        }

        if (!isProduction) {
            Divider(modifier = Modifier.padding(16.dp), color = VideoTheme.colors.baseSecondary)
            Text(style = VideoTheme.typography.titleXs, text = "Publish codec")
            Spacer(modifier = Modifier.width(16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.Center,
                overflow = FlowRowOverflow.Visible,
            ) {
                StreamButton(
                    modifier = Modifier.weight(1f),
                    style = VideoTheme.styles.buttonStyles.tertiaryButtonStyle(),
                    text = "AV1",
                    onClick = {
                        onPublishCodec("AV1")
                    },
                )
                Spacer(modifier = Modifier.width(5.dp))
                StreamButton(
                    modifier = Modifier.weight(1f),
                    style = VideoTheme.styles.buttonStyles.tertiaryButtonStyle(),
                    text = "VP8",
                    onClick = {
                        onPublishCodec("VP8")
                    },
                )
                Spacer(modifier = Modifier.width(5.dp))
                StreamButton(
                    modifier = Modifier.weight(1f),
                    style = VideoTheme.styles.buttonStyles.tertiaryButtonStyle(),
                    text = "VP9",
                    onClick = {
                        onPublishCodec("VP9")
                    },
                )
                Spacer(modifier = Modifier.width(5.dp))
                StreamButton(
                    modifier = Modifier.weight(1f),
                    style = VideoTheme.styles.buttonStyles.tertiaryButtonStyle(),
                    text = "H264",
                    onClick = {
                        onPublishCodec("H264")
                    },
                )
            }

            Text(style = VideoTheme.typography.titleXs, text = "Subscribe codec")
            Spacer(modifier = Modifier.width(16.dp))

            FlowRow(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.Center,
                overflow = FlowRowOverflow.Visible,
            ) {
                StreamButton(
                    modifier = Modifier.weight(1f),
                    style = VideoTheme.styles.buttonStyles.tertiaryButtonStyle(),
                    text = "AV1",
                    onClick = {
                        onSubscribeCodec("AV1")
                    },
                )
                Spacer(modifier = Modifier.width(5.dp))
                StreamButton(
                    modifier = Modifier.weight(1f),
                    style = VideoTheme.styles.buttonStyles.tertiaryButtonStyle(),
                    text = "VP8",
                    onClick = {
                        onSubscribeCodec("VP8")
                    },
                )
                Spacer(modifier = Modifier.width(5.dp))
                StreamButton(
                    modifier = Modifier.weight(1f),
                    style = VideoTheme.styles.buttonStyles.tertiaryButtonStyle(),
                    text = "VP9",
                    onClick = {
                        onSubscribeCodec("VP9")
                    },
                )
                Spacer(modifier = Modifier.width(5.dp))
                StreamButton(
                    modifier = Modifier.weight(1f),
                    style = VideoTheme.styles.buttonStyles.tertiaryButtonStyle(),
                    text = "H264",
                    onClick = {
                        onSubscribeCodec("H264")
                    },
                )
            }
        }
    }
}

@Composable
private fun CallJoinBody(
    modifier: Modifier,
    openCamera: () -> Unit,
    callJoinViewModel: CallJoinViewModel = hiltViewModel(),
    isNetworkAvailable: Boolean,
) {
    val selectedEnv by AppConfig.currentEnvironment.collectAsStateWithLifecycle()
    val user by if (LocalInspectionMode.current) {
        remember { mutableStateOf(previewUsers[0]) }
    } else {
        callJoinViewModel.user.collectAsState(initial = null)
    }

    if (!isNetworkAvailable) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            StreamLogo()
            Spacer(modifier = Modifier.height(25.dp))
            AppName(selectedEnv)
            Spacer(modifier = Modifier.height(25.dp))
            Description(text = stringResource(id = R.string.you_are_offline))
        }
    } else {
        if (user != null) {
            CallActualContent(modifier = modifier.fillMaxSize(), onJoinCall = {
                callJoinViewModel.handleUiEvent(CallJoinEvent.JoinCall(callId = it))
            }, onNewCall = {
                callJoinViewModel.handleUiEvent(CallJoinEvent.JoinCall())
            }, gotoQR = {
                openCamera()
            })
        }
    }
}

@Composable
private fun CallActualContent(
    modifier: Modifier = Modifier,
    onJoinCall: (String) -> Unit,
    onNewCall: () -> Unit,
    gotoQR: () -> Unit,
) = Box(modifier = Modifier.background(VideoTheme.colors.baseSheetPrimary)) {
    Column(
        modifier = modifier
            .padding(horizontal = VideoTheme.dimens.spacingM)
            .semantics { testTagsAsResourceId = true },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StreamLogo()
        Spacer(modifier = Modifier.height(VideoTheme.dimens.spacingL))
        AppName()
        Spacer(modifier = Modifier.height(20.dp))
        Description(text = stringResource(id = R.string.join_description))
        Spacer(modifier = Modifier.height(VideoTheme.dimens.spacingL))
        JoinCallForm {
            onJoinCall(it)
        }
        Spacer(modifier = Modifier.height(VideoTheme.dimens.spacingS))
        StreamButton(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("start_new_call"),
            text = stringResource(id = R.string.start_a_new_call),
            icon = Icons.Default.VideoCall,
            onClick = { onNewCall() },
        )
        Spacer(modifier = Modifier.height(VideoTheme.dimens.spacingS))
        StreamButton(
            style = VideoTheme.styles.buttonStyles.tertiaryButtonStyle(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("scan_qr_code"),
            text = stringResource(id = R.string.scan_qr_code),
            icon = Icons.Default.QrCodeScanner,
            onClick = { gotoQR() },
        )
    }
}

@Composable
private fun StreamLogo() {
    Image(
        modifier = Modifier.size(102.dp),
        painter = painterResource(id = R.drawable.ic_stream_video_meeting_logo),
        contentDescription = null,
    )
}

@Composable
private fun AppName(env: StreamEnvironment? = null) {
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
            append(env?.displayName ?: "")
        },
        color = Color.White,
        fontSize = 24.sp,
    )
}

@Composable
private fun Description(text: String) {
    Text(
        text = text,
        style = VideoTheme.typography.bodyM,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(0.dp, 320.dp),
    )
}

@Composable
private fun Label(text: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 35.dp),
        text = text,
        color = Color(0xFF979797),
        fontSize = 13.sp,
    )
}

@Composable
private fun JoinCallForm(
    joinCall: (String) -> Unit,
) {
    var callId by remember {
        mutableStateOf(
            TextFieldValue(
                if (BuildConfig.FLAVOR == StreamFlavors.development) {
                    "default:79cYh3J5JgGk"
                } else {
                    ""
                },
            ),
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
    ) {
        StreamTextField(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            onValueChange = { callId = it },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Email,
            ),
            style = VideoTheme.styles.textFieldStyles.defaultTextField(),
            value = callId,
            placeholder = stringResource(id = R.string.join_call_call_id_hint),
            keyboardActions = KeyboardActions(
                onDone = {
                    joinCall(callId.text)
                },
            ),
        )

        StreamButton(
            icon = Icons.AutoMirrored.Filled.Login,
            style = VideoTheme.styles.buttonStyles.secondaryButtonStyle(),
            modifier = Modifier
                .padding(start = 16.dp)
                .fillMaxHeight()
                .testTag("join_call"),
            onClick = {
                joinCall(callId.text)
            },
            text = stringResource(id = R.string.join_call),
        )
    }
}

@Composable
private fun SignOutDialog(
    onConfirmation: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    StreamDialogPositiveNegative(
        style = VideoTheme.styles.dialogStyles.defaultDialogStyle(),
        positiveButton = Triple(
            stringResource(id = R.string.sign_out),
            VideoTheme.styles.buttonStyles.secondaryButtonStyle(),
        ) {
            onConfirmation()
        },
        negativeButton = Triple(
            stringResource(R.string.cancel),
            VideoTheme.styles.buttonStyles.tertiaryButtonStyle(),
        ) {
            onDismissRequest()
        },
        title = stringResource(id = R.string.sign_out),
        contentText = stringResource(R.string.are_you_sure_sign_out),
    )
}

class BelowElementPositionProvider(
    private val anchorBounds: androidx.compose.ui.geometry.Rect,
    private val screenPadding: Int = 8, // Padding from screen edges
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = anchorBounds.left.coerceIn(
            screenPadding,
            (windowSize.width - popupContentSize.width - screenPadding),
        )

        val y = (this.anchorBounds.bottom + screenPadding).coerceIn(
            screenPadding.toFloat(),
            (windowSize.height - popupContentSize.height - screenPadding).toFloat(),
        ).toInt()

        return IntOffset(x, y)
    }
}

@Preview
@Composable
private fun CallJoinScreenPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallActualContent(onJoinCall = {}, onNewCall = {}, gotoQR = {})
    }
}

@Preview
@Composable
private fun CallJoinScreenHeader() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallJoinHeader(previewUsers[0], false, true, {}, {}, {})
    }
}

@Preview
@Composable
private fun JoinSettingsMenuPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        JoinScreenSettingsMenu(true, false, {}, {})
    }
}
