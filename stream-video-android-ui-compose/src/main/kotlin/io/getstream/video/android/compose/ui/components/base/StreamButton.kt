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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.theme.design.StreamTokens

/**
 * A pill shaped button that hosts any content.
 *
 * The button reserves a 48dp touch target around its visual size, provides [LocalContentColor]
 * with the [style] content color and [LocalTextStyle] with the body emphasis typography.
 *
 * @param onClick Called when the button is clicked.
 * @param modifier The modifier applied to the touch target of the button.
 * @param enabled Whether the button accepts clicks. Disabled buttons use the disabled colors of [style].
 * @param onClickLabel The accessibility label describing the click action.
 * @param style The colors of the button. See [StreamButtonStyleDefaults].
 * @param size The visual size of the button.
 * @param content The content of the button, centered inside the visual bounds.
 */
@Composable
public fun StreamButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    style: StreamButtonStyle = StreamButtonStyleDefaults.primarySolid,
    size: StreamButtonSize = StreamButtonSize.Medium,
    content: @Composable () -> Unit,
) {
    val containerColor = style.containerColor(enabled)
    val borderColor = style.borderColor(enabled)
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(minWidth = size.minimumSize, minHeight = size.minimumSize)
            .clip(CircleShape)
            .then(if (containerColor != null) Modifier.background(containerColor) else Modifier)
            .then(
                if (borderColor != null) {
                    Modifier.border(StreamTokens.strokeW100, borderColor, CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(
                onClick = onClick,
                onClickLabel = onClickLabel,
                enabled = enabled,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
            ),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides style.contentColor(enabled),
            LocalTextStyle provides VideoTheme.typography.bodyEmphasis,
            content = content,
        )
    }
}

/**
 * A [StreamButton] with a single icon and no label.
 *
 * @param onClick Called when the button is clicked.
 * @param icon The icon painted at the icon size of [size], tinted with the content color of [style].
 * @param contentDescription The accessibility description of the icon, or null when a parent describes it.
 * @param modifier The modifier applied to the touch target of the button.
 * @param enabled Whether the button accepts clicks.
 * @param style The colors of the button. See [StreamButtonStyleDefaults].
 * @param size The visual size of the button.
 */
@Composable
public fun StreamIconButton(
    onClick: () -> Unit,
    icon: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: StreamButtonStyle = StreamButtonStyleDefaults.secondarySolid,
    size: StreamButtonSize = StreamButtonSize.Medium,
) {
    StreamButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        style = style,
        size = size,
    ) {
        Icon(
            painter = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(size.iconSize),
        )
    }
}

/**
 * A [StreamButton] with a label and optional leading and trailing icons.
 *
 * @param onClick Called when the button is clicked.
 * @param text The label of the button, kept on a single line.
 * @param modifier The modifier applied to the touch target of the button.
 * @param enabled Whether the button accepts clicks.
 * @param style The colors of the button. See [StreamButtonStyleDefaults].
 * @param size The visual size of the button.
 * @param leadingIcon The icon shown before the label, or null for none.
 * @param trailingIcon The icon shown after the label, or null for none.
 */
@Composable
public fun StreamTextButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: StreamButtonStyle = StreamButtonStyleDefaults.primarySolid,
    size: StreamButtonSize = StreamButtonSize.Medium,
    leadingIcon: Painter? = null,
    trailingIcon: Painter? = null,
) {
    StreamButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        style = style,
        size = size,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = size.labelPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                StreamTokens.spacingXs,
                Alignment.CenterHorizontally,
            ),
        ) {
            leadingIcon?.let {
                Icon(
                    painter = it,
                    contentDescription = null,
                    modifier = Modifier.size(size.iconSize),
                )
            }
            Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            trailingIcon?.let {
                Icon(
                    painter = it,
                    contentDescription = null,
                    modifier = Modifier.size(size.iconSize),
                )
            }
        }
    }
}
