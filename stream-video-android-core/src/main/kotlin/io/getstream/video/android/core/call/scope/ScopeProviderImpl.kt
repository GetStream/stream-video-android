/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.jvm.Throws

/**
 * Implementation of [ScopeProvider] that manages coroutine scopes for RTC sessions.
 *
 * This implementation ensures that:
 * - Each Call gets its own dedicated single thread executor
 * - RtcSessions use the thread from their parent Call
 * - Executors are created on demand and cleaned up with the Call
 * - Scopes are properly managed and cleaned up
 */
internal class ScopeProviderImpl(
    private val coroutineScope: CoroutineScope,
) : ScopeProvider {

    private val logger by taggedLogger("ScopeProviderImpl")
    private var executor: ExecutorService? = null
    private var isCleanedUp = false

    @Throws(IllegalStateException::class)
    override fun getCoroutineScope(supervisorJob: CompletableJob): CoroutineScope {
        // Future: We should not throw exception. Instead return Result.Fail
        check(!isCleanedUp) { "ScopeProvider has been cleaned up" }
        logger.d { "Creating coroutine scope for RTC session main" }
        return CoroutineScope(
            coroutineScope.coroutineContext +
                supervisorJob +
                CoroutineName("rtc-session-main"),
        )
    }

    @Throws(IllegalStateException::class)
    override fun getRtcSessionScope(supervisorJob: CompletableJob, callId: String): CoroutineScope {
        // Future: We should not throw exception. Instead return Result.Fail
        check(!isCleanedUp) { "ScopeProvider has been cleaned up" }
        logger.d { "Creating RTC session scope for callId: $callId" }

        // Get or create executor for this call
        if (executor == null) {
            logger.d { "Creating new single thread executor for callId: $callId" }
            executor = Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "rtc-call-$callId")
            }
        }

        val currentExecutor = executor
        return if (currentExecutor != null) {
            logger.d { "Creating RTC session scope with dedicated executor for callId: $callId" }
            CoroutineScope(
                coroutineScope.coroutineContext +
                    supervisorJob +
                    CoroutineName("rtc-session-coroutine") +
                    currentExecutor.asCoroutineDispatcher(),
            )
        } else {
            // Fallback to regular scope without executor if executor is null
            logger.w { "Executor is null, falling back to regular scope for callId: $callId" }
            CoroutineScope(
                coroutineScope.coroutineContext +
                    supervisorJob +
                    CoroutineName("rtc-session-coroutine"),
            )
        }
    }

    override fun cleanup() {
        if (isCleanedUp) return
        isCleanedUp = true
        logger.d { "Cleaning up ScopeProvider and shutting down executor" }

        // Shutdown the executor
        executor?.shutdown()
        executor = null
    }

    override fun reset() {
        logger.d { "Resetting ScopeProvider to allow reuse" }
        isCleanedUp = false
        // executor is already null after cleanup, will be recreated on next use
    }
}
