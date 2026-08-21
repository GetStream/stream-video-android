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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewGridCall

@Preview(device = Devices.AUTOMOTIVE_1024p, widthDp = 1440, heightDp = 720)
@Composable
private fun LandscapeParticipantsRootPreview1() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        LandscapeParticipantsPreview1()
    }
}

@Preview(device = Devices.AUTOMOTIVE_1024p, widthDp = 1440, heightDp = 720)
@Composable
private fun LandscapeParticipantsRootPreview2() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        LandscapeParticipantsPreview2()
    }
}

@Preview(device = Devices.AUTOMOTIVE_1024p, widthDp = 1440, heightDp = 720)
@Composable
private fun LandscapeParticipantsRootPreview3() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        LandscapeParticipantsPreview3()
    }
}

@Preview(device = Devices.AUTOMOTIVE_1024p, widthDp = 1440, heightDp = 720)
@Composable
private fun LandscapeParticipantsRootPreview4() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        LandscapeParticipantsPreview4()
    }
}

@Preview(device = Devices.AUTOMOTIVE_1024p, widthDp = 1440, heightDp = 720)
@Composable
private fun LandscapeParticipantsRootPreview5() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        LandscapeParticipantsPreview5()
    }
}

@Preview(device = Devices.AUTOMOTIVE_1024p, widthDp = 1440, heightDp = 720)
@Composable
private fun LandscapeParticipantsRootPreview6() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        LandscapeParticipantsPreview6()
    }
}

@Preview(device = Devices.AUTOMOTIVE_1024p, widthDp = 1440, heightDp = 720)
@Composable
private fun LandscapeParticipantsRootPreview7() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        LandscapeParticipantsPreview7()
    }
}

@Composable
internal fun LandscapeParticipantsPreview1() {
    LandscapeParticipants(participantCount = 1)
}

@Composable
internal fun LandscapeParticipantsPreview2() {
    LandscapeParticipants(participantCount = 2)
}

@Composable
internal fun LandscapeParticipantsPreview3() {
    LandscapeParticipants(participantCount = 3)
}

@Composable
internal fun LandscapeParticipantsPreview4() {
    LandscapeParticipants(participantCount = 4)
}

@Composable
internal fun LandscapeParticipantsPreview5() {
    LandscapeParticipants(participantCount = 5)
}

@Composable
internal fun LandscapeParticipantsPreview6() {
    LandscapeParticipants(participantCount = 6)
}

@Composable
internal fun LandscapeParticipantsPreview7() {
    LandscapeParticipants(participantCount = 7)
}

@Composable
private fun LandscapeParticipants(participantCount: Int) {
    val gridCall = previewGridCall(participantCount)
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    Box(
        modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
    ) {
        LandscapeVideoRenderer(
            call = gridCall.call,
            dominantSpeaker = gridCall.participants[0],
            callParticipants = gridCall.participants,
            modifier = Modifier.fillMaxSize(),
            parentSize = IntSize(screenWidth, screenHeight),
        )
    }
}
