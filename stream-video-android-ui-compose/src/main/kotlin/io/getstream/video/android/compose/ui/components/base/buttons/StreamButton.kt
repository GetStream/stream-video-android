package io.getstream.video.android.compose.ui.components.base.buttons

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.v2.VideoTheme


@Composable
public fun GenericStreamButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    style: StreamButtonStyle = DefaultStreamButtonStyles.primaryButtonStyle(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
): Unit =
    Button(
        interactionSource = interactionSource,
        enabled = enabled,
        modifier = style.modifier,
        elevation = style.elevation,
        shape = style.shape,
        colors = style.colors,
        border = style.border,
        contentPadding = style.contentPadding,
        onClick = onClick,
        content = content
    )

@Composable
public fun StreamButton(
    icon: ImageVector? = null,
    text: String,
    onClick: () -> Unit = { },
    enabled: Boolean = true,
    style: StreamButtonStyle,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
): Unit = GenericStreamButton(
    style = style,
    onClick = onClick,
    interactionSource = interactionSource,
    enabled = enabled
) {
    icon?.let {
        Icon(
            imageVector = icon,
            contentDescription = "",
            tint = if (enabled) style.iconStyle else style.iconStyle.disabledColor
        )
        Spacer(modifier = Modifier.width(style.iconStyle.padding.calculateTopPadding()))
    }
    Text(
        style = style.textStyle,
        text = text,
        color = androidx.compose.ui.graphics.Color.White,
    )
}

@Preview
@Composable
private fun StreamButtonPreview() {
    VideoTheme {
        Column {
            // Default
            StreamButton(text = "Primary Button", style = DefaultStreamButtonStyles.primaryButtonStyle())
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(text = "Secondary Button", style = DefaultStreamButtonStyles.secondaryButtonStyle())
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(text = "Tetriary Button", style = DefaultStreamButtonStyles.tetriaryButtonStyle())
            Spacer(modifier = Modifier.height(48.dp))

            // Disabled
            StreamButton(enabled = false, text = "Primary Button", style = DefaultStreamButtonStyles.primaryButtonStyle())
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(enabled = false, text = "Secondary Button", style = DefaultStreamButtonStyles.secondaryButtonStyle())
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(enabled = false, text = "Tetriary Button", style = DefaultStreamButtonStyles.tetriaryButtonStyle())
            Spacer(modifier = Modifier.height(48.dp))

            // With icon
            StreamButton(icon = Icons.Filled.AccessAlarm, text = "Primary Button", style = DefaultStreamButtonStyles.primaryButtonStyle())
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(icon = Icons.Filled.AccessAlarm, text = "Secondary Button", style = DefaultStreamButtonStyles.secondaryButtonStyle())
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(icon = Icons.Filled.AccessAlarm, text = "Tetriary Button", style = DefaultStreamButtonStyles.tetriaryButtonStyle())
        }
    }
}