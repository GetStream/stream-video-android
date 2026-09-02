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
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SignalWifiBad
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomStart
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.ParticipantLabelSoundIndicatorContentParams
import io.getstream.video.android.compose.theme.ParticipantVideoActionsContentParams
import io.getstream.video.android.compose.theme.ParticipantVideoConnectionIndicatorContentParams
import io.getstream.video.android.compose.theme.ParticipantVideoFallbackContentParams
import io.getstream.video.android.compose.theme.ParticipantVideoLabelContentParams
import io.getstream.video.android.compose.theme.ParticipantVideoReactionContentParams
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.theme.design.StreamTokens
import io.getstream.video.android.compose.ui.components.avatar.LocalAvatarPreviewProvider
import io.getstream.video.android.compose.ui.components.call.pinning.ParticipantAction
import io.getstream.video.android.compose.ui.components.call.pinning.participantActions
import io.getstream.video.android.compose.ui.components.indicator.GenericIndicator
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
 * @param mirrorMode Controls horizontal mirroring of the video stream. Defaults to [MirrorMode.AUTO] which mirrors
 * only the local self-view when using the front camera; remote participants are never mirrored.
 * @param actionsContent Content to show action picker with call actions related to the selected participant.
 */

@Composable
public fun ParticipantVideo(
    call: Call,
    participant: ParticipantState,
    modifier: Modifier = Modifier,
    style: VideoRendererStyle = RegularVideoRendererStyle(),
    labelContent: @Composable BoxScope.(ParticipantState) -> Unit = {
        DefaultLabelSlot(call, participant, style)
    },
    connectionIndicatorContent: @Composable BoxScope.(NetworkQuality) -> Unit = {
        DefaultConnectionIndicatorSlot(it)
    },
    scalingType: VideoScalingType = VideoScalingType.SCALE_ASPECT_FILL,
    videoFallbackContent: @Composable (Call) -> Unit = {
        DefaultVideoFallbackSlot(call, participant)
    },
    reactionContent: @Composable BoxScope.(ParticipantState) -> Unit = {
        DefaultReactionSlot(participant, style)
    },
    mirrorMode: MirrorMode = MirrorMode.AUTO,
    actionsContent: @Composable BoxScope.(
        actions: List<ParticipantAction>,
        call: Call,
        participant: ParticipantState,
    ) -> Unit = { actions, call, participant ->
        DefaultActionsSlot(actions, call, participant)
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

    val containerShape = RoundedCornerShape(StreamTokens.radiusXl)
    val containerModifier = if (style.isFocused && participants.size > 1) {
        modifier.border(
            border = if (style.isScreenSharing) {
                BorderStroke(
                    StreamTokens.size2,
                    VideoTheme.colors.accentPrimary,
                )
            } else {
                BorderStroke(
                    StreamTokens.size2,
                    VideoTheme.colors.accentPrimary,
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
            .background(VideoTheme.colors.backgroundCoreSurfaceDefault),
    ) {
        ParticipantVideoRenderer(
            call = call,
            participant = participant,
            scalingType = scalingType,
            mirrorMode = mirrorMode,
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

@Deprecated(
    "Use ParticipantVideo which accepts mirrorMode instead.",
    ReplaceWith(
        "ParticipantVideo(call, participant, modifier, style, labelContent, connectionIndicatorContent, scalingType, videoFallbackContent, reactionContent, actionsContent, mirrorMode)",
    ),
)
@Composable
public fun ParticipantVideo(
    call: Call,
    participant: ParticipantState,
    modifier: Modifier = Modifier,
    style: VideoRendererStyle = RegularVideoRendererStyle(),
    labelContent: @Composable BoxScope.(ParticipantState) -> Unit = {
        DefaultLabelSlot(call, participant, style)
    },
    connectionIndicatorContent: @Composable BoxScope.(NetworkQuality) -> Unit = {
        DefaultConnectionIndicatorSlot(it)
    },
    scalingType: VideoScalingType = VideoScalingType.SCALE_ASPECT_FILL,
    videoFallbackContent: @Composable (Call) -> Unit = {
        DefaultVideoFallbackSlot(call, participant)
    },
    reactionContent: @Composable BoxScope.(ParticipantState) -> Unit = {
        DefaultReactionSlot(participant, style)
    },
    actionsContent: @Composable BoxScope.(
        actions: List<ParticipantAction>,
        call: Call,
        participant: ParticipantState,
    ) -> Unit = { actions, call, participant ->
        DefaultActionsSlot(actions, call, participant)
    },
) {
    ParticipantVideo(
        call,
        participant,
        modifier,
        style,
        labelContent,
        connectionIndicatorContent,
        scalingType,
        videoFallbackContent,
        reactionContent,
        MirrorMode.AUTO,
        actionsContent,
    )
}

/**
 * Renders a single participant with a given call, which contains all the call states.
 * Also displays participant information with a label and connection quality indicator.
 *
 * @param call The call that contains all the participants state and tracks.
 * @param participant Participant to render.
 * @param scalingType The scaling type for the video renderer.
 * @param mirrorMode Controls horizontal mirroring of the video stream. Defaults to [MirrorMode.AUTO] which mirrors
 * only the local self-view when using the front camera; remote participants are never mirrored.
 * @param videoFallbackContent Content is shown the video track is failed to load or not available.
 */
@OptIn(StreamVideoUiDelicateApi::class)
@Composable
public fun ParticipantVideoRenderer(
    call: Call,
    participant: ParticipantState,
    scalingType: VideoScalingType = VideoScalingType.SCALE_ASPECT_FILL,
    mirrorMode: MirrorMode = MirrorMode.AUTO,
    videoFallbackContent: @Composable (Call) -> Unit = {
        DefaultVideoFallbackSlot(call, participant)
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
    val mirror = when (mirrorMode) {
        MirrorMode.AUTO -> cameraDirection == CameraDirection.Front && me?.sessionId == participant.sessionId
        MirrorMode.ALWAYS -> true
        MirrorMode.NEVER -> false
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
        with(VideoTheme.componentFactory) {
            ParticipantLabelSoundIndicatorContent(
                params = ParticipantLabelSoundIndicatorContentParams(
                    // we always draw the audio indicator for the local participant for lower delay
                    // and for now don't draw the indicator for other participants due to the lag
                    // (so we ingore participant.isSpeaking)
                    isSpeaking = participant.isLocal,
                    isAudioEnabled = audioEnabled,
                    audioLevel = audioLevel,
                    modifier = Modifier
                        .align(CenterVertically)
                        .padding(
                            vertical = StreamTokens.spacingXxs,
                            horizontal = StreamTokens.spacingXs,
                        )
                        .testTag("Stream_ParticipantMicrophone_Enabled_$audioEnabled"),
                ),
            )
        }
    },
) {
    val audioEnabled by participant.audioEnabled.collectAsStateWithLifecycle()
    val pinnedParticipants by call.state.pinnedParticipants.collectAsStateWithLifecycle()
    val pinned = pinnedParticipants.containsKey(participant.sessionId)

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
        with(VideoTheme.componentFactory) {
            ParticipantLabelSoundIndicatorContent(
                params = ParticipantLabelSoundIndicatorContentParams(
                    isSpeaking = isSpeaking,
                    isAudioEnabled = hasAudio,
                    audioLevel = audioLevel,
                    modifier = Modifier
                        .align(CenterVertically)
                        .padding(horizontal = StreamTokens.spacingXs),
                ),
            )
        }
    },
) {
    var componentWidth by remember { mutableStateOf(0.dp) }
    componentWidth = 100.dp
    // get local density from composable
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .align(labelPosition)
            .height(StreamTokens.size32)
            .wrapContentWidth()
            .background(
                VideoTheme.colors.backgroundCoreOverlayDarkStrong,
                shape = RoundedCornerShape(
                    topStart = ZeroCornerSize,
                    topEnd = StreamTokens.radiusXl,
                    bottomEnd = ZeroCornerSize,
                    bottomStart = ZeroCornerSize,
                ),
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
                    .padding(start = StreamTokens.spacingMd)
                    .align(CenterVertically)
                    .testTag("Stream_ParticipantName"),
                text = nameLabel,
                style = VideoTheme.typography.captionDefault,
                color = VideoTheme.colors.textOnAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (isPinned) {
                Spacer(modifier = Modifier.size(StreamTokens.spacingMd))
                GenericIndicator {
                    Icon(

                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(StreamTokens.size16),
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = "Pin",
                        tint = VideoTheme.colors.textOnAccent,
                    )
                }
            }

            if (isPaused) {
                Spacer(modifier = Modifier.size(StreamTokens.spacingMd))
                GenericIndicator {
                    Icon(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(StreamTokens.size16),
                        imageVector = Icons.Filled.SignalWifiBad,
                        contentDescription = "Pause",
                        tint = VideoTheme.colors.textOnAccent,
                    )
                }
            }
            soundIndicatorContent.invoke(this)
        }
    }
}

@Composable
private fun BoxScope.DefaultLabelSlot(
    call: Call,
    participant: ParticipantState,
    style: VideoRendererStyle,
) {
    with(VideoTheme.componentFactory) {
        ParticipantVideoLabelContent(
            params = ParticipantVideoLabelContentParams(
                call = call,
                participant = participant,
                labelPosition = style.labelPosition,
            ),
        )
    }
}

@Composable
private fun BoxScope.DefaultConnectionIndicatorSlot(networkQuality: NetworkQuality) {
    with(VideoTheme.componentFactory) {
        ParticipantVideoConnectionIndicatorContent(
            params = ParticipantVideoConnectionIndicatorContentParams(
                networkQuality = networkQuality,
            ),
        )
    }
}

@Composable
private fun DefaultVideoFallbackSlot(call: Call, participant: ParticipantState) {
    VideoTheme.componentFactory.ParticipantVideoFallbackContent(
        params = ParticipantVideoFallbackContentParams(call = call, participant = participant),
    )
}

@Composable
private fun BoxScope.DefaultReactionSlot(
    participant: ParticipantState,
    style: VideoRendererStyle,
) {
    with(VideoTheme.componentFactory) {
        ParticipantVideoReactionContent(
            params = ParticipantVideoReactionContentParams(
                participant = participant,
                style = style,
            ),
        )
    }
}

@Composable
private fun BoxScope.DefaultActionsSlot(
    actions: List<ParticipantAction>,
    call: Call,
    participant: ParticipantState,
) {
    with(VideoTheme.componentFactory) {
        ParticipantVideoActionsContent(
            params = ParticipantVideoActionsContentParams(
                actions = actions,
                call = call,
                participant = participant,
            ),
        )
    }
}

@Composable
internal fun BoxScope.DefaultReaction(
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
            StreamTokens.size48
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
        val emojiMapper = VideoTheme.config.reactionMapper
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
