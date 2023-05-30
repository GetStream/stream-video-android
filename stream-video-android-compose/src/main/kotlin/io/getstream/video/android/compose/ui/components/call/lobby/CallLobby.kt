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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantLabel
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.call.state.CallDeviceState
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.model.User

@Composable
public fun CallLobby(
    call: Call,
    user: User,
    modifier: Modifier = Modifier,
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
    }
) {
    val participant = remember(user) { ParticipantState(initialUser = user, call = call) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(VideoTheme.colors.callLobbyBackground)
    ) {
        if (callDeviceState.isCameraEnabled) {
            onRenderedContent.invoke(video)
        } else {
            onDisabledContent.invoke()
        }

        ParticipantLabel(
            participant = participant,
            labelPosition = labelPosition,
            hasAudio = callDeviceState.isMicrophoneEnabled
        )
    }
}

@Composable
private fun OnRenderedContent(
    call: Call,
    video: ParticipantState.Video,
) {
    VideoRenderer(
        modifier = Modifier.fillMaxSize(),
        call = call,
        media = video
    )
}

@Composable
private fun OnDisabledContent(user: User) {
    Box(modifier = Modifier.fillMaxSize()) {
        UserAvatar(
            modifier = Modifier
                .size(VideoTheme.dimens.callAvatarSize)
                .align(Alignment.Center),
            user = user
        )
    }
}
