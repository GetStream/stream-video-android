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

import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

/** Manages the state and lifecycle transitions of the current Call coroutine scope generation. */
internal class CallScopeGenerationManager(
    private val parentCoroutineContext: CoroutineContext,
) {

    private class GenerationState(
        val debugInstanceId: Int,
        val callScope: CoroutineScope,
        val callJob: CompletableJob,
    ) {
        var executor: ExecutorService? = null
        var isCleanedUp: Boolean = false
    }

    /** Resources detached from a generation when cleanup changes it to the cleaned state. */
    internal data class CleanupResult(
        val debugInstanceId: Int,
        val callJob: Job,
        val executor: ExecutorService?,
        val cleanedUpNow: Boolean,
    )

    private val lifecycleLock = Any()
    private val debugInstanceCounter = AtomicInteger(0)
    private val current = AtomicReference(createGeneration())

    internal val currentCallScope: CoroutineScope
        get() = current.get().callScope

    internal val currentDebugInstanceId: Int
        get() = current.get().debugInstanceId

    internal fun requireActive(): Int = synchronized(lifecycleLock) {
        current.get().also(::checkActive).debugInstanceId
    }

    internal fun getOrCreateExecutor(
        callId: String,
    ): ExecutorService = synchronized(lifecycleLock) {
        val generation = current.get().also(::checkActive)
        generation.executor ?: Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "rtc-call-$callId")
        }.also {
            generation.executor = it
        }
    }

    internal fun cleanup(): CleanupResult = synchronized(lifecycleLock) {
        val generation = current.get()
        if (generation.isCleanedUp) {
            CleanupResult(
                debugInstanceId = generation.debugInstanceId,
                callJob = generation.callJob,
                executor = null,
                cleanedUpNow = false,
            )
        } else {
            generation.isCleanedUp = true
            val executor = generation.executor
            generation.executor = null
            CleanupResult(
                debugInstanceId = generation.debugInstanceId,
                callJob = generation.callJob,
                executor = executor,
                cleanedUpNow = true,
            )
        }
    }

    internal fun restart(): Int = synchronized(lifecycleLock) {
        check(current.get().isCleanedUp) {
            "ScopeProvider cannot restart before its current generation is cleaned up"
        }
        createGeneration().also(current::set).debugInstanceId
    }

    private fun createGeneration(): GenerationState {
        val callJob = SupervisorJob(parentCoroutineContext[Job])
        return GenerationState(
            debugInstanceId = debugInstanceCounter.incrementAndGet(),
            callScope = CoroutineScope(parentCoroutineContext + callJob),
            callJob = callJob,
        )
    }

    private fun checkActive(generation: GenerationState) {
        check(!generation.isCleanedUp) { "ScopeProvider has been cleaned up" }
    }
}
