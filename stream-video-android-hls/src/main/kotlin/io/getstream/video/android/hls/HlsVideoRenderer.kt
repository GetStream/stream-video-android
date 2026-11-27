/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.hls

import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.internal.ExperimentalStreamVideoApi
import io.getstream.video.android.core.model.PlaybackType

/**
 * Shows the call's HLS broadcast.
 *
 * Renders nothing unless [Call.state.playbackType] is [PlaybackType.Hls], which requires an
 * [ExoHlsPlayerFactory] registered on the client. The call owns the player: this renderer only
 * shows its video, playback and audio continue while it is not shown, and leaving the call
 * releases it. Recreating the renderer, for example on rotation, shows the same player without
 * loading the stream again.
 *
 * The broadcast is an already composed layout, so the frame is fitted rather than cropped.
 *
 * [Call.setIncomingAudioEnabled] only reaches WebRTC tracks, so HLS audio is muted through
 * [audioEnabled] instead.
 *
 * @param audioEnabled Plays the broadcast audio when true, mutes it when false.
 */
@ExperimentalStreamVideoApi
@Composable
public fun HlsVideoRenderer(
    call: Call,
    modifier: Modifier = Modifier,
    audioEnabled: Boolean = true,
) {
    val player by call.hlsPlayer.collectAsStateWithLifecycle()
    val current = player ?: return
    SideEffect { current.setAudioEnabled(audioEnabled) }
    val aspectRatio by current.videoAspectRatio.collectAsStateWithLifecycle()
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        key(current) {
            AndroidView(
                factory = { context -> SurfaceView(context).also { current.attach(it) } },
                onRelease = { surface -> current.detach(surface) },
                modifier = (aspectRatio?.let { Modifier.aspectRatio(it) } ?: Modifier.fillMaxSize())
                    .testTag("hls_video_renderer"),
            )
        }
    }
}
