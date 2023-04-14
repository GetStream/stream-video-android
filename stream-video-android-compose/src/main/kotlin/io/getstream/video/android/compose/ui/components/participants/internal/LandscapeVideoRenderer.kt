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

package io.getstream.video.android.compose.ui.components.participants.internal

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.getstream.video.android.common.util.MockUtils
import io.getstream.video.android.common.util.mockCall
import io.getstream.video.android.common.util.mockParticipants
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.participants.CallSingleVideoRenderer
import io.getstream.video.android.compose.ui.components.participants.LocalVideoContent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState

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
    val remoteParticipants = callParticipants.filter { !it.isLocal }.take(3)
    val primarySpeakingUser = primarySpeaker?.initialUser

    when (callParticipants.size) {
        0 -> Unit
        1 -> {
            val participant = callParticipants.first()

            CallSingleVideoRenderer(
                modifier = Modifier.fillMaxHeight(),
                call = call,
                participant = participant,
                onRender = onRender,
                isFocused = primarySpeakingUser?.id == participant.initialUser.id,
                paddingValues = paddingValues
            )
        }

        2, 3 -> {
            val rowItemWeight = 1f / callParticipants.size

            Row(modifier = modifier) {
                remoteParticipants.forEach { participant ->
                    CallSingleVideoRenderer(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(rowItemWeight),
                        call = call,
                        participant = participant,
                        isFocused = primarySpeakingUser?.id == participant.initialUser.id,
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
            val fiveParticipant = callParticipants[4]

            Column(modifier) {
                Row(modifier = Modifier.weight(1f)) {
                    CallSingleVideoRenderer(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = firstParticipant,
                        isFocused = primarySpeakingUser?.id == firstParticipant.initialUser.id
                    )

                    CallSingleVideoRenderer(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = secondParticipant,
                        isFocused = primarySpeakingUser?.id == secondParticipant.initialUser.id
                    )
                }

                Row(modifier = Modifier.weight(1f)) {
                    CallSingleVideoRenderer(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = thirdParticipant,
                        isFocused = primarySpeakingUser?.id == thirdParticipant.initialUser.id,
                        paddingValues = paddingValues
                    )

                    CallSingleVideoRenderer(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = fourthParticipant,
                        onRender = onRender,
                        isFocused = primarySpeakingUser?.id == fourthParticipant.initialUser.id,
                        paddingValues = paddingValues
                    )

                    CallSingleVideoRenderer(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = fiveParticipant,
                        onRender = onRender,
                        isFocused = primarySpeakingUser?.id == fiveParticipant.initialUser.id,
                        paddingValues = paddingValues
                    )
                }
            }
        }

        else -> {
            val rowCount = callParticipants.size / 2
            val heightDivision = 2
            val maxGridItemCount = 6
            LazyVerticalGrid(
                modifier = modifier, columns = GridCells.Fixed(rowCount)
            ) {
                items(
                    items = callParticipants.take(maxGridItemCount),
                    key = { it.initialUser.id }
                ) { participant ->
                    CallSingleVideoRenderer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(parentSize.height.dp / heightDivision),
                        call = call,
                        participant = participant,
                        isFocused = primarySpeakingUser?.id == participant.initialUser.id
                    )
                }
            }
        }
    }

    if (callParticipants.size in 2..3) {
        LocalVideoContent(
            call = call,
            localParticipant = callParticipants.first { it.isLocal },
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

@Preview(device = Devices.AUTOMOTIVE_1024p, widthDp = 1440, heightDp = 720)
@Composable
private fun LandscapeParticipantsPreview1() {
    MockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = mockParticipants

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
    MockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = mockParticipants

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
    MockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = mockParticipants

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
    MockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = mockParticipants

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
    MockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = mockParticipants

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
    MockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val participants = mockParticipants

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
