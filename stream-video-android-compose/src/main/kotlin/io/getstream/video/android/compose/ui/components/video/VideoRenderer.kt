/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-video-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.video.android.compose.ui.components.video

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.getstream.video.android.model.Room
import io.getstream.video.android.model.VideoTrack
import org.webrtc.SurfaceViewRenderer

@Composable
public fun VideoRenderer(
    room: Room,
    videoTrack: VideoTrack,
    modifier: Modifier = Modifier,
    onRender: (View) -> Unit = {}
) {
    val boundVideoTrack: MutableState<VideoTrack?> = remember { mutableStateOf(null) }
    var view: SurfaceViewRenderer? by remember { mutableStateOf(null) }

    DisposableEffect(room, videoTrack) {
        onDispose {
            cleanupVideoTrack(view, boundVideoTrack)
        }
    }

    DisposableEffect(currentCompositeKeyHash.toString()) {
        onDispose {
            view?.release()
        }
    }

    AndroidView(
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                this.setZOrderOnTop(false)
                this.setZOrderMediaOverlay(false)
                room.initRenderer(this, videoTrack.streamId, onRender)
                setupVideoIfNeeded(boundVideoTrack, videoTrack, this)

                view = this
            }
        },
        update = { v ->
            setupVideoIfNeeded(boundVideoTrack, videoTrack, v)
        },
        modifier = modifier,
    )
}

private fun cleanupVideoTrack(
    view: SurfaceViewRenderer?,
    boundVideoTrack: MutableState<VideoTrack?>
) {
    view?.let { boundVideoTrack.value?.video?.removeSink(it) }
    boundVideoTrack.value = null
}

private fun setupVideoIfNeeded(
    boundVideoTrack: MutableState<VideoTrack?>,
    videoTrack: VideoTrack,
    view: SurfaceViewRenderer
) {
    if (boundVideoTrack.value == videoTrack) {
        return
    }

    cleanupVideoTrack(view, boundVideoTrack)

    boundVideoTrack.value = videoTrack
    videoTrack.video.addSink(view)
}
