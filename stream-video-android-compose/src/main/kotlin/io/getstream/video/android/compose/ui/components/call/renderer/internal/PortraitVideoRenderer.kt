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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
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
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.mock.mockCall
import io.getstream.video.android.mock.mockParticipantList

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
) {
    val remoteParticipants by call.state.remoteParticipants.collectAsStateWithLifecycle()

    if (callParticipants.isEmpty() ||
        (remoteParticipants.isEmpty() && callParticipants.size > 1)
    ) {
        return
    }

    val paddedModifier = modifier.padding(VideoTheme.dimens.participantsGridPadding)
    when (callParticipants.size) {
        1, 2 -> {
            val participant = if (remoteParticipants.isEmpty()) {
                callParticipants.first()
            } else {
                remoteParticipants.first()
            }
            videoRenderer.invoke(
                modifier = paddedModifier,
                call = call,
                participant = participant,
                style = style.copy(
                    isFocused = dominantSpeaker?.sessionId == participant.sessionId,
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
            )
        }

        4, 5, 6 -> {
            val columnSize = when (callParticipants.size) {
                4 -> Pair(2, 2)
                5 -> Pair(2, 3)
                else -> Pair(3, 3) // 6, because if 7 we are not in this branch
            }
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
                )
            }
        }
        else -> {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val gridState = lazyGridStateWithVisibilityNotification(call = call)
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
                                modifier = paddedModifier.height(itemHeight),
                                call = call,
                                participant = participant,
                                style = style.copy(
                                    isFocused = dominantSpeaker?.sessionId == participant.sessionId,
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
) {
    Column(modifier) {
        repeat(remoteParticipants.size) {
            val participant = remoteParticipants[it]
            videoRenderer.invoke(
                modifier = paddedModifier.weight(1f),
                call = call,
                participant = participant,
                style = style.copy(
                    isFocused = dominantSpeaker?.sessionId == participant.sessionId,
                ),
            )
        }
    }
}

@Preview
@Composable
private fun PortraitParticipantsPreview1() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = mockParticipantList

        Box(
            modifier = Modifier.background(color = VideoTheme.colors.appBackground),
        ) {
            PortraitVideoRenderer(
                call = mockCall,
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
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = mockParticipantList

        Box(
            modifier = Modifier.background(color = VideoTheme.colors.appBackground),
        ) {
            PortraitVideoRenderer(
                call = mockCall,
                dominantSpeaker = mockParticipantList[0],
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
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = mockParticipantList

        Box(
            modifier = Modifier.background(color = VideoTheme.colors.appBackground),
        ) {
            PortraitVideoRenderer(
                call = mockCall,
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
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = mockParticipantList

        Box(
            modifier = Modifier.background(color = VideoTheme.colors.appBackground),
        ) {
            PortraitVideoRenderer(
                call = mockCall,
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
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = mockParticipantList

        Box(
            modifier = Modifier.background(color = VideoTheme.colors.appBackground),
        ) {
            PortraitVideoRenderer(
                call = mockCall,
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
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = mockParticipantList

        Box(
            modifier = Modifier.background(color = VideoTheme.colors.appBackground),
        ) {
            PortraitVideoRenderer(
                call = mockCall,
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
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = mockParticipantList

        Box(
            modifier = Modifier.background(color = VideoTheme.colors.appBackground),
        ) {
            PortraitVideoRenderer(
                call = mockCall,
                dominantSpeaker = participants[0],
                callParticipants = participants.take(7),
                modifier = Modifier.fillMaxSize(),
                parentSize = IntSize(screenWidth, screenHeight),
            )
        }
    }
}
