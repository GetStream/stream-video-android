package io.getstream.video.android.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import io.getstream.video.android.compose.theme.VideoTheme

@Composable
fun StreamImageButton(
    modifier: Modifier,
    enabled: Boolean = true,
    @DrawableRes imageRes: Int,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = VideoTheme.colors.primaryAccent,
            contentColor = VideoTheme.colors.primaryAccent,
            disabledBackgroundColor = Colors.description,
            disabledContentColor = Colors.description,
        ),
        onClick = onClick,
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null
        )
    }
}