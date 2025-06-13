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

package io.getstream.video.android.compose.ui.components.call.lobby

import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
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
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.model.User
import io.getstream.video.android.ui.common.R

/**
 * Represents the UI in a preview call that renders a local video track to pre-display a video
 * before joining a call.
 *
 * @param modifier Modifier for styling.
 * @param call The call includes states and will be rendered with participants.
 * @param user A user to display their name and avatar image on the preview.
 * @param labelPosition The position of the user audio state label.
 * @param video A participant video to render on the preview renderer.
 * @param permissions Android permissions that should be required to render a video call properly.
 * @param onRenderedContent A video renderer, which renders a local video track before joining a call.
 * @param onDisabledContent Content is shown that a local camera is disabled. It displays user avatar by default.
 * @param onCallAction Handler when the user triggers a Call Control Action.
 * @param lobbyControlsContent Content is shown that allows users to trigger different actions to control a preview call.
 * @param onRendered An interface that will be invoked when the video is rendered.
 */
@Composable
public fun CallLobby(
    modifier: Modifier = Modifier,
    call: Call,
    user: User = StreamVideo.instance().user,
    labelPosition: Alignment = Alignment.BottomStart,
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
    DefaultPermissionHandler(videoPermission = permissions)

    MediaPiPLifecycle(call = call)
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val boxModifier = Modifier
        .then(if (isPortrait) Modifier.height(280.dp) else Modifier.height(200.dp))
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

        Spacer(modifier = Modifier.height(VideoTheme.dimens.spacingM))

        lobbyControlsContent.invoke(Modifier.align(Alignment.Start), call)
    }
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
    VideoRenderer(
        modifier = Modifier
            .fillMaxSize()
            .background(VideoTheme.colors.baseSheetTertiary)
            .testTag("on_rendered_content"),
        call = call,
        video = video,
        onRendered = onRendered,
    )
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

@Preview
@Composable
private fun CallLobbyPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallLobby(
            call = previewCall,
            video = ParticipantState.Video(
                sessionId = previewCall.sessionId,
                track = VideoTrack(
                    streamId = previewCall.sessionId,
                    video = org.webrtc.VideoTrack(1000L),
                ),
                enabled = true,
            ),
        )
    }
}
