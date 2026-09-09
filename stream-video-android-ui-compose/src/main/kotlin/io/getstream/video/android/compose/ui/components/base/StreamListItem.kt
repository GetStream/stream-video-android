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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.theme.design.StreamTokens

/**
 * A row of a menu or a sheet with a title, an optional subtitle, and leading and trailing slots.
 *
 * The slots receive [LocalContentColor] matching the title color, so plain icons inherit it.
 *
 * @param title The main text, kept on a single line.
 * @param modifier The modifier applied to the row.
 * @param onClick Called when the row is tapped, or null for a static row.
 * @param enabled Whether the row accepts taps. Disabled rows use the disabled text color.
 * @param subtitle The secondary text below the title, or null for none.
 * @param leadingContent The content before the texts, usually a 20dp icon, or null for none.
 * @param trailingContent The content after the texts, such as a chevron or a switch, or null for none.
 */
@Composable
internal fun StreamListItem(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    subtitle: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val colors = VideoTheme.colors
    val titleColor = if (enabled) colors.textPrimary else colors.textDisabled
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = StreamTokens.spacingXxs)
            .defaultMinSize(minHeight = StreamTokens.size48)
            .clip(RoundedCornerShape(StreamTokens.radiusMd))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        onClick = onClick,
                        enabled = enabled,
                        role = Role.Button,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = StreamTokens.spacingSm, vertical = StreamTokens.spacingXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(StreamTokens.spacingSm),
    ) {
        CompositionLocalProvider(LocalContentColor provides titleColor) {
            leadingContent?.invoke()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(StreamTokens.spacingXxxs),
            ) {
                Text(
                    text = title,
                    style = VideoTheme.typography.bodyDefault,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = VideoTheme.typography.captionDefault,
                        color = if (enabled) colors.textSecondary else colors.textDisabled,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            trailingContent?.invoke()
        }
    }
}
