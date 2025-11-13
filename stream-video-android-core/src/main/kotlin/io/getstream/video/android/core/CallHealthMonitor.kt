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

package io.getstream.video.android.core

import io.getstream.webrtc.PeerConnection
import kotlinx.coroutines.CoroutineScope

/**
 * Monitors
 * - Publisher and subscriber Peer connection states -> immediately reconnect
 * - Network up/down -> mark down instantly when down. reconnect when up
 * - Interval every 2 seconds. check and decide what to do
 *
 * Calls call.reconnectOrSwitchSfu() when needed
 *
 * Notes
 * - There is a delay after a restart till connections show healthy again
 * - So we shouldn't immediately try to reconnect if we're already reconnecting
 *
 */
public class CallHealthMonitor(
    val call: Call,
    val callScope: CoroutineScope,
    val onIceRecoveryFailed: () -> Unit,
) {

    val badStates = listOf(
        PeerConnection.PeerConnectionState.DISCONNECTED,
        PeerConnection.PeerConnectionState.FAILED,
        PeerConnection.PeerConnectionState.CLOSED,
    )

    val badStatesExcludingClosed = listOf(
        PeerConnection.PeerConnectionState.DISCONNECTED,
        PeerConnection.PeerConnectionState.FAILED,
    )

    fun start() {
        // no-op
    }

    fun stop() {
        // no-op
    }

    fun stopTimer() {
        // no-op
    }

    val goodStates = listOf(
        PeerConnection.PeerConnectionState.NEW, // New is good, means we're not using it yet
        PeerConnection.PeerConnectionState.CONNECTED,
        PeerConnection.PeerConnectionState.CONNECTING,
    )

    /**
     * Checks the peer connection states.
     * Launches reconnect() if not healthy
     */
    @Synchronized
    fun check() {
        // no-op
    }

    /**
     * Only 1 reconnect attempt runs at the same time
     * Will skip if we already tried to reconnect less than reconnectDebounceMs ms ago
     */
    suspend fun reconnect(forceRestart: Boolean) {
        // no-op
    }
}
