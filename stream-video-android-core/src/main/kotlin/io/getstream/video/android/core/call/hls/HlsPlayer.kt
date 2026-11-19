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
 * released when the call is left. A renderer only shows and hides it with [attach] and [detach],
 * so a renderer that is recreated, for example on rotation, resumes the same player.
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

    /** Draws the broadcast on [surfaceView] and plays it. Main thread only. */
    public fun attach(surfaceView: SurfaceView)

    /**
     * Stops drawing on [surfaceView] and pauses. The player keeps its buffer, so a later [attach]
     * resumes without loading the stream again. Main thread only.
     */
    public fun detach(surfaceView: SurfaceView)

    /** Mutes the broadcast audio when [enabled] is false. Playback continues either way. */
    public fun setAudioEnabled(enabled: Boolean)

    /** Stops playback for good. Called by the SDK when the call is left, from any thread. */
    public fun release()
}

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
