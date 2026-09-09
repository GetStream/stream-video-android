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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoCall
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
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
import io.getstream.video.android.R
import io.getstream.video.android.app
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.compose.ui.components.base.StreamButtonSize
import io.getstream.video.android.compose.ui.components.base.StreamButtonStyleDefaults
import io.getstream.video.android.compose.ui.components.base.StreamDialog
import io.getstream.video.android.compose.ui.components.base.StreamIconButton
import io.getstream.video.android.compose.ui.components.base.StreamTextButton
import io.getstream.video.android.compose.ui.components.base.StreamTextField
import io.getstream.video.android.defaultCallId
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewUsers
import io.getstream.video.android.model.User
import io.getstream.video.android.tooling.util.StreamBuildFlavorUtil
import io.getstream.video.android.ui.CallSettingsScreen
import io.getstream.video.android.ui.LogFilesScreen
import io.getstream.video.android.ui.SingleButtonDialog
import io.getstream.video.android.util.config.AppConfig
import io.getstream.video.android.util.config.types.StreamEnvironment

@Composable
fun CallJoinScreen(
    prefilledCallId: String? = null,
    callJoinViewModel: CallJoinViewModel = hiltViewModel(),
    navigateToCallLobby: (callId: String) -> Unit,
    navigateUpToLogin: (autoLogIn: Boolean) -> Unit,
    navigateToDirectCallJoin: () -> Unit,
    navigateToBarcodeScanner: () -> Unit = {},
) {
    val uiState by callJoinViewModel.uiState.collectAsState(CallJoinUiState.Nothing)
    val user by callJoinViewModel.user.collectAsState(initial = null)

    var isSignOutDialogVisible by remember { mutableStateOf(false) }
    val isLoggedOut by callJoinViewModel.isLoggedOut.collectAsState(initial = false)
    val isNetworkAvailable by callJoinViewModel.isNetworkAvailable.collectAsStateWithLifecycle()

    var renderLogsFileUi by remember { mutableStateOf(false) }
    var renderCallSettingsUi by remember { mutableStateOf(false) }

    HandleCallJoinUiState(
        callJoinUiState = uiState,
        navigateToCallLobby = navigateToCallLobby,
        navigateUpToLogin = { navigateUpToLogin(true) },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VideoTheme.colors.backgroundCoreApp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CallJoinHeader(
            user = user,
            showDirectCall = StreamBuildFlavorUtil.isDevelopment || StreamBuildFlavorUtil.isE2eTesting,
            onAvatarLongClick = { if (isNetworkAvailable) isSignOutDialogVisible = true },
            onDirectCallClick = navigateToDirectCallJoin,
            onSignOutClick = {
                callJoinViewModel.autoLogInAfterLogOut = false
                callJoinViewModel.logOut()
            },
            onLogsClick = {
                renderLogsFileUi = true
            },
            onCallSettingsClink = {
                renderCallSettingsUi = true
            },
        )

        CallJoinBody(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterHorizontally)
                .verticalScroll(rememberScrollState())
                .weight(1f),
            prefilledCallId = prefilledCallId,
            openCamera = { navigateToBarcodeScanner() },
            callJoinViewModel = callJoinViewModel,
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
    val appContext = LocalContext.current.applicationContext
    val policyViolation by appContext.app.policyViolationUiData.collectAsStateWithLifecycle()
    policyViolation?.let {
        SingleButtonDialog(it.title, it.message, it.actionButtonText) {
            appContext.app.policyViolationUiData.value = null
        }
    }

    if (renderLogsFileUi) {
        LogFilesScreen({
            renderLogsFileUi = false
        })
    }

    if (renderCallSettingsUi) {
        CallSettingsScreen(onClose = {
            renderCallSettingsUi = false
        })
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
    isProduction: Boolean = StreamBuildFlavorUtil.isProduction,
    showDirectCall: Boolean = user?.custom?.get("email")?.contains("getstreamio") == true,
    onAvatarLongClick: () -> Unit,
    onDirectCallClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onLogsClick: () -> Unit,
    onCallSettingsClink: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(16.dp)
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
                    modifier = Modifier.size(44.dp),
                    userImage = it.image,
                    userName = it.userNameOrId,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            modifier = Modifier
                .weight(1f)
                .testTag("Stream_UserName"),
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

            StreamIconButton(
                onClick = { showMenu = !showMenu },
                icon = rememberVectorPainter(Icons.Default.Settings),
                contentDescription = null,
                modifier = Modifier
                    .onGloballyPositioned { coordinates ->
                        val buttonBounds = coordinates.boundsInParent()
                        popupPosition = IntOffset(
                            x = buttonBounds.right.toInt() - buttonSize.width,
                            y = buttonBounds.bottom.toInt(),
                        )
                        buttonSize = coordinates.size
                    }
                    .testTag("Stream_SettingsIcon"),
                style = if (showMenu) StreamButtonStyleDefaults.primarySolid else StreamButtonStyleDefaults.secondarySolid,
            )

            if (showMenu) {
                Popup(
                    onDismissRequest = {
                        showMenu = !showMenu
                    },
                    offset = popupPosition,
                ) {
                    Column(
                        modifier = Modifier
                            .width(200.dp)
                            .background(
                                VideoTheme.colors.backgroundCoreSurfaceDefault,
                                RoundedCornerShape(24.dp),
                            )
                            .padding(16.dp),
                    ) {
                        if (showDirectCall) {
                            StreamTextButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("Stream_DirectCallButton"),
                                text = stringResource(id = R.string.direct_call),
                                leadingIcon = rememberVectorPainter(Icons.Default.Call),
                                style = StreamButtonStyleDefaults.secondarySolid,
                                onClick = {
                                    showMenu = false
                                    onDirectCallClick.invoke()
                                },
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        if (!isProduction) {
                            StreamTextButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("Stream_CallSettingsButton"),
                                leadingIcon = rememberVectorPainter(Icons.Default.Settings),
                                style = StreamButtonStyleDefaults.secondaryGhost,
                                text = stringResource(id = R.string.call_settings),
                                onClick = {
                                    showMenu = false
                                    onCallSettingsClink()
                                },
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            StreamTextButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("Stream_ExportLogsButton"),
                                leadingIcon = rememberVectorPainter(
                                    Icons.AutoMirrored.Default.DriveFileMove,
                                ),
                                style = StreamButtonStyleDefaults.secondaryGhost,
                                text = stringResource(id = R.string.logs),
                                onClick = {
                                    showMenu = false
                                    onLogsClick()
                                },
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            StreamTextButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("Stream_SignOutButton"),
                                leadingIcon = rememberVectorPainter(
                                    Icons.AutoMirrored.Filled.Logout,
                                ),
                                style = StreamButtonStyleDefaults.secondaryGhost,
                                text = stringResource(id = R.string.sign_out),
                                onClick = {
                                    showMenu = false
                                    onSignOutClick()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallJoinBody(
    modifier: Modifier,
    prefilledCallId: String? = null,
    openCamera: () -> Unit,
    callJoinViewModel: CallJoinViewModel = hiltViewModel(),
    isNetworkAvailable: Boolean,
) {
    val user by if (LocalInspectionMode.current) {
        remember { mutableStateOf(previewUsers[0]) }
    } else {
        callJoinViewModel.user.collectAsState(initial = null)
    }

    if (!isNetworkAvailable) {
        NoInternetUiResponsive()
    } else {
        if (user != null) {
            CallActualContentResponsive(
                modifier = modifier.fillMaxSize(),
                onJoinCall = {
                    callJoinViewModel.handleUiEvent(CallJoinEvent.JoinCall(callId = it))
                },
                onNewCall = {
                    callJoinViewModel.handleUiEvent(CallJoinEvent.JoinCall())
                },
                gotoQR = {
                    openCamera()
                },
                prefilledCallId = prefilledCallId,
            )
        }
    }
}

@Composable
private fun CallActualContentResponsive(
    modifier: Modifier = Modifier,
    onJoinCall: (String) -> Unit,
    onNewCall: () -> Unit,
    gotoQR: () -> Unit,
    prefilledCallId: String? = null,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape) {
        CallActualContentLandscape(modifier, onJoinCall, onNewCall, gotoQR, prefilledCallId)
    } else {
        CallActualContentPortrait(modifier, onJoinCall, onNewCall, gotoQR, prefilledCallId)
    }
}

@Composable
private fun CallActualContentPortrait(
    modifier: Modifier = Modifier,
    onJoinCall: (String) -> Unit,
    onNewCall: () -> Unit,
    gotoQR: () -> Unit,
    prefilledCallId: String? = null,
) = Box(modifier = Modifier.background(VideoTheme.colors.backgroundCoreApp)) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .semantics { testTagsAsResourceId = true },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StreamLogo(Modifier.size(102.dp))
        Spacer(modifier = Modifier.height(24.dp))
        AppName()
        Spacer(modifier = Modifier.height(20.dp))
        Description(text = stringResource(id = R.string.join_description))
        Spacer(modifier = Modifier.height(24.dp))
        JoinCallForm(prefilledCallId) {
            onJoinCall(it)
        }
        Spacer(modifier = Modifier.height(8.dp))
        StreamTextButton(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("Stream_StartNewCallButton"),
            text = stringResource(id = R.string.start_a_new_call),
            leadingIcon = rememberVectorPainter(Icons.Default.VideoCall),
            onClick = { onNewCall() },
        )
        Spacer(modifier = Modifier.height(8.dp))
        StreamTextButton(
            style = StreamButtonStyleDefaults.secondaryGhost,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("Stream_ScanQrCodeButton"),
            text = stringResource(id = R.string.scan_qr_code),
            leadingIcon = rememberVectorPainter(Icons.Default.QrCodeScanner),
            onClick = { gotoQR() },
        )
    }
}

@Composable
private fun CallActualContentLandscape(
    modifier: Modifier = Modifier,
    onJoinCall: (String) -> Unit,
    onNewCall: () -> Unit,
    gotoQR: () -> Unit,
    prefilledCallId: String? = null,
) = Box(modifier = Modifier.background(VideoTheme.colors.backgroundCoreApp)) {
    Row {
        Column(
            modifier = modifier
                .padding(horizontal = 16.dp)
                .weight(1f)
                .semantics { testTagsAsResourceId = true },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StreamLogo(Modifier.size(72.dp))
            Spacer(modifier = Modifier.height(8.dp))
            AppName()
            Description(text = stringResource(id = R.string.join_description))
            Spacer(modifier = Modifier.height(8.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .align(Alignment.CenterVertically),
        ) {
            JoinCallForm(prefilledCallId) {
                onJoinCall(it)
            }
            Spacer(modifier = Modifier.height(8.dp))
            StreamTextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("Stream_StartNewCallButton"),
                text = stringResource(id = R.string.start_a_new_call),
                leadingIcon = rememberVectorPainter(Icons.Default.VideoCall),
                onClick = { onNewCall() },
            )
            Spacer(modifier = Modifier.height(8.dp))
            StreamTextButton(
                style = StreamButtonStyleDefaults.secondaryGhost,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("Stream_ScanQrCodeButton"),
                text = stringResource(id = R.string.scan_qr_code),
                leadingIcon = rememberVectorPainter(Icons.Default.QrCodeScanner),
                onClick = { gotoQR() },
            )
        }
    }
}

@Composable
private fun StreamLogo(modifier: Modifier) {
    Image(
        modifier = modifier,
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
                    spanStyle = SpanStyle(VideoTheme.colors.accentSuccess),
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
        style = VideoTheme.typography.bodyDefault,
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
    prefilledCallId: String? = null,
    joinCall: (String) -> Unit,
) {
    var callId by remember {
        mutableStateOf(
            TextFieldValue(
                if (prefilledCallId?.isNotEmpty() == true) {
                    prefilledCallId
                } else {
                    defaultCallId
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
                .fillMaxHeight()
                .testTag("Stream_CallIdInputField"),
            onValueChange = { callId = it },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Email,
            ),
            value = callId,
            placeholder = stringResource(id = R.string.join_call_call_id_hint),
            keyboardActions = KeyboardActions(
                onDone = {
                    joinCall(callId.text)
                },
            ),
        )

        StreamTextButton(
            leadingIcon = rememberVectorPainter(Icons.AutoMirrored.Filled.Login),
            style = StreamButtonStyleDefaults.primarySolid,
            modifier = Modifier
                .padding(start = 16.dp)
                .fillMaxHeight()
                .testTag("Stream_JoinCallButton"),
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
    StreamDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(id = R.string.sign_out),
        message = stringResource(R.string.are_you_sure_sign_out),
    ) {
        StreamTextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onConfirmation,
            text = stringResource(id = R.string.sign_out),
            size = StreamButtonSize.Large,
        )
        StreamTextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onDismissRequest,
            text = stringResource(R.string.cancel),
            style = StreamButtonStyleDefaults.secondaryOutline,
            size = StreamButtonSize.Large,
        )
    }
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

@Composable
private fun NoInternetUiResponsive() {
    val selectedEnv by AppConfig.currentEnvironment.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape) {
        NoInternetUiLandscape(selectedEnv)
    } else {
        NoInternetUiPortrait(selectedEnv)
    }
}

@Composable
private fun NoInternetUiPortrait(selectedEnv: StreamEnvironment) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StreamLogo(Modifier.size(102.dp))
        Spacer(modifier = Modifier.height(25.dp))
        AppName(selectedEnv)
        Spacer(modifier = Modifier.height(25.dp))
        Description(text = stringResource(id = R.string.you_are_offline))
    }
}

@Composable
private fun NoInternetUiLandscape(selectedEnv: StreamEnvironment) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StreamLogo(Modifier.size(72.dp))
        Spacer(modifier = Modifier.height(12.dp))
        AppName(selectedEnv)
        Spacer(modifier = Modifier.height(12.dp))
        Description(text = stringResource(id = R.string.you_are_offline))
    }
}

@Preview(
    name = "Portrait Preview",
    showBackground = true,
    uiMode = Configuration.UI_MODE_TYPE_NORMAL,
    device = "spec:width=411dp,height=891dp,dpi=420",
)
@Composable
private fun CallJoinScreenPortraitPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallActualContentPortrait(onJoinCall = {}, onNewCall = {}, gotoQR = {})
    }
}

@Preview(
    name = "Landscape Preview",
    showBackground = true,
    uiMode = Configuration.UI_MODE_TYPE_NORMAL,
    device = "spec:width=891dp,height=411dp,dpi=420",
)
@Composable
private fun CallJoinScreenLandscapePreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallActualContentLandscape(onJoinCall = {}, onNewCall = {}, gotoQR = {})
    }
}

@Preview
@Composable
private fun CallJoinScreenHeader() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallJoinHeader(previewUsers[0], false, true, {}, {}, {}, {}, {})
    }
}
