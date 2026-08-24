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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
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
 *
 * High-level [run] algorithm:
 * ```
 * run
 * ├── acquireWaiter
 * │   └── selectFlightLocked
 * │       ├── createFlightLocked
 * │       └── removeFlightIfCurrentLocked
 * └── awaitSharedResult
 *     └── releaseWaiter
 * ```
 *
 * All map mutations and the [closed] flag go through [mutex]. Callers only reuse a flight
 * while its deferred is still [Deferred.isActive] — a `Cancelling` job is not joinable.
 */
internal class StreamRefCountedSingleFlightProcessor(
    private val scope: CoroutineScope,
) {
    private class Flight<T>(
        val key: String,
        val deferred: Deferred<Result<T>>,
        var waiters: Int,
    )

    private class Acquired<T>(
        val flight: Flight<T>,
        val coalesced: Boolean,
    )

    private val mutex = Mutex()
    private val flights = ConcurrentHashMap<String, Flight<*>>()
    private val closed = AtomicBoolean(false)

    /**
     * Runs [block] once for [key] while concurrent callers await the same result.
     *
     * Returns [Result.failure] with a [ClosedSendChannelException] if [stop] has already
     * been called. [CancellationException] is still rethrown when this waiter (or the shared
     * job, including last-waiter cancel) is cancelled.
     *
     * [onCoalesced] runs on this waiter when it attaches to an already-running flight
     * (before awaiting the shared result).
     */
    suspend fun <T> run(
        key: String,
        onCoalesced: () -> Unit = {},
        block: suspend () -> T,
    ): Result<T> {
        val acquired = acquireWaiter(key, block)
            ?: return Result.failure(
                ClosedSendChannelException("RefCountedSingleFlight is closed"),
            )
        if (acquired.coalesced) onCoalesced()
        return awaitSharedResult(acquired.flight)
    }

    private suspend fun <T> acquireWaiter(
        key: String,
        block: suspend () -> T,
    ): Acquired<T>? = mutex.withLock {
        if (closed.get()) return@withLock null
        selectFlightLocked(key, block)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> selectFlightLocked(
        key: String,
        block: suspend () -> T,
    ): Acquired<T> {
        val running = flights[key]?.takeIf { it.deferred.isActive } as Flight<T>?
        if (running != null) {
            running.waiters++
            return Acquired(running, coalesced = true)
        }
        return Acquired(createFlightLocked(key, block), coalesced = false)
    }

    private fun <T> createFlightLocked(
        key: String,
        block: suspend () -> T,
    ): Flight<T> {
        lateinit var deferred: Deferred<Result<T>>
        deferred = scope.async {
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
                    removeFlightIfCurrentLocked(key, deferred)
                }
            }
        }
        return Flight(key = key, deferred = deferred, waiters = 1).also { flights[key] = it }
    }

    private fun removeFlightIfCurrentLocked(key: String, deferred: Deferred<*>) {
        if (flights[key]?.deferred === deferred) {
            flights.remove(key)
        }
    }

    private fun cancelAndDetachLocked(flight: Flight<*>) {
        removeFlightIfCurrentLocked(flight.key, flight.deferred)
        flight.deferred.cancel()
    }

    private suspend fun <T> awaitSharedResult(flight: Flight<T>): Result<T> {
        var released = false
        suspend fun releaseWaiter(cancelIfLast: Boolean) {
            if (released) return
            released = true
            // Decrement, map removal, and cancel must stay under one lock so a new run() cannot
            // attach to a flight that is about to be cancelled (waiters already at 0).
            mutex.withLock {
                flight.waiters = (flight.waiters - 1).coerceAtLeast(0)
                if (flight.waiters != 0) return@withLock
                // Always detach when the last waiter leaves. A deferred created on an already
                // cancelled scope can die before its body/finally runs, which would otherwise
                // leave a stale map entry (`has(key) == true`).
                if (cancelIfLast && flight.deferred.isActive) {
                    cancelAndDetachLocked(flight)
                } else {
                    removeFlightIfCurrentLocked(flight.key, flight.deferred)
                }
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

    suspend fun cancel(key: String): Result<Unit> = runCatching {
        mutex.withLock {
            val flight = flights[key] ?: return@withLock
            cancelAndDetachLocked(flight)
        }
    }

    suspend fun clear(cancelRunning: Boolean): Result<Unit> = runCatching {
        mutex.withLock {
            if (cancelRunning) {
                val snapshot = flights.values.toList()
                flights.clear()
                snapshot.forEach { it.deferred.cancel() }
            } else {
                flights.clear()
            }
        }
    }

    suspend fun stop(): Result<Unit> = runCatching {
        mutex.withLock {
            if (closed.compareAndSet(false, true)) {
                val snapshot = flights.values.toList()
                flights.clear()
                snapshot.forEach { it.deferred.cancel() }
            }
        }
    }
}
