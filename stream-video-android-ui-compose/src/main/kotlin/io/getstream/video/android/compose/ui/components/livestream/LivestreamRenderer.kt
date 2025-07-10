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

package io.getstream.video.android.compose.ui.components.livestream

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.compose.ui.components.video.config.VideoRendererConfig
import io.getstream.video.android.compose.ui.components.video.config.videoRenderConfig
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.ui.common.R
import io.getstream.webrtc.android.ui.VideoTextureViewRenderer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter

@Composable
internal fun LivestreamRenderer(
    call: Call,
    configuration: VideoRendererConfig = videoRenderConfig(),
    enablePausing: Boolean,
    onPausedPlayer: ((isPaused: Boolean) -> Unit)? = {},
    livestreamFlow: Flow<ParticipantState.Video?> = call.state.livestream,
    onAudioToggle: (Boolean) -> Unit = {},
) {
    var isPaused by rememberSaveable { mutableStateOf(false) }
    val livestream by livestreamFlow
        .filter { !isPaused }
        .collectAsStateWithLifecycle(null)
    var videoTextureView: VideoTextureViewRenderer? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        VideoRenderer(
            videoRendererConfig = configuration,
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = enablePausing) {
                    if (onPausedPlayer != null) {
                        isPaused = !isPaused
                        onPausedPlayer.invoke(isPaused)

                        val hostVideoTrack = livestream?.track?.video
                        Log.d("Noob", "isPaused=$isPaused, hostVideoTrack=${hostVideoTrack?.id()}")

                        if (isPaused) {
                            videoTextureView?.pauseVideo()
                        } else {
                            videoTextureView?.resumeVideo()
                        }
                        onAudioToggle(!isPaused)
                    }
                },
            call = call,
            video = livestream,
            onRendered = { renderer ->
                videoTextureView = renderer
            },
        )

        AnimatedVisibility(
            visible = isPaused,
            enter = scaleIn(spring(Spring.DampingRatioMediumBouncy), initialScale = 1.5f),
            exit = scaleOut(tween(150)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Image(
                modifier = Modifier.alpha(0.75f),
                painter = painterResource(
                    id = R.drawable.stream_video_ic_play,
                ),
                contentDescription = null,
            )
        }
    }
}

@Preview
@Composable
private fun LiveStreamRendererPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        LivestreamRenderer(call = previewCall, enablePausing = true)
    }
}
