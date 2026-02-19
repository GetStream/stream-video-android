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

package io.getstream.video.android.compose.ui.components.background

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.AvatarImagePreview
import io.getstream.video.android.mock.StreamPreviewDataUtils

/**
 * Renders a call background that shows either a static image or user images based on the call state.
 *
 * @param modifier Modifier for styling.
 * @param backgroundContent The background content to render.
 * @param content The content to render on top of the background.
 */
@Composable
public fun CallBackground(
    modifier: Modifier = Modifier,
    backgroundContent: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = VideoTheme.colors.baseSheetTertiary),
    ) {
        backgroundContent?.invoke(this)
        content()
    }
}

@Preview
@Composable
private fun CallBackgroundPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallBackground {
            Box(modifier = Modifier.align(Alignment.Center)) {
                AvatarImagePreview()
            }
        }
    }
}
