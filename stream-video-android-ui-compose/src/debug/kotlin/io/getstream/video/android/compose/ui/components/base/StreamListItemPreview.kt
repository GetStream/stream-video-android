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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.theme.design.StreamTokens

@Preview
@Composable
private fun StreamListItemsRootPreview() {
    VideoTheme {
        StreamListItemsPreview()
    }
}

/**
 * A menu made of headings, list items with leading icons, trailing chevrons and separators.
 */
@Composable
internal fun StreamListItemsPreview() {
    Column {
        StreamMenuHeading(text = "Layout")
        StreamListItem(title = "Grid", onClick = {
        }, leadingContent = { MenuIcon(R.drawable.stream_design_ic_grid_fill) })
        StreamListItem(
            title = "Spotlight",
            onClick = {},
            leadingContent = { MenuIcon(R.drawable.stream_design_ic_speaker_top_fill) },
            trailingContent = { MenuIcon(R.drawable.stream_design_ic_chevron_right) },
        )
        StreamMenuSeparator()
        StreamListItem(
            title = "Record Library",
            subtitle = "3 recordings",
            onClick = {},
            leadingContent = { MenuIcon(R.drawable.stream_design_ic_record_library_fill) },
        )
        StreamListItem(title = "Stats", onClick = {
        }, enabled = false, leadingContent = {
            MenuIcon(
                R.drawable.stream_design_ic_stats_fill,
            )
        })
        StreamListItem(title = "A very long title that does not fit on a single line of the sheet")
    }
}

@Composable
private fun MenuIcon(id: Int) {
    Icon(
        painter = painterResource(id),
        contentDescription = null,
        modifier = Modifier.size(StreamTokens.iconSizeMd),
    )
}
