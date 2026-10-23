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

import kotlinx.coroutines.Job

/** Stores and synchronizes the cleanup state shared by leave, cleanup and the next join. */
internal class CallCleanupState {
    private data class PendingJobs(
        val cleanup: Job?,
        val cancelledCall: Job?,
    )

    private val lock = Any()
    private var cleanupJob: Job? = null
    private var cancelledCallJob: Job? = null
    private var cleanedUp = false

    @Volatile
    private var destroyed = false

    /** True after leave starts and until cleanup completes and the Call is restarted. */
    val isDestroyed: Boolean get() = destroyed

    /** Runs leave setup once and stores the asynchronous cleanup job before releasing the lock. */
    fun startLeave(createCleanupJob: () -> Job) {
        synchronized(lock) {
            if (destroyed) return
            destroyed = true
            cleanupJob = createCleanupJob()
        }
    }

    /** Runs terminal cleanup once while keeping its job bookkeeping atomic with joining. */
    fun cleanupOnce(cleanup: () -> Unit) {
        synchronized(lock) {
            if (cleanedUp) return
            cleanedUp = true
            destroyed = true
            cleanup()
        }
    }

    /** Records the cancelled Call job while [cleanupOnce] still owns the cleanup lock. */
    fun recordCancelledCallJob(job: Job?) {
        synchronized(lock) {
            cancelledCallJob = job
        }
    }

    /** Waits for cleanup and cancellation, then atomically restores the active lifecycle state. */
    suspend fun prepareForJoin(restart: () -> Unit) {
        while (true) {
            val pending = synchronized(lock) {
                PendingJobs(cleanupJob, cancelledCallJob)
            }
            pending.cleanup?.join()
            pending.cancelledCall?.join()

            synchronized(lock) {
                if (cleanupJob !== pending.cleanup || cancelledCallJob !== pending.cancelledCall) {
                    return@synchronized
                }
                if (destroyed) {
                    restart()
                    destroyed = false
                    cleanupJob = null
                    cancelledCallJob = null
                    cleanedUp = false
                }
                return
            }
        }
    }
}
