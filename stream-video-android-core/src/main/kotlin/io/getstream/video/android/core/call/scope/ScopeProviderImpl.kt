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

import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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

    private var executor: ExecutorService? = null
    private var isCleanedUp = false

    override fun getCoroutineScope(supervisorJob: CompletableJob): CoroutineScope {
        check(!isCleanedUp) { "ScopeProvider has been cleaned up" }
        return CoroutineScope(
            coroutineScope.coroutineContext +
                supervisorJob +
                CoroutineName("rtc-session-main"),
        )
    }

    override fun getRtcSessionScope(supervisorJob: CompletableJob, callId: String): CoroutineScope {
        check(!isCleanedUp) { "ScopeProvider has been cleaned up" }

        // Get or create executor for this call
        if (executor == null) {
            executor = Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "rtc-call-$callId")
            }
        }

        val currentExecutor = executor ?: error("Executor should not be null at this point")
        return CoroutineScope(
            coroutineScope.coroutineContext +
                supervisorJob +
                CoroutineName("rtc-session-coroutine") +
                currentExecutor.asCoroutineDispatcher(),
        )
    }

    override fun cleanup() {
        if (isCleanedUp) return
        isCleanedUp = true

        // Shutdown the executor
        executor?.shutdown()
        executor = null
    }
}
