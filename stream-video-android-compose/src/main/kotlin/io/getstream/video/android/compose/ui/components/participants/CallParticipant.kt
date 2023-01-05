/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.compose.ui.components.participants

import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.audio.SoundIndicator
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.CallParticipantState
import io.getstream.video.android.model.VideoTrack
import io.getstream.video.android.model.toUser
import stream.video.sfu.models.TrackType

/**
 * Represents a single participant in a call.
 *
 * @param call The active call.
 * @param participant Participant to render.
 * @param modifier Modifier for styling.
 * @param labelPosition The position of the user audio state label.
 * @param isFocused If the participant is focused or not.
 * @param onRender Handler when the Video renders.
 */
@Composable
public fun CallParticipant(
    call: Call,
    participant: CallParticipantState,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    labelPosition: Alignment = Alignment.BottomStart,
    isFocused: Boolean = false,
    onRender: (View) -> Unit = {}
) {
    val track = participant.videoTrack

    val containerModifier =
        if (isFocused) modifier.border(
            BorderStroke(
                3.dp,
                VideoTheme.colors.infoAccent
            )
        ) else modifier

    Box(modifier = containerModifier.padding(paddingValues)) {
        ParticipantVideo(
            call = call,
            participant = participant,
            track = track,
            onRender = onRender
        )

        ParticipantLabel(participant, labelPosition)
    }
}

@Composable
private fun ParticipantVideo(
    call: Call,
    participant: CallParticipantState,
    track: VideoTrack?,
    onRender: (View) -> Unit
) {
    val isVideoEnabled = try {
        track?.video?.enabled() == true
    } catch (error: Throwable) {
        false
    }

    if (track != null && isVideoEnabled) {
        VideoRenderer(
            call = call,
            videoTrack = track,
            sessionId = participant.sessionId,
            onRender = onRender,
            trackType = TrackType.TRACK_TYPE_VIDEO
        )
    } else {
        UserAvatar(
            modifier = Modifier.onSizeChanged {
                call.updateParticipantTrackSize(
                    participant.sessionId,
                    it.width,
                    it.height
                )
            },
            shape = RectangleShape,
            user = participant.toUser()
        )
    }
}

@Composable
private fun BoxScope.ParticipantLabel(
    participant: CallParticipantState,
    labelPosition: Alignment
) {
    Row(
        modifier = Modifier
            .align(labelPosition)
            .padding(8.dp)
            .height(24.dp)
            .wrapContentWidth()
            .background(
                Color.DarkGray,
                shape = RoundedCornerShape(8.dp)
            ),
        verticalAlignment = CenterVertically,
    ) {
        SoundIndicator(
            hasSound = participant.hasAudio,
            isSpeaking = participant.isSpeaking,
            modifier = Modifier
                .align(CenterVertically)
                .padding(start = 8.dp)
        )

        val name = participant.name.ifEmpty {
            participant.id
        }
        Text(
            modifier = Modifier
                .widthIn(max = 64.dp)
                .padding(horizontal = 4.dp)
                .align(CenterVertically),
            text = name,
            style = VideoTheme.typography.body,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
