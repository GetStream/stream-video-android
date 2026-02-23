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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.let
import kotlin.takeIf

internal class CallReInitializer(
    val clientScope: CoroutineScope,
    val onReInitialise: () -> Unit,
) {

    private val logger by taggedLogger("CallConcurrencyManager")
    internal val cleanupMutex = Mutex()
    internal var cleanupJob: Job? = null

    internal var needsReinitialization = AtomicBoolean(false) // TODO Rahul should be atomic

    @Volatile
    internal var currentSupervisorJob: Job = SupervisorJob()

    @Volatile
    internal var currentScope: CoroutineScope =
        CoroutineScope(clientScope.coroutineContext + currentSupervisorJob)

    internal suspend fun waitFromCleanup() {
        val job = cleanupMutex.withLock {
            cleanupJob?.takeIf { it.isActive }
        }
        job?.let {
            logger.d { "[join] Waiting for cleanup job: $it" }
            try {
                withTimeout(5000) { it.join() }
                logger.d { "[join] Cleanup complete" }
            } catch (e: TimeoutCancellationException) {
                logger.w { "[join] Cleanup timeout, proceeding anyway" }
            }

            cleanupMutex.withLock {
                if (cleanupJob == it) {
                    cleanupJob = null
                }
            }
        }
    }

    internal suspend fun reinitialiseCoroutinesIfNeeded() {
        val needsReinit = cleanupMutex.withLock {
            if (needsReinitialization.get()) {
                needsReinitialization.set(false)
                true
            } else {
                false
            }
        }

        if (needsReinit) {
            reinitializeCoroutines()
        }
    }

    internal fun reinitializeCoroutines() {
        synchronized(this) {
            currentSupervisorJob = SupervisorJob()
            currentScope = CoroutineScope(
                clientScope.coroutineContext + currentSupervisorJob,
            )
            onReInitialise()
        }
    }

    internal fun cleanupJobReference(newCleanupJob: Job) {
        newCleanupJob.invokeOnCompletion {
            currentScope.launch {
                cleanupMutex.withLock {
                    if (newCleanupJob == cleanupJob) {
                        cleanupJob = null
                        logger.v { "[cleanupJobReference] Cleared job reference" }
                    }
                }
            }
        }
    }

    internal fun cleanupLockVars(newCleanupJob: Job) {
        currentScope.launch {
            cleanupMutex.withLock {
                needsReinitialization.set(true)
                cleanupJob = newCleanupJob // TODO Rahul, I cannot understand why it is assigned
                logger.d { "[cleanupLockVars] Cleanup job assigned" }
            }
        }
    }
}
