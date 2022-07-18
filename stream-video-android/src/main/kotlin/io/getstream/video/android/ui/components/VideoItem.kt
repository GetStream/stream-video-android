package io.getstream.video.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack
import org.webrtc.SurfaceViewRenderer

@Composable
public fun VideoItem(
    room: Room,
    videoTrack: VideoTrack,
    modifier: Modifier = Modifier
) {
    val track by remember { mutableStateOf(videoTrack) }
    val room by remember { mutableStateOf(room) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                room.initVideoRenderer(this)
                track.addRenderer(this)
            }
        },
        update = { renderer ->
            renderer.release()
            room.initVideoRenderer(renderer)
            track.addRenderer(renderer)
        }
    )
}