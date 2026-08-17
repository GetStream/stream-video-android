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

package io.getstream.video.android.core.utils

// package io.getstream.android.core.internal.processing

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Single-flight that coalesces concurrent calls by key, runs the shared work on [scope],
 * and tracks how many callers are still awaiting.
 *
 * Compared to [StreamSingleFlightProcessorImpl]:
 * - Cancelling **one** waiter does **not** cancel the shared job (work is owned by [scope]).
 * - Cancelling the **last** waiter **does** cancel the shared job (sole-caller cancel still
 *   aborts the operation).
 * - [CancellationException] is rethrown to the cancelled waiter instead of being wrapped in
 *   [Result.failure].
 *
 * Use this when the operation should survive Activity/ViewModel teardown of some waiters
 * (e.g. screen handoff, UI join + call-scoped auto-join) but should not keep running after
 * nobody is waiting — unless [scope] itself is cancelled (leave / call cleanup).
 *
 * Candidate for Stream Android Core v2 alongside [StreamSingleFlightProcessorImpl].
 */
internal class StreamRefCountedSingleFlightProcessor(
    private val scope: CoroutineScope,
) {
    private class Flight<T>(
        val deferred: Deferred<Result<T>>,
        var waiters: Int,
    )

    private val mutex = Mutex()
    private val flights = mutableMapOf<String, Flight<*>>()
    private val closed = AtomicBoolean(false)

    /**
     * Runs [block] once for [key] while concurrent callers await the same result.
     *
     * Returns [Result.failure] with a [ClosedSendChannelException] if [stop] has already
     * been called. [CancellationException] is still rethrown when this waiter (or the shared
     * job, including last-waiter cancel) is cancelled.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> run(key: String, block: suspend () -> T): Result<T> {
        if (closed.get()) {
            return Result.failure(ClosedSendChannelException("RefCountedSingleFlight is closed"))
        }

        val flight = mutex.withLock {
            val running = flights[key]?.takeUnless { it.deferred.isCompleted } as Flight<T>?
            if (running != null) {
                running.waiters++
                running
            } else {
                val deferred = scope.async {
                    try {
                        // Complete normally even when [block] fails so the scope does not see an
                        // uncaught child exception; waiters receive Result.failure after await.
                        try {
                            Result.success(block())
                        } catch (ce: CancellationException) {
                            throw ce
                        } catch (t: Throwable) {
                            Result.failure(t)
                        }
                    } finally {
                        mutex.withLock {
                            if (flights[key]?.deferred === this@async) {
                                flights.remove(key)
                            }
                        }
                    }
                }
                Flight(deferred = deferred, waiters = 1).also { flights[key] = it }
            }
        }

        var released = false
        suspend fun releaseWaiter(cancelIfLast: Boolean) {
            if (released) return
            released = true
            val shouldCancelShared = mutex.withLock {
                flight.waiters = (flight.waiters - 1).coerceAtLeast(0)
                cancelIfLast && flight.waiters == 0 && flight.deferred.isActive
            }
            if (shouldCancelShared) {
                flight.deferred.cancel()
            }
        }

        return try {
            flight.deferred.await()
        } catch (ce: CancellationException) {
            // NonCancellable: waiter bookkeeping must run while this coroutine is cancelling.
            withContext(NonCancellable) {
                releaseWaiter(cancelIfLast = true)
            }
            throw ce
        } finally {
            withContext(NonCancellable) {
                releaseWaiter(cancelIfLast = false)
            }
        }
    }

    fun has(key: String): Boolean = flights.containsKey(key)

    fun cancel(key: String): Result<Unit> = runCatching {
        flights[key]?.deferred?.cancel()
    }

    fun clear(cancelRunning: Boolean): Result<Unit> = runCatching {
        if (cancelRunning) {
            flights.values.forEach { it.deferred.cancel() }
        }
        flights.clear()
    }

    fun stop(): Result<Unit> = runCatching {
        if (closed.compareAndSet(false, true)) {
            clear(cancelRunning = true).getOrThrow()
        }
    }
}
