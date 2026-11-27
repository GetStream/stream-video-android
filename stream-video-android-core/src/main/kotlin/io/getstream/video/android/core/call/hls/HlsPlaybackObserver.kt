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

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The SDK's listener on one installed [HlsPlayer]. Publishes the player's state and last error
 * to the call state, and forwards both to the stats reporter when HLS stats are enabled.
 *
 * Installed when HLS playback starts and [close]d when it is cleared. A callback that arrives
 * after [close], for example one already posted to the main thread, changes nothing.
 */
internal class HlsPlaybackObserver(
    private val playbackState: MutableStateFlow<HlsPlaybackState?>,
    private val playbackError: MutableStateFlow<String?>,
    private val statsReporter: HlsStatsReporter?,
) : HlsPlaybackListener {

    private val lock = Any()

    // Guarded by [lock].
    private var closed = false

    override fun onPlaybackStateChanged(state: HlsPlaybackState) {
        synchronized(lock) {
            if (closed) return
            playbackState.value = state
            if (state == HlsPlaybackState.Playing) playbackError.value = null
            statsReporter?.onPlaybackStateChanged(state)
        }
    }

    override fun onPlaybackError(code: String) {
        synchronized(lock) {
            if (closed) return
            playbackError.value = code
            statsReporter?.onPlaybackError(code)
        }
    }

    /** Stops publishing and forwarding, and resets the published state. Safe to call again. */
    fun close() {
        synchronized(lock) {
            closed = true
            playbackState.value = null
            playbackError.value = null
        }
    }
}
