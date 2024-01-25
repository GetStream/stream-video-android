package io.getstream.video.android.compose.ui.components.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.getstream.video.android.compose.theme.base.VideoTheme
import io.getstream.video.android.compose.ui.components.base.styling.DefaultStreamButtonStyles
import io.getstream.video.android.compose.ui.components.base.styling.DialogStyle
import io.getstream.video.android.compose.ui.components.base.styling.StreamButtonStyle
import io.getstream.video.android.compose.ui.components.base.styling.StreamDialogStyles
import io.getstream.video.android.compose.ui.components.base.styling.StyleSize


@Composable
public fun StreamDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    style: DialogStyle,
    dialogProperties: DialogProperties = DialogProperties(),
    content: @Composable BoxScope.() -> Unit
): Unit = Dialog(onDismissRequest = onDismiss, dialogProperties) {
    Box(
        modifier = modifier
            .clip(style.shape)
            .padding(style.contentPaddings),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
public fun StreamDialogPositiveNegative(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    dialogProperties: DialogProperties = DialogProperties(),
    style: DialogStyle,
    title: String? = null,
    icon: ImageVector? = null,
    contentText: String? = null,
    positiveButton: Triple<String, StreamButtonStyle, () -> Unit>,
    negativeButton: Triple<String, StreamButtonStyle, () -> Unit>? = null,
    content: (@Composable () -> Unit)? = null
): Unit = StreamDialog(
    modifier = modifier,
    style = style,
    onDismiss = onDismiss,
    dialogProperties = dialogProperties
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            icon?.let {
                val iconStyle = style.iconStyle
                Icon(
                    imageVector = icon,
                    contentDescription = "",
                    tint = iconStyle.color,
                )
                Spacer(modifier = Modifier.width(iconStyle.padding.calculateTopPadding()))
            }
            title?.let {
                Text(text = it, style = style.titleStyle)
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
        contentText?.let {
            Text(text = it, style = style.contentTextStyle)
            Spacer(modifier = Modifier.size(32.dp))
        }
        content?.let {
            content()
            Spacer(modifier = Modifier.size(32.dp))
        }
        val horizontalArr = if (negativeButton == null) {
            Arrangement.End
        } else {
            Arrangement.SpaceBetween
        }
        Spacer(modifier = Modifier.size(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = horizontalArr) {
            negativeButton?.let {
                StreamButton(
                    modifier = Modifier.weight(1f),
                    text = it.first,
                    style = it.second,
                    onClick = it.third
                )
                Spacer(modifier = Modifier.size(16.dp))
            }
            StreamButton(
                modifier = Modifier.weight(1f),
                text = positiveButton.first,
                style = positiveButton.second,
                onClick = positiveButton.third
            )
        }
    }
}

@Preview
@Composable
private fun StreamDialogPreview() {
    VideoTheme {
        StreamDialogPositiveNegative(
            icon = Icons.Default.StopCircle,
            title = "This Call is Being Recorded",
            style = StreamDialogStyles.defaultDialogStyle(),
            contentText = "By staying in the call you’re consenting to being recorded.",
            positiveButton = Triple(
                "Continue",
                DefaultStreamButtonStyles.secondaryButtonStyle(StyleSize.S)
            ) {
                // Do nothing
            },
            negativeButton = Triple(
                "Leave call",
                DefaultStreamButtonStyles.tetriaryButtonStyle(StyleSize.S)
            ) {
                // Do nothing
            },
        )
    }
}