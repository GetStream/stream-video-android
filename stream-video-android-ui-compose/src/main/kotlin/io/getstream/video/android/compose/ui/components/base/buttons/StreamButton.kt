package io.getstream.video.android.compose.ui.components.base.buttons

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.v2.VideoTheme
import io.getstream.video.android.compose.ui.components.base.styling.StyleState


@Composable
public fun GenericStreamButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    style: StreamButtonStyle = DefaultStreamButtonStyles.primaryButtonStyle(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
): Unit =
    Button(
        interactionSource = interactionSource,
        enabled = enabled,
        modifier = modifier,
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
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    text: String,
    onClick: () -> Unit = { },
    enabled: Boolean = true,
    style: StreamButtonStyle,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
): Unit = GenericStreamButton(
    modifier = modifier,
    style = style,
    onClick = onClick,
    interactionSource = interactionSource,
    enabled = enabled
) {
    val pressed by interactionSource.collectIsPressedAsState()
    val state = if (enabled) {
        StyleState.ENABLED
    } else if (pressed) {
        StyleState.PRESSED
    } else {
        StyleState.DISABLED
    }

    icon?.let {
        val iconStyle = style.iconStyle.of(state = state).value
        Icon(
            imageVector = icon,
            contentDescription = "",
            tint = iconStyle.color
        )
        Spacer(modifier = Modifier.width(iconStyle.padding.calculateTopPadding()))
    }
    val textStyle = style.textStyle.of(state = state)
    Text(
        style = textStyle.value.platform,
        text = text
    )
}

@Preview
@Composable
private fun StreamButtonPreview() {
    VideoTheme {
        Column {
            // Default
            StreamButton(
                text = "Primary Button",
                style = DefaultStreamButtonStyles.primaryButtonStyle()
            )
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                text = "Secondary Button",
                style = DefaultStreamButtonStyles.secondaryButtonStyle()
            )
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                text = "Tetriary Button",
                style = DefaultStreamButtonStyles.tetriaryButtonStyle()
            )
            Spacer(modifier = Modifier.height(48.dp))

            // Disabled
            StreamButton(
                enabled = false,
                text = "Primary Button",
                style = DefaultStreamButtonStyles.primaryButtonStyle()
            )
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                enabled = false,
                text = "Secondary Button",
                style = DefaultStreamButtonStyles.secondaryButtonStyle()
            )
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                enabled = false,
                text = "Tetriary Button",
                style = DefaultStreamButtonStyles.tetriaryButtonStyle()
            )
            Spacer(modifier = Modifier.height(48.dp))

            // Pressed


            // With icon
            StreamButton(
                icon = Icons.Filled.AccessAlarm,
                text = "Primary Button",
                style = DefaultStreamButtonStyles.primaryButtonStyle()
            )
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                icon = Icons.Filled.AccessAlarm,
                text = "Secondary Button",
                style = DefaultStreamButtonStyles.secondaryButtonStyle()
            )
            Spacer(modifier = Modifier.height(24.dp))
            StreamButton(
                icon = Icons.Filled.AccessAlarm,
                text = "Tetriary Button",
                style = DefaultStreamButtonStyles.tetriaryButtonStyle()
            )
        }
    }
}