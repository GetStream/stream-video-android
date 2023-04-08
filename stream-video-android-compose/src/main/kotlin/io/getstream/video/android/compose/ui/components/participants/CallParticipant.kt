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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Alignment.Companion.BottomStart
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.getstream.video.android.common.model.getSoundIndicatorState
import io.getstream.video.android.common.util.mockVideoTrackWrapper
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.audio.SoundIndicator
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.compose.ui.components.connection.ConnectionQualityIndicator
import io.getstream.video.android.compose.ui.components.previews.ParticipantsProvider
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.model.Call
import io.getstream.video.android.core.model.CallParticipantState
import io.getstream.video.android.core.model.toUser
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
    call: Call?,
    participant: CallParticipantState,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    labelPosition: Alignment = BottomStart,
    isFocused: Boolean = false,
    onRender: (View) -> Unit = {}
) {
    val containerModifier = if (isFocused) modifier.border(
        BorderStroke(
            VideoTheme.dimens.callParticipantFocusedBorderWidth, VideoTheme.colors.infoAccent
        )
    ) else modifier

    Box(modifier = containerModifier.padding(paddingValues)) {
        ParticipantVideo(
            call = call, participant = participant, onRender = onRender
        )

        if (!participant.isLocal) {
            ParticipantLabel(participant, labelPosition)
        }

        val connectionIndicatorAlignment = if (participant.isLocal) {
            BottomStart
        } else {
            BottomEnd
        }

        ConnectionQualityIndicator(
            connectionQuality = participant.connectionQuality,
            modifier = Modifier.align(connectionIndicatorAlignment)
        )
    }
}

@Composable
internal fun ParticipantVideo(
    call: Call?,
    participant: CallParticipantState,
    onRender: (View) -> Unit
) {
    val track = participant.videoTrack

    val isVideoEnabled = try {
        track?.video?.enabled() == true
    } catch (error: Throwable) {
        false
    }

    if ((LocalInspectionMode.current || call == null)) {
        VideoRenderer(
            modifier = Modifier.fillMaxSize(),
            call = call,
            videoTrackWrapper = track ?: mockVideoTrackWrapper,
            sessionId = participant.sessionId,
            onRender = onRender,
            trackType = TrackType.TRACK_TYPE_VIDEO
        )
        return
    }

    if (track != null && isVideoEnabled && TrackType.TRACK_TYPE_VIDEO in participant.publishedTracks) {
        VideoRenderer(
            call = call,
            videoTrackWrapper = track,
            sessionId = participant.sessionId,
            onRender = onRender,
            trackType = TrackType.TRACK_TYPE_VIDEO
        )
    } else {
        UserAvatar(
            modifier = Modifier.onSizeChanged {
                call.updateParticipantTrackSize(
                    participant.sessionId, it.width, it.height
                )
            },
            shape = RectangleShape, user = participant.toUser()
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
            .padding(VideoTheme.dimens.callParticipantLabelPadding)
            .height(VideoTheme.dimens.callParticipantLabelHeight)
            .wrapContentWidth()
            .background(
                Color.DarkGray, shape = RoundedCornerShape(8.dp)
            ),
        verticalAlignment = CenterVertically,
    ) {
        SoundIndicator(
            state = getSoundIndicatorState(
                hasAudio = participant.hasAudio, isSpeaking = participant.isSpeaking
            ),
            modifier = Modifier
                .align(CenterVertically)
                .padding(start = VideoTheme.dimens.callParticipantSoundIndicatorPaddingStart)
        )

        val name = participant.name.ifEmpty {
            participant.id
        }
        Text(
            modifier = Modifier
                .widthIn(max = VideoTheme.dimens.callParticipantLabelTextMaxWidth)
                .padding(horizontal = VideoTheme.dimens.callParticipantLabelTextPadding)
                .align(CenterVertically),
            text = name,
            style = VideoTheme.typography.body,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview
@Composable
private fun CallParticipantPreview(
    @PreviewParameter(ParticipantsProvider::class) callParticipants: List<CallParticipantState>
) {
    VideoTheme {
        CallParticipant(
            call = null,
            participant = callParticipants[0]
        )
    }
}

@Preview
@Composable
private fun ParticipantVideoPreview(
    @PreviewParameter(ParticipantsProvider::class) callParticipants: List<CallParticipantState>
) {
    VideoTheme {
        ParticipantVideo(
            call = null,
            participant = callParticipants[0]
        ) {}
    }
}
