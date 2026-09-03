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

package io.getstream.video.android.compose.ui.components.call.controls.actions

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamButtonSize

@Preview
@Composable
private fun ToggleActionInProgressRootPreview() {
    VideoTheme {
        ToggleActionInProgressPreview()
    }
}

/**
 * A pending toggle in both states next to a plain action, in every button size.
 */
@Composable
internal fun ToggleActionInProgressPreview() {
    val icon = painterResource(R.drawable.stream_design_ic_caption_fill)
    Row {
        StreamButtonSize.entries.forEach { size ->
            ToggleAction(
                isActionActive = true,
                iconOnOff = Pair(icon, icon),
                progress = true,
                size = size,
            ) {}
            ToggleAction(
                isActionActive = false,
                iconOnOff = Pair(icon, icon),
                progress = true,
                size = size,
            ) {}
            GenericAction(icon = icon, size = size) {}
        }
    }
}
