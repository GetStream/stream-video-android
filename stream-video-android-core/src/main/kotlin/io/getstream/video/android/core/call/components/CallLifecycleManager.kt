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
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallLeaveReason
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.SdkCause
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.internal.telecom.TelecomCallController
import io.getstream.video.android.core.utils.AtomicUnitCall
import io.getstream.video.android.core.utils.safeCall
import kotlinx.coroutines.launch

/**
 * Owns the call's lifecycle / teardown for a [Call]: leaving, ending, the single-shot
 * leave guard ([AtomicUnitCall]), the destroyed flag, and the ordered cleanup of state,
 * session, jobs and media.
 */
internal class CallLifecycleManager(
    private val call: Call,
) {
    private val logger by taggedLogger("Call:LifecycleManager:${call.type}:${call.id}")

    private val clientImpl get() = call.clientImpl
    private val state get() = call.state
    private val session get() = call.session
    private val callAnalytics get() = call.callAnalytics

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
        logger.d { "[leave] #ringing; call_cid:${call.cid}" }
        internalLeave(reason)
    }

    fun leave(reason: String = "user") {
        logger.d { "[leave] #ringing; no args, call_cid:${call.cid}" }
        internalLeave(CallLeaveReason.Custom(reason))
    }

    private fun internalLeave(reason: CallLeaveReason) = atomicLeave {
        call.stopConnectionMonitors()
        callAnalytics.stopObservers()
        call.cancelSfuObservers()
        state._connection.value = RealtimeConnection.Disconnected
        logger.v { "[leave] #ringing; call_id = ${call.id}" }
        if (isDestroyed) {
            logger.w { "[leave] #ringing; Call already destroyed, ignoring" }
            return@atomicLeave
        }
        isDestroyed = true

        sfuSocketReconnectionTime = null

        /**
         * TODO Rahul, need to check which call has owned the media at the moment(probably use active call)
         */
        call.stopScreenSharing()
        call.camera.disable()
        call.microphone.disable()

        if (call.id == call.client.state.activeCall.value?.id) {
            call.client.state.removeActiveCall(call) // Will also stop CallService
        }

        if (call.id == call.client.state.ringingCall.value?.id) {
            call.client.state.removeRingingCall(call)
        }

        TelecomCallController(call.client.context)
            .leaveCall(call)

        (call.client as StreamVideoClient).onCallCleanUp(call)

        clientImpl.scope.launch {
            val leaveReason = "[reason=${reason::class.simpleName}, message=${reason.message}]"
            callAnalytics.onCallLeave(session, reason)
            safeCall {
                session.value?.sfuTracer?.trace("leave-call", leaveReason)
                val stats = call.collectStats()
                session.value?.sendCallStats(stats)
            }
            // Must complete before cleanup() cancels the session's supervisor job.
            safeCall { session.value?.sendLeaveEvent(leaveReason) }
            cleanup()
        }
    }

    /** ends the call for yourself as well as other users */
    suspend fun end(): Result<Unit> {
        // end the call for everyone
        val result = clientImpl.endCall(call.type, call.id)
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
        session.value?.cleanup()
        call.shutDownJobsGracefully()
        call.stopStatsReporting()
        call.mediaManager.cleanup() // TODO Rahul, Verify Later: need to check which call has owned the media at the moment(probably use active call)
        session.value = null
        // Cleanup the call's scope provider
        call.scopeProvider.cleanup()
    }
}
