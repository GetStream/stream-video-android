package io.getstream.video.android.compose.ui.components.base.styling

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.compose.theme.v2.VideoTheme

public class BadgeStyle(
    public val size: Dp,
    public val color: Color,
    public val textStyle: TextStyle,
) : StreamStyle

public open class BadgeStyleProvider {
    @Composable
    public fun defaultBadgeStyle(): BadgeStyle = BadgeStyle(
        color = VideoTheme.colors.alertSuccess,
        size = 16.dp,
        textStyle = TextStyle(
            fontSize = 10.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.W600,
            color = VideoTheme.colors.buttonPrimaryPressed,
        ),
    )
}

public object StreamBadgeStyles : BadgeStyleProvider()