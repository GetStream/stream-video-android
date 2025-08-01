/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.compose.ui.components.call.renderer.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.renderer.DefaultFloatingParticipantVideo
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantVideo
import io.getstream.video.android.compose.ui.components.call.renderer.RegularVideoRendererStyle
import io.getstream.video.android.compose.ui.components.call.renderer.VideoRendererStyle
import io.getstream.video.android.compose.ui.components.call.renderer.copy
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.mock.previewParticipantsList

/**
 * Renders call participants based on the number of people in a call, in portrait mode.
 *
 * @param call The state of the call.
 * @param dominantSpeaker The primary speaker in the call.
 * @param callParticipants The list of participants in the call.
 * @param modifier Modifier for styling.
 * @param parentSize The size of the parent.
 * @param style Defined properties for styling a single video call track.
 * @param videoRenderer A single video renderer renders each individual participant.
 * @param floatingVideoRenderer A floating video renderer renders an individual participant.
 */
@Composable
internal fun BoxScope.PortraitVideoRenderer(
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
    floatingVideoRenderer: @Composable (BoxScope.(call: Call, IntSize) -> Unit)? = null,
) {
    val remoteParticipants by call.state.remoteParticipants.collectAsStateWithLifecycle()

    if (callParticipants.isEmpty() ||
        (remoteParticipants.isEmpty() && callParticipants.size > 1)
    ) {
        return
    }

    val isDomSpeakerSpeaking by dominantSpeaker
        ?.speaking
        ?.collectAsStateWithLifecycle(initialValue = false)
        ?: remember { mutableStateOf(false) }

    val paddedModifier = modifier.padding(VideoTheme.dimens.spacingXXs)
    when (callParticipants.size) {
        1, 2 -> {
            val participant = if (remoteParticipants.isEmpty()) {
                callParticipants.first()
            } else {
                remoteParticipants.first()
            }
            videoRenderer.invoke(
                paddedModifier,
                call,
                participant,
                style.copy(
                    isFocused = isDomSpeakerSpeaking && (dominantSpeaker?.sessionId == participant.sessionId),
                ),
            )
        }

        3 -> {
            ParticipantColumn(
                modifier,
                remoteParticipants,
                videoRenderer,
                paddedModifier,
                call,
                style,
                dominantSpeaker,
                0,
            )
        }

        4 -> {
            val columnSize = Pair(2, 2)
            Row(modifier) {
                ParticipantColumn(
                    modifier = modifier.weight(1f),
                    remoteParticipants = callParticipants.take(columnSize.first),
                    videoRenderer = videoRenderer,
                    paddedModifier = paddedModifier,
                    call = call,
                    style = style,
                    dominantSpeaker = dominantSpeaker,
                )

                ParticipantColumn(
                    modifier = modifier.weight(1f),
                    remoteParticipants = callParticipants.takeLast(columnSize.second),
                    videoRenderer = videoRenderer,
                    paddedModifier = paddedModifier,
                    call = call,
                    style = style,
                    dominantSpeaker = dominantSpeaker,
                    expectedColumnSize = columnSize.first,
                )
            }
        }

        5, 6 -> {
            val columnSize = if (callParticipants.size == 5) Pair(3, 2) else Pair(3, 3)

            Row(modifier) {
                ParticipantColumn(
                    modifier = modifier.weight(1f),
                    remoteParticipants = callParticipants.take(columnSize.first),
                    videoRenderer = videoRenderer,
                    paddedModifier = paddedModifier,
                    call = call,
                    style = style,
                    dominantSpeaker = dominantSpeaker,
                )

                ParticipantColumn(
                    modifier = modifier.weight(1f),
                    remoteParticipants = callParticipants.takeLast(columnSize.second),
                    videoRenderer = videoRenderer,
                    paddedModifier = paddedModifier,
                    call = call,
                    style = style,
                    dominantSpeaker = dominantSpeaker,
                    expectedColumnSize = columnSize.first,
                )
            }
        }

        else -> {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val gridState =
                    lazyStateWithVisibilityNotification(
                        call = call,
                        original = rememberLazyGridState(),
                    )
                LazyVerticalGrid(
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    content = {
                        items(
                            count = callParticipants.size,
                            key = { callParticipants[it].sessionId },
                        ) { key ->
                            // make 3 items exactly fit available height
                            val itemHeight = with(LocalDensity.current) {
                                (constraints.maxHeight / 3).toDp()
                            }
                            val participant = callParticipants[key]
                            videoRenderer.invoke(
                                paddedModifier.height(itemHeight),
                                call,
                                participant,
                                style.copy(
                                    isFocused = isDomSpeakerSpeaking && (dominantSpeaker?.sessionId == participant.sessionId),
                                ),
                            )
                        }
                    },
                )
            }
        }
    }

    if (callParticipants.size in 2..3) {
        val currentLocal by call.state.me.collectAsStateWithLifecycle()

        if (currentLocal != null) {
            floatingVideoRenderer?.invoke(this, call, parentSize)
                ?: DefaultFloatingParticipantVideo(
                    call = call,
                    me = currentLocal!!,
                    callParticipants = callParticipants,
                    parentSize = parentSize,
                    style = style,
                )
        } else if (LocalInspectionMode.current) {
            floatingVideoRenderer?.invoke(this, call, parentSize)
                ?: DefaultFloatingParticipantVideo(
                    call = call,
                    me = callParticipants.first(),
                    callParticipants = callParticipants,
                    parentSize = parentSize,
                    style = style,
                )
        }
    }
}

@Composable
private fun ParticipantColumn(
    modifier: Modifier,
    remoteParticipants: List<ParticipantState>,
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
    expectedColumnSize: Int = remoteParticipants.size,
) {
    val isDomSpeakerSpeaking by dominantSpeaker
        ?.speaking
        ?.collectAsStateWithLifecycle(initialValue = false)
        ?: remember { mutableStateOf(false) }

    Column(modifier) {
        repeat(remoteParticipants.size) {
            val participant = remoteParticipants[it]
            videoRenderer.invoke(
                paddedModifier.weight(1f),
                call,
                participant,
                style.copy(
                    isFocused = isDomSpeakerSpeaking && (dominantSpeaker?.sessionId == participant.sessionId),
                ),
            )
        }
        repeat(expectedColumnSize - remoteParticipants.size) {
            Box(modifier = paddedModifier.weight(1f))
        }
    }
}

@Preview
@Composable
private fun PortraitParticipantsPreview1() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = previewParticipantsList

        Box(
            modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
        ) {
            PortraitVideoRenderer(
                call = previewCall,
                dominantSpeaker = participants[0],
                callParticipants = participants.take(1),
                modifier = Modifier.fillMaxSize(),
                parentSize = IntSize(screenWidth, screenHeight),
            )
        }
    }
}

@Preview
@Composable
private fun PortraitParticipantsPreview2() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = previewParticipantsList

        Box(
            modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
        ) {
            PortraitVideoRenderer(
                call = previewCall,
                dominantSpeaker = previewParticipantsList[0],
                callParticipants = participants.take(2),
                modifier = Modifier.fillMaxSize(),
                parentSize = IntSize(screenWidth, screenHeight),
            )
        }
    }
}

@Preview
@Composable
private fun PortraitParticipantsPreview3() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = previewParticipantsList

        Box(
            modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
        ) {
            PortraitVideoRenderer(
                call = previewCall,
                dominantSpeaker = participants[0],
                callParticipants = participants.take(3),
                modifier = Modifier.fillMaxSize(),
                parentSize = IntSize(screenWidth, screenHeight),
            )
        }
    }
}

@Preview
@Composable
private fun PortraitParticipantsPreview4() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = previewParticipantsList

        Box(
            modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
        ) {
            PortraitVideoRenderer(
                call = previewCall,
                dominantSpeaker = participants[0],
                callParticipants = participants.take(4),
                modifier = Modifier.fillMaxSize(),
                parentSize = IntSize(screenWidth, screenHeight),
            )
        }
    }
}

@Preview
@Composable
private fun PortraitParticipantsPreview5() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = previewParticipantsList

        Box(
            modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
        ) {
            PortraitVideoRenderer(
                call = previewCall,
                dominantSpeaker = participants[0],
                callParticipants = participants.take(5),
                modifier = Modifier.fillMaxSize(),
                parentSize = IntSize(screenWidth, screenHeight),
            )
        }
    }
}

@Preview
@Composable
private fun PortraitParticipantsPreview6() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = previewParticipantsList

        Box(
            modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
        ) {
            PortraitVideoRenderer(
                call = previewCall,
                dominantSpeaker = participants[0],
                callParticipants = participants.take(6),
                modifier = Modifier.fillMaxSize(),
                parentSize = IntSize(screenWidth, screenHeight),
            )
        }
    }
}

@Preview
@Composable
private fun PortraitParticipantsPreview7() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = previewParticipantsList

        Box(
            modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
        ) {
            PortraitVideoRenderer(
                call = previewCall,
                dominantSpeaker = participants[0],
                callParticipants = participants.take(7),
                modifier = Modifier.fillMaxSize(),
                parentSize = IntSize(screenWidth, screenHeight),
            )
        }
    }
}
