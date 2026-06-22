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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.mock.previewParticipantsList

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
