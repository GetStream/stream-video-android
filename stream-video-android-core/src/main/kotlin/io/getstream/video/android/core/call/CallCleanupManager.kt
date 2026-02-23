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
import io.getstream.video.android.core.socket.common.scope.ClientScope
import io.getstream.video.android.core.socket.common.scope.UserScope
import io.getstream.video.android.core.utils.safeCall
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlin.sequences.forEach

/**
 * Manages call cleanup
 */
internal class CallCleanupManager(
    private val call: Call,
    private val sessionManager: CallSessionManager,
    private val callApiDelegate: CallApiDelegate,
    private val client: StreamVideoClient,
    private val callReInitializer: CallReInitializer,
    private val mediaManagerProvider: () -> MediaManagerImpl, // ← Lambda provider
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

        val currentSession = sessionManager.session.get()
        // Leave session
        currentSession?.leaveWithReason(
            "[reason=$reason, error=${disconnectionReason?.message}]",
        )

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
        callApiDelegate.stopScreenSharing()
        with(mediaManager) {
            camera.disable()
            microphone.disable()
        }

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
                currentSession?.let { session ->
                    with(session) {
                        sfuTracer.trace(
                            "leave-call",
                            "[reason=$reason, error=${disconnectionReason?.message}]",
                        )
                        session.sendCallStats(callStatsReporter.collectStats(session))
                    }
                }
            }
            cleanup()
        }
        with(callReInitializer) {
            cleanupJobReference(newCleanupJob)
            cleanupLockVars(newCleanupJob)
        }
    }

    internal fun cleanup() {
        logger.d { "[cleanup] Starting cleanup" }

        sessionManager.cleanup()
        shutDownJobsGracefully()
        callStatsReporter.cancelJobs()

        // Access mediaManager through lazy provider
        cleanupMedia()
        call.scopeProvider.cleanup()
        logger.d { "[cleanup] Cleanup complete" }
    }

    fun cleanupMedia() {
        mediaManager.cleanup()
    }

    internal fun shutDownJobsGracefully() {
        UserScope(ClientScope()).launch {
            callReInitializer.currentSupervisorJob.children.forEach { it.join() }
            callReInitializer.currentSupervisorJob.cancel()
        }
        callReInitializer.currentScope.cancel()
    }
}
