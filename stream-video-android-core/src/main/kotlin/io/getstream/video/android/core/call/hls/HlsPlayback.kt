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

package io.getstream.video.android.core.call.hls

import android.content.Context
import android.view.SurfaceView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.internal.InternalStreamVideoApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Plays one HLS playlist and draws it on a [SurfaceView] supplied by the UI layer.
 *
 * One instance is shared by every renderer of the same [Call], so two tiles cannot start two
 * players for the same broadcast.
 */
@InternalStreamVideoApi
public interface HlsPlayback {
    public val playlistUrl: String

    /**
     * Display width divided by height of the decoded video, or null until the first frame size is
     * known. A [SurfaceView] stretches frames to its own bounds, so the UI sizes the surface with it.
     */
    public val videoAspectRatio: StateFlow<Float?>

    public fun attach(surfaceView: SurfaceView)

    public fun detach(surfaceView: SurfaceView)

    public fun release()
}

internal class Media3HlsPlayback(
    context: Context,
    override val playlistUrl: String,
) : HlsPlayback {

    internal val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()

    private val _videoAspectRatio = MutableStateFlow<Float?>(null)
    override val videoAspectRatio: StateFlow<Float?> = _videoAspectRatio

    init {
        try {
            player.addListener(
                object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        videoAspectRatioOf(videoSize)?.let { _videoAspectRatio.value = it }
                    }
                },
            )
            player.setMediaItem(MediaItem.fromUri(playlistUrl))
            player.playWhenReady = true
            player.prepare()
        } catch (error: Throwable) {
            player.release()
            throw error
        }
    }

    private var attached: SurfaceView? = null
    private var released = false

    override fun attach(surfaceView: SurfaceView) {
        if (released || attached === surfaceView) return
        attached?.let { player.clearVideoSurfaceView(it) }
        player.setVideoSurfaceView(surfaceView)
        attached = surfaceView
    }

    override fun detach(surfaceView: SurfaceView) {
        if (released || attached !== surfaceView) return
        player.clearVideoSurfaceView(surfaceView)
        attached = null
    }

    override fun release() {
        if (released) return
        released = true
        attached = null
        player.release()
    }
}

/** Display aspect ratio of [videoSize], accounting for non-square pixels. Null while unknown. */
internal fun videoAspectRatioOf(videoSize: VideoSize): Float? {
    if (videoSize.width <= 0 || videoSize.height <= 0) return null
    return videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
}

/**
 * Hands out one [HlsPlayback] per [Call], reference counted across the renderers that show it.
 */
@InternalStreamVideoApi
public object HlsPlaybackStore {

    private class Ref(val playback: HlsPlayback, var count: Int)

    private val byCall = HashMap<Call, Ref>()

    /**
     * Builds the [HlsPlayback] for a playlist. Tests replace this so renderers can be exercised
     * without starting a decoder.
     */
    public var factory: (Context, String) -> HlsPlayback = { context, playlistUrl ->
        Media3HlsPlayback(context, playlistUrl)
    }

    public fun acquire(context: Context, call: Call, playlistUrl: String): HlsPlayback {
        synchronized(byCall) {
            val existing = byCall[call]
            if (existing != null && existing.playback.playlistUrl == playlistUrl) {
                existing.count += 1
                return existing.playback
            }
            // Drop the previous player before creating the next one. A factory failure must not
            // leave a released player in the map, or the next acquire would release it again.
            if (existing != null) {
                byCall.remove(call)
                existing.playback.release()
            }
            val created = factory(context, playlistUrl)
            byCall[call] = Ref(created, count = 1)
            return created
        }
    }

    public fun release(call: Call, playback: HlsPlayback) {
        synchronized(byCall) {
            val existing = byCall[call] ?: return
            if (existing.playback !== playback) return
            existing.count -= 1
            if (existing.count <= 0) {
                byCall.remove(call)
                playback.release()
            }
        }
    }

    /** Drops every player. Test-only. */
    public fun reset() {
        synchronized(byCall) {
            byCall.values.forEach { it.playback.release() }
            byCall.clear()
        }
    }
}
