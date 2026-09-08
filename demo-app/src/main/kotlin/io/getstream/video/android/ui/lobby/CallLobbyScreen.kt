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

package io.getstream.video.android.ui.lobby

import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.BuildConfig
import io.getstream.video.android.CallActivity
import io.getstream.video.android.R
import io.getstream.video.android.compose.permission.VideoPermissionsState
import io.getstream.video.android.compose.permission.rememberCallPermissionsState
import io.getstream.video.android.compose.theme.CallLobbyJoinContentParams
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.compose.ui.components.base.StreamButtonStyleDefaults
import io.getstream.video.android.compose.ui.components.base.StreamIconButton
import io.getstream.video.android.compose.ui.components.call.lobby.CallLobby
import io.getstream.video.android.compose.ui.components.call.lobby.buildDefaultLobbyControlActions
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleHifiAudio
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.core.events.ParticipantCount
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.mock.previewUsers
import io.getstream.video.android.model.User
import io.getstream.video.android.ui.common.StreamCallActivity
import kotlinx.coroutines.delay
import io.getstream.video.android.compose.R as ComposeR

@Composable
fun CallLobbyScreen(
    callLobbyViewModel: CallLobbyViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val isLoading by callLobbyViewModel.isLoading.collectAsStateWithLifecycle()
    val isMicrophoneEnabled by callLobbyViewModel.microphoneEnabled.collectAsStateWithLifecycle()
    val isCameraEnabled by callLobbyViewModel.cameraEnabled.collectAsStateWithLifecycle()
    val hifiAudioEnabled by callLobbyViewModel.hifiAudioEnabled.collectAsStateWithLifecycle()
    val settingsLoaded by callLobbyViewModel.settingsLoaded.collectAsStateWithLifecycle()
    val participantCounts by callLobbyViewModel.call.state.participantCounts.collectAsStateWithLifecycle()
    val call by remember {
        mutableStateOf(callLobbyViewModel.call)
    }

    val showHifiAudioToggle = settingsLoaded && hifiAudioEnabled
    val isNewCall = callLobbyViewModel.isNewCall

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VideoTheme.colors.backgroundCoreApp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("call_lobby"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CallLobbyHeader(
                onBack = {
                    callLobbyViewModel.leaveCall()
                    onBack()
                },
                callLobbyViewModel = callLobbyViewModel,
            )

            CallLobbyBodyResponsive(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                call = call,
                isMicrophoneEnabled = isMicrophoneEnabled,
                isCameraEnabled = isCameraEnabled,
                showHifiAudioToggle = showHifiAudioToggle,
                isNewCall = isNewCall,
                participantCounts = participantCounts,
                onToggleCamera = callLobbyViewModel::enableCamera,
                onToggleMicrophone = callLobbyViewModel::enableMicrophone,
                onToggleHifiAudio = callLobbyViewModel::setAudioBitrateProfile,
                onJoinCall = { callLobbyViewModel.handleUiEvent(CallLobbyEvent.JoinCall) },
            )
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = VideoTheme.colors.accentPrimary,
            )
        }
    }
}

@Composable
private fun CallLobbyHeader(
    callLobbyViewModel: CallLobbyViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val uiState by callLobbyViewModel.uiState.collectAsState(initial = CallLobbyUiState.Nothing)
    val isLoggedOut by callLobbyViewModel.isLoggedOut.collectAsState(initial = false)
    val user = callLobbyViewModel.user.collectAsState(initial = null)

    HandleCallLobbyUiState(
        callLobbyUiState = uiState,
        callLobbyViewModel = callLobbyViewModel,
    )

    CallLobbyHeaderContent(user, onBack)

    LaunchedEffect(key1 = isLoggedOut) {
        if (isLoggedOut) {
            onBack.invoke()
        }
    }
}

/** The account header: avatar, user id and the close action. Demo only, not part of the SDK lobby. */
@Composable
private fun CallLobbyHeaderContent(
    user: State<User?>,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val userValue = user.value
        if (userValue != null) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                UserAvatar(
                    modifier = Modifier.size(40.dp),
                    userImage = userValue.image,
                    userName = userValue.userNameOrId,
                )
            }
        }

        Text(
            modifier = Modifier.weight(1f),
            text = userValue?.id.orEmpty(),
            style = VideoTheme.typography.headingExtraSmall,
            color = VideoTheme.colors.textPrimary,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
        StreamIconButton(
            modifier = Modifier.testTag("Stream_LobbyCloseButton"),
            onClick = onBack,
            icon = painterResource(ComposeR.drawable.stream_design_ic_xmark),
            contentDescription = stringResource(id = R.string.cancel),
            style = StreamButtonStyleDefaults.secondaryGhost,
        )
    }
}

@Composable
private fun CallLobbyBodyResponsive(
    modifier: Modifier = Modifier,
    call: Call,
    isCameraEnabled: Boolean,
    isMicrophoneEnabled: Boolean,
    showHifiAudioToggle: Boolean = false,
    isNewCall: Boolean,
    participantCounts: ParticipantCount?,
    onToggleCamera: (Boolean) -> Unit,
    onToggleMicrophone: (Boolean) -> Unit,
    onToggleHifiAudio: (Boolean) -> Unit,
    onJoinCall: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape) {
        CallLobbyBodyLandscape(
            modifier = modifier,
            call = call,
            isCameraEnabled = isCameraEnabled,
            isMicrophoneEnabled = isMicrophoneEnabled,
            showHifiAudioToggle = showHifiAudioToggle,
            isNewCall = isNewCall,
            participantCounts = participantCounts,
            onToggleCamera = onToggleCamera,
            onToggleMicrophone = onToggleMicrophone,
            onToggleHifiAudio = onToggleHifiAudio,
            onJoinCall = onJoinCall,
        )
    } else {
        CallLobbyBodyPortrait(
            modifier = modifier,
            call = call,
            isCameraEnabled = isCameraEnabled,
            isMicrophoneEnabled = isMicrophoneEnabled,
            showHifiAudioToggle = showHifiAudioToggle,
            isNewCall = isNewCall,
            participantCounts = participantCounts,
            onToggleCamera = onToggleCamera,
            onToggleMicrophone = onToggleMicrophone,
            onToggleHifiAudio = onToggleHifiAudio,
            onJoinCall = onJoinCall,
        )
    }
}

@Composable
private fun CallLobbyBodyPortrait(
    modifier: Modifier = Modifier,
    call: Call,
    isCameraEnabled: Boolean,
    isMicrophoneEnabled: Boolean,
    showHifiAudioToggle: Boolean = false,
    isNewCall: Boolean,
    participantCounts: ParticipantCount?,
    onToggleCamera: (Boolean) -> Unit,
    onToggleMicrophone: (Boolean) -> Unit,
    onToggleHifiAudio: (Boolean) -> Unit,
    onJoinCall: () -> Unit,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 40.dp)
            .semantics { testTagsAsResourceId = true },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LobbyTitle(isNewCall = isNewCall, participantCounts = participantCounts)
        Spacer(modifier = Modifier.height(32.dp))
        DemoCallLobby(
            call = call,
            isCameraEnabled = isCameraEnabled,
            isMicrophoneEnabled = isMicrophoneEnabled,
            showHifiAudioToggle = showHifiAudioToggle,
            isNewCall = isNewCall,
            onToggleCamera = onToggleCamera,
            onToggleMicrophone = onToggleMicrophone,
            onToggleHifiAudio = onToggleHifiAudio,
            onJoinCall = onJoinCall,
        )
    }
}

@Composable
private fun CallLobbyBodyLandscape(
    modifier: Modifier = Modifier,
    call: Call,
    isCameraEnabled: Boolean,
    isMicrophoneEnabled: Boolean,
    showHifiAudioToggle: Boolean = false,
    isNewCall: Boolean,
    participantCounts: ParticipantCount?,
    onToggleCamera: (Boolean) -> Unit,
    onToggleMicrophone: (Boolean) -> Unit,
    onToggleHifiAudio: (Boolean) -> Unit,
    onJoinCall: () -> Unit,
) {
    Row(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .semantics { testTagsAsResourceId = true },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            LobbyTitle(isNewCall = isNewCall, participantCounts = participantCounts)
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            DemoCallLobby(
                call = call,
                isCameraEnabled = isCameraEnabled,
                isMicrophoneEnabled = isMicrophoneEnabled,
                showHifiAudioToggle = showHifiAudioToggle,
                isNewCall = isNewCall,
                onToggleCamera = onToggleCamera,
                onToggleMicrophone = onToggleMicrophone,
                onToggleHifiAudio = onToggleHifiAudio,
                onJoinCall = onJoinCall,
            )
        }
    }
}

/**
 * The SDK lobby with the demo specific controls: the high quality audio toggle in development
 * builds, and a join label that reads "Start Call" while nobody is in the call yet.
 */
@Composable
private fun DemoCallLobby(
    call: Call,
    isCameraEnabled: Boolean,
    isMicrophoneEnabled: Boolean,
    showHifiAudioToggle: Boolean,
    isNewCall: Boolean,
    onToggleCamera: (Boolean) -> Unit,
    onToggleMicrophone: (Boolean) -> Unit,
    onToggleHifiAudio: (Boolean) -> Unit,
    onJoinCall: () -> Unit,
) {
    val onCallAction: (CallAction) -> Unit = { action ->
        when (action) {
            is ToggleCamera -> onToggleCamera(action.isEnabled)
            is ToggleMicrophone -> onToggleMicrophone(action.isEnabled)
            is ToggleHifiAudio -> onToggleHifiAudio(action.isHifiAudioEnabled)
            else -> Unit
        }
    }
    val permissions = rememberCallPermissionsState(call = call)
    val joinLabel = if (isNewCall) {
        stringResource(id = R.string.start_call)
    } else {
        stringResource(id = R.string.join_call)
    }

    CallLobby(
        call = call,
        modifier = Modifier.fillMaxWidth(),
        isCameraEnabled = isCameraEnabled,
        isMicrophoneEnabled = isMicrophoneEnabled,
        permissions = permissions,
        onCallAction = onCallAction,
        lobbyControlsContent = { modifier, _ ->
            LobbyControls(
                modifier = modifier,
                call = call,
                isCameraEnabled = isCameraEnabled,
                isMicrophoneEnabled = isMicrophoneEnabled,
                showHifiAudioToggle = showHifiAudioToggle,
                permissions = permissions,
                onCallAction = onCallAction,
            )
        },
        onJoinCall = onJoinCall,
        joinCallContent = { modifier, lobbyCall ->
            VideoTheme.componentFactory.CallLobbyJoinContent(
                params = CallLobbyJoinContentParams(
                    call = lobbyCall,
                    onJoinCall = onJoinCall,
                    text = joinLabel,
                    modifier = modifier,
                ),
            )
        },
    )

    if (BuildConfig.BUILD_TYPE == "benchmark") {
        LaunchedEffect(key1 = Unit) {
            delay(300)
            onToggleCamera(true)
            onToggleMicrophone(true)
        }
    }
}

@Composable
private fun LobbyControls(
    modifier: Modifier,
    call: Call,
    isCameraEnabled: Boolean,
    isMicrophoneEnabled: Boolean,
    showHifiAudioToggle: Boolean,
    permissions: VideoPermissionsState,
    onCallAction: (CallAction) -> Unit,
) {
    val isCameraUnavailable = permissions.isCameraPermissionDenied
    val isMicrophoneUnavailable = permissions.isMicrophonePermissionDenied
    val actions = buildDefaultLobbyControlActions(
        call = call,
        onCallAction = { action ->
            val needsPermission = (action is ToggleCamera && isCameraUnavailable) ||
                (action is ToggleMicrophone && isMicrophoneUnavailable)
            if (needsPermission) permissions.launchPermissionRequest() else onCallAction(action)
        },
        isCameraEnabled = isCameraEnabled,
        isMicrophoneEnabled = isMicrophoneEnabled,
        isCameraUnavailable = isCameraUnavailable,
        isMicrophoneUnavailable = isMicrophoneUnavailable,
        showHifiAudioToggle = showHifiAudioToggle,
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.forEach { action -> action() }
    }
}

/** The title block above the lobby. The title carries the participants-count tag the E2E suite asserts on. */
@Composable
private fun LobbyTitle(isNewCall: Boolean, participantCounts: ParticipantCount?) {
    val totalParticipants = participantCounts?.total ?: 0

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            modifier = Modifier.size(32.dp),
            painter = painterResource(ComposeR.drawable.stream_design_ic_language),
            tint = VideoTheme.colors.accentPrimary,
            contentDescription = null,
        )
        Text(
            modifier = Modifier.testTag("Stream_ParticipantsCount_$totalParticipants"),
            text = if (isNewCall) {
                stringResource(id = R.string.set_up_your_call)
            } else {
                stringResource(id = R.string.set_up_your_call_before_joining)
            },
            style = VideoTheme.typography.headingLarge,
            color = VideoTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HandleCallLobbyUiState(
    callLobbyUiState: CallLobbyUiState,
    callLobbyViewModel: CallLobbyViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(key1 = callLobbyUiState) {
        when (callLobbyUiState) {
            is CallLobbyUiState.JoinCompleted -> {
                val intent = StreamCallActivity.callIntent(
                    context = context,
                    cid = callLobbyViewModel.callId,
                    clazz = CallActivity::class.java,
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)
            }

            is CallLobbyUiState.JoinFailed -> {
                Toast.makeText(context, callLobbyUiState.reason, Toast.LENGTH_SHORT).show()
            }

            else -> Unit
        }
    }
}

@Preview
@Composable
private fun CallLobbyHeaderPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallLobbyHeaderContent(
            user = remember {
                mutableStateOf(previewUsers[0])
            },
        ) {
        }
    }
}

@Preview(
    name = "Portrait Preview",
    showBackground = true,
    uiMode = Configuration.UI_MODE_TYPE_NORMAL,
    device = "spec:width=411dp,height=891dp,dpi=420",
)
@Composable
private fun CallLobbyBodyPortraitPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallLobbyBodyPortrait(
            modifier = Modifier.background(VideoTheme.colors.backgroundCoreApp),
            isCameraEnabled = false,
            isMicrophoneEnabled = false,
            call = previewCall,
            isNewCall = false,
            participantCounts = ParticipantCount(1, 1),
            onToggleMicrophone = {},
            onToggleCamera = {},
            onToggleHifiAudio = {},
            onJoinCall = {},
        )
    }
}

@Preview(
    name = "Landscape Preview",
    showBackground = true,
    uiMode = Configuration.UI_MODE_TYPE_NORMAL,
    device = "spec:width=891dp,height=411dp,dpi=420",
)
@Composable
private fun CallLobbyBodyLandscapePreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallLobbyBodyLandscape(
            modifier = Modifier.background(VideoTheme.colors.backgroundCoreApp),
            isCameraEnabled = false,
            isMicrophoneEnabled = false,
            call = previewCall,
            isNewCall = true,
            participantCounts = ParticipantCount(0, 0),
            onToggleMicrophone = {},
            onToggleCamera = {},
            onToggleHifiAudio = {},
            onJoinCall = {},
        )
    }
}
