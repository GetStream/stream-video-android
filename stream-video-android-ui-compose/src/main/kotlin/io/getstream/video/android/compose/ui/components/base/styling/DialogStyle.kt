package io.getstream.video.android.compose.ui.components.base.styling

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.compose.theme.base.StreamDimens
import io.getstream.video.android.compose.theme.base.VideoTheme


public data class DialogStyle(
    public val shape: Shape,
    public val backgroundColor: Color,
    public val titleStyle: TextStyle,
    public val contentTextStyle: TextStyle,
    public val iconStyle: IconStyle,
    public val contentPaddings: PaddingValues,
) : StreamStyle

public open class DialogStyleProvider {
    @Composable
    public fun defaultDialogStyle(): DialogStyle = DialogStyle(
        shape = VideoTheme.shapes.dialog,
        backgroundColor = VideoTheme.colors.baseSheetSecondary,
        titleStyle = StreamTextStyles.defaultTitle(StyleSize.S).default.platform,
        contentTextStyle = StreamTextStyles.defaultBody(StyleSize.S).default.platform,
        iconStyle = StreamIconStyles.defaultIconStyle().default,
        contentPaddings = PaddingValues(VideoTheme.dimens.spacingL),
    )
}

public object StreamDialogStyles : DialogStyleProvider()