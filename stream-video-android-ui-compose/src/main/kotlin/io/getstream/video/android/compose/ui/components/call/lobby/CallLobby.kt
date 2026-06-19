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

package io.getstream.video.android.compose.ui.components.call.lobby

import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.lifecycle.MediaPiPLifecycle
import io.getstream.video.android.compose.permission.VideoPermissionsState
import io.getstream.video.android.compose.permission.rememberCallPermissionsState
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.compose.ui.components.call.controls.ControlActions
import io.getstream.video.android.compose.ui.components.call.controls.actions.DefaultOnCallActionHandler
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantLabel
import io.getstream.video.android.compose.ui.components.indicator.MicrophoneIndicator
import io.getstream.video.android.compose.ui.components.video.DefaultMediaTrackFallbackContent
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.compose.ui.components.video.config.videoRenderConfig
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.core.pip.PictureInPictureConfiguration
import io.getstream.video.android.model.User
import io.getstream.video.android.ui.common.R

/**
 * Represents the UI in a preview call that renders a local video track to pre-display a video
 * before joining a call.
 *
 * @param modifier Modifier for styling.
 * @param call The call includes states and will be rendered with participants.
 * @param user A user to display their name and avatar image on the preview.
 * @param video A participant video to render on the preview renderer.
 * @param permissions Android permissions that should be required to render a video call properly.
 * @param onRenderedContent A video renderer, which renders a local video track before joining a call.
 * @param onDisabledContent Content is shown that a local camera is disabled. It displays user avatar by default.
 * @param participantLabelContent Slot for the participant label overlaid on the preview. Defaults to a
 * label showing the user's name and microphone state at [Alignment.BottomStart]. Pass `{}` to hide the
 * label entirely, or override to provide custom positioning and content (use [BoxScope.align] inside).
 * @param onCallAction Handler when the user triggers a Call Control Action.
 * @param lobbyControlsContent Content is shown that allows users to trigger different actions to control a preview call.
 * @param onRendered An interface that will be invoked when the video is rendered.
 */
@Composable
public fun CallLobby(
    modifier: Modifier = Modifier,
    call: Call,
    user: User = StreamVideo.instance().user,
    isCameraEnabled: Boolean = if (LocalInspectionMode.current) {
        true
    } else {
        call.camera.isEnabled.value
    },
    isMicrophoneEnabled: Boolean = if (LocalInspectionMode.current) {
        true
    } else {
        call.microphone.isEnabled.value
    },
    video: ParticipantState.Video = ParticipantState.Video(
        sessionId = call.sessionId,
        track = VideoTrack(
            streamId = call.sessionId,
            video = if (LocalInspectionMode.current) {
                org.webrtc.VideoTrack(1000L)
            } else {
                call.camera.mediaManager.videoTrack
            },
        ),
        enabled = isCameraEnabled,
        paused = false,
    ),
    permissions: VideoPermissionsState = rememberCallPermissionsState(call = call),
    onRendered: (View) -> Unit = {},
    onRenderedContent: @Composable (video: ParticipantState.Video) -> Unit = {
        OnRenderedContent(call = call, video = it, onRendered = onRendered)
    },
    onDisabledContent: @Composable () -> Unit = {
        OnDisabledContent(user = user)
    },
    participantLabelContent: @Composable BoxScope.() -> Unit = {
        DefaultParticipantLabel(
            user = user,
            isMicrophoneEnabled = isMicrophoneEnabled,
            labelPosition = Alignment.BottomStart,
        )
    },
    onCallAction: (CallAction) -> Unit = {
        DefaultOnCallActionHandler.onCallAction(call, it)
    },
    lobbyControlsContent: @Composable (modifier: Modifier, call: Call) -> Unit = { modifier, call ->
        ControlActions(
            modifier = modifier,
            call = call,
            actions = buildDefaultLobbyControlActions(
                call = call,
                onCallAction = onCallAction,
                isCameraEnabled = isCameraEnabled,
                isMicrophoneEnabled = isMicrophoneEnabled,
            ),
        )
    },
) {
    DefaultPermissionHandler(videoPermission = permissions)

    MediaPiPLifecycle(call = call, PictureInPictureConfiguration(false, false))
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val screenHeightDp = configuration.screenHeightDp

    val boxModifier = Modifier
        .responsiveHeight(isPortrait, screenHeightDp)
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))

    Column(modifier = modifier) {
        Box(
            modifier = boxModifier,
        ) {
            if (isCameraEnabled) {
                onRenderedContent.invoke(video)
            } else {
                onDisabledContent.invoke()
            }

            participantLabelContent()
        }

        Spacer(modifier = Modifier.height(VideoTheme.dimens.spacingM))

        lobbyControlsContent.invoke(Modifier.align(Alignment.Start), call)
    }
}

@Deprecated(
    message = "Use CallLobby with the participantLabelContent slot for full control over the lobby " +
        "label. Pass `{}` to hide it, or override to customize content and position.",
    replaceWith = ReplaceWith(
        "CallLobby(modifier = modifier, call = call, user = user, " +
            "isCameraEnabled = isCameraEnabled, isMicrophoneEnabled = isMicrophoneEnabled, " +
            "video = video, permissions = permissions, onRendered = onRendered, " +
            "onRenderedContent = onRenderedContent, onDisabledContent = onDisabledContent, " +
            "onCallAction = onCallAction, lobbyControlsContent = lobbyControlsContent)",
    ),
)
@Composable
public fun CallLobby(
    modifier: Modifier = Modifier,
    call: Call,
    user: User = StreamVideo.instance().user,
    labelPosition: Alignment,
    isCameraEnabled: Boolean = if (LocalInspectionMode.current) {
        true
    } else {
        call.camera.isEnabled.value
    },
    isMicrophoneEnabled: Boolean = if (LocalInspectionMode.current) {
        true
    } else {
        call.microphone.isEnabled.value
    },
    video: ParticipantState.Video = ParticipantState.Video(
        sessionId = call.sessionId,
        track = VideoTrack(
            streamId = call.sessionId,
            video = if (LocalInspectionMode.current) {
                org.webrtc.VideoTrack(1000L)
            } else {
                call.camera.mediaManager.videoTrack
            },
        ),
        enabled = isCameraEnabled,
        paused = false,
    ),
    permissions: VideoPermissionsState = rememberCallPermissionsState(call = call),
    onRendered: (View) -> Unit = {},
    onRenderedContent: @Composable (video: ParticipantState.Video) -> Unit = {
        OnRenderedContent(call = call, video = it, onRendered = onRendered)
    },
    onDisabledContent: @Composable () -> Unit = {
        OnDisabledContent(user = user)
    },
    onCallAction: (CallAction) -> Unit = {
        DefaultOnCallActionHandler.onCallAction(call, it)
    },
    lobbyControlsContent: @Composable (modifier: Modifier, call: Call) -> Unit = { modifier, call ->
        ControlActions(
            modifier = modifier,
            call = call,
            actions = buildDefaultLobbyControlActions(
                call = call,
                onCallAction = onCallAction,
                isCameraEnabled = isCameraEnabled,
                isMicrophoneEnabled = isMicrophoneEnabled,
            ),
        )
    },
) {
    CallLobby(
        modifier = modifier,
        call = call,
        user = user,
        isCameraEnabled = isCameraEnabled,
        isMicrophoneEnabled = isMicrophoneEnabled,
        video = video,
        permissions = permissions,
        onRendered = onRendered,
        onRenderedContent = onRenderedContent,
        onDisabledContent = onDisabledContent,
        participantLabelContent = {
            DefaultParticipantLabel(
                user = user,
                isMicrophoneEnabled = isMicrophoneEnabled,
                labelPosition = labelPosition,
            )
        },
        onCallAction = onCallAction,
        lobbyControlsContent = lobbyControlsContent,
    )
}

@Composable
private fun BoxScope.DefaultParticipantLabel(
    user: User,
    isMicrophoneEnabled: Boolean,
    labelPosition: Alignment,
) {
    val nameLabel = if (user.id == StreamVideo.instance().user.id) {
        stringResource(id = R.string.stream_video_myself)
    } else {
        user.userNameOrId
    }

    ParticipantLabel(
        nameLabel = nameLabel,
        labelPosition = labelPosition,
        hasAudio = isMicrophoneEnabled,
        soundIndicatorContent = {
            MicrophoneIndicator(
                modifier = Modifier
                    .padding(horizontal = VideoTheme.dimens.spacingM)
                    .testTag("Stream_UserMicrophone_Enabled_$isMicrophoneEnabled"),
                isMicrophoneEnabled = isMicrophoneEnabled,
            )
        },
        isSpeaking = false,
    )
}

@Composable
private fun DefaultPermissionHandler(
    videoPermission: VideoPermissionsState,
) {
    LaunchedEffect(key1 = videoPermission) {
        videoPermission.launchPermissionRequest()
    }
}

@Composable
private fun OnRenderedContent(
    call: Call,
    video: ParticipantState.Video,
    onRendered: (View) -> Unit = {},
) {
    var isVideoRendered by remember { mutableStateOf(false) }

    LaunchedEffect(video.track?.streamId) {
        isVideoRendered = false
    }

    val videoRendererConfig = remember {
        videoRenderConfig {
            // Loading UI is shown as an overlay until the first frame is rendered.
            fallbackContent = {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VideoTheme.colors.baseSheetTertiary)
            .testTag("on_rendered_content"),
    ) {
        VideoRenderer(
            modifier = Modifier.fillMaxSize(),
            call = call,
            video = video,
            videoRendererConfig = videoRendererConfig,
            onRendered = {
                isVideoRendered = true
                onRendered(it)
            },
        )
        if (!isVideoRendered) {
            DefaultMediaTrackFallbackContent(
                modifier = Modifier.fillMaxSize(),
                call = call,
            )
        }
    }
}

@Composable
private fun OnDisabledContent(user: User) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VideoTheme.colors.baseSheetTertiary)
            .testTag("on_disabled_content"),
    ) {
        UserAvatar(
            modifier = Modifier
                .size(VideoTheme.dimens.genericMax)
                .align(Alignment.Center),
            userImage = user.image,
            userName = user.name.takeUnless { it.isNullOrBlank() } ?: user.id,
        )
    }
}

private fun Modifier.responsiveHeight(isPortrait: Boolean, screenHeightDp: Int): Modifier {
    val isSmallDevice = screenHeightDp <= 640
    val height = if (isPortrait && isSmallDevice) {
        180.dp
    } else if (isPortrait) {
        280.dp
    } else {
        200.dp
    }
    return this.then(Modifier.height(height))
}
