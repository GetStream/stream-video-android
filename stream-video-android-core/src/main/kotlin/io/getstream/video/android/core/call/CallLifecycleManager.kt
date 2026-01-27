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

package io.getstream.video.android.core.call

import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.MediaManagerImpl
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.internal.telecom.TelecomCallController
import io.getstream.video.android.core.utils.safeCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

/**
 * Manages the lifecycle of a Call - joining, leaving, and cleanup.
 */
internal class CallLifecycleManager(
    private val call: Call,
    private val sessionManager: CallSessionManager,
    private val client: StreamVideoClient,
    private val callReInitializer: CallReInitializer,
    private val mediaManagerProvider: () -> MediaManagerImpl, // ← Lambda provider
    private val clientScope: CoroutineScope,
    private val callStatsReporter: CallStatsReporter,
) {
    private val logger by taggedLogger("CallLifecycleManager")
    private val mediaManager: MediaManagerImpl by lazy {
        mediaManagerProvider()
    }

    fun leave(reason: String = "user") {
        logger.d { "[leave] #ringing; no args, call_cid:${call.cid}" }

        callReInitializer.currentScope.launch {
            val shouldProceed = !isCleanupInProgress()
            if (shouldProceed) {
                internalLeave(null, reason)
            }
        }
    }

    private suspend fun isCleanupInProgress(): Boolean {
        return callReInitializer.cleanupMutex.withLock {
            val currentJob = callReInitializer.cleanupJob
            if (currentJob?.isActive == true) {
                logger.w {
                    "[isCleanupInProgress] Cleanup already in progress (job: $currentJob), " +
                        "ignoring duplicate leave call"
                }
                true
            } else {
                logger.v { "[isCleanupInProgress] No active cleanup, proceeding with leave" }
                false
            }
        }
    }

    private fun internalLeave(disconnectionReason: Throwable?, reason: String) = call.atomicLeave {
        sessionManager.cleanupMonitor()

        // Leave session
        sessionManager.session?.leaveWithReason(
            "[reason=$reason, error=${disconnectionReason?.message}]",
        )

        // Cancel network monitoring
        sessionManager.cleanupNetworkMonitoring()

        // Update connection state
        call.state._connection.value = RealtimeConnection.Disconnected

        logger.v { "[leave] #ringing; disconnectionReason: $disconnectionReason, call_id = ${call.id}" }
        if (call.isDestroyed.get()) {
            logger.w { "[leave] #ringing; Call already destroyed, ignoring" }
            return@atomicLeave
        }
        call.isDestroyed.set(true)

        /**
         * TODO Rahul, need to check which call has owned the media at the moment(probably use active call)
         */
        call.stopScreenSharing()
        mediaManager.camera.disable()
        mediaManager.microphone.disable()

        if (call.id == client.state.activeCall.value?.id) {
            client.state.removeActiveCall(call) // Will also stop CallService
        }

        if (call.id == client.state.ringingCall.value?.id) {
            client.state.removeRingingCall(call)
        }

        TelecomCallController(client.context)
            .leaveCall(call)

        client.onCallCleanUp(call)

        val newCleanupJob = client.scope.launch {
            safeCall {
                sessionManager.session?.sfuTracer?.trace(
                    "leave-call",
                    "[reason=$reason, error=${disconnectionReason?.message}]",
                )
                val stats = callStatsReporter.collectStats(sessionManager.session)
                sessionManager.session?.sendCallStats(stats)
            }
            cleanup()
        }
        callReInitializer.cleanupJobReference(newCleanupJob)
        callReInitializer.cleanupLockVars(newCleanupJob)
    }

    internal fun cleanup() {
        logger.d { "[cleanup] Starting cleanup" }

        sessionManager.cleanup()
        shutDownJobsGracefully()
//        sessionManager.callStatsReportingJob?.cancel() //TODO Rahul, check its usage

        // Access mediaManager through lazy provider
        mediaManager.cleanup()
        call.scopeProvider.cleanup()
        logger.d { "[cleanup] Cleanup complete" }
    }

    private fun shutDownJobsGracefully() {
        clientScope.launch {
            callReInitializer.currentSupervisorJob.children.forEach { it.join() }
            callReInitializer.currentSupervisorJob.cancel()
        }
        callReInitializer.currentScope.cancel()
    }
}
