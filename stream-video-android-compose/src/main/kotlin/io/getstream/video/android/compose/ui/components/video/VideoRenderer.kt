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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.VideoTrack
import io.getstream.video.android.ui.TextureViewRenderer

/**
 * Renders a single video track based on the call state.
 *
 * @param call The call state that contains all the tracks and participants.
 * @param videoTrack The track containing the video stream for a given participant.
 * @param modifier Modifier for styling.
 * @param onRender Handler when the view is rendered.
 */
@Composable
public fun VideoRenderer(
    call: Call,
    videoTrack: VideoTrack,
    modifier: Modifier = Modifier,
    onRender: (View) -> Unit = {},
) {
    val trackState: MutableState<VideoTrack?> = remember { mutableStateOf(null) }
    var view: TextureViewRenderer? by remember { mutableStateOf(null) }

    DisposableEffect(call, videoTrack) {
        onDispose {
            cleanTrack(view, trackState)
        }
    }

    AndroidView(
        factory = { context ->
            TextureViewRenderer(context).apply {
                call.initRenderer(this, videoTrack.streamId, onRender = onRender)
                setupVideo(trackState, videoTrack, this)

                view = this
            }
        },
        update = { v -> setupVideo(trackState, videoTrack, v) },
        modifier = modifier,
    )
}

private fun cleanTrack(
    view: TextureViewRenderer?,
    trackState: MutableState<VideoTrack?>,
) {
    view?.let { trackState.value?.video?.removeSink(it) }
    trackState.value = null
}

private fun setupVideo(
    trackState: MutableState<VideoTrack?>,
    track: VideoTrack,
    renderer: TextureViewRenderer,
) {
    if (trackState.value == track) {
        return
    }

    cleanTrack(renderer, trackState)

    trackState.value = track
    track.video.addSink(renderer)
}
