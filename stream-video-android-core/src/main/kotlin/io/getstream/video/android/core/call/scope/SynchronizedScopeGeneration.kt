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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

/**
 * Provides synchronized access to the resources that change between Call scope generations.
 *
 * Synchronization and Call scope creation are isolated here so [ScopeProviderImpl] can focus on
 * exposing scopes and applying cleanup and restart lifecycle operations.
 */
internal class SynchronizedScopeGeneration(
    private val parentCoroutineContext: CoroutineContext,
) {

    internal data class CallScopeGeneration(
        val debugInstanceId: Int,
        val callScope: CoroutineScope,
        val callJob: CompletableJob,
    )

    internal data class ExecutorAccess(
        val generation: CallScopeGeneration,
        val executor: ExecutorService,
        val created: Boolean,
    )

    internal data class CleanupAccess(
        val generation: CallScopeGeneration,
        val executor: ExecutorService?,
        val cleanedUpNow: Boolean,
    )

    private class CallScopeGenerationState(
        val generation: CallScopeGeneration,
    ) {
        val lock = Any()
        var executor: ExecutorService? = null
        var isCleanedUp: Boolean = false
    }

    private val restartLock = Any()
    private val debugInstanceCounter = AtomicInteger(0)
    private val current = AtomicReference(createGeneration())

    internal fun current(): CallScopeGeneration = current.get().generation

    internal fun requireActive(): CallScopeGeneration {
        val currentGeneration = current.get()
        return synchronized(currentGeneration.lock) {
            checkActive(currentGeneration)
            currentGeneration.generation
        }
    }

    internal fun getOrCreateExecutor(
        factory: () -> ExecutorService,
    ): ExecutorAccess {
        val currentGeneration = current.get()
        return synchronized(currentGeneration.lock) {
            checkActive(currentGeneration)
            val existingExecutor = currentGeneration.executor
            if (existingExecutor != null) {
                ExecutorAccess(
                    generation = currentGeneration.generation,
                    executor = existingExecutor,
                    created = false,
                )
            } else {
                val newExecutor = factory()
                currentGeneration.executor = newExecutor
                ExecutorAccess(
                    generation = currentGeneration.generation,
                    executor = newExecutor,
                    created = true,
                )
            }
        }
    }

    internal fun cleanup(): CleanupAccess {
        val currentGeneration = current.get()
        return synchronized(currentGeneration.lock) {
            if (currentGeneration.isCleanedUp) {
                return@synchronized CleanupAccess(
                    generation = currentGeneration.generation,
                    executor = null,
                    cleanedUpNow = false,
                )
            }

            currentGeneration.isCleanedUp = true
            val executor = currentGeneration.executor
            currentGeneration.executor = null
            CleanupAccess(
                generation = currentGeneration.generation,
                executor = executor,
                cleanedUpNow = true,
            )
        }
    }

    internal fun restart(): CallScopeGeneration = synchronized(restartLock) {
        val currentGeneration = current.get()
        synchronized(currentGeneration.lock) {
            check(currentGeneration.isCleanedUp) {
                "ScopeProvider cannot restart before its current generation is cleaned up"
            }
            createGeneration().also(current::set).generation
        }
    }

    private fun createGeneration(): CallScopeGenerationState {
        val callJob = SupervisorJob(parentCoroutineContext[Job])
        return CallScopeGenerationState(
            generation = CallScopeGeneration(
                debugInstanceId = debugInstanceCounter.incrementAndGet(),
                callScope = CoroutineScope(parentCoroutineContext + callJob),
                callJob = callJob,
            ),
        )
    }

    private fun checkActive(generation: CallScopeGenerationState) {
        check(!generation.isCleanedUp) { "ScopeProvider has been cleaned up" }
    }
}
