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

@file:OptIn(StreamVideoUiDelicateApi::class)

package io.getstream.video.android.compose.ui.components.video

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalWifiBad
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.log.StreamLog
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.video.VideoScalingType.Companion.toCommonScalingType
import io.getstream.video.android.compose.ui.components.video.config.VideoRendererConfig
import io.getstream.video.android.compose.ui.components.video.config.videoRenderConfig
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.call.utils.ALL_PARTICIPANTS
import io.getstream.video.android.core.model.MediaTrack
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.ui.common.renderer.StreamVideoTextureViewRenderer
import io.getstream.video.android.ui.common.util.StreamVideoUiDelicateApi
import io.getstream.webrtc.android.ui.VideoTextureViewRenderer
import java.util.UUID

@Composable
public fun VideoRenderer(
    modifier: Modifier = Modifier,
    call: Call,
    video: ParticipantState.Media?,
    videoRendererConfig: VideoRendererConfig = videoRenderConfig(),
    onRendered: (VideoTextureViewRenderer) -> Unit = {},
) {
    StreamLog.d("VideoRenderer") { "Rendering video" }
    Box(
        modifier = modifier
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

        if (video?.paused == true) {
            videoRendererConfig.badNetworkContent.invoke(call)
        }

        if (video?.enabled == true && !video.paused) {
            val viewportId = remember(call, video) {
                try {
                    UUID.randomUUID().toString()
                } catch (e: Exception) {
                    // In case the UUID generation fails, we fallback to a random double string.
                    Math.random().toString()
                }
            }
            val sessionId = video.sessionId
            val videoEnabledOverrides by call.state.participantVideoEnabledOverrides.collectAsStateWithLifecycle()

            if (isIncomingVideoEnabled(call, sessionId, videoEnabledOverrides)) {
                val mediaTrack = video.track
                val trackType = video.type

                var view: VideoTextureViewRenderer? by remember { mutableStateOf(null) }

                if (videoRendererConfig.updateVisibility) {
                    DisposableEffect(call, video) {
                        // inform the call that we want to render this video track. (this will trigger a subscription to the track)
                        call.setVisibility(sessionId, trackType, true, viewportId)

                        onDispose {
                            cleanTrack(view, mediaTrack)
                            // inform the call that we no longer want to render this video track
                            call.setVisibility(sessionId, trackType, false, viewportId)
                        }
                    }
                }

                if (mediaTrack != null) {
                    StreamLog.d("VideoRenderer") { "Rendering video track: $mediaTrack" }
                    Box(
                        modifier = videoRendererConfig.modifiers.containerModifier.invoke(this)
                            .testTag("Stream_VideoViewWithMediaTrack"),
                        contentAlignment = Alignment.Center,
                    ) {
                        StreamLog.d("VideoRenderer") { "Rendering video viewportId: $viewportId" }
                        AndroidView(
                            factory = { context ->
                                StreamVideoTextureViewRenderer(context).apply {
                                    StreamLog.d(
                                        "VideoRenderer",
                                    ) { "Rendering video (init renderer)" }
                                    call.initRenderer(
                                        viewportId = viewportId,
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
                                StreamLog.d("VideoRenderer") { "Rendering video (update renderer)" }
                                v.setMirror(videoRendererConfig.mirrorStream)
                                v.setScalingType(
                                    videoRendererConfig.scalingType.toCommonScalingType(),
                                )
                                setupVideo(mediaTrack, v)
                            },
                            modifier = videoRendererConfig
                                .modifiers
                                .componentModifier(
                                    this,
                                )
                                .testTag("video_renderer"),
                        )
                    }
                } else {
                    // Do something when there is no media track
                }
            }
        } else {
            // Do something when the video is not enabled
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

private fun isIncomingVideoEnabled(call: Call, sessionId: String, videoEnabledOverrides: Map<String, Boolean?>) =
    (videoEnabledOverrides[sessionId] ?: videoEnabledOverrides[ALL_PARTICIPANTS]) != false ||
        call.state.me.value?.sessionId == sessionId

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

@Composable
internal fun DefaultBadNetworkFallbackContent(
    modifier: Modifier,
    call: Call,
) {
    Row(
        modifier = modifier
            .padding(16.dp)
            .background(
                color = VideoTheme.colors.baseSheetQuarternary,
                shape = VideoTheme.shapes.sheet,
            )
            .testTag("video_renderer_fallback_bad_network"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.padding(12.dp).align(CenterVertically),
            imageVector = Icons.Default.SignalWifiBad,
            contentDescription = null,
            tint = VideoTheme.colors.basePrimary,
        )
        Text(
            modifier = Modifier.padding(12.dp),
            text = stringResource(
                id = io.getstream.video.android.ui.common.R.string.stream_video_call_bad_network,
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
                paused = false,
            ),
        )
    }
}

@Preview
@Composable
private fun VideoRendererPausedPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        VideoRenderer(
            call = previewCall,
            video = ParticipantState.Video(
                track = VideoTrack("", org.webrtc.VideoTrack(123)),
                enabled = true,
                sessionId = "",
                paused = true,
            ),
        )
    }
}

@Preview
@Composable
private fun VideoRendererPausedPreview2() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        DefaultBadNetworkFallbackContent(
            call = previewCall,
            modifier = Modifier,
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
