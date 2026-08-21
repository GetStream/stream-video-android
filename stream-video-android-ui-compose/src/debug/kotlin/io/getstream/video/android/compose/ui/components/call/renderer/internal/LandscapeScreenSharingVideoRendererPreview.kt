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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.model.ScreenSharingSession
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.mock.previewParticipant
import io.getstream.video.android.mock.previewParticipantsList

@Preview(
    widthDp = 1440,
    heightDp = 720,
)
@Composable
private fun LandscapeScreenSharingContentRootPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        LandscapeScreenSharingContentPreview()
    }
}

@Preview(
    widthDp = 1440,
    heightDp = 720,
)
@Composable
private fun LandscapeScreenSharingMyContentRootPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        LandscapeScreenSharingMyContentPreview()
    }
}

@Composable
internal fun LandscapeScreenSharingContentPreview() {
    LandscapeScreenSharingVideoRenderer(
        call = previewCall,
        session = ScreenSharingSession(participant = previewParticipantsList[0]),
        participants = previewParticipantsList,
        dominantSpeaker = previewParticipant,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
internal fun LandscapeScreenSharingMyContentPreview() {
    LandscapeScreenSharingVideoRenderer(
        call = previewCall,
        session = ScreenSharingSession(participant = previewParticipantsList[0]),
        participants = previewParticipantsList,
        dominantSpeaker = previewParticipant,
        modifier = Modifier.fillMaxSize(),
    )
}
