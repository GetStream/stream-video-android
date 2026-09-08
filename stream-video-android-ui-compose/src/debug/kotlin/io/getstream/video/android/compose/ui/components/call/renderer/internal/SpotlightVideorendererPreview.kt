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

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.mock.previewParticipant
import io.getstream.video.android.mock.previewParticipantsList

@Preview
@Composable
private fun SpotlightParticipantsPreviewRoot() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        SpotlightParticipantsPreview()
    }
}

@Composable
internal fun SpotlightParticipantsPreview() {
    SpotlightVideoRenderer(
        call = previewCall,
        speaker = previewParticipant,
        participants = previewParticipantsList,
    )
}

@Preview
@Composable
private fun SpotlightTwoParticipantsPreviewRoot() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        SpotlightTwoParticipantsPreview()
    }
}

@Composable
internal fun SpotlightTwoParticipantsPreview() {
    SpotlightVideoRenderer(
        call = previewCall,
        speaker = previewParticipant,
        participants = previewParticipantsList.take(3),
    )
}

@Preview(
    device = Devices.AUTOMOTIVE_1024p,
    widthDp = 1440,
    heightDp = 720,
)
@Composable
private fun SpotlightParticipantsLandscapePreviewRoot() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        SpotlightParticipantsLandscapePreview()
    }
}

@Composable
internal fun SpotlightParticipantsLandscapePreview() {
    SpotlightVideoRenderer(
        call = previewCall,
        orientation = ORIENTATION_LANDSCAPE,
        speaker = previewParticipant,
        participants = previewParticipantsList,
    )
}

@Preview(
    device = Devices.AUTOMOTIVE_1024p,
    widthDp = 1440,
    heightDp = 720,
)
@Composable
private fun SpotlightThreeParticipantsLandscapePreviewRoot() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        SpotlightThreeParticipantsLandscapePreview()
    }
}

@Composable
internal fun SpotlightThreeParticipantsLandscapePreview() {
    SpotlightVideoRenderer(
        call = previewCall,
        orientation = ORIENTATION_LANDSCAPE,
        speaker = previewParticipant,
        participants = previewParticipantsList.take(3),
    )
}
