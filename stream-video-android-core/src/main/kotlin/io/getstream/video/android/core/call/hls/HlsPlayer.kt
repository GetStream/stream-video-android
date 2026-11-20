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
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideoBuilder
import kotlinx.coroutines.flow.StateFlow

/**
 * Plays the HLS broadcast of one [Call].
 *
 * The call owns its player: one is created when the backend serves the call as HLS, and it is
 * released when the call is left. Joining alone plays nothing: playback, including audio, starts
 * at the first [attach]. From then on it continues while no renderer is shown, the same as a
 * WebRTC call, and a renderer that is recreated, for example on rotation, shows the same player.
 *
 * The player draws to one surface at a time: showing the same call in two renderers at once is
 * not supported.
 *
 * Beta: HLS delivery is enabled per call type by Stream and may change.
 */
public interface HlsPlayer {

    public val playlistUrl: String

    /**
     * Display width divided by height of the decoded video, or null until the first frame size is
     * known. A [SurfaceView] stretches frames to its own bounds, so the UI sizes the surface with it.
     */
    public val videoAspectRatio: StateFlow<Float?>

    /** Draws the broadcast on [surfaceView], starting playback if needed. Main thread only. */
    public fun attach(surfaceView: SurfaceView)

    /**
     * Stops drawing on [surfaceView]. Playback continues until [release]. Ignored if another
     * surface was attached since. Main thread only.
     */
    public fun detach(surfaceView: SurfaceView)

    /** Mutes the broadcast audio when [enabled] is false. Playback continues either way. */
    public fun setAudioEnabled(enabled: Boolean)

    /** Stops playback for good. Called by the SDK when the call is left, from any thread. */
    public fun release()

    /**
     * Receives the player's state and errors for the call's HLS stats reports, or stops when
     * [listener] is null. Called by the SDK from any thread; the listener is called on the main
     * thread. A player that does not report keeps the default, and the call then sends no reports.
     */
    public fun setPlaybackListener(listener: HlsPlaybackListener?) {}

    /**
     * Current readings for an HLS stats report, or null before playback starts or after [release].
     * Main thread only.
     */
    public fun readPlaybackMetrics(): HlsPlaybackMetrics? = null
}

/** Player state and errors reported to the SDK. See [HlsPlayer.setPlaybackListener]. */
public interface HlsPlaybackListener {

    /** The player moved to [state]. Repeating the current state is allowed and ignored. */
    public fun onPlaybackStateChanged(state: HlsPlaybackState)

    /**
     * Playback failed. [code] names the error, for example `ERROR_CODE_BEHIND_LIVE_WINDOW`, and
     * must not contain the playlist URL.
     */
    public fun onPlaybackError(code: String)
}

/**
 * Player states the SDK derives stalls and watch time from. A stall is [Playing] followed by
 * [Buffering]; the initial [Loading], a [Seeking] and a [Paused] are not stalls.
 */
public enum class HlsPlaybackState {
    /** Loading before the first frame plays. */
    Loading,

    /** Playing media. */
    Playing,

    /** Waiting for data after playback had started. */
    Buffering,

    /** Loading after a seek. */
    Seeking,

    /** Not playing because playback is paused or suppressed. */
    Paused,

    /** The broadcast ended. */
    Ended,
}

/**
 * Current player readings for an HLS stats report. A value the player does not know yet is 0.
 *
 * @property bitrateKbps Bitrate of the rendition playing now.
 * @property bufferLengthMs Media buffered ahead of the playhead.
 * @property liveLatencyMs Distance behind the live edge.
 * @property droppedFrames Video frames dropped by the decoder since playback started.
 * @property width Width of the rendition playing now, in pixels.
 * @property height Height of the rendition playing now, in pixels.
 */
public class HlsPlaybackMetrics(
    public val bitrateKbps: Int = 0,
    public val bufferLengthMs: Long = 0,
    public val liveLatencyMs: Long = 0,
    public val droppedFrames: Int = 0,
    public val width: Int = 0,
    public val height: Int = 0,
)

/**
 * Builds the [HlsPlayer] for a call the backend serves as HLS. Pass one to
 * [StreamVideoBuilder] to enable HLS playback: without it the SDK never asks the backend for HLS,
 * and every call joins over WebRTC.
 *
 * The `stream-video-android-hls` artifact provides the ExoPlayer implementation.
 */
public fun interface HlsPlayerFactory {

    /**
     * Called from a background thread while the call is joining, so it must return quickly and
     * must not touch a thread-bound player yet.
     */
    public fun create(context: Context, playlistUrl: String): HlsPlayer
}
