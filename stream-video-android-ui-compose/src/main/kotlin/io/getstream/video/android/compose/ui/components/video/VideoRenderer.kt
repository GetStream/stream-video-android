/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.getstream.log.StreamLog
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.video.VideoScalingType.Companion.toCommonScalingType
import io.getstream.video.android.compose.ui.components.video.config.VideoRendererConfig
import io.getstream.video.android.compose.ui.components.video.config.videoRenderConfig
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.model.MediaTrack
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.ui.common.renderer.StreamVideoTextureViewRenderer
import io.getstream.webrtc.android.ui.VideoTextureViewRenderer

@Composable
public fun VideoRenderer(
    modifier: Modifier = Modifier,
    call: Call,
    video: ParticipantState.Media?,
    videoRendererConfig: VideoRendererConfig = videoRenderConfig(),
    onRendered: (VideoTextureViewRenderer) -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("video_renderer_container"),
    ) {
        if (LocalInspectionMode.current) {
            Image(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("video_renderer"),
                painter = painterResource(
                    id = io.getstream.video.android.ui.common.R.drawable.stream_video_call_sample,
                ),
                contentScale = ContentScale.Crop,
                contentDescription = null,
            )
            return
        }

        // Show avatar always behind the video.
        videoRendererConfig.fallbackContent.invoke(call)

        if (video?.enabled == true) {
            val mediaTrack = video.track
            val sessionId = video.sessionId
            val trackType = video.type

            var view: VideoTextureViewRenderer? by remember { mutableStateOf(null) }

            DisposableEffect(call, video) {
                // inform the call that we want to render this video track. (this will trigger a subscription to the track)
                call.setVisibility(sessionId, trackType, true)

                onDispose {
                    cleanTrack(view, mediaTrack)
                    // inform the call that we no longer want to render this video track
                    call.setVisibility(sessionId, trackType, false)
                }
            }

            if (mediaTrack != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AndroidView(
                        factory = { context ->
                            StreamVideoTextureViewRenderer(context).apply {
                                call.initRenderer(
                                    videoRenderer = this,
                                    sessionId = sessionId,
                                    trackType = trackType,
                                    onRendered = onRendered,
                                )
                                setMirror(videoRendererConfig.mirrorStream)
                                setScalingType(
                                    videoRendererConfig.scalingType.toCommonScalingType(),
                                )
                                setupVideo(mediaTrack, this)

                                view = this
                            }
                        },
                        update = { v ->
                            v.setMirror(videoRendererConfig.mirrorStream)
                            v.setScalingType(
                                videoRendererConfig.scalingType.toCommonScalingType(),
                            )
                            setupVideo(mediaTrack, v)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("video_renderer"),
                    )
                }
            }
        }
    }
}

/**
 * Renders a single video track based on the call state.
 *
 * @param call The call state that contains all the tracks and participants.
 * @param video A media contains a video track or an audio track to be rendered.
 * @param modifier Modifier for styling.
 * @param videoScalingType Setup the video scale type of this renderer.
 * @param videoFallbackContent Content is shown the video track is failed to load or not available.
 * @param onRendered An interface that will be invoked when the video is rendered.
 */
@Deprecated("Use VideoRenderer which accepts `videoConfig` instead.")
@Composable
public fun VideoRenderer(
    call: Call,
    video: ParticipantState.Media?,
    modifier: Modifier = Modifier,
    videoScalingType: VideoScalingType = VideoScalingType.SCALE_ASPECT_FILL,
    videoFallbackContent: @Composable (Call) -> Unit = {
        DefaultMediaTrackFallbackContent(
            modifier,
            call,
        )
    },
    onRendered: (VideoTextureViewRenderer) -> Unit = {},
) {
    VideoRenderer(
        call = call,
        video = video,
        modifier = modifier,
        videoRendererConfig = videoRenderConfig {
            this.videoScalingType = videoScalingType
            this.fallbackContent = videoFallbackContent
        },
        onRendered = onRendered,
    )
}

private fun cleanTrack(
    view: VideoTextureViewRenderer?,
    mediaTrack: MediaTrack?,
) {
    if (view != null && mediaTrack is VideoTrack) {
        try {
            mediaTrack.video.removeSink(view)
        } catch (e: Exception) {
            // The MediaStreamTrack can be already disposed at this point (from other parts of the code)
            // Removing the Sink at this point will throw a IllegalStateException("MediaStreamTrack has been disposed.")
            // See MediaStreamTrack.checkMediaStreamTrackExists()
            StreamLog.w("VideoRenderer") { "Failed to removeSink in onDispose:  ${e.message}" }
        }
    }
}

private fun setupVideo(
    mediaTrack: MediaTrack?,
    renderer: VideoTextureViewRenderer,
) {
    cleanTrack(renderer, mediaTrack)

    try {
        if (mediaTrack is VideoTrack) {
            mediaTrack.video.addSink(renderer)
        }
    } catch (e: Exception) {
        StreamLog.w("VideoRenderer") { e.message.toString() }
    }
}

@Composable
internal fun DefaultMediaTrackFallbackContent(
    modifier: Modifier,
    call: Call,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VideoTheme.colors.baseSheetTertiary)
            .testTag("video_renderer_fallback"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = Modifier.padding(30.dp),
            text = stringResource(
                id = io.getstream.video.android.ui.common.R.string.stream_video_call_rendering_failed,
                call.sessionId,
            ),
            color = VideoTheme.colors.basePrimary,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
        )
    }
}

@Preview
@Composable
private fun VideoRendererPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        VideoRenderer(
            call = previewCall,
            video = ParticipantState.Video(
                track = VideoTrack("", org.webrtc.VideoTrack(123)),
                enabled = true,
                sessionId = "",
            ),
        )
    }
}

@Preview
@Composable
private fun VideoRendererFallbackPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        DefaultMediaTrackFallbackContent(modifier = Modifier, call = previewCall)
    }
}
