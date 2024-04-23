package io.getstream.video.android.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.R
import io.getstream.video.android.compose.theme.VideoTheme

@Composable
fun FullScreenCircleProgressBar(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VideoTheme.colors.baseSheetPrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                painter = painterResource(id = R.drawable.stream_calls_logo),
                contentDescription = null,
                contentScale = ContentScale.FillWidth
            )
            Spacer(modifier = Modifier.size(32.dp))
            CircularProgressIndicator(color = VideoTheme.colors.brandPrimary)
            Text(
                text = text, style = VideoTheme.typography.bodyL
            )
        }
    }
}

@Preview
@Composable
private fun FullScreenProgressBarPreview() {
    VideoTheme {
        FullScreenCircleProgressBar(text = "Loading...")
    }
}