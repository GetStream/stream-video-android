/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SignalWifiBad
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Alignment.Companion.BottomStart
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.TopStart
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import io.getstream.video.android.compose.ui.components.call.pinning.ParticipantAction
import io.getstream.video.android.compose.ui.components.call.pinning.ParticipantActions
import io.getstream.video.android.compose.ui.components.call.pinning.participantActions
import io.getstream.video.android.compose.ui.components.indicator.GenericIndicator
import io.getstream.video.android.compose.ui.components.indicator.NetworkQualityIndicator
import io.getstream.video.android.compose.ui.components.indicator.SoundIndicator
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.compose.ui.components.video.VideoScalingType
import io.getstream.video.android.compose.ui.components.video.config.videoRenderConfig
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CameraDirection
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.model.NetworkQuality
import io.getstream.video.android.core.model.Reaction
import io.getstream.video.android.core.model.ReactionState
import io.getstream.video.android.core.model.VisibilityOnScreenState
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.mock.previewParticipantsList
import io.getstream.video.android.ui.common.R
import io.getstream.video.android.ui.common.util.StreamVideoUiDelicateApi
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
 * @param actionsContent Content to show action picker with call actions related to the selected participant.
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
                .height(VideoTheme.dimens.componentHeightM)
                .testTag("Stream_ParticipantNetworkQualityIndicator"),
        )
    },
    scalingType: VideoScalingType = VideoScalingType.SCALE_ASPECT_FILL,
    videoFallbackContent: @Composable (Call) -> Unit = {
        val userName by participant.userNameOrId.collectAsStateWithLifecycle()
        val userImage by participant.image.collectAsStateWithLifecycle()
        UserAvatarBackground(userImage = userImage, userName = userName)
    },
    reactionContent: @Composable BoxScope.(ParticipantState) -> Unit = {
        DefaultReaction(
            participant = participant,
            style = style,
        )
    },
    actionsContent: @Composable BoxScope.(
        actions: List<ParticipantAction>,
        call: Call,
        participant: ParticipantState,
    ) -> Unit = { actions, call, participant ->
        ParticipantActions(
            Modifier
                .align(TopStart)
                .padding(8.dp)
                .testTag("Stream_ParticipantActionsIcon"),
            actions,
            call,
            participant,
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

    val containerShape = VideoTheme.shapes.sheet
    val containerModifier = if (style.isFocused && participants.size > 1) {
        modifier.border(
            border = if (style.isScreenSharing) {
                BorderStroke(
                    VideoTheme.dimens.genericXXs,
                    VideoTheme.colors.brandPrimary,
                )
            } else {
                BorderStroke(
                    VideoTheme.dimens.genericXXs,
                    VideoTheme.colors.brandPrimary,
                )
            },
            shape = containerShape,
        )
    } else {
        modifier
    }
    Box(
        modifier = containerModifier
            .clip(containerShape)
            .background(VideoTheme.colors.baseSheetTertiary),
    ) {
        ParticipantVideoRenderer(
            call = call,
            participant = participant,
            scalingType = scalingType,
            videoFallbackContent = videoFallbackContent,
        )

        actionsContent.invoke(this, participantActions, call, participant)

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
 * @param scalingType The scaling type for the video renderer.
 * @param videoFallbackContent Content is shown the video track is failed to load or not available.
 */
@OptIn(StreamVideoUiDelicateApi::class)
@Composable
public fun ParticipantVideoRenderer(
    call: Call,
    participant: ParticipantState,
    scalingType: VideoScalingType = VideoScalingType.SCALE_ASPECT_FILL,
    videoFallbackContent: @Composable (Call) -> Unit = {
        val userName by participant.userNameOrId.collectAsStateWithLifecycle()
        val userImage by participant.image.collectAsStateWithLifecycle()
        UserAvatarBackground(userImage = userImage, userName = userName)
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
    val cameraDirection by call.camera.direction.collectAsStateWithLifecycle()
    val me by call.state.me.collectAsStateWithLifecycle()
    val mirror by remember(cameraDirection) {
        mutableStateOf(
            cameraDirection == CameraDirection.Front && me?.sessionId == participant.sessionId,
        )
    }
    val videoRendererConfig = remember(mirror, scalingType, videoFallbackContent) {
        videoRenderConfig {
            mirrorStream = mirror
            this.videoScalingType = scalingType
            this.fallbackContent = videoFallbackContent
        }
    }
    VideoRenderer(
        call = call,
        video = video,
        videoRendererConfig = videoRendererConfig,
    )
}

@Composable
public fun BoxScope.ParticipantLabel(
    call: Call,
    participant: ParticipantState,
    labelPosition: Alignment = BottomStart,
    soundIndicatorContent: @Composable RowScope.() -> Unit = {
        val audioEnabled by participant.audioEnabled.collectAsStateWithLifecycle()
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
                .padding(
                    vertical = VideoTheme.dimens.spacingXs,
                    horizontal = VideoTheme.dimens.spacingS,
                )
                .testTag("Stream_ParticipantMicrophone_Enabled_$audioEnabled"),
        )
    },
) {
    val audioEnabled by participant.audioEnabled.collectAsStateWithLifecycle()
    val pinned by remember {
        derivedStateOf { call.state.pinnedParticipants.value.contains(participant.sessionId) }
    }
    val userNameOrId by participant.userNameOrId.collectAsStateWithLifecycle()
    val nameLabel = if (participant.isLocal) {
        stringResource(id = R.string.stream_video_myself)
    } else {
        userNameOrId
    }
    val paused = participant.videoPaused.collectAsStateWithLifecycle()

    ParticipantLabel(
        nameLabel = nameLabel,
        isPinned = pinned,
        labelPosition = labelPosition,
        hasAudio = audioEnabled,
        // we always draw the audio indicator for the local participant for lower delay
        // and for now don't draw the indicator for other participants due to the lag
        // (so we ingore participant.isSpeaking)
        isSpeaking = participant.isLocal,
        isPaused = paused.value,
        soundIndicatorContent = soundIndicatorContent,
    )
}

@Composable
public fun BoxScope.ParticipantLabel(
    nameLabel: String,
    isPinned: Boolean = false,
    labelPosition: Alignment = BottomStart,
    hasAudio: Boolean = false,
    isSpeaking: Boolean = false,
    isPaused: Boolean = false,
    audioLevel: Float = 0f,
    soundIndicatorContent: @Composable RowScope.() -> Unit = {
        SoundIndicator(
            isSpeaking = isSpeaking,
            isAudioEnabled = hasAudio,
            audioLevel = audioLevel,
            modifier = Modifier
                .align(CenterVertically)
                .padding(horizontal = VideoTheme.dimens.spacingS),
        )
    },
) {
    var componentWidth by remember { mutableStateOf(0.dp) }
    componentWidth = VideoTheme.dimens.genericMax
    // get local density from composable
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .align(labelPosition)
            .height(VideoTheme.dimens.componentHeightM)
            .wrapContentWidth()
            .background(
                VideoTheme.colors.baseSheetQuarternary,
                shape = RoundedCornerShape(topEnd = VideoTheme.dimens.roundnessM),
            )
            .onGloballyPositioned {
                componentWidth = with(density) {
                    it.size.width.toDp()
                }
            },
    ) {
        Row(
            modifier = Modifier.align(Center),
            verticalAlignment = CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .widthIn(max = componentWidth)
                    .padding(start = VideoTheme.dimens.spacingM)
                    .align(CenterVertically)
                    .testTag("Stream_ParticipantName"),
                text = nameLabel,
                style = VideoTheme.typography.bodyS,
                color = VideoTheme.colors.basePrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (isPinned) {
                Spacer(modifier = Modifier.size(VideoTheme.dimens.spacingM))
                GenericIndicator {
                    Icon(

                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(VideoTheme.dimens.genericM),
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = "Pin",
                        tint = Color.White,
                    )
                }
            }

            if (isPaused) {
                Spacer(modifier = Modifier.size(VideoTheme.dimens.spacingM))
                GenericIndicator {
                    Icon(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(VideoTheme.dimens.genericM),
                        imageVector = Icons.Filled.SignalWifiBad,
                        contentDescription = "Pause",
                        tint = Color.White,
                    )
                }
            }
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
            VideoTheme.dimens.componentHeightL
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
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        ParticipantVideo(
            call = previewCall,
            participant = previewParticipantsList[1],
        )
    }
}

@Preview
@Composable
private fun ParticipantLabelPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        Box {
            ParticipantLabel(
                nameLabel = "The name",
                isPinned = true,
                labelPosition = BottomStart,
                hasAudio = true,
                isSpeaking = true,
                audioLevel = 0f,
                soundIndicatorContent = {
                    SoundIndicator(
                        isSpeaking = true,
                        isAudioEnabled = true,
                        audioLevel = 0.8f,
                        modifier = Modifier
                            .align(CenterVertically)
                            .padding(horizontal = VideoTheme.dimens.spacingS),
                    )
                },
            )
        }
    }
}

@Preview
@Composable
private fun ParticipantLabelPausedPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        Box {
            ParticipantLabel(
                nameLabel = "The name",
                isPinned = true,
                labelPosition = BottomStart,
                hasAudio = true,
                isSpeaking = true,
                isPaused = true,
                audioLevel = 0f,
                soundIndicatorContent = {
                    SoundIndicator(
                        isSpeaking = true,
                        isAudioEnabled = true,
                        audioLevel = 0.8f,
                        modifier = Modifier
                            .align(CenterVertically)
                            .padding(horizontal = VideoTheme.dimens.spacingS),
                    )
                },
            )
        }
    }
}

@Preview
@Composable
private fun ParticipantVideoPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        ParticipantVideoRenderer(
            call = previewCall,
            participant = previewParticipantsList[1],
        )
    }
}
