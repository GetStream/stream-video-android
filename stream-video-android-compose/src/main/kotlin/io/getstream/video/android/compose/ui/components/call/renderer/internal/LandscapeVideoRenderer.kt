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

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.renderer.CallSingleVideoRenderer
import io.getstream.video.android.compose.ui.components.call.renderer.LocalVideoContent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.mock.mockCall
import io.getstream.video.android.mock.mockParticipantList

/**
 * Renders call participants based on the number of people in a call, in landscape mode.
 *
 * @param call The state of the call.
 * @param primarySpeaker The primary speaker in the call.
 * @param callParticipants The list of participants in the call.
 * @param modifier Modifier for styling.
 * @param paddingValues The padding within the parent.
 * @param parentSize The size of the parent.
 * @param onRender Handler when the video content renders.
 */
@Composable
internal fun BoxScope.LandscapeVideoRenderer(
    call: Call,
    primarySpeaker: ParticipantState?,
    callParticipants: List<ParticipantState>,
    modifier: Modifier,
    paddingValues: PaddingValues,
    parentSize: IntSize,
    onRender: (View) -> Unit
) {
    val remoteParticipants by call.state.remoteParticipants.collectAsStateWithLifecycle()

    when (callParticipants.size) {
        0 -> Unit
        1 -> {
            val participant = callParticipants.first()

            CallSingleVideoRenderer(
                modifier = Modifier.fillMaxHeight(),
                call = call,
                participant = participant,
                onRender = onRender,
                isFocused = primarySpeaker?.sessionId == participant.sessionId,
                paddingValues = paddingValues
            )
        }

        2, 3 -> {
            val rowItemWeight = 1f / callParticipants.size

            Row(modifier = modifier) {
                remoteParticipants.take(callParticipants.size - 1).forEach { participant ->
                    CallSingleVideoRenderer(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(rowItemWeight),
                        call = call,
                        participant = participant,
                        onRender = onRender,
                        isFocused = primarySpeaker?.sessionId == participant.sessionId,
                        paddingValues = paddingValues
                    )
                }
            }
        }

        4 -> {
            val firstParticipant = callParticipants[0]
            val secondParticipant = callParticipants[1]
            val thirdParticipant = callParticipants[2]
            val fourthParticipant = callParticipants[3]

            Column(modifier) {
                Row(modifier = Modifier.weight(1f)) {
                    CallSingleVideoRenderer(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = firstParticipant,
                        onRender = onRender,
                        isFocused = primarySpeaker?.sessionId == firstParticipant.sessionId,
                    )

                    CallSingleVideoRenderer(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = secondParticipant,
                        onRender = onRender,
                        isFocused = primarySpeaker?.sessionId == secondParticipant.sessionId,
                    )
                }

                Row(modifier = Modifier.weight(1f)) {
                    CallSingleVideoRenderer(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = thirdParticipant,
                        onRender = onRender,
                        isFocused = primarySpeaker?.sessionId == thirdParticipant.sessionId,
                        paddingValues = paddingValues
                    )

                    CallSingleVideoRenderer(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = fourthParticipant,
                        onRender = onRender,
                        isFocused = primarySpeaker?.sessionId == fourthParticipant.sessionId,
                        paddingValues = paddingValues
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

            Column(modifier) {
                Row(modifier = Modifier.weight(1f)) {
                    CallSingleVideoRenderer(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = firstParticipant,
                        onRender = onRender,
                        isFocused = primarySpeaker?.sessionId == firstParticipant.sessionId,
                    )

                    CallSingleVideoRenderer(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = secondParticipant,
                        onRender = onRender,
                        isFocused = primarySpeaker?.sessionId == secondParticipant.sessionId,
                    )
                }

                Row(modifier = Modifier.weight(1f)) {
                    CallSingleVideoRenderer(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = thirdParticipant,
                        onRender = onRender,
                        isFocused = primarySpeaker?.sessionId == thirdParticipant.sessionId,
                        paddingValues = paddingValues
                    )

                    CallSingleVideoRenderer(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = fourthParticipant,
                        onRender = onRender,
                        isFocused = primarySpeaker?.sessionId == fourthParticipant.sessionId,
                        paddingValues = paddingValues
                    )

                    CallSingleVideoRenderer(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = fifthParticipant,
                        onRender = onRender,
                        isFocused = primarySpeaker?.sessionId == fifthParticipant.sessionId,
                        paddingValues = paddingValues
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

            Column(modifier) {
                Row(modifier = Modifier.weight(1f)) {
                    CallSingleVideoRenderer(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = firstParticipant,
                        onRender = onRender,
                        isFocused = primarySpeaker?.sessionId == firstParticipant.sessionId,
                    )

                    CallSingleVideoRenderer(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = secondParticipant,
                        onRender = onRender,
                        isFocused = primarySpeaker?.sessionId == secondParticipant.sessionId,
                    )

                    CallSingleVideoRenderer(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = thirdParticipant,
                        onRender = onRender,
                        isFocused = primarySpeaker?.sessionId == thirdParticipant.sessionId,
                        paddingValues = paddingValues
                    )
                }

                Row(modifier = Modifier.weight(1f)) {

                    CallSingleVideoRenderer(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = fourthParticipant,
                        onRender = onRender,
                        isFocused = primarySpeaker?.sessionId == fourthParticipant.sessionId,
                        paddingValues = paddingValues
                    )

                    CallSingleVideoRenderer(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = fifthParticipant,
                        onRender = onRender,
                        isFocused = primarySpeaker?.sessionId == fifthParticipant.sessionId,
                        paddingValues = paddingValues
                    )

                    CallSingleVideoRenderer(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = sixthParticipant,
                        onRender = onRender,
                        isFocused = primarySpeaker?.sessionId == sixthParticipant.sessionId,
                        paddingValues = paddingValues
                    )
                }
            }
        }
    }

    if (callParticipants.size in 2..3) {
        val currentLocal by call.state.me.collectAsStateWithLifecycle()

        if (currentLocal != null || LocalInspectionMode.current) {
            LocalVideoContent(
                call = call,
                localParticipant = if (LocalInspectionMode.current) {
                    callParticipants.first()
                } else {
                    currentLocal!!
                },
                parentBounds = parentSize,
                modifier = Modifier
                    .size(
                        height = VideoTheme.dimens.floatingVideoHeight,
                        width = VideoTheme.dimens.floatingVideoWidth
                    )
                    .clip(VideoTheme.shapes.floatingParticipant)
                    .align(Alignment.TopEnd),
                paddingValues = paddingValues
            )
        }
    }
}

@Preview(device = Devices.AUTOMOTIVE_1024p, widthDp = 1440, heightDp = 720)
@Composable
private fun LandscapeParticipantsPreview1() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = mockParticipantList

        Box(
            modifier = Modifier.background(color = VideoTheme.colors.appBackground)
        ) {
            LandscapeVideoRenderer(
                call = mockCall,
                primarySpeaker = participants[0],
                callParticipants = participants.take(1),
                modifier = Modifier.fillMaxSize(),
                paddingValues = PaddingValues(0.dp),
                parentSize = IntSize(screenWidth, screenHeight)
            ) {}
        }
    }
}

@Preview(device = Devices.AUTOMOTIVE_1024p, widthDp = 1440, heightDp = 720)
@Composable
private fun LandscapeParticipantsPreview2() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = mockParticipantList

        Box(
            modifier = Modifier.background(color = VideoTheme.colors.appBackground)
        ) {
            LandscapeVideoRenderer(
                call = mockCall,
                primarySpeaker = participants[0],
                callParticipants = participants.take(2),
                modifier = Modifier.fillMaxSize(),
                paddingValues = PaddingValues(0.dp),
                parentSize = IntSize(screenWidth, screenHeight)
            ) {}
        }
    }
}

@Preview(device = Devices.AUTOMOTIVE_1024p, widthDp = 1440, heightDp = 720)
@Composable
private fun LandscapeParticipantsPreview3() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = mockParticipantList

        Box(
            modifier = Modifier.background(color = VideoTheme.colors.appBackground)
        ) {
            LandscapeVideoRenderer(
                call = mockCall,
                primarySpeaker = participants[0],
                callParticipants = participants.take(3),
                modifier = Modifier.fillMaxSize(),
                paddingValues = PaddingValues(0.dp),
                parentSize = IntSize(screenWidth, screenHeight)
            ) {}
        }
    }
}

@Preview(device = Devices.AUTOMOTIVE_1024p, widthDp = 1440, heightDp = 720)
@Composable
private fun LandscapeParticipantsPreview4() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = mockParticipantList

        Box(
            modifier = Modifier.background(color = VideoTheme.colors.appBackground)
        ) {
            LandscapeVideoRenderer(
                call = mockCall,
                primarySpeaker = participants[0],
                callParticipants = participants.take(4),
                modifier = Modifier.fillMaxSize(),
                paddingValues = PaddingValues(0.dp),
                parentSize = IntSize(screenWidth, screenHeight)
            ) {}
        }
    }
}

@Preview(device = Devices.AUTOMOTIVE_1024p, widthDp = 1440, heightDp = 720)
@Composable
private fun LandscapeParticipantsPreview5() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = mockParticipantList

        Box(
            modifier = Modifier.background(color = VideoTheme.colors.appBackground)
        ) {
            LandscapeVideoRenderer(
                call = mockCall,
                primarySpeaker = participants[0],
                callParticipants = participants.take(5),
                modifier = Modifier.fillMaxSize(),
                paddingValues = PaddingValues(0.dp),
                parentSize = IntSize(screenWidth, screenHeight)
            ) {}
        }
    }
}

@Preview(device = Devices.AUTOMOTIVE_1024p, widthDp = 1440, heightDp = 720)
@Composable
private fun LandscapeParticipantsPreview6() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = mockParticipantList

        Box(
            modifier = Modifier.background(color = VideoTheme.colors.appBackground)
        ) {
            LandscapeVideoRenderer(
                call = mockCall,
                primarySpeaker = participants[0],
                callParticipants = participants.take(6),
                modifier = Modifier.fillMaxSize(),
                paddingValues = PaddingValues(0.dp),
                parentSize = IntSize(screenWidth, screenHeight)
            ) {}
        }
    }
}
