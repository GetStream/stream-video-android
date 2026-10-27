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

import io.getstream.log.taggedLogger
import io.getstream.result.Result
import io.getstream.video.android.core.CallLeaveReason
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.SdkCause
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.analytics.call.CallAnalytics
import io.getstream.video.android.core.utils.safeCall
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Owns leaving, ending, the leave guard and the ordered cleanup of state, session, jobs and media.
 * A later join waits for cleanup and cancellation before attaching fresh call resources.
 *
 * This component is constructed early in [io.getstream.video.android.core.Call] (the reconnect
 * and event pipeline depend on it), so collaborators created later are injected as lazy
 * providers rather than eager values.
 */
internal class CallLifecycleManager(
    private val clientImpl: StreamVideoClient,
    private val sessionManager: CallSessionManager,
    private val callRegistry: ClientCallRegistry,
    /** Detaches and cancels the call scope, returning its job so rejoin can await cancellation. */
    private val shutDownJobs: () -> Job?,
    private val restartCoordinator: CallRestartCoordinator,
    private val state: CallState,
    private val callAnalytics: CallAnalytics,
    private val statsReporter: CallStatsReporter,
    private val media: CallMediaManager,
    private val iceMonitor: CallIceConnectionMonitor,
    // Lazy providers: both sit on a cycle with this component — the connectivity monitor takes the
    // lifecycle manager to leave on timeout, and the session monitor is built from that monitor.
    private val sessionMonitor: () -> SessionMonitor,
    private val connectivityMonitor: () -> CallConnectivityMonitor,
    private val type: String,
    private val id: String,
) {
    private val logger by taggedLogger("Call:LifecycleManager:$type:$id")

    private val cid = "$type:$id"
    private val cleanupState = CallCleanupState()

    /** True between leave and the next join, including while cleanup is still running. */
    val isDestroyed: Boolean get() = cleanupState.isDestroyed

    /**
     * Time (in millis) when the full reconnection flow started. Will be null again once
     * the reconnection flow ends (success or failure)
     */
    private var sfuSocketReconnectionTime: Long? = null

    /** Wait outside the call scope, which may already be cancelled by leave. */
    suspend fun prepareForJoin(): Result<Unit> =
        cleanupState.prepareForJoin {
            restartCoordinator.restart()
        }

    fun leave(reason: CallLeaveReason) {
        logger.d { "[leave] #ringing; call_cid:$cid" }
        internalLeave(reason)
    }

    fun leave(reason: String = "user") {
        logger.d { "[leave] #ringing; no args, call_cid:$cid" }
        internalLeave(CallLeaveReason.Custom(reason))
    }

    /**
     * Starts the terminal leave operation for the current Call lifecycle.
     *
     * Leave must run through [CallCleanupState.startLeave] because multiple SDK paths may request it
     * concurrently. The cleanup state atomically accepts only the first request, marks the current
     * lifecycle as destroyed, and retains the asynchronous leave job. A later `Call.join()` can then
     * wait for that job instead of recreating the Call while leave signaling or cleanup is still in
     * progress.
     *
     * The asynchronous work runs on the client scope because [cleanup] cancels the Call scope that
     * initiated leave. Running it on the Call scope could cancel the leave event before it is sent.
     */
    private fun internalLeave(reason: CallLeaveReason) {
        cleanupState.startLeave {
            stopConnectionMonitors()
            callAnalytics.stopObservers()
            sessionMonitor().cancelSfuObservers()
            state._connection.value = RealtimeConnection.Disconnected
            logger.v { "[leave] #ringing; call_id = $id" }
            sfuSocketReconnectionTime = null

            /**
             * TODO Rahul, need to check which call has owned the media at the moment(probably use active call)
             */
            media.disableLocalCapture()

            callRegistry.detach()

            clientImpl.scope.launch {
                try {
                    val leaveReason = "[reason=${reason::class.simpleName}, message=${reason.message}]"
                    callAnalytics.onCallLeave(sessionManager.session, reason)
                    safeCall {
                        sessionManager.session.value?.sfuTracer?.trace("leave-call", leaveReason)
                        val stats = statsReporter.collectStats()
                        sessionManager.session.value?.sendCallStats(stats)
                    }
                    // Must complete before cleanup() cancels the session's supervisor job.
                    safeCall { sessionManager.session.value?.sendLeaveEvent(leaveReason) }
                } finally {
                    cleanup()
                }
            }
        }
    }

    /** ends the call for yourself as well as other users */
    suspend fun end(): Result<Unit> {
        // end the call for everyone
        val result = clientImpl.endCall(type, id)
        // cleanup
        leave(
            CallLeaveReason.SdkDriven(
                cause = SdkCause.END_CALL,
                message = "CALL_ENDED", // Call ended by local user
            ),
        )
        return result
    }

    /**
     * Performs terminal resource cleanup for the current Call lifecycle.
     *
     * Cleanup must run through [CallCleanupState.cleanupOnce] because it can be reached from the
     * asynchronous leave `finally` block and from direct cleanup paths. The cleanup state ensures
     * these paths cannot release the same session, media, and coroutine resources more than once.
     * It also marks the lifecycle as destroyed and records the cancelled Call job before cleanup
     * completes, allowing a later `Call.join()` to wait for the old scope's children and `finally`
     * blocks before restarting the Call.
     */
    fun cleanup() {
        cleanupState.cleanupOnce {
            try {
                state.cleanup()
                sessionManager.session.value?.cleanup()
            } finally {
                cleanupState.recordCancelledCallJob(shutDownJobs())
                try {
                    statsReporter.stop()
                    media.cleanup()
                } finally {
                    sessionManager.setActiveSession(null)
                }
            }
        }
    }

    /** Stops the ICE and connectivity monitors. */
    private fun stopConnectionMonitors() {
        iceMonitor.stop()
        connectivityMonitor().cancelLeaveTimeout()
        connectivityMonitor().unsubscribe()
    }
}
