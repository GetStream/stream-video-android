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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.compose.ui.components.call.controls.CallControls
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantLabel
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CallDeviceState
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.model.User
import io.getstream.video.android.ui.common.R

@Composable
public fun CallLobby(
    modifier: Modifier = Modifier,
    call: Call,
    user: User = StreamVideo.instance().user,
    callDeviceState: CallDeviceState,
    labelPosition: Alignment = Alignment.BottomStart,
    video: ParticipantState.Video = ParticipantState.Video(
        sessionId = call.sessionId.orEmpty(),
        track = VideoTrack(
            streamId = call.sessionId.orEmpty(),
            video = call.camera.mediaManager.videoTrack
        ),
        enabled = callDeviceState.isCameraEnabled
    ),
    onRenderedContent: @Composable (video: ParticipantState.Video) -> Unit = {
        OnRenderedContent(call = call, video = it)
    },
    onDisabledContent: @Composable () -> Unit = {
        OnDisabledContent(user = user)
    },
    onCallAction: (CallAction) -> Unit = {},
    callControlsContent: @Composable (call: Call) -> Unit = {
        CallControls(
            callDeviceState = callDeviceState,
            onCallAction = onCallAction
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
            if (callDeviceState.isCameraEnabled) {
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
                hasAudio = callDeviceState.isMicrophoneEnabled,
                isSpeaking = false
            )
        }

        Spacer(modifier = Modifier.height(VideoTheme.dimens.lobbyCallActionsPadding))

        callControlsContent.invoke(call)
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
        media = video
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

@Composable
private fun CallLobbyPreview() {
    VideoTheme {
    }
}
