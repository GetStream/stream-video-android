package io.getstream.video.android.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import io.getstream.video.android.ui.common.*

@Immutable
public data class StreamIcons(
    public val joinCall: ImageVector
) {
    public companion object {
        @Composable
        public fun defaultIcons() : StreamIcons = StreamIcons(
            joinCall = ImageVector.vectorResource(id = R.drawable.stream_video_ic_join_call)
        )
    }
}