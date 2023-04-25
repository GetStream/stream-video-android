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

package io.getstream.video.android.compose.ui.components.call.renderer

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Alignment.Companion.BottomStart
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.common.model.getSoundIndicatorState
import io.getstream.video.android.common.util.MockUtils
import io.getstream.video.android.common.util.mockCall
import io.getstream.video.android.common.util.mockParticipants
import io.getstream.video.android.common.util.mockVideoTrackWrapper
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.audio.SoundIndicator
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.compose.ui.components.connection.ConnectionQualityIndicator
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
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
public fun CallSingleVideoRenderer(
    call: Call,
    participant: ParticipantState,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    labelPosition: Alignment = BottomStart,
    isFocused: Boolean = false,
    isScreenSharing: Boolean = false,
    isShowConnectionQualityIndicator: Boolean = true,
    onRender: (View) -> Unit = {}
) {
    val containerModifier = if (isFocused) modifier.border(
        border = if (isScreenSharing) {
            BorderStroke(
                VideoTheme.dimens.callParticipantScreenSharingFocusedBorderWidth,
                VideoTheme.colors.callFocusedBorder
            )
        } else {
            BorderStroke(
                VideoTheme.dimens.callParticipantFocusedBorderWidth,
                VideoTheme.colors.callFocusedBorder
            )
        },
        shape = if (isScreenSharing) {
            RoundedCornerShape(VideoTheme.dimens.screenShareParticipantsRadius)
        } else {
            RectangleShape
        }
    ) else modifier

    Box(
        modifier = containerModifier.padding(paddingValues).apply {
            if (isScreenSharing) {
                clip(RoundedCornerShape(VideoTheme.dimens.screenShareParticipantsRadius))
            }
        }
    ) {
        ParticipantVideoRenderer(call = call, participant = participant, onRender = onRender)

        ParticipantLabel(participant, labelPosition)

        if (isShowConnectionQualityIndicator) {
            val connectionQuality by participant.connectionQuality.collectAsState()
            ConnectionQualityIndicator(
                connectionQuality = connectionQuality,
                modifier = Modifier.align(BottomEnd)
            )
        }
    }
}

@Composable
internal fun ParticipantVideoRenderer(
    call: Call,
    participant: ParticipantState,
    onRender: (View) -> Unit
) {
    val track = participant.videoTrack.collectAsState()
    val isVideoEnabled by participant.videoEnabled.collectAsState()

    if (LocalInspectionMode.current) {
        VideoRenderer(
            modifier = Modifier.fillMaxSize(),
            call = call,
            videoTrackWrapper = track.value ?: mockVideoTrackWrapper,
            sessionId = participant.sessionId,
            onRender = onRender,
            trackType = TrackType.TRACK_TYPE_VIDEO
        )
        return
    }

    val isLocalVideo = participant.sessionId == call.sessionId
    if (!isLocalVideo) {
        println(isLocalVideo)
    }

    val videoTrack = track.value
    if (videoTrack != null && isVideoEnabled) {
        VideoRenderer(
            call = call,
            videoTrackWrapper = videoTrack,
            sessionId = participant.sessionId,
            onRender = onRender,
            trackType = TrackType.TRACK_TYPE_VIDEO
        )
    } else {
        UserAvatar(
            shape = RectangleShape,
            user = participant.user.collectAsState().value
        )
    }
}

@Composable
internal fun BoxScope.ParticipantLabel(
    participant: ParticipantState,
    labelPosition: Alignment
) {
    val userNameOrId by participant.userNameOrId.collectAsState()
    val nameLabel = if (participant.isLocal) {
        stringResource(id = io.getstream.video.android.ui.common.R.string.stream_video_myself)
    } else {
        userNameOrId
    }

    Row(
        modifier = Modifier
            .align(labelPosition)
            .padding(VideoTheme.dimens.callParticipantLabelPadding)
            .height(VideoTheme.dimens.callParticipantLabelHeight)
            .wrapContentWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                VideoTheme.colors.participantLabelBackground,
                shape = RoundedCornerShape(8.dp)
            ),
        verticalAlignment = CenterVertically,
    ) {
        Text(
            modifier = Modifier
                .widthIn(max = VideoTheme.dimens.callParticipantLabelTextMaxWidth)
                .padding(start = VideoTheme.dimens.callParticipantLabelTextPaddingStart)
                .align(CenterVertically),
            text = nameLabel,
            style = VideoTheme.typography.body,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        SoundIndicator(
            state = getSoundIndicatorState(
                hasAudio = participant.audioEnabled.collectAsState().value,
                isSpeaking = participant.speaking.collectAsState().value
            ),
            modifier = Modifier
                .align(CenterVertically)
                .padding(horizontal = VideoTheme.dimens.callParticipantSoundIndicatorPadding)
        )
    }
}

@Preview
@Composable
private fun CallParticipantPreview() {
    MockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallSingleVideoRenderer(
            call = mockCall,
            participant = mockParticipants[1],
            isFocused = true
        )
    }
}

@Preview
@Composable
private fun ParticipantLabelPreview() {
    MockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        Box {
            ParticipantLabel(
                participant = mockParticipants[1],
                BottomStart,
            )
        }
    }
}

@Preview
@Composable
private fun ParticipantVideoPreview() {
    MockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        ParticipantVideoRenderer(
            call = mockCall,
            participant = mockParticipants[1],
        ) {}
    }
}
