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
import io.getstream.video.android.core.call.scope.ScopeProvider
import io.getstream.video.android.core.utils.AtomicUnitCall
import io.getstream.video.android.core.utils.safeCall
import kotlinx.coroutines.launch

/**
 * Owns the call's lifecycle / teardown: leaving, ending, the single-shot leave guard
 * ([AtomicUnitCall]), the destroyed flag, and the ordered cleanup of state, session,
 * jobs and media.
 *
 * This component is constructed early in [io.getstream.video.android.core.Call] (the reconnect
 * and event pipeline depend on it), so collaborators created later are injected as lazy
 * providers rather than eager values.
 */
internal class CallLifecycleManager(
    private val clientImpl: StreamVideoClient,
    private val sessionManager: CallSessionManager,
    private val scopeProvider: ScopeProvider,
    private val callRegistry: ClientCallRegistry,
    /** Cancels the call's supervisor job and scope once in-flight children complete. */
    private val shutDownJobs: () -> Unit,
    // Lazy providers: constructed after this component during Call initialization.
    private val stateProvider: () -> CallState,
    private val callAnalyticsProvider: () -> CallAnalytics,
    private val statsReporter: () -> CallStatsReporter,
    private val media: () -> CallMediaManager,
    private val sessionMonitor: () -> SessionMonitor,
    private val iceMonitor: () -> CallIceConnectionMonitor,
    private val connectivityMonitor: () -> CallConnectivityMonitor,
    private val type: String,
    private val id: String,
) {
    private val logger by taggedLogger("Call:LifecycleManager:$type:$id")

    private val cid = "$type:$id"
    private val state get() = stateProvider()
    private val callAnalytics get() = callAnalyticsProvider()

    // Atomic controls
    private var atomicLeave = AtomicUnitCall()

    /** Call has been left and the object is cleaned up and destroyed. */
    var isDestroyed = false

    /**
     * Time (in millis) when the full reconnection flow started. Will be null again once
     * the reconnection flow ends (success or failure)
     */
    private var sfuSocketReconnectionTime: Long? = null

    /** Resets the leave guard so a fresh join can run after a previous leave. */
    fun resetLeaveGuard() {
        atomicLeave = AtomicUnitCall()
    }

    fun leave(reason: CallLeaveReason) {
        logger.d { "[leave] #ringing; call_cid:$cid" }
        internalLeave(reason)
    }

    fun leave(reason: String = "user") {
        logger.d { "[leave] #ringing; no args, call_cid:$cid" }
        internalLeave(CallLeaveReason.Custom(reason))
    }

    private fun internalLeave(reason: CallLeaveReason) = atomicLeave {
        stopConnectionMonitors()
        callAnalytics.stopObservers()
        sessionMonitor().cancelSfuObservers()
        state._connection.value = RealtimeConnection.Disconnected
        logger.v { "[leave] #ringing; call_id = $id" }
        if (isDestroyed) {
            logger.w { "[leave] #ringing; Call already destroyed, ignoring" }
            return@atomicLeave
        }
        isDestroyed = true

        sfuSocketReconnectionTime = null

        /**
         * TODO Rahul, need to check which call has owned the media at the moment(probably use active call)
         */
        media().disableLocalCapture()

        callRegistry.detach()

        clientImpl.scope.launch {
            val leaveReason = "[reason=${reason::class.simpleName}, message=${reason.message}]"
            callAnalytics.onCallLeave(sessionManager.session, reason)
            safeCall {
                sessionManager.session.value?.sfuTracer?.trace("leave-call", leaveReason)
                val stats = statsReporter().collectStats()
                sessionManager.session.value?.sendCallStats(stats)
            }
            // Must complete before cleanup() cancels the session's supervisor job.
            safeCall { sessionManager.session.value?.sendLeaveEvent(leaveReason) }
            cleanup()
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

    fun cleanup() {
        // monitor.stop()
        state.cleanup()
        sessionManager.session.value?.cleanup()
        shutDownJobs()
        statsReporter().stop()
        media().cleanup() // TODO Rahul, Verify Later: need to check which call has owned the media at the moment(probably use active call)
        sessionManager.setActiveSession(null)
        // Cleanup the call's scope provider
        scopeProvider.cleanup()
    }

    /** Stops the ICE and connectivity monitors. */
    private fun stopConnectionMonitors() {
        iceMonitor().stop()
        connectivityMonitor().cancelLeaveTimeout()
        connectivityMonitor().unsubscribe()
    }
}
