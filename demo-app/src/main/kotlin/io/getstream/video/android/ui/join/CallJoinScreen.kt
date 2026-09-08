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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
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
import io.getstream.video.android.compose.R as ComposeR

@Composable
fun CallJoinScreen(
    prefilledCallId: String? = null,
    callJoinViewModel: CallJoinViewModel = hiltViewModel(),
    navigateToCallLobby: (callId: String, isNewCall: Boolean) -> Unit,
    navigateUpToLogin: (autoLogIn: Boolean) -> Unit,
    navigateToDirectCallJoin: () -> Unit,
    navigateToBarcodeScanner: () -> Unit = {},
) {
    val uiState by callJoinViewModel.uiState.collectAsState(CallJoinUiState.Nothing)
    val user by callJoinViewModel.user.collectAsState(initial = null)
    val isLoggedOut by callJoinViewModel.isLoggedOut.collectAsState(initial = false)
    val isNetworkAvailable by callJoinViewModel.isNetworkAvailable.collectAsStateWithLifecycle()

    var isSignOutDialogVisible by remember { mutableStateOf(false) }
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
            onLeaveClick = { if (isNetworkAvailable) isSignOutDialogVisible = true },
            onLogsClick = { renderLogsFileUi = true },
            onCallSettingsClick = { renderCallSettingsUi = true },
        )

        CallJoinBody(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            user = user,
            prefilledCallId = prefilledCallId,
            isNetworkAvailable = isNetworkAvailable,
            onJoinCall = { callJoinViewModel.handleUiEvent(CallJoinEvent.JoinCall(callId = it)) },
            onNewCall = { callJoinViewModel.handleUiEvent(CallJoinEvent.JoinCall()) },
            onScanQrCode = navigateToBarcodeScanner,
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
    navigateToCallLobby: (callId: String, isNewCall: Boolean) -> Unit,
    navigateUpToLogin: () -> Unit,
) {
    LaunchedEffect(key1 = callJoinUiState) {
        when (callJoinUiState) {
            is CallJoinUiState.JoinCompleted -> navigateToCallLobby.invoke(
                callJoinUiState.callId,
                callJoinUiState.isNewCall,
            )

            is CallJoinUiState.GoBackToLogin -> navigateUpToLogin.invoke()

            else -> Unit
        }
    }
}

/**
 * The account header: avatar, user name and either the developer menu or the sign out action.
 * Production builds without the menu sign out through the trailing icon; the avatar long press stays
 * as a second entry point.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CallJoinHeader(
    user: User?,
    isProduction: Boolean = StreamBuildFlavorUtil.isProduction,
    showDirectCall: Boolean = user?.custom?.get("email")?.contains("getstreamio") == true,
    onAvatarLongClick: () -> Unit,
    onDirectCallClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onLeaveClick: () -> Unit,
    onLogsClick: () -> Unit,
    onCallSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        user?.let {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .then(
                        if (isProduction) {
                            Modifier.combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                                onLongClick = onAvatarLongClick,
                            )
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                UserAvatar(
                    modifier = Modifier.size(40.dp),
                    userImage = it.image,
                    userName = it.userNameOrId,
                )
            }
        }

        Text(
            modifier = Modifier
                .weight(1f)
                .testTag("Stream_UserName"),
            text = user?.userNameOrId.orEmpty(),
            style = VideoTheme.typography.headingExtraSmall,
            color = VideoTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (!isProduction || showDirectCall) {
            DeveloperMenu(
                isProduction = isProduction,
                showDirectCall = showDirectCall,
                onDirectCallClick = onDirectCallClick,
                onSignOutClick = onSignOutClick,
                onLogsClick = onLogsClick,
                onCallSettingsClick = onCallSettingsClick,
            )
        } else {
            StreamIconButton(
                onClick = onLeaveClick,
                icon = painterResource(ComposeR.drawable.stream_design_ic_leave),
                contentDescription = stringResource(id = R.string.sign_out),
                style = StreamButtonStyleDefaults.secondaryGhost,
            )
        }
    }
}

@Composable
private fun DeveloperMenu(
    isProduction: Boolean,
    showDirectCall: Boolean,
    onDirectCallClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onLogsClick: () -> Unit,
    onCallSettingsClick: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var popupPosition by remember { mutableStateOf(IntOffset(0, 0)) }
    var buttonSize by remember { mutableStateOf(IntSize(0, 0)) }

    StreamIconButton(
        onClick = { showMenu = !showMenu },
        icon = rememberVectorPainter(Icons.Default.Settings),
        contentDescription = stringResource(id = R.string.call_settings),
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
        style = if (showMenu) {
            StreamButtonStyleDefaults.primarySolid
        } else {
            StreamButtonStyleDefaults.secondaryGhost
        },
    )

    if (showMenu) {
        Popup(
            onDismissRequest = { showMenu = false },
            offset = popupPosition,
        ) {
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .background(
                        VideoTheme.colors.backgroundCoreElevation1,
                        RoundedCornerShape(16.dp),
                    )
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (showDirectCall) {
                    StreamTextButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("Stream_DirectCallButton"),
                        text = stringResource(id = R.string.direct_call),
                        leadingIcon = rememberVectorPainter(Icons.Default.Call),
                        style = StreamButtonStyleDefaults.secondaryGhost,
                        onClick = {
                            showMenu = false
                            onDirectCallClick.invoke()
                        },
                    )
                }
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
                            onCallSettingsClick()
                        },
                    )
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
                    StreamTextButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("Stream_SignOutButton"),
                        leadingIcon = rememberVectorPainter(Icons.AutoMirrored.Filled.Logout),
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

@Composable
private fun CallJoinBody(
    modifier: Modifier,
    user: User?,
    prefilledCallId: String?,
    isNetworkAvailable: Boolean,
    onJoinCall: (String) -> Unit,
    onNewCall: () -> Unit,
    onScanQrCode: () -> Unit,
) {
    if (!isNetworkAvailable) {
        NoInternetUi(modifier = modifier)
    } else if (user != null) {
        CallActualContentResponsive(
            modifier = modifier,
            prefilledCallId = prefilledCallId,
            onJoinCall = onJoinCall,
            onNewCall = onNewCall,
            onScanQrCode = onScanQrCode,
        )
    }
}

@Composable
private fun CallActualContentResponsive(
    modifier: Modifier = Modifier,
    prefilledCallId: String? = null,
    onJoinCall: (String) -> Unit,
    onNewCall: () -> Unit,
    onScanQrCode: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape) {
        CallActualContentLandscape(
            modifier = modifier,
            prefilledCallId = prefilledCallId,
            onJoinCall = onJoinCall,
            onNewCall = onNewCall,
            onScanQrCode = onScanQrCode,
        )
    } else {
        CallActualContentPortrait(
            modifier = modifier,
            prefilledCallId = prefilledCallId,
            onJoinCall = onJoinCall,
            onNewCall = onNewCall,
            onScanQrCode = onScanQrCode,
        )
    }
}

@Composable
private fun CallActualContentPortrait(
    modifier: Modifier = Modifier,
    prefilledCallId: String? = null,
    onJoinCall: (String) -> Unit,
    onNewCall: () -> Unit,
    onScanQrCode: () -> Unit,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 40.dp)
            .semantics { testTagsAsResourceId = true },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StartCallIntro(
            modifier = Modifier.padding(horizontal = 40.dp),
            description = stringResource(id = R.string.join_description),
        )
        Spacer(modifier = Modifier.height(40.dp))
        StartCallForm(
            prefilledCallId = prefilledCallId,
            onJoinCall = onJoinCall,
            onNewCall = onNewCall,
            onScanQrCode = onScanQrCode,
        )
    }
}

@Composable
private fun CallActualContentLandscape(
    modifier: Modifier = Modifier,
    prefilledCallId: String? = null,
    onJoinCall: (String) -> Unit,
    onNewCall: () -> Unit,
    onScanQrCode: () -> Unit,
) {
    Row(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .semantics { testTagsAsResourceId = true },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StartCallIntro(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp),
            description = stringResource(id = R.string.join_description),
        )
        StartCallForm(
            modifier = Modifier.weight(1f),
            prefilledCallId = prefilledCallId,
            onJoinCall = onJoinCall,
            onNewCall = onNewCall,
            onScanQrCode = onScanQrCode,
        )
    }
}

/** The illustration, title and description shared by the online and the offline states. */
@Composable
private fun StartCallIntro(
    modifier: Modifier = Modifier,
    description: String,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Image(
            modifier = Modifier.size(width = 234.dp, height = 160.dp),
            painter = painterResource(id = R.drawable.start_call_illustration),
            contentDescription = null,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.stream_video_calling),
                style = VideoTheme.typography.headingLarge,
                color = VideoTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = description,
                style = VideoTheme.typography.bodyDefault,
                color = VideoTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The call id field with its join action, the "OR" separator, and the new call and QR code actions.
 * Joining is disabled while the field is blank.
 */
@Composable
private fun StartCallForm(
    modifier: Modifier = Modifier,
    prefilledCallId: String? = null,
    onJoinCall: (String) -> Unit,
    onNewCall: () -> Unit,
    onScanQrCode: () -> Unit,
) {
    var callId by remember {
        mutableStateOf(TextFieldValue(prefilledCallId?.takeIf { it.isNotEmpty() } ?: defaultCallId))
    }
    val canJoin = callId.text.isNotBlank()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            StreamTextField(
                modifier = Modifier
                    .weight(1f)
                    .testTag("Stream_CallIdInputField"),
                value = callId,
                onValueChange = { callId = it },
                placeholder = stringResource(id = R.string.join_call_call_id_hint),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
                keyboardActions = KeyboardActions(
                    onDone = { if (canJoin) onJoinCall(callId.text) },
                ),
            )
            StreamTextButton(
                modifier = Modifier.testTag("Stream_JoinCallButton"),
                text = stringResource(id = R.string.join_call),
                style = StreamButtonStyleDefaults.primaryGhost,
                size = StreamButtonSize.Large,
                enabled = canJoin,
                onClick = { onJoinCall(callId.text) },
            )
        }

        OrSeparator()

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StreamTextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("Stream_StartNewCallButton"),
                text = stringResource(id = R.string.start_a_new_call),
                size = StreamButtonSize.Large,
                onClick = onNewCall,
            )
            StreamTextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("Stream_ScanQrCodeButton"),
                text = stringResource(id = R.string.scan_qr_code),
                style = StreamButtonStyleDefaults.secondaryGhost,
                size = StreamButtonSize.Large,
                leadingIcon = painterResource(ComposeR.drawable.stream_design_ic_qr_code_fill),
                onClick = onScanQrCode,
            )
        }
    }
}

@Composable
private fun OrSeparator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SeparatorLine(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(id = R.string.or),
            style = VideoTheme.typography.metadataEmphasis,
            color = VideoTheme.colors.textDisabled,
        )
        SeparatorLine(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SeparatorLine(modifier: Modifier) {
    Box(
        modifier = modifier
            .height(1.dp)
            .background(VideoTheme.colors.borderCoreDefault),
    )
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

@Composable
private fun NoInternetUi(modifier: Modifier = Modifier) {
    val selectedEnv by AppConfig.currentEnvironment.collectAsStateWithLifecycle()
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StartCallIntro(
            modifier = Modifier.padding(horizontal = 40.dp),
            description = stringResource(id = R.string.you_are_offline),
        )
        Spacer(modifier = Modifier.height(16.dp))
        EnvironmentLabel(selectedEnv)
    }
}

@Composable
private fun EnvironmentLabel(env: StreamEnvironment) {
    Text(
        text = env.displayName,
        style = VideoTheme.typography.captionDefault,
        color = VideoTheme.colors.textTertiary,
        textAlign = TextAlign.Center,
    )
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
        Column(modifier = Modifier.background(VideoTheme.colors.backgroundCoreApp)) {
            CallJoinHeader(previewUsers[0], true, false, {}, {}, {}, {}, {}, {})
            CallActualContentPortrait(
                modifier = Modifier.weight(1f),
                onJoinCall = {},
                onNewCall = {},
                onScanQrCode = {},
            )
        }
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
        CallActualContentLandscape(
            modifier = Modifier.background(VideoTheme.colors.backgroundCoreApp),
            onJoinCall = {},
            onNewCall = {},
            onScanQrCode = {},
        )
    }
}

@Preview
@Composable
private fun CallJoinScreenHeaderPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallJoinHeader(previewUsers[0], false, true, {}, {}, {}, {}, {}, {})
    }
}
