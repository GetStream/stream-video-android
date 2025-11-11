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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LockPerson
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.BuildConfig
import io.getstream.video.android.CallActivity
import io.getstream.video.android.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.compose.ui.components.avatar.UserAvatarBackground
import io.getstream.video.android.compose.ui.components.base.StreamButton
import io.getstream.video.android.compose.ui.components.call.controls.ControlActions
import io.getstream.video.android.compose.ui.components.call.lobby.CallLobby
import io.getstream.video.android.compose.ui.components.call.lobby.buildDefaultLobbyControlActions
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.compose.ui.components.video.config.videoRenderConfig
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
    val call by remember {
        mutableStateOf(callLobbyViewModel.call)
    }

    val showHifiAudioToggle = settingsLoaded && hifiAudioEnabled

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(VideoTheme.colors.baseSheetPrimary)
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
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth()
                    .weight(1f),
                isMicrophoneEnabled = isMicrophoneEnabled,
                isCameraEnabled = isCameraEnabled,
                showHifiAudioToggle = showHifiAudioToggle,
                onToggleCamera = {
                    callLobbyViewModel.enableCamera(it)
                },
                onToggleMicrophone = {
                    callLobbyViewModel.enableMicrophone(it)
                },
                onToggleHifiAudio = {
                    callLobbyViewModel.setAudioBitrateProfile(it)
                },
                call = call,
            ) {
                LobbyDescription(callLobbyViewModel = callLobbyViewModel)
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = VideoTheme.colors.brandPrimary,
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

@Composable
private fun CallLobbyHeaderContent(
    user: State<User?>,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(
                horizontal = VideoTheme.dimens.spacingM,
                vertical = VideoTheme.dimens.spacingXs,
            )
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val userValue = user.value
        if (userValue != null) {
            UserAvatar(
                modifier = Modifier.size(32.dp),
                userImage = userValue.image,
                userName = userValue.userNameOrId,
            )

            Spacer(modifier = Modifier.width(4.dp))
        }

        Text(
            modifier = Modifier.weight(1f),
            color = Color.White,
            text = userValue?.id.orEmpty(),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            fontSize = 16.sp,
        )
        IconButton(
            modifier = Modifier
                .padding(8.dp)
                .testTag("Stream_LobbyCloseButton"),
            onClick = {
                onBack()
            },
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = VideoTheme.colors.basePrimary,
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CallLobbyBodyResponsive(
    modifier: Modifier = Modifier,
    call: Call,
    isCameraEnabled: Boolean,
    isMicrophoneEnabled: Boolean,
    showHifiAudioToggle: Boolean = false,
    onToggleCamera: (Boolean) -> Unit,
    onToggleMicrophone: (Boolean) -> Unit,
    onToggleHifiAudio: (Boolean) -> Unit,
    description: @Composable () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape) {
        CallLobbyBodyLandscape(
            modifier,
            call,
            isCameraEnabled,
            isMicrophoneEnabled,
            showHifiAudioToggle,
            onToggleCamera,
            onToggleMicrophone,
            onToggleHifiAudio,
            description,
        )
    } else {
        CallLobbyBodyPortrait(
            modifier,
            call,
            isCameraEnabled,
            isMicrophoneEnabled,
            showHifiAudioToggle,
            onToggleCamera,
            onToggleMicrophone,
            onToggleHifiAudio,
            description,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CallLobbyBodyPortrait(
    modifier: Modifier = Modifier,
    call: Call,
    isCameraEnabled: Boolean,
    isMicrophoneEnabled: Boolean,
    showHifiAudioToggle: Boolean = false,
    onToggleCamera: (Boolean) -> Unit,
    onToggleMicrophone: (Boolean) -> Unit,
    onToggleHifiAudio: (Boolean) -> Unit,
    description: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VideoTheme.colors.baseSheetPrimary)
            .semantics { testTagsAsResourceId = true },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Text and Spacer elements remain unchanged

        // LaunchedEffect to handle initial setup might need adjustments
        // based on how you handle benchmarks or initial setup externally

        Icon(
            modifier = Modifier.size(36.dp),
            imageVector = Icons.Default.Language,
            tint = VideoTheme.colors.brandGreen,
            contentDescription = "",
        )
        Text(
            modifier = Modifier.padding(VideoTheme.dimens.spacingM),
            text = "Set up your test call",
            style = VideoTheme.typography.titleS,
        )
        val onCallAction: (CallAction) -> Unit = { action ->
            when (action) {
                is ToggleCamera -> onToggleCamera(action.isEnabled)
                is ToggleMicrophone -> onToggleMicrophone(action.isEnabled)
                is ToggleHifiAudio -> onToggleHifiAudio(action.isHifiAudioEnabled)
                else -> Unit
            }
        }
        CallLobby(
            call = call,
            modifier = Modifier
                .fillMaxWidth()
                .padding(VideoTheme.dimens.spacingM),
            isCameraEnabled = isCameraEnabled,
            isMicrophoneEnabled = isMicrophoneEnabled,
            onRenderedContent = {
                val videoRendererConfig = remember {
                    videoRenderConfig {
                        this.fallbackContent = {
                            val userName = it.user.userNameOrId
                            val userImage = it.user.image
                            UserAvatarBackground(userImage = userImage, userName = userName)
                        }
                    }
                }
                VideoRenderer(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VideoTheme.colors.baseSheetTertiary)
                        .testTag("on_rendered_content"),
                    call = call,
                    video = it,
                    videoRendererConfig = videoRendererConfig,
                )
            },
            onCallAction = onCallAction,
            lobbyControlsContent = { modifier, _ ->
                ControlActions(
                    modifier = modifier,
                    call = call,
                    actions = buildDefaultLobbyControlActions(
                        call = call,
                        onCallAction = onCallAction,
                        isCameraEnabled = isCameraEnabled,
                        isMicrophoneEnabled = isMicrophoneEnabled,
                        showHifiAudioToggle = showHifiAudioToggle,
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
        description()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CallLobbyBodyLandscape(
    modifier: Modifier = Modifier,
    call: Call,
    isCameraEnabled: Boolean,
    isMicrophoneEnabled: Boolean,
    showHifiAudioToggle: Boolean = false,
    onToggleCamera: (Boolean) -> Unit,
    onToggleMicrophone: (Boolean) -> Unit,
    onToggleHifiAudio: (Boolean) -> Unit,
    description: @Composable () -> Unit,
) {
    Box(modifier = Modifier.background(VideoTheme.colors.baseSheetPrimary)) {
        Row() {
            Column(
                modifier = modifier
                    .weight(1f)
                    .semantics { testTagsAsResourceId = true },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val onCallAction: (CallAction) -> Unit = { action ->
                    when (action) {
                        is ToggleCamera -> onToggleCamera(action.isEnabled)
                        is ToggleMicrophone -> onToggleMicrophone(action.isEnabled)
                        is ToggleHifiAudio -> onToggleHifiAudio(action.isHifiAudioEnabled)
                        else -> Unit
                    }
                }

                CallLobby(
                    call = call,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = VideoTheme.dimens.spacingM,
                            end = VideoTheme.dimens.spacingM,
                        ),
                    isCameraEnabled = isCameraEnabled,
                    isMicrophoneEnabled = isMicrophoneEnabled,
                    onRenderedContent = {
                        Box(
                            modifier = Modifier.fillMaxHeight(),
                            contentAlignment = Alignment.Center,
                        ) {
                            val videoRendererConfig = remember {
                                videoRenderConfig {
                                    this.fallbackContent = {
                                        val userName = it.user.userNameOrId
                                        val userImage = it.user.image
                                        UserAvatarBackground(userImage = userImage, userName = userName)
                                    }
                                }
                            }
                            VideoRenderer(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(VideoTheme.colors.baseSheetTertiary)
                                    .testTag("on_rendered_content"),
                                call = call,
                                video = it,
                                videoRendererConfig = videoRendererConfig,
                            )
                        }
                    },
                    onCallAction = onCallAction,
                    lobbyControlsContent = { _, _ ->
                        ControlActions(
                            modifier = Modifier.padding(),
                            call = call,
                            actions = buildDefaultLobbyControlActions(
                                call = call,
                                onCallAction = onCallAction,
                                isCameraEnabled = isCameraEnabled,
                                isMicrophoneEnabled = isMicrophoneEnabled,
                                showHifiAudioToggle = showHifiAudioToggle,
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

            Column(
                modifier = modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Text and Spacer elements remain unchanged

                // LaunchedEffect to handle initial setup might need adjustments
                // based on how you handle benchmarks or initial setup externally
                Icon(
                    modifier = Modifier.size(36.dp),
                    imageVector = Icons.Default.Language,
                    tint = VideoTheme.colors.brandGreen,
                    contentDescription = "",
                )
                Text(
                    modifier = Modifier.padding(VideoTheme.dimens.spacingM),
                    text = "Set up your test call",
                    style = VideoTheme.typography.titleS,
                )
                description()
            }
        }
    }
}

@Composable
private fun LobbyDescription(
    callLobbyViewModel: CallLobbyViewModel,
) {
    val participantCounts by callLobbyViewModel.call.state.participantCounts.collectAsStateWithLifecycle()

    LobbyDescriptionContent(participantCounts = participantCounts) {
        callLobbyViewModel.handleUiEvent(
            CallLobbyEvent.JoinCall,
        )
    }
}

@Composable
private fun LobbyDescriptionContent(participantCounts: ParticipantCount?, onClick: () -> Unit) {
    val totalParticipants = participantCounts?.total ?: 0
    val anonParticipants = participantCounts?.anonymous ?: 0

    val text = if (totalParticipants != 0) {
        Pair(
            stringResource(
                id = R.string.join_call_description,
                totalParticipants,
                anonParticipants,
            ),
            stringResource(id = R.string.join_call),
        )
    } else {
        Pair(
            "Start a private test call. This demo is\nbuilt on Stream’s SDKs and runs on our \nglobal edge network.",
            "Start a test call",
        )
    }
    Column(
        modifier = Modifier.padding(VideoTheme.dimens.spacingM),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .testTag("Stream_ParticipantsCount_$totalParticipants"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.LockPerson,
                tint = VideoTheme.colors.basePrimary,
                contentDescription = "",
            )

            Text(
                modifier = Modifier.padding(horizontal = VideoTheme.dimens.spacingM),
                text = text.first,
                style = VideoTheme.typography.bodyS,
            )
        }
        Spacer(modifier = Modifier.size(VideoTheme.dimens.spacingM))
        StreamButton(
            style = VideoTheme.styles.buttonStyles.secondaryButtonStyle(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("Stream_JoinCallButton"),
            text = text.second,
            onClick = onClick,
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
            isCameraEnabled = false,
            isMicrophoneEnabled = false,
            call = previewCall,
            onToggleMicrophone = {},
            onToggleCamera = {},
            onToggleHifiAudio = {},
        ) {
            LobbyDescriptionContent(participantCounts = ParticipantCount(1, 1)) {}
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
private fun CallLobbyBodyLandscapePreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallLobbyBodyLandscape(
            isCameraEnabled = false,
            isMicrophoneEnabled = false,
            call = previewCall,
            onToggleMicrophone = {},
            onToggleCamera = {},
            onToggleHifiAudio = {},
        ) {
            LobbyDescriptionContent(participantCounts = ParticipantCount(1, 1)) {}
        }
    }
}
