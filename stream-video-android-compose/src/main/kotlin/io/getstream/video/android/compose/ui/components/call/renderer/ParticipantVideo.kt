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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Alignment.Companion.BottomStart
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.LocalAvatarPreviewProvider
import io.getstream.video.android.compose.ui.components.avatar.UserAvatarBackground
import io.getstream.video.android.compose.ui.components.connection.NetworkQualityIndicator
import io.getstream.video.android.compose.ui.components.indicator.SoundIndicator
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.model.NetworkQuality
import io.getstream.video.android.core.model.Reaction
import io.getstream.video.android.core.model.ReactionState
import io.getstream.video.android.core.model.VisibilityOnScreenState
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
 * @param style Defined properties for styling a single video call track.
 * @param labelContent Content is shown that displays participant's name and device states.
 * @param connectionIndicatorContent Content is shown that indicates the connection quality.
 * @param videoFallbackContent Content is shown the video track is failed to load or not available.
 * @param reactionContent Content is shown for the reaction.
 */
@Composable
public fun ParticipantVideo(
    call: Call,
    participant: ParticipantState,
    modifier: Modifier = Modifier,
    style: VideoRendererStyle = RegularVideoRendererStyle(),
    labelContent: @Composable BoxScope.(ParticipantState) -> Unit = {
        ParticipantLabel(call, participant, style.labelPosition)
    },
    connectionIndicatorContent: @Composable BoxScope.(NetworkQuality) -> Unit = {
        NetworkQualityIndicator(
            networkQuality = it,
            modifier = Modifier
                .align(BottomEnd)
                .height(VideoTheme.dimens.participantLabelHeight),
        )
    },
    videoFallbackContent: @Composable (Call) -> Unit = {
        val userName by participant.userNameOrId.collectAsStateWithLifecycle()
        val userImage by participant.image.collectAsStateWithLifecycle()
        UserAvatarBackground(userName = userName, userImage = userImage)
    },
    reactionContent: @Composable BoxScope.(ParticipantState) -> Unit = {
        DefaultReaction(
            participant = participant,
            style = style,
        )
    },
) {
    val connectionQuality by participant.networkQuality.collectAsStateWithLifecycle()
    val participants by call.state.participants.collectAsStateWithLifecycle()

    DisposableEffect(call, participant.sessionId) {
        // Inform the call of this participant visibility on screen, affects sorting order.
        updateParticipantVisibility(participant.sessionId, call, VisibilityOnScreenState.VISIBLE)

        onDispose {
            updateParticipantVisibility(
                participant.sessionId,
                call,
                VisibilityOnScreenState.INVISIBLE,
            )
        }
    }

    val containerShape = if (style.isScreenSharing) {
        RoundedCornerShape(VideoTheme.dimens.screenShareParticipantsRadius)
    } else {
        VideoTheme.shapes.participantContainerShape
    }
    val containerModifier = if (style.isFocused && participants.size > 1) {
        modifier.border(
            border = if (style.isScreenSharing) {
                BorderStroke(
                    VideoTheme.dimens.participantScreenSharingFocusedBorderWidth,
                    VideoTheme.colors.callFocusedBorder,
                )
            } else {
                BorderStroke(
                    VideoTheme.dimens.participantFocusedBorderWidth,
                    VideoTheme.colors.callFocusedBorder,
                )
            },
            shape = containerShape,
        )
    } else {
        modifier
    }
    val paddedContent = containerModifier.padding(VideoTheme.dimens.participantsGridPadding)

    Box(
        modifier = paddedContent.clip(containerShape),
    ) {
        ParticipantVideoRenderer(
            call = call,
            participant = participant,
            videoFallbackContent = videoFallbackContent,
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
        val userName by participant.userNameOrId.collectAsStateWithLifecycle()
        val userImage by participant.image.collectAsStateWithLifecycle()
        UserAvatarBackground(userName = userName, userImage = userImage)
    },
) {
    if (LocalInspectionMode.current) {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .testTag("participant_video_renderer"),
            painter = painterResource(
                id = LocalAvatarPreviewProvider.getLocalAvatarPreviewPlaceholder(),
            ),
            contentScale = ContentScale.Crop,
            contentDescription = null,
        )
        return
    }

    val video by participant.video.collectAsStateWithLifecycle()

    VideoRenderer(
        call = call,
        video = video,
        videoFallbackContent = videoFallbackContent,
    )
}

@Composable
public fun BoxScope.ParticipantLabel(
    call: Call,
    participant: ParticipantState,
    labelPosition: Alignment = BottomStart,
    soundIndicatorContent: @Composable RowScope.() -> Unit = {
        val audioEnabled by participant.audioEnabled.collectAsStateWithLifecycle()
        val speaking by participant.speaking.collectAsStateWithLifecycle()
        val audioLevel by if (participant.isLocal) {
            call.localMicrophoneAudioLevel.collectAsStateWithLifecycle()
        } else {
            participant.audioLevel.collectAsStateWithLifecycle()
        }
        SoundIndicator(
            // we always draw the audio indicator for the local participant for lower delay
            // and for now don't draw the indicator for other participants due to the lag
            // (so we ingore participant.isSpeaking)
            isSpeaking = participant.isLocal,
            isAudioEnabled = audioEnabled,
            audioLevel = audioLevel,
            modifier = Modifier
                .align(CenterVertically)
                .padding(horizontal = VideoTheme.dimens.participantSoundIndicatorPadding),
        )
    },
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
        // we always draw the audio indicator for the local participant for lower delay
        // and for now don't draw the indicator for other participants due to the lag
        // (so we ingore participant.isSpeaking)
        isSpeaking = participant.isLocal,
        soundIndicatorContent = soundIndicatorContent,
    )
}

@Composable
public fun BoxScope.ParticipantLabel(
    nameLabel: String,
    labelPosition: Alignment = BottomStart,
    hasAudio: Boolean = false,
    isSpeaking: Boolean = false,
    audioLevel: Float = 0f,
    soundIndicatorContent: @Composable RowScope.() -> Unit = {
        SoundIndicator(
            isSpeaking = isSpeaking,
            isAudioEnabled = hasAudio,
            audioLevel = audioLevel,
            modifier = Modifier
                .align(CenterVertically)
                .padding(horizontal = VideoTheme.dimens.participantSoundIndicatorPadding),
        )
    },
) {
    Box(
        modifier = Modifier
            .align(labelPosition)
            .height(VideoTheme.dimens.participantLabelHeight)
            .wrapContentWidth()
            .background(
                VideoTheme.colors.participantLabelBackground,
                shape = VideoTheme.shapes.participantLabelShape,
            ),
    ) {
        Row(
            modifier = Modifier.align(Center),
            verticalAlignment = CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .widthIn(max = VideoTheme.dimens.participantLabelTextMaxWidth)
                    .padding(start = VideoTheme.dimens.participantLabelTextPaddingStart)
                    .align(CenterVertically),
                text = nameLabel,
                style = VideoTheme.typography.body,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            soundIndicatorContent.invoke(this)
        }
    }
}

@Composable
private fun BoxScope.DefaultReaction(
    participant: ParticipantState,
    style: VideoRendererStyle,
) {
    val reactions by participant.reactions.collectAsStateWithLifecycle()
    val reaction = reactions.lastOrNull { it.createdAt + 3000 > System.currentTimeMillis() }
    var currentReaction: Reaction? by remember { mutableStateOf(null) }
    var reactionState: ReactionState by remember { mutableStateOf(ReactionState.Nothing) }

    LaunchedEffect(key1 = reaction) {
        if (reactionState == ReactionState.Nothing) {
            currentReaction?.let { participant.consumeReaction(it) }
            currentReaction = reaction

            // deliberately execute this instead of animation finish listener to remove animation on the screen.
            if (reaction != null) {
                reactionState = ReactionState.Running
                delay(style.reactionDuration * 2 - 50L)
                participant.consumeReaction(reaction)
                currentReaction = null
                reactionState = ReactionState.Nothing
            }
        } else {
            if (currentReaction != null) {
                participant.consumeReaction(currentReaction!!)
                reactionState = ReactionState.Nothing
                currentReaction = null
                delay(style.reactionDuration * 2 - 50L)
            }
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
                easing = LinearOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "reaction",
    )

    val emojiCode = currentReaction?.response?.emojiCode
    if (currentReaction != null && emojiCode != null) {
        val emojiMapper = VideoTheme.reactionMapper
        val emojiText = emojiMapper.map(emojiCode)
        Text(
            text = emojiText,
            modifier = Modifier.align(style.reactionPosition),
            fontSize = size.value.sp,
        )
    }
}

private fun updateParticipantVisibility(
    sessionId: String,
    call: Call,
    visibilityOnScreenState: VisibilityOnScreenState,
) {
    call.state.updateParticipantVisibility(
        sessionId,
        visibilityOnScreenState,
    )
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
                call = mockCall,
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
        )
    }
}
