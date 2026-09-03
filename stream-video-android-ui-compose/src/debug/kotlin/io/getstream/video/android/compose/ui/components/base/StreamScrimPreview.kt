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

package io.getstream.video.android.compose.ui.components.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.theme.design.StreamTokens

@Preview
@Composable
private fun StreamScrimRootPreview() {
    VideoTheme {
        StreamScrimPreview()
    }
}

/**
 * The scrim over a menu, to show how much it dims the content behind it.
 */
@Composable
internal fun StreamScrimPreview() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.padding(StreamTokens.spacingMd)) {
            StreamListItemsPreview()
        }
        StreamScrim()
    }
}
