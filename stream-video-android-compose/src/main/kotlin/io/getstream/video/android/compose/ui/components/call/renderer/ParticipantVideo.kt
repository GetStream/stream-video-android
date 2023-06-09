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

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.common.model.getSoundIndicatorState
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.audio.SoundIndicator
import io.getstream.video.android.compose.ui.components.avatar.UserAvatarBackground
import io.getstream.video.android.compose.ui.components.connection.NetworkQualityIndicator
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.model.NetworkQuality
import io.getstream.video.android.core.model.Reaction
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.mock.mockCall
import io.getstream.video.android.mock.mockParticipantList
import io.getstream.video.android.ui.common.R
import kotlinx.coroutines.delay

/**
 * Renders a single participant with a given call, which contains all the call states.
 * Also displays participant information with a label and connection quality indicator.
 *
 * @param call The call that contains all the participants state and tracks.
 * @param participant Participant to render.
 * @param modifier Modifier for styling.
 * @param style Represents a regular video call render styles.
 * @param labelContent Content is shown that displays participant's name and device states.
 * @param connectionIndicatorContent Content is shown that indicates the connection quality.
 * @param videoFallbackContent Content is shown the video track is failed to load or not available.
 */
@Composable
public fun ParticipantVideo(
    call: Call,
    participant: ParticipantState,
    modifier: Modifier = Modifier,
    style: VideoRendererStyle = RegularVideoRendererStyle(),
    labelContent: @Composable BoxScope.(ParticipantState) -> Unit = {
        ParticipantLabel(participant, style.labelPosition)
    },
    connectionIndicatorContent: @Composable BoxScope.(NetworkQuality) -> Unit = {
        NetworkQualityIndicator(
            networkQuality = it,
            modifier = Modifier.align(BottomEnd)
        )
    },
    videoFallbackContent: @Composable (Call) -> Unit = {
        val user by participant.user.collectAsStateWithLifecycle()
        UserAvatarBackground(user = user)
    },
    reactionContent: @Composable BoxScope.(ParticipantState) -> Unit = {
        DefaultReaction(
            participant = participant,
            style = style
        )
    }
) {
    val connectionQuality by participant.networkQuality.collectAsStateWithLifecycle()
    val participants by call.state.participants.collectAsStateWithLifecycle()

    val containerModifier = if (style.isFocused && participants.size > 1) modifier.border(
        border = if (style.isScreenSharing) {
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
        shape = if (style.isScreenSharing) {
            RoundedCornerShape(VideoTheme.dimens.screenShareParticipantsRadius)
        } else {
            RectangleShape
        }
    ) else modifier

    Box(
        modifier = containerModifier.apply {
            if (style.isScreenSharing) {
                clip(RoundedCornerShape(VideoTheme.dimens.screenShareParticipantsRadius))
            }
        }
    ) {
        ParticipantVideoRenderer(
            call = call,
            participant = participant,
            videoFallbackContent = videoFallbackContent
        )

        if (style.isShowingParticipantLabel) {
            labelContent.invoke(this, participant)
        }

        if (style.isShowingConnectionQualityIndicator) {
            connectionIndicatorContent.invoke(this, connectionQuality)
        }

        if (style.isShowingReactions) {
            reactionContent.invoke(this, participant)
        }
    }
}

/**
 * Renders a single participant with a given call, which contains all the call states.
 * Also displays participant information with a label and connection quality indicator.
 *
 * @param call The call that contains all the participants state and tracks.
 * @param participant Participant to render.
 * @param videoFallbackContent Content is shown the video track is failed to load or not available.
 */
@Composable
public fun ParticipantVideoRenderer(
    call: Call,
    participant: ParticipantState,
    videoFallbackContent: @Composable (Call) -> Unit = {
        val user by participant.user.collectAsStateWithLifecycle()
        UserAvatarBackground(user = user)
    },
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

    VideoRenderer(
        call = call,
        video = video,
        videoFallbackContent = videoFallbackContent
    )
}

@Composable
public fun BoxScope.ParticipantLabel(
    participant: ParticipantState,
    labelPosition: Alignment = BottomStart,
    soundIndicatorContent: @Composable RowScope.() -> Unit = {
        val audioEnabled by participant.audioEnabled.collectAsStateWithLifecycle()
        val speaking by participant.speaking.collectAsStateWithLifecycle()
        SoundIndicator(
            state = getSoundIndicatorState(
                hasAudio = audioEnabled,
                isSpeaking = speaking
            ),
            modifier = Modifier
                .align(CenterVertically)
                .padding(horizontal = VideoTheme.dimens.callParticipantSoundIndicatorPadding)
        )
    }
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
        isSpeaking = speaking,
        soundIndicatorContent = soundIndicatorContent
    )
}

@Composable
public fun BoxScope.ParticipantLabel(
    nameLabel: String,
    labelPosition: Alignment = BottomStart,
    hasAudio: Boolean = false,
    isSpeaking: Boolean = false,
    soundIndicatorContent: @Composable RowScope.() -> Unit = {
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

        soundIndicatorContent.invoke(this)
    }
}

@Composable
private fun BoxScope.DefaultReaction(
    participant: ParticipantState,
    style: VideoRendererStyle
) {
    val reactions by participant.reactions.collectAsStateWithLifecycle()
    val reaction = reactions.lastOrNull {
        !it.isConsumed && it.createdAt + 3000 > System.currentTimeMillis()
    }
    var currentReaction: Reaction? by remember { mutableStateOf(null) }

    LaunchedEffect(key1 = reaction) {
        currentReaction?.let { participant.consumeReaction(it) }
        currentReaction = reaction

        if (reaction != null) {
            delay(style.reactionDuration * 2 - 50L)
            participant.consumeReaction(reaction)
            currentReaction = null
        }
    }

    val size: Dp by animateDpAsState(
        targetValue = if (currentReaction != null) {
            VideoTheme.dimens.reactionSize
        } else {
            0.dp
        },
        animationSpec = repeatable(
            iterations = 2,
            animation = tween(
                durationMillis = style.reactionDuration,
                easing = LinearOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reaction"
    )

    if (currentReaction != null) {
        Text(
            text = currentReaction!!.response.emojiCode ?: "",
            modifier = Modifier.align(style.reactionPosition),
            fontSize = size.value.sp
        )
    }
}

@Preview
@Composable
private fun CallParticipantPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        ParticipantVideo(
            call = mockCall,
            participant = mockParticipantList[1],
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
