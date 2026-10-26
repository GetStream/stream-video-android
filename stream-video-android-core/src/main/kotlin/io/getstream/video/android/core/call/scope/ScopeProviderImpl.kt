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

package io.getstream.video.android.core.call.scope

import io.getstream.log.taggedLogger
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import kotlin.jvm.Throws

/**
 * Coroutine-scope owner for a reusable Call and its RTC session.
 *
 * This implementation ensures that:
 * - Each generation has its own call [Job]
 * - Each Call gets its own dedicated single thread executor
 * - RtcSessions use the thread from their parent Call
 * - Cleanup cancels the current generation; a later join restarts this provider
 */
internal class ScopeProviderImpl(
    private val coroutineScope: CoroutineScope,
    private val callCid: String,
) : ScopeProvider {

    private val logger by taggedLogger("ScopeProvider:$callCid")
    private val generation = SynchronizedScopeGeneration(
        parentCoroutineContext = coroutineScope.coroutineContext,
    )
    private val restartListeners = CopyOnWriteArrayList<() -> Unit>()

    internal val debugInstanceId: Int get() = generation.current().debugInstanceId

    override val callScope: CoroutineScope
        get() = generation.current().callScope

    @Throws(IllegalStateException::class)
    override fun getCoroutineScope(supervisorJob: CompletableJob): CoroutineScope {
        val currentGeneration = generation.requireActive()
        logger.d { "${currentGeneration.logPrefix()} Creating coroutine scope for RTC session main" }
        return CoroutineScope(
            coroutineScope.coroutineContext +
                supervisorJob +
                CoroutineName("rtc-session-main"),
        )
    }

    @Throws(IllegalStateException::class)
    override fun getRtcSessionScope(supervisorJob: CompletableJob, callId: String): CoroutineScope {
        val executorAccess = generation.getOrCreateExecutor {
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "rtc-call-$callId")
            }
        }
        if (executorAccess.created) {
            logger.d {
                "${executorAccess.generation.logPrefix()} Creating new single thread executor for callId: $callId"
            }
        }
        logger.d {
            "${executorAccess.generation.logPrefix()} Creating RTC session scope with dedicated executor for callId: $callId"
        }
        return CoroutineScope(
            coroutineScope.coroutineContext +
                supervisorJob +
                CoroutineName("rtc-session-coroutine") +
                executorAccess.executor.asCoroutineDispatcher(),
        )
    }

    override fun cleanup(): Job? {
        val cleanupAccess = generation.cleanup()
        if (cleanupAccess.cleanedUpNow) {
            cleanupAccess.generation.callJob.cancel()
            logger.d {
                "${cleanupAccess.generation.logPrefix()} Cleaning up ScopeProvider and shutting down executor"
            }
            cleanupAccess.executor?.shutdown()
        }
        return cleanupAccess.generation.callJob
    }

    override fun restart() {
        val nextGeneration = generation.restart()
        logger.d { "${nextGeneration.logPrefix()} Restarted ScopeProvider" }
        restartListeners.forEach { it() }
    }

    internal fun onRestart(listener: () -> Unit) {
        restartListeners += listener
    }

    private fun SynchronizedScopeGeneration.CallScopeGeneration.logPrefix(): String =
        "[debug-instance-$debugInstanceId]"
}
