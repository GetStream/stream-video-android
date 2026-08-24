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

package io.getstream.video.android.compose.ui.components.call.activecall

import android.content.res.Configuration.UI_MODE_TYPE_CAR
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall

@Preview
@Composable
private fun CallContentMultipleParticipantsRootPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallContentMultipleParticipantsPreview()
    }
}

@Composable
internal fun CallContentMultipleParticipantsPreview() {
    CallContent(call = previewCall)
}

@Preview(
    widthDp = 640,
    heightDp = 360,
    uiMode = UI_MODE_TYPE_CAR,
)
@Composable
private fun CallContentPreviewLandscape() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallContent(call = previewCall)
    }
}

@Preview
@Composable
private fun CallContentDeprecatedOverloadRootPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallContentDeprecatedOverloadPreview()
    }
}

/**
 * Pins the deprecated [CallContent] overload (the one taking `enableInPictureInPicture`), which
 * must keep rendering exactly like the current overload until it is removed.
 */
@Suppress("DEPRECATION")
@Composable
internal fun CallContentDeprecatedOverloadPreview() {
    CallContent(
        call = previewCall,
        enableInPictureInPicture = false,
    )
}
