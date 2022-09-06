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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.viewinterop.AndroidView
import io.getstream.video.android.model.VideoRoom
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
public fun VideoRenderer(
    videoRoom: VideoRoom,
    videoTrack: VideoTrack,
    modifier: Modifier = Modifier
) {

    val videoSinkVisibility = remember(videoTrack) { ComposeVisibility() }
    var boundVideoTrack by remember { mutableStateOf<VideoTrack?>(null) }
    var view: SurfaceViewRenderer? by remember { mutableStateOf(null) }

    fun cleanupVideoTrack() {
        view?.let { boundVideoTrack?.removeSink(it) }
        boundVideoTrack = null
    }

    fun setupVideoIfNeeded(videoTrack: VideoTrack, view: SurfaceViewRenderer) {
        if (boundVideoTrack == videoTrack) {
            return
        }

        cleanupVideoTrack()

        boundVideoTrack = videoTrack
        // TODO - figure out remote vs local
        videoTrack.addSink(view)
//        if (videoTrack is RemoteVideoTrack) {
//            videoTrack.addSink(view, videoSinkVisibility)
//        } else {
//            videoTrack.addRenderer(view)
//        }
    }

    DisposableEffect(videoTrack) {
        onDispose {
            videoSinkVisibility.onDispose()
            cleanupVideoTrack()
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
                videoRoom.init(this)
                setupVideoIfNeeded(videoTrack, this)

                view = this
            }
        },
        update = { v -> setupVideoIfNeeded(videoTrack, v) },
        modifier = modifier
            .onGloballyPositioned { videoSinkVisibility.onGloballyPositioned(it) },
    )
}
