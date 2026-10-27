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

import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import kotlinx.coroutines.Job
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Synchronizes the lifecycle boundary required to reuse a
 * [io.getstream.video.android.core.Call] after it has been left.
 *
 * `Call.leave()` returns while cleanup continues asynchronously, and the same instance may receive
 * another `Call.join()` immediately afterward. The cancelled call scope cannot be reused, and the
 * new scope and RTC session must not start until the old cleanup job and call job have both finished.
 *
 * This class coordinates exactly these lifecycle operations:
 * - `Call.leave()` through [startLeave], making repeated leave requests idempotent and retaining the
 *   asynchronous cleanup job.
 * - [CallLifecycleManager.cleanup] through [cleanupOnce] and [recordCancelledCallJob], ensuring
 *   terminal cleanup runs once and exposing the cancelled call job that still needs to finish.
 * - `Call.join()` through [prepareForJoin], waiting for both jobs before restarting the call scope
 *   and session-specific state.
 *
 * Cache access through `StreamVideo.call(type, id)` is coordinated separately by
 * `StreamVideoClient`; this class only owns synchronization between leave, cleanup, and rejoin for
 * one retained `Call` instance.
 */
internal class CallCleanupState {
    private companion object {
        private const val CLEANUP_TIMEOUT_MS = 30_000L
    }

    /**
     * The cleanup jobs that this join preparation actually waited for.
     *
     * [prepareForJoin] reads and waits for [cleanup] first. That job records [cancelledCall] while
     * releasing the previous call scope, so the cancelled job is read only after cleanup finishes.
     * The pair is then compared with the stored jobs to detect a genuinely newer leave, cleanup, or
     * concurrent restart.
     *
     * @property cleanup the asynchronous leave and cleanup work started by [startLeave].
     * @property cancelledCall the cancelled call-scope job whose children and `finally` blocks may
     *   still be finishing.
     */
    private data class PendingJobs(
        val cleanup: Job?,
        val cancelledCall: Job?,
    )

    /**
     * Protects transitions involving multiple lifecycle fields.
     *
     * Individual atomic or volatile fields would not make compound operations such as capturing
     * both pending jobs or clearing all state after a successful restart atomic.
     */
    private val lock = Any()

    /**
     * The asynchronous job created when `Call.leave()` starts.
     *
     * Because leave returns before cleanup finishes, [prepareForJoin] retains and waits for this job
     * before attempting to restart the same Call instance.
     */
    private var cleanupJob: Job? = null

    /**
     * The job backing the previous call coroutine scope after that scope has been cancelled.
     *
     * Cancellation is not completion: child coroutines and `finally` blocks may still be running,
     * so [prepareForJoin] waits for this job before creating the next call lifecycle.
     */
    private var cancelledCallJob: Job? = null

    /**
     * Whether terminal cleanup has already run for the current Call lifecycle.
     *
     * This makes repeated calls to [CallLifecycleManager.cleanup] idempotent. A successful
     * [prepareForJoin] resets it for the newly created lifecycle, so a later cleanup is respected.
     */
    private var cleanedUp = false

    /**
     * Terminal marker for the current Call lifecycle.
     *
     * Both `Call.leave()` through [startLeave] and direct cleanup through [cleanupOnce] set this to
     * true. It remains true while asynchronous cleanup finishes and if restart fails. Only a later
     * successful [prepareForJoin] clears it for the newly created lifecycle. A leave or cleanup
     * invoked after that restart sets it to true again and therefore remains the final action.
     */
    private val destroyed = AtomicBoolean(false)

    /** True after leave starts and until cleanup completes and the Call is restarted. */
    val isDestroyed: Boolean get() = destroyed.get()

    /** Runs leave setup once and stores the asynchronous cleanup job before releasing the lock. */
    fun startLeave(createCleanupJob: () -> Job) {
        synchronized(lock) {
            if (!destroyed.compareAndSet(false, true)) return
            cleanupJob = createCleanupJob()
        }
    }

    /** Runs terminal cleanup once and marks the current Call lifecycle as destroyed. */
    fun cleanupOnce(cleanup: () -> Unit) {
        synchronized(lock) {
            if (cleanedUp) return
            cleanedUp = true
            destroyed.set(true)
            cleanup()
        }
    }

    /** Records the cancelled Call job while [cleanupOnce] still owns the cleanup lock. */
    fun recordCancelledCallJob(job: Job?) {
        synchronized(lock) {
            cancelledCallJob = job
        }
    }

    /**
     * Waits for cleanup and cancellation, then atomically restores the active lifecycle state.
     *
     * ```text
     * Call.join()
     *     ↓
     * Read leave cleanup job
     *     ↓
     * Wait for leave cleanup
     *     ↓
     * Read old call scope job
     *     ↓
     * Wait for old call scope to fully finish
     *     ↓
     * Check that no newer cleanup started
     *     ↓
     * Restart destroyed Call
     *     ↓
     * Allow join to continue
     * ```
     */
    suspend fun prepareForJoin(restart: () -> Result<Unit>): Result<Unit> {
        val pending = withTimeoutOrNull(CLEANUP_TIMEOUT_MS) {
            val cleanup = synchronized(lock) { cleanupJob }
            cleanup?.join()

            val cancelledCall = synchronized(lock) { cancelledCallJob }
            cancelledCall?.join()

            PendingJobs(cleanup, cancelledCall)
        } ?: return Failure(
            Error.GenericError(
                "Timed out waiting for the previous Call cleanup to finish.",
            ),
        )

        return synchronized(lock) {
            if (cleanupJob !== pending.cleanup || cancelledCallJob !== pending.cancelledCall) {
                return@synchronized if (destroyed.get()) {
                    Failure(
                        Error.GenericError(
                            "The Call was left or cleaned up while preparing to join.",
                        ),
                    )
                } else {
                    // Another concurrent join already restarted this Call.
                    Success(Unit)
                }
            }

            if (!destroyed.get()) return@synchronized Success(Unit)

            val result = restart()
            if (result !is Success) return@synchronized result

            destroyed.set(false)
            cleanupJob = null
            cancelledCallJob = null
            cleanedUp = false
            Success(Unit)
        }
    }
}
