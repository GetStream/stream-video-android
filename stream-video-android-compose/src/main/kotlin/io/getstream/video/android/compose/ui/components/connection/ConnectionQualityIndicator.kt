package io.getstream.video.android.compose.ui.components.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import stream.video.sfu.models.ConnectionQuality

@Composable
public fun ConnectionQualityIndicator(
    connectionQuality: ConnectionQuality,
    modifier: Modifier = Modifier
) {
    val quality = connectionQuality.value

    Box(
        modifier = modifier
            .padding(8.dp)
            .background(
                shape = RoundedCornerShape(8.dp),
                color = VideoTheme.colors.appBackground
            )
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier
                .height(height = 14.dp)
                .align(Alignment.Center),
            verticalAlignment = Alignment.Bottom
        ) {
            Spacer(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(0.33f)
                    .background(
                        color = if (quality >= 1) VideoTheme.colors.primaryAccent else Color.White,
                        shape = RoundedCornerShape(16.dp)
                    )
            )

            Spacer(modifier = Modifier.width(3.dp))

            Spacer(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(fraction = 0.66f)
                    .background(
                        color = if (quality >= 2) VideoTheme.colors.primaryAccent else Color.White,
                        shape = RoundedCornerShape(16.dp)
                    )
            )

            Spacer(modifier = Modifier.width(3.dp))

            Spacer(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(fraction = 1f)
                    .background(
                        color = if (quality >= 3) VideoTheme.colors.primaryAccent else Color.White,
                        shape = RoundedCornerShape(16.dp)
                    )
            )
        }
    }
}