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
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

/**
 * Single-flight that coalesces concurrent calls by key, runs the shared work on [scope],
 * and tracks how many callers are still awaiting.
 *
 * Compared to [StreamSingleFlightProcessorImpl]:
 * - Cancelling **one** waiter does **not** cancel the shared job (work is owned by [scope]).
 * - Cancelling the **last** waiter cancels the shared job only when [run] is called with
 *   [cancelIfLastWaiter] (the default). Pass `false` when the work must outlive the last
 *   UI waiter (Activity finish / screen handoff) and be torn down only by [scope] cancel
 *   (leave / call cleanup).
 * - [CancellationException] is rethrown to the cancelled waiter instead of being wrapped in
 *   [Result.failure].
 *
 * Use this when the operation should survive Activity/ViewModel teardown of some waiters
 * (e.g. screen handoff, UI join + call-scoped auto-join).
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
    private class WaiterRegistration(
        val job: Job,
        val attachment: Any?,
    )

    private class Flight<T>(
        val key: String,
        val deferred: Deferred<Result<T>>,
        var waiters: Int,
        val registrations: CopyOnWriteArrayList<WaiterRegistration> = CopyOnWriteArrayList(),
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
     * job, including last-waiter cancel when [cancelIfLastWaiter] is true) is cancelled.
     *
     * [onCoalesced] runs on this waiter when it attaches to an already-running flight
     * (before awaiting the shared result).
     *
     * [onLeader] runs once, under the same lock as flight creation, before any coalescer
     * can attach. Use it to snapshot leader-only state that [onCoalesced] will read.
     *
     * [attachment] is stored on the flight with this waiter's [Job].
     * [firstNonCancelledAttachment] returns the first non-null attachment whose waiter job
     * is not cancelled (completed successfully still counts).
     *
     * When [cancelIfLastWaiter] is false, the last cancelled waiter leaves the shared job
     * running and keeps the map entry so a later [run] can coalesce instead of starting a
     * second execution.
     */
    suspend fun <T> run(
        key: String,
        attachment: Any? = null,
        onCoalesced: () -> Unit = {},
        onLeader: () -> Unit = {},
        cancelIfLastWaiter: Boolean = true,
        block: suspend () -> T,
    ): Result<T> {
        val waiterJob = coroutineContext[Job]!!
        val acquired = acquireWaiter(key, waiterJob, attachment, onLeader, block)
            ?: return Result.failure(
                ClosedSendChannelException("RefCountedSingleFlight is closed"),
            )
        if (acquired.coalesced) onCoalesced()
        return awaitSharedResult(acquired.flight, cancelIfLastWaiter)
    }

    /**
     * First non-null [run] attachment whose waiter [Job] is not cancelled.
     * Cancelled waiters are skipped so a later still-active waiter can take over.
     */
    fun firstNonCancelledAttachment(key: String): Any? {
        val flight = flights[key] ?: return null
        return flight.registrations.firstOrNull {
            !it.job.isCancelled && it.attachment != null
        }?.attachment
    }

    private suspend fun <T> acquireWaiter(
        key: String,
        waiterJob: Job,
        attachment: Any?,
        onLeader: () -> Unit,
        block: suspend () -> T,
    ): Acquired<T>? = mutex.withLock {
        if (closed.get()) return@withLock null
        selectFlightLocked(key, waiterJob, attachment, onLeader, block)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> selectFlightLocked(
        key: String,
        waiterJob: Job,
        attachment: Any?,
        onLeader: () -> Unit,
        block: suspend () -> T,
    ): Acquired<T> {
        val running = flights[key]?.takeIf { it.deferred.isActive } as Flight<T>?
        if (running != null) {
            running.waiters++
            running.registrations.add(WaiterRegistration(waiterJob, attachment))
            return Acquired(running, coalesced = true)
        }
        return Acquired(
            createFlightLocked(key, waiterJob, attachment, onLeader, block),
            coalesced = false,
        )
    }

    private fun <T> createFlightLocked(
        key: String,
        waiterJob: Job,
        attachment: Any?,
        onLeader: () -> Unit,
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
        return Flight(key = key, deferred = deferred, waiters = 1).also {
            it.registrations.add(WaiterRegistration(waiterJob, attachment))
            flights[key] = it
            onLeader()
        }
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

    private suspend fun <T> awaitSharedResult(
        flight: Flight<T>,
        cancelIfLastWaiter: Boolean,
    ): Result<T> {
        var released = false
        suspend fun releaseWaiter(cancelIfLast: Boolean) {
            if (released) return
            released = true
            // Decrement, map removal, and cancel must stay under one lock so a new run() cannot
            // attach to a flight that is about to be cancelled (waiters already at 0).
            mutex.withLock {
                flight.waiters = (flight.waiters - 1).coerceAtLeast(0)
                if (flight.waiters != 0) return@withLock
                when {
                    cancelIfLast && flight.deferred.isActive -> cancelAndDetachLocked(flight)
                    // Job already dead (completed, failed, or scope cancelled): drop the map
                    // entry. A deferred created on an already-cancelled scope can die before
                    // its body/finally runs, which would otherwise leave `has(key) == true`.
                    !flight.deferred.isActive ->
                        removeFlightIfCurrentLocked(flight.key, flight.deferred)
                    // Last waiter left and the job is still running (cancelIfLastWaiter =
                    // false): keep the entry so a later run() coalesces instead of double-joining.
                }
            }
        }

        return try {
            flight.deferred.await()
        } catch (ce: CancellationException) {
            // NonCancellable: waiter bookkeeping must run while this coroutine is cancelling.
            withContext(NonCancellable) {
                releaseWaiter(cancelIfLast = cancelIfLastWaiter)
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
