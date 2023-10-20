package io.getstream.video.android.compose.ui.components.call.renderer.internal

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.renderer.FloatingParticipantVideo
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantVideo
import io.getstream.video.android.compose.ui.components.call.renderer.RegularVideoRendererStyle
import io.getstream.video.android.compose.ui.components.call.renderer.VideoRendererStyle
import io.getstream.video.android.compose.ui.components.call.renderer.copy
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState


/**
 * Renders call participants based on the number of people in a call, in landscape mode.
 *
 * @param orientation the screen orientation.
 * @param call The state of the call.
 * @param dominantSpeaker The primary speaker in the call.
 * @param callParticipants The list of participants in the call.
 * @param modifier Modifier for styling.
 * @param parentSize The size of the parent.
 * @param style Defined properties for styling a single video call track.
 * @param videoRenderer A single video renderer renders each individual participant.
 */
@Composable
internal fun BoxScope.CommonVideoRenderer(
    orientation: Int,
    call: Call,
    dominantSpeaker: ParticipantState?,
    callParticipants: List<ParticipantState>,
    modifier: Modifier,
    parentSize: IntSize,
    style: VideoRendererStyle = RegularVideoRendererStyle(),
    videoRenderer: @Composable (
        modifier: Modifier,
        call: Call,
        participant: ParticipantState,
        style: VideoRendererStyle,
    ) -> Unit = { videoModifier, videoCall, videoParticipant, videoStyle ->
        ParticipantVideo(
            modifier = videoModifier,
            call = videoCall,
            participant = videoParticipant,
            style = videoStyle,
        )
    },
) {
    val remoteParticipants by call.state.remoteParticipants.collectAsStateWithLifecycle()
    val paddedModifier = modifier.padding(VideoTheme.dimens.participantsGridPadding)
    when (callParticipants.size) {
        1, 2 -> {
            val participant = if (remoteParticipants.isEmpty()) {
                callParticipants.first()
            } else {
                remoteParticipants.first()
            }

            videoRenderer.invoke(
                paddedModifier.fillMaxHeight(), call, participant,
                style.copy(
                    isFocused = dominantSpeaker?.sessionId == participant.sessionId,
                ),
            )
        }

        3, 4 -> {
            if (orientation == ORIENTATION_LANDSCAPE) {
                val rowItemWeight = 1f / callParticipants.size
                Row(modifier = modifier) {
                    remoteParticipants.take(callParticipants.size - 1).forEach { participant ->
                        videoRenderer.invoke(
                            modifier = paddedModifier
                                .fillMaxHeight()
                                .weight(rowItemWeight),
                            call = call,
                            participant = participant,
                            style = style.copy(
                                isFocused = dominantSpeaker?.sessionId == participant.sessionId,
                            ),
                        )
                    }
                }
            } else {
                ParticipantColumnOrRow(
                    orientation,
                    modifier,
                    remoteParticipants,
                    videoRenderer,
                    paddedModifier,
                    call,
                    style,
                    dominantSpeaker,
                )
            }
        }

        5, 6 -> {
            val rowOrColumnSize = if (callParticipants.size == 5) Pair(3, 2) else Pair(3, 3)
            if (orientation == ORIENTATION_PORTRAIT) {
                Column(modifier) {
                    ParticipantColumnOrRow(
                        orientation = orientation,
                        modifier = Modifier.weight(1f),
                        participants = callParticipants.take(rowOrColumnSize.first),
                        videoRenderer = videoRenderer,
                        paddedModifier = paddedModifier,
                        call = call,
                        style = style,
                        dominantSpeaker = dominantSpeaker,
                    )
                    ParticipantColumnOrRow(
                        orientation = orientation,
                        modifier = Modifier.weight(1f),
                        participants = callParticipants.take(rowOrColumnSize.first),
                        videoRenderer = videoRenderer,
                        paddedModifier = paddedModifier,
                        call = call,
                        style = style,
                        dominantSpeaker = dominantSpeaker,
                        expectedRowSize = rowOrColumnSize.first,
                    )
                }
            } else {
                Row(modifier) {
                    ParticipantColumnOrRow(
                        orientation = orientation,
                        modifier = Modifier.weight(1f),
                        participants = callParticipants.take(rowOrColumnSize.first),
                        videoRenderer = videoRenderer,
                        paddedModifier = paddedModifier,
                        call = call,
                        style = style,
                        dominantSpeaker = dominantSpeaker,
                    )
                    ParticipantColumnOrRow(
                        orientation = orientation,
                        modifier = Modifier.weight(1f),
                        participants = callParticipants.take(rowOrColumnSize.first),
                        videoRenderer = videoRenderer,
                        paddedModifier = paddedModifier,
                        call = call,
                        style = style,
                        dominantSpeaker = dominantSpeaker,
                        expectedRowSize = rowOrColumnSize.first
                    )
                }
            }
        }

        else -> {
            BoxWithConstraints(modifier = Modifier.fillMaxHeight()) {
                val gridState = lazyGridStateWithVisibilityNotification(call = call)
                LazyVerticalGrid(
                    modifier = Modifier.fillMaxSize(),
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    content = {
                        items(
                            count = callParticipants.size,
                            key = { callParticipants[it].sessionId },
                        ) { key ->
                            // make 2 items exactly fit available height
                            val itemHeight = with(LocalDensity.current) {
                                (constraints.maxHeight / 2).toDp()
                            }
                            val participant = callParticipants[key]
                            videoRenderer.invoke(
                                paddedModifier.height(itemHeight), call, participant,
                                style.copy(
                                    isFocused = dominantSpeaker?.sessionId == participant.sessionId,
                                ),
                            )
                        }
                    },
                )
            }
        }
    }

    if (callParticipants.size in 2..4) {
        val currentLocal by call.state.me.collectAsStateWithLifecycle()
        if (currentLocal != null || LocalInspectionMode.current) {
            FloatingParticipantVideo(
                call = call,
                participant = if (LocalInspectionMode.current) {
                    callParticipants.first()
                } else {
                    currentLocal!!
                },
                style = style.copy(isShowingConnectionQualityIndicator = false),
                parentBounds = parentSize,
            )
        }
    }
}

@Composable
private fun ParticipantColumnOrRow(
    orientation: Int,
    modifier: Modifier,
    participants: List<ParticipantState>,
    videoRenderer: @Composable (
        modifier: Modifier,
        call: Call,
        participant: ParticipantState,
        style: VideoRendererStyle,
    ) -> Unit,
    paddedModifier: Modifier,
    call: Call,
    style: VideoRendererStyle,
    dominantSpeaker: ParticipantState?,
    expectedRowSize: Int = participants.size
) {
    if (orientation == ORIENTATION_PORTRAIT) {
        Column(modifier) {
            ParticipantColumnOrRowContent(
                modifier = paddedModifier.weight(1f),
                participants = participants,
                videoRenderer = videoRenderer,
                call = call,
                style = style,
                dominantSpeaker = dominantSpeaker,
                expectedRowSize = expectedRowSize,
            )
        }
    } else {
        Row(modifier) {
            ParticipantColumnOrRowContent(
                modifier = paddedModifier.weight(1f),
                participants = participants,
                videoRenderer = videoRenderer,
                call = call,
                style = style,
                dominantSpeaker = dominantSpeaker,
                expectedRowSize = expectedRowSize,
            )
        }
    }
}

@Composable
private fun ParticipantColumnOrRowContent(
    modifier: Modifier,
    participants: List<ParticipantState>,
    videoRenderer: @Composable (
        modifier: Modifier,
        call: Call,
        participant: ParticipantState,
        style: VideoRendererStyle,
    ) -> Unit,
    call: Call,
    style: VideoRendererStyle,
    dominantSpeaker: ParticipantState?,
    expectedRowSize: Int = participants.size) {
    repeat(participants.size) {
        val participant = participants[it]
        videoRenderer.invoke(
            modifier, call, participant,
            style.copy(
                isFocused = dominantSpeaker?.sessionId == participant.sessionId,
            ),
        )
    }
    repeat(expectedRowSize - participants.size) {
        Box(modifier = modifier)
    }
}