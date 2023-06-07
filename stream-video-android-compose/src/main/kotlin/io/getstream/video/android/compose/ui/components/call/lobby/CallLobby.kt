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

package io.getstream.video.android.compose.ui.components.call.lobby

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.compose.ui.components.call.controls.ControlActions
import io.getstream.video.android.compose.ui.components.call.controls.actions.DefaultOnCallActionHandler
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantLabel
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.mock.mockCall
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
 * @param onRenderedContent A video renderer, which renders a local video track before joining a call.
 * @param onDisabledContent Content is shown that a local camera is disabled. It displays user avatar by default.
 * @param onCallAction Handler when the user triggers a Call Control Action.
 * @param lobbyControlsContent Content is shown that allows users to trigger different actions to control a preview call.
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
        sessionId = call.sessionId.orEmpty(),
        track = VideoTrack(
            streamId = call.sessionId.orEmpty(),
            video = if (LocalInspectionMode.current) {
                org.webrtc.VideoTrack(1000L)
            } else {
                call.camera.mediaManager.videoTrack
            }
        ),
        enabled = isCameraEnabled
    ),
    onRenderedContent: @Composable (video: ParticipantState.Video) -> Unit = {
        OnRenderedContent(call = call, video = it)
    },
    onDisabledContent: @Composable () -> Unit = {
        OnDisabledContent(user = user)
    },
    onCallAction: (CallAction) -> Unit = { DefaultOnCallActionHandler.onCallAction(call, it) },
    lobbyControlsContent: @Composable (call: Call) -> Unit = {
        ControlActions(
            call = call,
            backgroundColor = Color.Transparent,
            shape = RectangleShape,
            elevation = 0.dp,
            actions = buildDefaultLobbyControlActions(
                call = call,
                onCallAction = onCallAction
            )
        )
    },
) {
    val participant = remember(user) { ParticipantState(initialUser = user, call = call) }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(VideoTheme.dimens.lobbyVideoHeight)
                .padding(horizontal = 30.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(VideoTheme.colors.callLobbyBackground)
        ) {
            if (isCameraEnabled) {
                onRenderedContent.invoke(video)
            } else {
                onDisabledContent.invoke()
            }

            val userNameOrId by participant.userNameOrId.collectAsStateWithLifecycle()
            val nameLabel = if (participant.isLocal) {
                stringResource(id = R.string.stream_video_myself)
            } else {
                userNameOrId
            }

            ParticipantLabel(
                nameLabel = nameLabel,
                labelPosition = labelPosition,
                hasAudio = isMicrophoneEnabled,
                isSpeaking = false
            )
        }

        Spacer(modifier = Modifier.height(VideoTheme.dimens.lobbyControlActionsPadding))

        lobbyControlsContent.invoke(call)
    }
}

@Composable
private fun OnRenderedContent(
    call: Call,
    video: ParticipantState.Video,
) {
    VideoRenderer(
        modifier = Modifier
            .fillMaxSize()
            .testTag("on_rendered_content"),
        call = call,
        video = video
    )
}

@Composable
private fun OnDisabledContent(user: User) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("on_disabled_content")
    ) {
        UserAvatar(
            modifier = Modifier
                .size(VideoTheme.dimens.callAvatarSize)
                .align(Alignment.Center),
            user = user
        )
    }
}

@Preview
@Composable
private fun CallLobbyPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallLobby(
            call = mockCall,
            video = ParticipantState.Video(
                sessionId = mockCall.sessionId.orEmpty(),
                track = VideoTrack(
                    streamId = mockCall.sessionId.orEmpty(),
                    video = org.webrtc.VideoTrack(1000L)
                ),
                enabled = true
            )
        )
    }
}
