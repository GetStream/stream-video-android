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
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.call.hls.HlsPlaybackListener
import io.getstream.video.android.core.call.hls.HlsPlaybackMetrics
import io.getstream.video.android.core.call.hls.HlsPlaybackState
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
    private var playbackListener: HlsPlaybackListener? = null
    private var reportedState: HlsPlaybackState? = null
    private var hasPlayed = false
    private var seeking = false
    private var droppedFrames = 0

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

    override fun release() = onMain(::releaseOnMain)

    override fun setPlaybackListener(listener: HlsPlaybackListener?) = onMain {
        if (released) return@onMain
        playbackListener = listener
        reportedState = null
        player?.let(::reportState)
    }

    @OptIn(UnstableApi::class)
    override fun readPlaybackMetrics(): HlsPlaybackMetrics? {
        val player = player ?: return null
        val format = player.videoFormat
        val liveOffsetMs = player.currentLiveOffset
        return HlsPlaybackMetrics(
            bitrateKbps = format?.bitrate?.takeIf { it != Format.NO_VALUE }?.div(1000) ?: 0,
            bufferLengthMs = player.totalBufferedDuration,
            liveLatencyMs = if (liveOffsetMs == C.TIME_UNSET) 0 else liveOffsetMs,
            droppedFrames = droppedFrames,
            width = format?.width?.takeIf { it != Format.NO_VALUE } ?: player.videoSize.width,
            height = format?.height?.takeIf { it != Format.NO_VALUE } ?: player.videoSize.height,
        )
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    private fun releaseOnMain() {
        if (released) return
        released = true
        surface = null
        playbackListener = null
        player?.release()
        player = null
    }

    private fun reportState(player: Player) {
        val state = hlsPlaybackStateOf(
            playbackState = player.playbackState,
            playWhenReady = player.playWhenReady,
            isPlaying = player.isPlaying,
            hasPlayed = hasPlayed,
            seeking = seeking,
        ) ?: return
        if (state == HlsPlaybackState.Playing) {
            hasPlayed = true
            seeking = false
        }
        if (state == reportedState) return
        reportedState = state
        playbackListener?.onPlaybackStateChanged(state)
    }

    private fun buildPlayer(): ExoPlayer {
        val player = ExoPlayer.Builder(context).build()
        try {
            player.addListener(
                object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        videoAspectRatioOf(videoSize)?.let { _videoAspectRatio.value = it }
                    }

                    override fun onPositionDiscontinuity(
                        oldPosition: Player.PositionInfo,
                        newPosition: Player.PositionInfo,
                        reason: Int,
                    ) {
                        if (reason == Player.DISCONTINUITY_REASON_SEEK) seeking = true
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        playbackListener?.onPlaybackError(error.errorCodeName)
                    }

                    override fun onEvents(player: Player, events: Player.Events) {
                        if (events.containsAny(
                                Player.EVENT_PLAYBACK_STATE_CHANGED,
                                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                                Player.EVENT_IS_PLAYING_CHANGED,
                            )
                        ) {
                            reportState(player)
                        }
                    }
                },
            )
            player.addAnalyticsListener(
                object : AnalyticsListener {
                    override fun onDroppedVideoFrames(
                        eventTime: AnalyticsListener.EventTime,
                        droppedFrames: Int,
                        elapsedMs: Long,
                    ) {
                        this@ExoHlsPlayer.droppedFrames += droppedFrames
                    }
                },
            )
            player.volume = volumeFor(audioEnabled)
            player.setMediaItem(MediaItem.fromUri(playlistUrl))
            player.prepare()
        } catch (error: Throwable) {
            player.release()
            throw error
        }
        return player
    }

    private fun volumeFor(enabled: Boolean): Float = if (enabled) 1f else 0f
}

/**
 * Maps ExoPlayer's state to the state the SDK derives stalls from. Buffering before the first
 * play is [HlsPlaybackState.Loading], and after a seek [HlsPlaybackState.Seeking], so neither
 * counts as a stall. Null while idle.
 */
internal fun hlsPlaybackStateOf(
    playbackState: @Player.State Int,
    playWhenReady: Boolean,
    isPlaying: Boolean,
    hasPlayed: Boolean,
    seeking: Boolean,
): HlsPlaybackState? = when {
    playbackState == Player.STATE_IDLE -> null
    playbackState == Player.STATE_ENDED -> HlsPlaybackState.Ended
    isPlaying -> HlsPlaybackState.Playing
    !playWhenReady -> HlsPlaybackState.Paused
    playbackState == Player.STATE_BUFFERING -> when {
        !hasPlayed -> HlsPlaybackState.Loading
        seeking -> HlsPlaybackState.Seeking
        else -> HlsPlaybackState.Buffering
    }
    // Ready with playWhenReady set but not playing: playback is suppressed, e.g. by audio focus.
    else -> HlsPlaybackState.Paused
}

/** Display aspect ratio of [videoSize], accounting for non-square pixels. Null while unknown. */
internal fun videoAspectRatioOf(videoSize: VideoSize): Float? {
    if (videoSize.width <= 0 || videoSize.height <= 0) return null
    return videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
}
