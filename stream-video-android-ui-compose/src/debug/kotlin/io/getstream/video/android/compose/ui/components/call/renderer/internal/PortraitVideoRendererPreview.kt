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

package io.getstream.video.android.compose.ui.components.call.renderer.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewGridCall

@Preview
@Composable
private fun PortraitParticipantsRootPreview1() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        PortraitParticipantsPreview1()
    }
}

@Preview
@Composable
private fun PortraitParticipantsRootPreview2() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        PortraitParticipantsPreview2()
    }
}

@Preview
@Composable
private fun PortraitParticipantsRootPreview3() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        PortraitParticipantsPreview3()
    }
}

@Preview
@Composable
private fun PortraitParticipantsRootPreview4() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        PortraitParticipantsPreview4()
    }
}

@Preview
@Composable
private fun PortraitParticipantsRootPreview5() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        PortraitParticipantsPreview5()
    }
}

@Preview
@Composable
private fun PortraitParticipantsRootPreview6() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        PortraitParticipantsPreview6()
    }
}

@Preview
@Composable
private fun PortraitParticipantsRootPreview7() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        PortraitParticipantsPreview7()
    }
}

@Composable
internal fun PortraitParticipantsPreview1() {
    PortraitParticipants(participantCount = 1)
}

@Composable
internal fun PortraitParticipantsPreview2() {
    PortraitParticipants(participantCount = 2)
}

@Composable
internal fun PortraitParticipantsPreview3() {
    PortraitParticipants(participantCount = 3)
}

@Composable
internal fun PortraitParticipantsPreview4() {
    PortraitParticipants(participantCount = 4)
}

@Composable
internal fun PortraitParticipantsPreview5() {
    PortraitParticipants(participantCount = 5)
}

@Composable
internal fun PortraitParticipantsPreview6() {
    PortraitParticipants(participantCount = 6)
}

@Composable
internal fun PortraitParticipantsPreview7() {
    PortraitParticipants(participantCount = 7)
}

@Composable
private fun PortraitParticipants(participantCount: Int) {
    val gridCall = remember(participantCount) { previewGridCall(participantCount) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    Box(
        modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
    ) {
        PortraitVideoRenderer(
            call = gridCall.call,
            dominantSpeaker = gridCall.participants[0],
            callParticipants = gridCall.participants,
            modifier = Modifier.fillMaxSize(),
            parentSize = IntSize(screenWidth, screenHeight),
        )
    }
}
