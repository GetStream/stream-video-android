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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
 * @param style Represents a regular video call render styles.
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
        style: VideoRendererStyle
    ) -> Unit = { videoModifier, videoCall, videoParticipant, videoStyle ->
        ParticipantVideo(
            modifier = videoModifier,
            call = videoCall,
            participant = videoParticipant,
            style = videoStyle
        )
    },
) {
    val remoteParticipants by call.state.remoteParticipants.collectAsStateWithLifecycle()

    if (callParticipants.isEmpty() ||
        (remoteParticipants.isEmpty() && callParticipants.size > 1)
    ) return

    when (callParticipants.size) {
        1 -> {
            val participant = callParticipants.first()

            videoRenderer.invoke(
                modifier = modifier,
                call = call,
                participant = participant,
                style = style.copy(
                    isFocused = dominantSpeaker?.sessionId == participant.sessionId
                )
            )
        }

        2 -> {
            val participant = remoteParticipants.first()

            videoRenderer.invoke(
                modifier = modifier,
                call = call,
                participant = participant,
                style = style.copy(
                    isFocused = dominantSpeaker?.sessionId == participant.sessionId
                )
            )
        }

        3 -> {
            val firstParticipant = remoteParticipants[0]
            val secondParticipant = remoteParticipants[1]

            Column(modifier) {
                videoRenderer.invoke(
                    modifier = Modifier.weight(1f),
                    call = call,
                    participant = firstParticipant,
                    style = style.copy(
                        isFocused = dominantSpeaker?.sessionId == firstParticipant.sessionId
                    )
                )

                videoRenderer.invoke(
                    modifier = Modifier.weight(1f),
                    call = call,
                    participant = secondParticipant,
                    style = style.copy(
                        isFocused = dominantSpeaker?.sessionId == secondParticipant.sessionId
                    )
                )
            }
        }

        4 -> {
            val firstParticipant = callParticipants[0]
            val secondParticipant = callParticipants[1]
            val thirdParticipant = callParticipants[2]
            val fourthParticipant = callParticipants[3]

            Row(modifier) {
                Column(modifier = Modifier.weight(1f)) {
                    videoRenderer.invoke(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = firstParticipant,
                        style = style.copy(
                            isFocused = dominantSpeaker?.sessionId == firstParticipant.sessionId
                        )
                    )

                    videoRenderer.invoke(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = secondParticipant,
                        style = style.copy(
                            isFocused = dominantSpeaker?.sessionId == secondParticipant.sessionId
                        )
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    videoRenderer.invoke(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = thirdParticipant,
                        style = style.copy(
                            isFocused = dominantSpeaker?.sessionId == thirdParticipant.sessionId
                        )
                    )

                    videoRenderer.invoke(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = fourthParticipant,
                        style = style.copy(
                            isFocused = dominantSpeaker?.sessionId == fourthParticipant.sessionId
                        )
                    )
                }
            }
        }

        5 -> {
            val firstParticipant = callParticipants[0]
            val secondParticipant = callParticipants[1]
            val thirdParticipant = callParticipants[2]
            val fourthParticipant = callParticipants[3]
            val fifthParticipant = callParticipants[4]

            Row(modifier) {
                Column(modifier = Modifier.weight(1f)) {
                    videoRenderer.invoke(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = firstParticipant,
                        style = style.copy(
                            isFocused = dominantSpeaker?.sessionId == firstParticipant.sessionId
                        )
                    )

                    videoRenderer.invoke(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = secondParticipant,
                        style = style.copy(
                            isFocused = dominantSpeaker?.sessionId == secondParticipant.sessionId
                        )
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    videoRenderer.invoke(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = thirdParticipant,
                        style = style.copy(
                            isFocused = dominantSpeaker?.sessionId == thirdParticipant.sessionId
                        )
                    )

                    videoRenderer.invoke(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = fourthParticipant,
                        style = style.copy(
                            isFocused = dominantSpeaker?.sessionId == fourthParticipant.sessionId
                        )
                    )

                    videoRenderer.invoke(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = fifthParticipant,
                        style = style.copy(
                            isFocused = dominantSpeaker?.sessionId == fifthParticipant.sessionId
                        )
                    )
                }
            }
        }

        else -> {
            val firstParticipant = callParticipants[0]
            val secondParticipant = callParticipants[1]
            val thirdParticipant = callParticipants[2]
            val fourthParticipant = callParticipants[3]
            val fifthParticipant = callParticipants[4]
            val sixthParticipant = callParticipants[5]

            Row(modifier) {
                Column(modifier = Modifier.weight(1f)) {
                    videoRenderer.invoke(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = firstParticipant,
                        style = style.copy(
                            isFocused = dominantSpeaker?.sessionId == firstParticipant.sessionId
                        )
                    )

                    videoRenderer.invoke(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = secondParticipant,
                        style = style.copy(
                            isFocused = dominantSpeaker?.sessionId == secondParticipant.sessionId
                        )
                    )

                    videoRenderer.invoke(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = thirdParticipant,
                        style = style.copy(
                            isFocused = dominantSpeaker?.sessionId == thirdParticipant.sessionId
                        )
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    videoRenderer.invoke(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = fourthParticipant,
                        style = style.copy(
                            isFocused = dominantSpeaker?.sessionId == fourthParticipant.sessionId
                        )
                    )

                    videoRenderer.invoke(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = fifthParticipant,
                        style = style.copy(
                            isFocused = dominantSpeaker?.sessionId == fifthParticipant.sessionId
                        )
                    )

                    videoRenderer.invoke(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = sixthParticipant,
                        style = style.copy(
                            isFocused = dominantSpeaker?.sessionId == sixthParticipant.sessionId
                        )
                    )
                }
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
                modifier = Modifier
                    .size(
                        height = VideoTheme.dimens.floatingVideoHeight,
                        width = VideoTheme.dimens.floatingVideoWidth
                    )
                    .clip(VideoTheme.shapes.floatingParticipant)
                    .align(Alignment.TopEnd),
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
            modifier = Modifier.background(color = VideoTheme.colors.appBackground)
        ) {
            PortraitVideoRenderer(
                call = mockCall,
                dominantSpeaker = participants[0],
                callParticipants = participants.take(1),
                modifier = Modifier.fillMaxSize(),
                parentSize = IntSize(screenWidth, screenHeight)
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
            modifier = Modifier.background(color = VideoTheme.colors.appBackground)
        ) {
            PortraitVideoRenderer(
                call = mockCall,
                dominantSpeaker = mockParticipantList[0],
                callParticipants = participants.take(2),
                modifier = Modifier.fillMaxSize(),
                parentSize = IntSize(screenWidth, screenHeight)
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
            modifier = Modifier.background(color = VideoTheme.colors.appBackground)
        ) {
            PortraitVideoRenderer(
                call = mockCall,
                dominantSpeaker = participants[0],
                callParticipants = participants.take(3),
                modifier = Modifier.fillMaxSize(),
                parentSize = IntSize(screenWidth, screenHeight)
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
            modifier = Modifier.background(color = VideoTheme.colors.appBackground)
        ) {
            PortraitVideoRenderer(
                call = mockCall,
                dominantSpeaker = participants[0],
                callParticipants = participants.take(4),
                modifier = Modifier.fillMaxSize(),
                parentSize = IntSize(screenWidth, screenHeight)
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
            modifier = Modifier.background(color = VideoTheme.colors.appBackground)
        ) {
            PortraitVideoRenderer(
                call = mockCall,
                dominantSpeaker = participants[0],
                callParticipants = participants.take(5),
                modifier = Modifier.fillMaxSize(),
                parentSize = IntSize(screenWidth, screenHeight)
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
            modifier = Modifier.background(color = VideoTheme.colors.appBackground)
        ) {
            PortraitVideoRenderer(
                call = mockCall,
                dominantSpeaker = participants[0],
                callParticipants = participants.take(6),
                modifier = Modifier.fillMaxSize(),
                parentSize = IntSize(screenWidth, screenHeight)
            )
        }
    }
}
