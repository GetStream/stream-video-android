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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Alignment.Companion.BottomStart
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.common.model.getSoundIndicatorState
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.audio.SoundIndicator
import io.getstream.video.android.compose.ui.components.avatar.UserAvatarBackground
import io.getstream.video.android.compose.ui.components.connection.ConnectionQualityIndicator
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.mock.mockCall
import io.getstream.video.android.mock.mockParticipantList
import io.getstream.video.android.ui.common.R
import stream.video.sfu.models.ConnectionQuality

/**
 * Renders a single participant with a given call, which contains all the call states.
 * Also displays participant information with a label and connection quality indicator.
 *
 * @param call The call that contains all the participants state and tracks.
 * @param participant Participant to render.
 * @param modifier Modifier for styling.
 * @param labelPosition The position of the user audio state label.
 * @param isFocused If the participant is focused or not.
 * @param isScreenSharing Represents is screen sharing or not.
 * @param isShowingConnectionQualityIndicator Whether displays the connection quality indicator or not.
 * @param labelContent Content is shown that displays participant's name and device states.
 * @param connectionIndicatorContent Content is shown that indicates the connection quality.
 * @param onRender Handler when the Video renders.
 */
@Composable
public fun CallSingleVideoRenderer(
    call: Call,
    participant: ParticipantState,
    modifier: Modifier = Modifier,
    labelPosition: Alignment = BottomStart,
    isFocused: Boolean = false,
    isScreenSharing: Boolean = false,
    isShowingConnectionQualityIndicator: Boolean = true,
    labelContent: @Composable BoxScope.(ParticipantState) -> Unit = {
        ParticipantLabel(participant, labelPosition)
    },
    connectionIndicatorContent: @Composable BoxScope.(ConnectionQuality) -> Unit = {
        ConnectionQualityIndicator(
            connectionQuality = it,
            modifier = Modifier.align(BottomEnd)
        )
    },
    onRender: (View) -> Unit = {}
) {
    val reactions by participant.reactions.collectAsStateWithLifecycle()
    val connectionQuality by participant.connectionQuality.collectAsStateWithLifecycle()

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
        modifier = containerModifier.apply {
            if (isScreenSharing) {
                clip(RoundedCornerShape(VideoTheme.dimens.screenShareParticipantsRadius))
            }
        }
    ) {
        ParticipantVideoRenderer(call = call, participant = participant, onRender = onRender)

        labelContent.invoke(this, participant)

        if (isShowingConnectionQualityIndicator) {
            connectionIndicatorContent.invoke(this, connectionQuality)
        }
    }
}

/**
 * Renders a single participant with a given call, which contains all the call states.
 * Also displays participant information with a label and connection quality indicator.
 *
 * @param call The call that contains all the participants state and tracks.
 * @param participant Participant to render.
 * @param onRender Handler when the Video renders.
 */
@Composable
public fun ParticipantVideoRenderer(
    call: Call,
    participant: ParticipantState,
    onRender: (View) -> Unit = {}
) {
    if (LocalInspectionMode.current) {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .testTag("participant_video_renderer"),
            painter = painterResource(id = R.drawable.stream_video_call_sample),
            contentScale = ContentScale.Crop,
            contentDescription = null
        )
        return
    }

    val video by participant.video.collectAsStateWithLifecycle()
    val user by participant.user.collectAsStateWithLifecycle()

    VideoRenderer(
        call = call,
        media = video,
        onRender = onRender,
        onRenderFailedContent = { UserAvatarBackground(user = user) }
    )
}

@Composable
internal fun BoxScope.ParticipantLabel(
    participant: ParticipantState,
    labelPosition: Alignment,
) {
    val audioEnabled by participant.audioEnabled.collectAsStateWithLifecycle()
    val speaking by participant.speaking.collectAsStateWithLifecycle()

    val userNameOrId by participant.userNameOrId.collectAsStateWithLifecycle()
    val nameLabel = if (participant.isLocal) {
        stringResource(id = R.string.stream_video_myself)
    } else {
        userNameOrId
    }

    ParticipantLabel(
        nameLabel = nameLabel,
        labelPosition = labelPosition,
        hasAudio = audioEnabled,
        isSpeaking = speaking
    )
}

@Composable
internal fun BoxScope.ParticipantLabel(
    nameLabel: String,
    labelPosition: Alignment,
    hasAudio: Boolean,
    isSpeaking: Boolean
) {
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
                hasAudio = hasAudio,
                isSpeaking = isSpeaking
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
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallSingleVideoRenderer(
            call = mockCall,
            participant = mockParticipantList[1],
            isFocused = true
        )
    }
}

@Preview
@Composable
private fun ParticipantLabelPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        Box {
            ParticipantLabel(
                participant = mockParticipantList[1],
                BottomStart,
            )
        }
    }
}

@Preview
@Composable
private fun ParticipantVideoPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        ParticipantVideoRenderer(
            call = mockCall,
            participant = mockParticipantList[1],
        ) {}
    }
}
