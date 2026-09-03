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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.theme.design.StreamTokens

/**
 * The heading of a group of [StreamListItem]s.
 *
 * @param text The heading, kept on a single line.
 * @param modifier The modifier applied to the heading row.
 */
@Composable
internal fun StreamMenuHeading(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = StreamTokens.spacingXxs)
            .defaultMinSize(minHeight = StreamTokens.size40)
            .padding(StreamTokens.spacingSm),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = VideoTheme.typography.bodyEmphasis,
            color = VideoTheme.colors.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * A thin line with vertical breathing room that separates groups of [StreamListItem]s.
 *
 * @param modifier The modifier applied to the separator.
 */
@Composable
internal fun StreamMenuSeparator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(StreamTokens.size8),
        contentAlignment = Alignment.Center,
    ) {
        Divider(
            modifier = Modifier.padding(horizontal = StreamTokens.spacingMd),
            color = VideoTheme.colors.borderCoreSubtle,
            thickness = StreamTokens.strokeW100,
        )
    }
}
