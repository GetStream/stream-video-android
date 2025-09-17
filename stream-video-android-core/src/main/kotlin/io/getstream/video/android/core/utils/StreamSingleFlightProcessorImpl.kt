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

package io.getstream.video.android.core.utils

// package io.getstream.android.core.internal.processing

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ClosedSendChannelException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * SingleFlight implementation that coalesces concurrent calls by key.
 *
 * The shared work runs in [scope] (recommend a `CoroutineScope(SupervisorJob() + Dispatchers.IO)`),
 * so cancelling one awaiting caller does not cancel the shared execution.
 */
internal class StreamSingleFlightProcessorImpl(
    private val scope: CoroutineScope,
    private val flights: ConcurrentMap<Any, Deferred<Result<*>>> = ConcurrentHashMap(),
) {

    private val closed = AtomicBoolean(false)

    @Suppress("UNCHECKED_CAST")
    suspend fun <T> run(key: String, block: suspend () -> T): Result<T> {
        if (closed.get()) {
            return Result.failure(ClosedSendChannelException("SingleFlight is closed"))
        }

        // Fast path: already in flight
        flights[key]?.let { existing ->
            return try {
                existing.await() as Result<T>
            } catch (t: Throwable) {
                // If the deferred itself was cancelled, surface as failure Result
                Result.failure(t)
            }
        }

        // Create the shared execution (lazy): only the winner starts it.
        val newExecution =
            scope.async(start = CoroutineStart.LAZY) {
                // Always produce a Result (success/failure). Awaiters never throw for block
                // failures.
                try {
                    Result.success(block())
                } catch (ce: CancellationException) {
                    Result.failure(ce)
                } catch (t: Throwable) {
                    Result.failure(t)
                }
            }

        // Install ours or join the winner
        val existing = flights.putIfAbsent(key, newExecution)
        val job = existing ?: newExecution.also { it.start() }

        return try {
            job.await() as Result<T>
        } catch (t: Throwable) {
            Result.failure(t)
        } finally {
            // Remove only if this exact job is still mapped
            flights.remove(key, job)
        }
    }

    fun <T> has(key: String): Boolean {
        return flights.containsKey(key)
    }

    fun <T> cancel(key: String): Result<Unit> = runCatching {
        flights[key]?.cancel()
    }

    fun clear(cancelRunning: Boolean): Result<Unit> = runCatching {
        if (cancelRunning) {
            // Cancel current refs; we don't await. Callers will see failure on await.
            flights.values.forEach { it.cancel() }
        }
        flights.clear()
    }

    fun stop(): Result<Unit> = runCatching {
        // Idempotent: first caller flips to closed and cancels all; others no-op
        if (closed.compareAndSet(false, true)) {
            clear(cancelRunning = true).getOrThrow()
        }
    }
}
