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

package io.getstream.video.android.core.call.components

import io.getstream.android.video.generated.models.JoinCallResponse
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.analytics.call.CallAnalytics
import io.getstream.video.android.core.events.JoinCallResponseEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Owns the SFU signal/event observers for a call and (re)wires the per-session monitoring
 * whenever a session is established or swapped (join, rejoin, migrate).
 *
 * Extracted from the [io.getstream.video.android.core.Call] facade so the join and reconnect
 * flows can drive session monitoring directly instead of routing through the facade. It owns
 * the observer [Job]s it launches and fans out to the stats, ICE and connectivity monitors.
 */
internal class SessionMonitor(
    private val type: String,
    private val id: String,
    private val scope: CoroutineScope,
    private val state: CallState,
    private val sessionManager: CallSessionManager,
    private val statsReporter: CallStatsReporter,
    private val iceMonitor: CallIceConnectionMonitor,
    private val connectivityMonitor: CallConnectivityMonitor,
    private val callAnalytics: CallAnalytics,
) {
    private val logger by taggedLogger("Call:SessionMonitor:$type:$id")

    private var sfuListener: Job? = null
    private var sfuEvents: Job? = null

    /** Cancels the active SFU signal/event observers, if any. */
    fun cancelSfuObservers() {
        sfuEvents?.cancel()
        sfuListener?.cancel()
    }

    /**
     * (Re)establishes monitoring for the current session: restarts stats reporting, subscribes
     * to the SFU signal socket (to keep the fast-reconnect deadline fresh), wires the analytics
     * observers, and starts the ICE and connectivity monitors.
     */
    fun monitorSession(result: JoinCallResponse) {
        sfuEvents?.cancel()
        sfuListener?.cancel()
        statsReporter.start(result.statsOptions.reportingIntervalMs.toLong())
        // listen to Signal WS
        sfuEvents = scope.launch {
            sessionManager.session.value?.let {
                it.socket.events().collect { event ->
                    if (event is JoinCallResponseEvent) {
                        sessionManager.reconnectDeadlineMillis = event.fastReconnectDeadlineSeconds * 1000
                        logger.d {
                            "[join] #deadline for reconnect is ${sessionManager.reconnectDeadlineMillis / 1000} seconds"
                        }
                    }
                }
            }
        }
        callAnalytics.peerConnectionAnalytics.stopAndObservePeerConnections(sessionManager.session)
        callAnalytics.audioAnalytics.observeFirstRemoteParticipantAudioMuteState(
            sessionManager.session,
            state.participants,
        )
        iceMonitor.start()
        connectivityMonitor.subscribe()
    }
}
