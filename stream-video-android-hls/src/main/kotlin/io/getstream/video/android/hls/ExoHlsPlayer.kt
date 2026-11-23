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

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.call.hls.HlsPlayer
import io.getstream.video.android.core.call.hls.HlsPlayerFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Plays HLS broadcasts with Media3 ExoPlayer. Pass it to [StreamVideoBuilder] to enable HLS
 * playback, and show a call with [HlsVideoRenderer].
 *
 * ```kotlin
 * StreamVideoBuilder(
 *     context = context,
 *     apiKey = apiKey,
 *     user = user,
 *     token = token,
 *     hlsPlayerFactory = ExoHlsPlayerFactory(),
 * ).build()
 * ```
 */
public class ExoHlsPlayerFactory : HlsPlayerFactory {

    override fun create(context: Context, playlistUrl: String): HlsPlayer =
        ExoHlsPlayer(context.applicationContext, playlistUrl)
}

/**
 * [HlsPlayer] backed by ExoPlayer. ExoPlayer is bound to the thread that builds it, so it is built
 * on the main thread at the first [attach] rather than when the call joins.
 */
internal class ExoHlsPlayer(
    private val context: Context,
    override val playlistUrl: String,
) : HlsPlayer {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _videoAspectRatio = MutableStateFlow<Float?>(null)
    override val videoAspectRatio: StateFlow<Float?> = _videoAspectRatio.asStateFlow()

    // Main thread only.
    private var player: ExoPlayer? = null
    private var surface: SurfaceView? = null
    private var audioEnabled = true
    private var released = false

    internal val exoPlayer: ExoPlayer? get() = player
    internal val attachedSurface: SurfaceView? get() = surface

    override fun attach(surfaceView: SurfaceView) {
        if (released) return
        val player = player ?: buildPlayer().also { player = it }
        if (surface !== surfaceView) {
            surface?.let(player::clearVideoSurfaceView)
            player.setVideoSurfaceView(surfaceView)
            surface = surfaceView
        }
        player.play()
    }

    override fun detach(surfaceView: SurfaceView) {
        if (released || surface !== surfaceView) return
        player?.clearVideoSurfaceView(surfaceView)
        surface = null
    }

    override fun setAudioEnabled(enabled: Boolean) {
        audioEnabled = enabled
        player?.volume = volumeFor(enabled)
    }

    override fun release() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            releaseOnMain()
        } else {
            mainHandler.post(::releaseOnMain)
        }
    }

    private fun releaseOnMain() {
        if (released) return
        released = true
        surface = null
        player?.release()
        player = null
    }

    private fun buildPlayer(): ExoPlayer {
        val player = ExoPlayer.Builder(context).build()
        try {
            player.addListener(
                object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        videoAspectRatioOf(videoSize)?.let { _videoAspectRatio.value = it }
                    }
                },
            )
            player.volume = volumeFor(audioEnabled)
            // Set the MIME type so playlist URLs without an .m3u8 extension still play as HLS.
            player.setMediaItem(
                MediaItem.Builder()
                    .setUri(playlistUrl)
                    .setMimeType(MimeTypes.APPLICATION_M3U8)
                    .build(),
            )
            player.prepare()
        } catch (error: Throwable) {
            player.release()
            throw error
        }
        return player
    }

    private fun volumeFor(enabled: Boolean): Float = if (enabled) 1f else 0f
}

/** Display aspect ratio of [videoSize], accounting for non-square pixels. Null while unknown. */
internal fun videoAspectRatioOf(videoSize: VideoSize): Float? {
    if (videoSize.width <= 0 || videoSize.height <= 0) return null
    return videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
}
