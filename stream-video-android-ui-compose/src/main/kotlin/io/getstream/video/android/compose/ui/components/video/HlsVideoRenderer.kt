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

package io.getstream.video.android.compose.ui.components.video

import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.hls.HlsPlayback
import io.getstream.video.android.core.call.hls.HlsPlaybackStore
import io.getstream.video.android.core.model.PlaybackType

/**
 * Plays the call's HLS broadcast.
 *
 * Renders nothing while [Call.state.playbackType] is not [PlaybackType.Hls]. The playlist is on
 * the call. One player is shared by every [HlsVideoRenderer] of the same call, and released when
 * the last one leaves.
 *
 * The broadcast is an already composed layout, so the frame is fitted rather than cropped.
 */
@Composable
public fun HlsVideoRenderer(
    call: Call,
    modifier: Modifier = Modifier,
) {
    HlsVideoRendererImpl(call = call, store = defaultHlsPlaybackStore, modifier = modifier)
}

private val defaultHlsPlaybackStore = HlsPlaybackStore()

@Composable
internal fun HlsVideoRendererImpl(
    call: Call,
    store: HlsPlaybackStore,
    modifier: Modifier = Modifier,
) {
    val playbackType by call.state.playbackType.collectAsStateWithLifecycle()
    val playlistUrl = (playbackType as? PlaybackType.Hls)?.playlistUrl ?: return
    val context = LocalContext.current
    var playback by remember(store, call, playlistUrl) { mutableStateOf<HlsPlayback?>(null) }
    // Acquired here rather than in remember: an abandoned composition never runs onDispose,
    // so a player started during composition would never be released.
    DisposableEffect(store, call, playlistUrl) {
        val acquired = store.acquire(context, call, playlistUrl)
        playback = acquired
        onDispose { store.release(call, acquired) }
    }
    val current = playback ?: return
    val aspectRatio by current.videoAspectRatio.collectAsStateWithLifecycle()
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        key(current) {
            AndroidView(
                factory = { viewContext ->
                    SurfaceView(viewContext).also { current.attach(it) }
                },
                onRelease = { surface -> current.detach(surface) },
                modifier = (aspectRatio?.let { Modifier.aspectRatio(it) } ?: Modifier.fillMaxSize())
                    .testTag("hls_video_renderer"),
            )
        }
    }
}
