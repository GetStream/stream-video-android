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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import io.getstream.video.android.compose.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.theme.design.StreamTokens

/**
 * Overlays a count badge on the top end corner of [content].
 *
 * @param modifier The modifier applied to the box that holds the content and the badge.
 * @param text The badge label, usually a count, or null for a badge without a label.
 * @param showWithoutValue Whether an empty badge is shown when [text] is null.
 * @param content The content decorated by the badge. It receives the modifier to apply to its root.
 */
@Composable
public fun StreamBadgeBox(
    modifier: Modifier = Modifier,
    text: String? = null,
    showWithoutValue: Boolean = true,
    content: @Composable BoxScope.(Modifier) -> Unit,
) {
    Box(modifier = modifier) {
        content(Modifier.testTag("Stream_ParticipantsMenuIcon"))
        if (text != null || showWithoutValue) {
            Badge(text = text, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

/**
 * Draws a small error badge on the top end corner of the node, for a control whose device is
 * unavailable, for example a microphone without the recording permission. A draw modifier so the
 * badge sits on the 48dp hit target without wrapping the control in another layout node.
 */
@Composable
internal fun Modifier.errorBadge(): Modifier {
    val badgeColor = VideoTheme.colors.badgeBgError
    val borderColor = VideoTheme.colors.badgeBorder
    val iconTint = ColorFilter.tint(VideoTheme.colors.badgeTextOnAccent)
    val icon = painterResource(R.drawable.stream_design_ic_exclamation_mark_fill)
    return drawWithContent {
        drawContent()
        val badgeSize = StreamTokens.size20.toPx()
        val iconSize = StreamTokens.iconSizeSm.toPx()
        val stroke = StreamTokens.strokeW200.toPx()
        val left = if (layoutDirection == LayoutDirection.Rtl) 0f else size.width - badgeSize
        val center = Offset(left + badgeSize / 2, badgeSize / 2)
        drawCircle(color = badgeColor, radius = badgeSize / 2, center = center)
        drawCircle(
            color = borderColor,
            radius = badgeSize / 2 - stroke / 2,
            center = center,
            style = Stroke(width = stroke),
        )
        translate(left = center.x - iconSize / 2, top = center.y - iconSize / 2) {
            with(icon) { draw(size = Size(iconSize, iconSize), colorFilter = iconTint) }
        }
    }
}

@Composable
private fun Badge(text: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(StreamTokens.size16)
            .defaultMinSize(minWidth = StreamTokens.size16)
            .background(VideoTheme.colors.badgeBgPrimary, CircleShape)
            .padding(horizontal = StreamTokens.spacingXxs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            modifier = Modifier.testTag("Stream_ParticipantsCountBadge"),
            text = text.orEmpty(),
            style = VideoTheme.typography.metadataEmphasis,
            color = VideoTheme.colors.badgeTextOnAccent,
            maxLines = 1,
        )
    }
}
