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
import io.getstream.result.Result
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CreateCallOptions
import io.getstream.video.android.core.MediaManagerImpl
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.utils.AtomicUnitCall
import io.getstream.video.android.core.utils.safeCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.util.UUID

/**
 * Manages the lifecycle of a Call - joining, leaving, and cleanup.
 */
internal class CallLifecycleManager(
    private val call: Call,
    private val sessionManager: CallSessionManager,
    private val callLocks: CallLocks,
    private val mediaManagerProvider: () -> MediaManagerImpl, // ← Lambda provider
    private val clientScope: CoroutineScope,
) {
    private val logger by taggedLogger("CallLifecycleManager")
    private var hasBeenLeft = false
    private val mediaManager: MediaManagerImpl by lazy {
        mediaManagerProvider()
    }

    val lifecycleScope: CoroutineScope get() = callLocks.currentScope

    fun leave(reason: String = "user") {
        logger.d { "[leave] reason: $reason" }

        callLocks.currentScope.launch {
            val shouldProceed = callLocks.cleanupMutex.withLock {
                val currentJob = callLocks.cleanupJob
                if (currentJob?.isActive == true) {
                    logger.w {
                        "[leave] Cleanup already in progress (job: $currentJob), " +
                            "ignoring duplicate leave call"
                    }
                    false
                } else {
                    logger.v { "[leave] No active cleanup, proceeding with leave" }
                    true
                }
            }

            if (shouldProceed) {
                performLeave(reason)
            }
        }
    }

    private fun performLeave(reason: String) {
        logger.d { "[performLeave] Starting leave process" }

        // All immediate cleanup operations
        call.stopScreenSharing()

        // Access mediaManager through lazy provider - only initialized if needed
        mediaManager.camera.disable()
        mediaManager.microphone.disable()

        // Launch cleanup job
        val newCleanupJob = clientScope.launch(Dispatchers.IO) {
            logger.d { "[cleanupJob] Starting" }

            safeCall {
                sessionManager.session?.sfuTracer?.trace("leave-call", "[reason=$reason]")
                val stats = call.collectStats()
                sessionManager.session?.sendCallStats(stats)
            }

            cleanup()

            logger.d { "[cleanupJob] Complete" }
        }

        // Clear job reference when it completes
        newCleanupJob.invokeOnCompletion {
            callLocks.currentScope.launch {
                callLocks.cleanupMutex.withLock {
                    if (callLocks.cleanupJob == newCleanupJob) {
                        callLocks.cleanupJob = null
                        logger.v { "[cleanupJob] Cleared job reference" }
                    }
                }
            }
        }

        // Thread-safe assignment
        callLocks.currentScope.launch {
            callLocks.cleanupMutex.withLock {
                hasBeenLeft = true
                callLocks.cleanupJob = newCleanupJob
                logger.d { "[performLeave] Cleanup job assigned" }
            }
        }

        logger.d { "[performLeave] Leave complete, cleanup in background" }
    }

    suspend fun join(
        create: Boolean = false,
        createOptions: CreateCallOptions? = null,
        ring: Boolean = false,
        notify: Boolean = false,
    ): Result<RtcSession> {
        logger.d { "[join] Starting join" }

        // Wait for cleanup
        val job = callLocks.cleanupMutex.withLock { callLocks.cleanupJob?.takeIf { it.isActive } }

        job?.let {
            logger.d { "[join] Waiting for cleanup job: $it" }
            try {
                withTimeout(5000) { it.join() }
                logger.d { "[join] Cleanup complete" }
            } catch (e: TimeoutCancellationException) {
                logger.w { "[join] Cleanup timeout, proceeding anyway" }
            }

            callLocks.cleanupMutex.withLock {
                if (callLocks.cleanupJob == it) {
                    callLocks.cleanupJob = null
                }
            }
        }

        // Reinitialize if needed
        val needsReinit = callLocks.cleanupMutex.withLock {
            if (hasBeenLeft) {
                hasBeenLeft = false
                true
            } else {
                false
            }
        }

        if (needsReinit) {
            logger.d { "[join] Reinitializing for rejoin" }
            reinitialize()
        }

        // Delegate to session manager for actual join
        return sessionManager.join(create, createOptions, ring, notify)
    }

    private fun reinitialize() {
        synchronized(this) {
            val oldSessionId = call.sessionId
            val oldSupervisorJob = callLocks.currentSupervisorJob

            logger.d {
                "[reinitialize] Starting reinitialization. " +
                    "oldSessionId: $oldSessionId"
            }

            // Recreate coroutine infrastructure
            callLocks.currentSupervisorJob = SupervisorJob()
            callLocks.currentScope = CoroutineScope(
                clientScope.coroutineContext + callLocks.currentSupervisorJob,
            )

            // Generate new session ID
            call.sessionId = UUID.randomUUID().toString()

            // Reset connection state
            call.state._connection.value = RealtimeConnection.Disconnected

            // Reset atomic leave guard
            call.atomicLeave = AtomicUnitCall()

            logger.i {
                "[reinitialize] ✓ Reinitialization complete. " +
                    "oldSessionId: $oldSessionId → newSessionId: ${call.sessionId}, " +
                    "oldJob cancelled: ${oldSupervisorJob.isCancelled}"
            }
        }
    }

    internal fun cleanup() {
        logger.d { "[cleanup] Starting cleanup" }

        sessionManager.cleanup()
        shutDownJobsGracefully()
        sessionManager.callStatsReportingJob?.cancel()

        // Access mediaManager through lazy provider
        mediaManager.cleanup()

        call.scopeProvider.cleanup()

        logger.d { "[cleanup] Cleanup complete" }
    }

    private fun shutDownJobsGracefully() {
        clientScope.launch {
            callLocks.currentSupervisorJob.children.forEach { it.join() }
            callLocks.currentSupervisorJob.cancel()
        }
        callLocks.currentScope.cancel()
    }
}
