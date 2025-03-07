/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.PhoneMissed
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.styling.ButtonStyles
import io.getstream.video.android.compose.ui.components.base.styling.StreamBadgeStyles
import io.getstream.video.android.compose.ui.components.base.styling.StreamButtonStyle
import io.getstream.video.android.compose.ui.components.base.styling.StreamFixedSizeButtonStyle
import io.getstream.video.android.compose.ui.components.base.styling.StyleSize
import io.getstream.video.android.compose.ui.components.base.styling.styleState

@Composable
public fun GenericStreamButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    style: StreamButtonStyle = ButtonStyles.primaryButtonStyle(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit,
): Unit = Button(
    interactionSource = interactionSource,
    enabled = enabled,
    modifier = modifier,
    elevation = style.elevation,
    shape = style.shape,
    colors = style.colors,
    border = style.border,
    contentPadding = style.contentPadding,
    onClick = onClick,
    content = content,
)

@Composable
public fun StreamButton(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    text: String,
    enabled: Boolean = true,
    showProgress: Boolean = false,
    style: StreamButtonStyle = VideoTheme.styles.buttonStyles.primaryButtonStyle(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit = { },
): Unit = GenericStreamButton(
    modifier = modifier,
    style = style,
    onClick = onClick,
    interactionSource = interactionSource,
    enabled = enabled,
) {
    val state = styleState(interactionSource, enabled)
    icon?.let {
        val iconStyle = style.iconStyle.of(state = state).value
        Icon(
            imageVector = icon,
            contentDescription = "",
            tint = iconStyle.color,
        )
        Spacer(modifier = Modifier.width(iconStyle.padding.calculateTopPadding()))
    }
    val textStyle = style.textStyle.of(state = state)
    Text(
        style = textStyle.value.platform,
        text = text,
    )
    if (showProgress) {
        CircularProgressIndicator(
            color = textStyle.value.platform.color,
            modifier = Modifier.height(VideoTheme.dimens.genericS),
        )
    }
}

@Composable
public fun StreamIconButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    style: StreamFixedSizeButtonStyle,
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    showProgress: Boolean = false,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val state = styleState(interactionSource = interactionSource, enabled = enabled)
    val iconStyle = style.iconStyle.of(state = state).value

    Button(
        interactionSource = interactionSource,
        enabled = enabled,
        modifier = modifier
            .requiredWidth(style.width)
            .requiredHeight(style.height)
            .aspectRatio(style.height / style.width),
        elevation = style.elevation,
        shape = style.shape,
        colors = style.colors,
        border = style.border,
        contentPadding = iconStyle.padding,
        onClick = onClick,
    ) {
        if (showProgress) {
            CircularProgressIndicator(
                color = iconStyle.color,
            )
        } else {
            Icon(
                tint = iconStyle.color,
                imageVector = icon,
                contentDescription = icon.name,
            )
        }
    }
}

@Composable
public fun StreamDrawableButton(
    modifier: Modifier = Modifier,
    @DrawableRes drawable: Int,
    style: StreamFixedSizeButtonStyle,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit = {},
) {
    val state = styleState(interactionSource = interactionSource, enabled = enabled)
    val drawableStyle = style.drawableStyle.of(state = state).value

    Button(
        interactionSource = interactionSource,
        enabled = enabled,
        modifier = modifier
            .requiredWidth(style.width)
            .requiredHeight(style.height)
            .aspectRatio(style.height / style.width),
        elevation = style.elevation,
        shape = style.shape,
        colors = style.colors,
        border = style.border,
        contentPadding = drawableStyle.padding,
        onClick = onClick,
    ) {
        Image(
            painterResource(id = drawable),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = drawableStyle.scale,
            alpha = drawableStyle.alpha,
            colorFilter = drawableStyle.colorFilter,
        )
    }
}

@Composable
public fun GenericToggleButton(
    modifier: Modifier = Modifier,
    toggleState: State<ToggleableState> = rememberUpdatedState(newValue = ToggleableState(false)),
    onClick: (ToggleableState) -> Unit = {},
    onContent: @Composable BoxScope.(onClick: (ToggleableState) -> Unit) -> Unit,
    offContent: @Composable BoxScope.(onClick: (ToggleableState) -> Unit) -> Unit,
): Unit = Box(modifier = modifier) {
    if (toggleState.value == ToggleableState.On) {
        onContent(onClick)
    } else {
        offContent(onClick)
    }
}

@Composable
public fun StreamIconToggleButton(
    modifier: Modifier = Modifier,
    toggleState: State<ToggleableState> = rememberUpdatedState(newValue = ToggleableState(false)),
    onIcon: ImageVector,
    offIcon: ImageVector = onIcon,
    onStyle: StreamFixedSizeButtonStyle,
    showProgress: Boolean = false,
    offStyle: StreamFixedSizeButtonStyle = onStyle,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: (ToggleableState) -> Unit = {},
): Unit = GenericToggleButton(
    modifier = modifier,
    toggleState = toggleState,
    onClick = onClick,
    onContent = {
        StreamIconButton(
            showProgress = showProgress,
            interactionSource = interactionSource,
            enabled = enabled,
            icon = onIcon,
            style = onStyle,
            onClick = {
                it(toggleState.value)
            },
        )
    },
    offContent = {
        StreamIconButton(
            showProgress = showProgress,
            interactionSource = interactionSource,
            enabled = enabled,
            icon = offIcon,
            style = offStyle,
            onClick = {
                it(toggleState.value)
            },
        )
    },
)

@Composable
public fun StreamToggleButton(
    modifier: Modifier = Modifier,
    toggleState: State<ToggleableState> = rememberUpdatedState(newValue = ToggleableState(false)),
    onText: String,
    offText: String,
    onIcon: ImageVector? = null,
    offIcon: ImageVector? = onIcon,
    onStyle: StreamButtonStyle,
    offStyle: StreamButtonStyle = onStyle,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: (ToggleableState) -> Unit = {},
): Unit = GenericToggleButton(
    modifier = modifier,
    toggleState = toggleState,
    onClick = onClick,
    onContent = {
        GenericStreamButton(
            modifier = Modifier.align(Alignment.CenterStart),
            interactionSource = interactionSource,
            enabled = enabled,
            style = onStyle,
            onClick = {
                it(toggleState.value)
            },
        ) {
            val state = styleState(interactionSource, enabled)
            onIcon?.let {
                val iconStyle = onStyle.iconStyle.of(state = state).value
                Icon(
                    imageVector = it,
                    contentDescription = "",
                    tint = iconStyle.color,
                )
                Spacer(modifier = Modifier.width(iconStyle.padding.calculateTopPadding()))
            }
            val textStyle = onStyle.textStyle.of(state = state)
            Text(
                modifier = Modifier.fillMaxWidth(),
                style = textStyle.value.platform,
                text = onText,
            )
        }
    },
    offContent = {
        GenericStreamButton(
            modifier = Modifier.align(Alignment.CenterStart),
            interactionSource = interactionSource,
            enabled = enabled,
            style = offStyle,
            onClick = {
                it(toggleState.value)
            },
        ) {
            val state = styleState(interactionSource, enabled)
            offIcon?.let {
                val iconStyle = offStyle.iconStyle.of(state = state).value
                Icon(
                    imageVector = it,
                    contentDescription = "",
                    tint = iconStyle.color,
                )
                Spacer(modifier = Modifier.width(iconStyle.padding.calculateTopPadding()))
            }
            val textStyle = offStyle.textStyle.of(state = state)
            Text(
                modifier = Modifier.fillMaxWidth(),
                style = textStyle.value.platform,
                text = offText,
            )
        }
    },
)

@Composable
public fun StreamDrawableToggleButton(
    modifier: Modifier = Modifier,
    toggleState: State<ToggleableState> = rememberUpdatedState(newValue = ToggleableState(false)),
    @DrawableRes onDrawable: Int,
    @DrawableRes offDrawable: Int = onDrawable,
    onStyle: StreamFixedSizeButtonStyle,
    offStyle: StreamFixedSizeButtonStyle = onStyle,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: (ToggleableState) -> Unit = {},
) {
    GenericToggleButton(
        modifier = modifier,
        toggleState = toggleState,
        onClick = onClick,
        onContent = {
            StreamDrawableButton(
                drawable = onDrawable,
                style = onStyle,
                enabled = enabled,
                interactionSource = interactionSource,
                onClick = { it(toggleState.value) },
            )
        },
        offContent = {
            StreamDrawableButton(
                drawable = offDrawable,
                style = offStyle,
                enabled = enabled,
                interactionSource = interactionSource,
                onClick = { it(toggleState.value) },
            )
        },
    )
}

// Start of previews
@Preview
@Composable
private fun StreamIconButtonPreview() {
    VideoTheme {
        Column {
            Row {
                StreamIconButton(
                    icon = Icons.Default.GroupAdd,
                    style = ButtonStyles.primaryIconButtonStyle(),
                )
                Spacer(modifier = Modifier.size(16.dp))
                StreamIconButton(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    style = ButtonStyles.secondaryIconButtonStyle(),
                )
                Spacer(modifier = Modifier.size(16.dp))
                StreamIconButton(
                    icon = Icons.Default.Settings,
                    style = ButtonStyles.tertiaryIconButtonStyle(),
                )
                Spacer(modifier = Modifier.size(16.dp))
                StreamIconButton(
                    icon = Icons.AutoMirrored.Filled.PhoneMissed,
                    style = ButtonStyles.alertIconButtonStyle(),
                )
            }

            Spacer(modifier = Modifier.size(48.dp))
            Row {
                StreamIconButton(
                    enabled = false,
                    icon = Icons.Default.GroupAdd,
                    style = ButtonStyles.primaryIconButtonStyle(),
                )
                Spacer(modifier = Modifier.size(16.dp))
                StreamIconButton(
                    enabled = false,
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    style = ButtonStyles.secondaryIconButtonStyle(),
                )
                Spacer(modifier = Modifier.size(16.dp))
                StreamIconButton(
                    enabled = false,
                    icon = Icons.Default.Settings,
                    style = ButtonStyles.tertiaryIconButtonStyle(),
                )
                Spacer(modifier = Modifier.size(16.dp))
                StreamIconButton(
                    enabled = false,
                    icon = Icons.AutoMirrored.Filled.PhoneMissed,
                    style = ButtonStyles.alertIconButtonStyle(),
                )
            }

            Spacer(modifier = Modifier.size(48.dp))
            Row {
                Spacer(modifier = Modifier.size(16.dp))
                StreamIconButton(
                    icon = Icons.AutoMirrored.Filled.PhoneMissed,
                    style = ButtonStyles.alertIconButtonStyle(size = StyleSize.L),
                )
                Spacer(modifier = Modifier.size(16.dp))
                StreamIconButton(
                    icon = Icons.AutoMirrored.Filled.PhoneMissed,
                    style = ButtonStyles.alertIconButtonStyle(size = StyleSize.M),
                )
                Spacer(modifier = Modifier.size(16.dp))
                StreamIconButton(
                    icon = Icons.AutoMirrored.Filled.PhoneMissed,
                    style = ButtonStyles.alertIconButtonStyle(size = StyleSize.S),
                )
            }
        }
    }
}

@Preview
@Composable
private fun StreamDrawableButtonPreview() {
    VideoTheme {
        Column {
            Row {
                StreamDrawableButton(
                    drawable = R.drawable.example,
                    style = ButtonStyles.primaryDrawableButtonStyle(),
                )
                Spacer(modifier = Modifier.size(16.dp))
                StreamDrawableButton(
                    drawable = R.drawable.example,
                    style = ButtonStyles.primaryDrawableButtonStyle(),
                    enabled = false,
                )
            }
        }
    }
}

@Preview
@Composable
private fun StreamButtonPreview() {
    VideoTheme {
        Column {
            // Default
            StreamButton(
                text = "Primary Button",
                style = ButtonStyles.primaryButtonStyle(),
            )
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                text = "Secondary Button",
                style = ButtonStyles.secondaryButtonStyle(),
            )
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                text = "Tertiary Button",
                style = ButtonStyles.tertiaryButtonStyle(),
            )
            Spacer(modifier = Modifier.height(48.dp))
            StreamButton(
                text = "Alert Button",
                style = ButtonStyles.alertButtonStyle(),
            )
            Spacer(modifier = Modifier.height(48.dp))

            // Disabled
            StreamButton(
                enabled = false,
                text = "Primary Button",
                style = ButtonStyles.primaryButtonStyle(),
            )
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                enabled = false,
                text = "Secondary Button",
                style = ButtonStyles.secondaryButtonStyle(),
            )
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                enabled = false,
                text = "Tertiary Button",
                style = ButtonStyles.tertiaryButtonStyle(),
            )
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                enabled = false,
                text = "Alert Button",
                style = ButtonStyles.alertButtonStyle(),
            )
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Preview
@Composable
private fun StreamButtonWithIconPreview() {
    VideoTheme {
        Column {
            // With icon
            StreamButton(
                icon = Icons.Filled.AccessAlarm,
                text = "Primary Button",
                style = ButtonStyles.primaryButtonStyle(),
            )
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                icon = Icons.Filled.AccessAlarm,
                text = "Secondary Button",
                style = ButtonStyles.secondaryButtonStyle(),
            )
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                icon = Icons.Filled.AccessAlarm,
                text = "Tertiary Button",
                style = ButtonStyles.tertiaryButtonStyle(),
            )
        }
    }
}

@Preview
@Composable
private fun StreamButtonSizePreview() {
    VideoTheme {
        Column {
            // Size
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                text = "Small Button",
                style = ButtonStyles.secondaryButtonStyle(size = StyleSize.S),
            )
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                text = "Medium Button",
                style = ButtonStyles.secondaryButtonStyle(size = StyleSize.M),
            )
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                text = "Large Button",
                style = ButtonStyles.secondaryButtonStyle(size = StyleSize.L),
            )
        }
    }
}

@Preview
@Composable
private fun StreamToggleIconButtonPreview() {
    VideoTheme {
        Row {
            // Size
            StreamIconToggleButton(
                onStyle = ButtonStyles.primaryIconButtonStyle(),
                offStyle = ButtonStyles.alertIconButtonStyle(),
                onIcon = Icons.Default.Videocam,
                offIcon = Icons.Default.VideocamOff,
            )
            Spacer(modifier = Modifier.width(24.dp))
            StreamIconToggleButton(
                toggleState = rememberUpdatedState(newValue = ToggleableState.On),
                onStyle = ButtonStyles.primaryIconButtonStyle(),
                offStyle = ButtonStyles.alertIconButtonStyle(),
                onIcon = Icons.Default.Videocam,
                offIcon = Icons.Default.VideocamOff,
            )

            Spacer(modifier = Modifier.width(24.dp))
            StreamBadgeBox(
                style = StreamBadgeStyles.defaultBadgeStyle().copy(
                    color = VideoTheme.colors.alertCaution,
                    textStyle = VideoTheme.typography.labelXS.copy(color = Color.Black),
                ),
                text = "!",
            ) {
                StreamIconToggleButton(
                    enabled = false,
                    toggleState = rememberUpdatedState(newValue = ToggleableState.Off),
                    onStyle = ButtonStyles.secondaryIconButtonStyle(),
                    offStyle = ButtonStyles.alertIconButtonStyle(),
                    onIcon = Icons.Default.Videocam,
                    offIcon = Icons.Default.VideocamOff,
                )
            }
        }
    }
}

@Preview
@Composable
private fun StreamDrawableToggleButtonPreview() {
    VideoTheme {
        Row {
            StreamDrawableToggleButton(
                toggleState = rememberUpdatedState(newValue = ToggleableState.On), // On
                onDrawable = R.drawable.example,
                onStyle = ButtonStyles.drawableToggleButtonStyleOn(), // On
            )
            Spacer(modifier = Modifier.width(8.dp))
            StreamDrawableToggleButton(
                toggleState = rememberUpdatedState(newValue = ToggleableState.Off), // Off
                onDrawable = R.drawable.example,
                onStyle = ButtonStyles.drawableToggleButtonStyleOn(),
                offStyle = ButtonStyles.drawableToggleButtonStyleOff(),
            )
            Spacer(modifier = Modifier.width(8.dp))
            StreamDrawableToggleButton(
                toggleState = rememberUpdatedState(newValue = ToggleableState.On),
                onDrawable = R.drawable.example,
                onStyle = ButtonStyles.drawableToggleButtonStyleOn(),
                offStyle = ButtonStyles.drawableToggleButtonStyleOff(),
                enabled = false, // Disabled
            )
        }
    }
}

@Preview
@Composable
private fun StreamToggleButtonPreview() {
    VideoTheme {
        Column {
            // Size
            StreamToggleButton(
                modifier = Modifier.fillMaxWidth(),
                toggleState = rememberUpdatedState(newValue = ToggleableState.On),
                onText = "Grid",
                offText = "Grid",
                onStyle = ButtonStyles.toggleButtonStyleOn(),
                offStyle = ButtonStyles.toggleButtonStyleOff(),
                onIcon = Icons.Filled.GridView,
                offIcon = Icons.Filled.GridView,
            )
            Spacer(modifier = Modifier.width(24.dp))
            StreamToggleButton(
                modifier = Modifier.fillMaxWidth(),
                toggleState = rememberUpdatedState(newValue = ToggleableState.Off),
                onText = "Grid (On)",
                offText = "Grid (Off)",
                onStyle = ButtonStyles.toggleButtonStyleOn(),
                offStyle = ButtonStyles.toggleButtonStyleOff(),
                onIcon = Icons.Filled.GridView,
                offIcon = Icons.Filled.GridView,
            )
        }
    }
}

@Preview
@Composable
private fun StreamProgressButtonsPreview() {
    VideoTheme {
        Column {
            StreamButton(text = "Progress", showProgress = true)
            StreamIconButton(
                icon = Icons.Default.Camera,
                style = VideoTheme.styles.buttonStyles.secondaryIconButtonStyle(),
                showProgress = true,
            )
        }
    }
}
