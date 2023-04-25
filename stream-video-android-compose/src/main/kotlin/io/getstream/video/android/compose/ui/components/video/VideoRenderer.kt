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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import io.getstream.video.android.common.util.MockUtils
import io.getstream.video.android.common.util.mockCall
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.video.VideoScalingType.Companion.toCommonScalingType
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.model.TrackWrapper
import io.getstream.webrtc.android.ui.VideoTextureViewRenderer
import stream.video.sfu.models.TrackType

/**
 * Renders a single video track based on the call state.
 *
 * @param call The call state that contains all the tracks and participants.
 * @param videoTrackWrapper The track containing the video stream for a given participant.
 * @param modifier Modifier for styling.
 * @param onRender Handler when the view is rendered.
 */
@Composable
public fun VideoRenderer(
    call: Call,
    videoTrackWrapper: TrackWrapper,
    sessionId: String,
    trackType: TrackType,
    modifier: Modifier = Modifier,
    videoScalingType: VideoScalingType = VideoScalingType.SCALE_ASPECT_BALANCED,
    onRender: (View) -> Unit = {},
) {
    if (LocalInspectionMode.current) {
        Image(
            modifier = modifier
                .fillMaxSize()
                .testTag("video_renderer"),
            painter = painterResource(id = io.getstream.video.android.ui.common.R.drawable.stream_video_call_sample),
            contentScale = ContentScale.Crop,
            contentDescription = null
        )
        return
    }

    val isLocalVideo = sessionId == call.sessionId

    if (!isLocalVideo) {
        println(isLocalVideo)
    }

    val trackState: MutableState<TrackWrapper?> = remember { mutableStateOf(null) }
    var view: VideoTextureViewRenderer? by remember { mutableStateOf(null) }

    DisposableEffect(call, videoTrackWrapper) {
        onDispose {
            cleanTrack(view, trackState)
            call.setVisibility(sessionId, trackType, false)
        }
    }

    AndroidView(
        factory = { context ->
            VideoTextureViewRenderer(context).apply {
                call.initRenderer(
                    videoRenderer = this,
                    sessionId = sessionId,
                    trackType = trackType,
                    onRender = onRender
                )
                call.setVisibility(sessionId, trackType, true)
                setScalingType(scalingType = videoScalingType.toCommonScalingType())
                setupVideo(trackState, videoTrackWrapper, this)

                view = this
            }
        },
        update = { v -> setupVideo(trackState, videoTrackWrapper, v) },
        modifier = modifier.testTag("video_renderer"),
    )
}

private fun cleanTrack(
    view: VideoTextureViewRenderer?,
    trackState: MutableState<TrackWrapper?>,
) {
    view?.let { trackState.value?.video?.removeSink(it) }
    trackState.value = null
}

private fun setupVideo(
    trackState: MutableState<TrackWrapper?>,
    track: TrackWrapper,
    renderer: VideoTextureViewRenderer,
) {
    if (trackState.value == track) {
        return
    }

    cleanTrack(renderer, trackState)

    trackState.value = track
    track.video?.addSink(renderer) //cAZo0tsELD9B
}

@Preview
@Composable
private fun VideoRendererPreview() {
    MockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        VideoRenderer(
            call = mockCall,
            videoTrackWrapper = TrackWrapper("", org.webrtc.VideoTrack(123)),
            sessionId = "",
            trackType = TrackType.TRACK_TYPE_VIDEO
        )
    }
}
