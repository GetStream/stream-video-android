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
import io.getstream.result.Result
import io.getstream.result.Result.Success
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.CopyOnWriteArrayList
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
    private val generationManager = CallScopeGenerationManager(
        parentCoroutineContext = coroutineScope.coroutineContext,
    )
    private val restartListeners = CopyOnWriteArrayList<() -> Unit>()

    internal val debugInstanceId: Int get() = generationManager.currentDebugInstanceId

    override val callScope: CoroutineScope
        get() = generationManager.currentCallScope

    @Throws(IllegalStateException::class)
    override fun getCoroutineScope(supervisorJob: CompletableJob): CoroutineScope {
        val debugInstanceId = generationManager.requireActive()
        logger.d { "${debugInstanceId.logPrefix()} Creating coroutine scope for RTC session main" }
        return CoroutineScope(
            coroutineScope.coroutineContext +
                supervisorJob +
                CoroutineName("rtc-session-main"),
        )
    }

    @Throws(IllegalStateException::class)
    override fun getRtcSessionScope(supervisorJob: CompletableJob, callId: String): CoroutineScope {
        val executor = generationManager.getOrCreateExecutor(callId)
        logger.d {
            "${generationManager.currentDebugInstanceId.logPrefix()} Creating RTC session scope with dedicated executor for callId: $callId"
        }
        return CoroutineScope(
            coroutineScope.coroutineContext +
                supervisorJob +
                CoroutineName("rtc-session-coroutine") +
                executor.asCoroutineDispatcher(),
        )
    }

    override fun cleanup(): Job? {
        val cleanupResult = generationManager.cleanup()
        if (cleanupResult.cleanedUpNow) {
            cleanupResult.callJob.cancel()
            logger.d {
                "${cleanupResult.debugInstanceId.logPrefix()} Cleaning up ScopeProvider and shutting down executor"
            }
            cleanupResult.executor?.shutdown()
        }
        return cleanupResult.callJob
    }

    override fun restart(): Result<Unit> = generationManager.restart().also { result ->
        if (result is Success) {
            logger.d {
                "${generationManager.currentDebugInstanceId.logPrefix()} Restarted ScopeProvider"
            }
            restartListeners.forEach { it() }
        }
    }

    internal fun onRestart(listener: () -> Unit) {
        restartListeners += listener
    }

    private fun Int.logPrefix(): String = "[debug-instance-$this]"
}
