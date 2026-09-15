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

package io.getstream.video.android.core.ringing

import io.getstream.android.video.generated.models.GetCallRingStateResponse
import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.result.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Reads the ring state of an outgoing call when its outcome stops arriving over the websocket.
 *
 * `call.accepted` / `call.rejected` / `call.missed` are delivered best-effort with no
 * store-and-forward, and the coordinator socket's ping interval is longer than the ring window, so
 * a socket that dies silently may never be detected while the ring is still open. Without a second
 * source the caller stays on a ringing screen while the callee is already in the call.
 *
 * The poller is caller-side only and deliberately quiet: it starts only after a ring has gone
 * [RingStatePollingConfig.startAfterMs] without any ring event, and stops at the ring deadline, so
 * a ring that resolves normally never issues a request.
 *
 * @param scope the scope the poll loop runs in; cancelling it stops polling.
 * @param config timings, or null to disable polling entirely.
 * @param now a monotonic millisecond source, injected so tests can drive it from virtual time.
 * @param fetch reads the ring state of one call session.
 * @param onRingState applies a successful read. Called on the polling coroutine.
 */
internal class RingStatePoller(
    private val scope: CoroutineScope,
    private val config: RingStatePollingConfig?,
    private val now: () -> Long,
    private val fetch: suspend (callSessionId: String) -> Result<GetCallRingStateResponse>,
    private val onRingState: suspend (GetCallRingStateResponse) -> Unit,
) {
    private val logger by taggedLogger("Call:RingStatePoller")

    private var job: Job? = null

    /**
     * The last time a ring event proved the socket was still delivering. Polling waits for
     * [RingStatePollingConfig.startAfterMs] of quiet measured from here, so an event pushes the
     * first read further out rather than cancelling the poller: in a group ring a single rejection
     * does not settle the outcome, and the ring may still go silent afterwards.
     */
    @Volatile
    private var lastRingEventAt: Long = 0

    /**
     * Begins watching a ring.
     *
     * @param callSessionId reads the call's current session. Resolved on each turn until it first
     * returns a value and latched from then on: an outgoing ring does not always have a session by
     * the time it starts, and once the call ends the call no longer has a current session — which
     * is precisely the case polling exists to answer. Latching covers both.
     * @param ringTimeoutMs the ring window, resolved once polling starts. Polling stops there, so
     * it always resolves before the local auto-drop rather than racing it.
     */
    fun start(callSessionId: () -> String?, ringTimeoutMs: () -> Long?) {
        if (config == null) return
        if (job?.isActive == true) return

        lastRingEventAt = now()

        job = scope.launch {
            // The ring window bounds polling so it resolves before the local auto-drop rather
            // than racing it, but that window is the app's own setting and can be minutes. The
            // SDK's own ceiling applies on top, so a ring that runs long cannot turn into an
            // unbounded read loop against the shard. A missing window falls back to the ceiling
            // rather than disabling polling, which is safe now that one exists.
            val ringWindow = ringTimeoutMs()?.takeIf { it > 0 } ?: config.maxDurationMs
            val deadline = now() + minOf(ringWindow, config.maxDurationMs)
            var sessionId: String? = null

            logger.d { "[start] deadline in ${deadline - now()}ms" }
            while (isActive) {
                val quietFor = now() - lastRingEventAt
                val waitFor = if (quietFor >= config.startAfterMs) {
                    config.intervalMs
                } else {
                    config.startAfterMs - quietFor
                }
                // Strictly before the deadline: a read landing exactly on it races the local
                // auto-drop, which is what bounding the poller is meant to prevent.
                if (now() + waitFor >= deadline) {
                    logger.d { "[start] ring deadline reached, stopping" }
                    return@launch
                }
                delay(waitFor)

                // A ring event during the wait restarts the quiet period instead of polling now.
                if (now() - lastRingEventAt < config.startAfterMs) continue

                sessionId = sessionId ?: callSessionId()?.takeIf { it.isNotEmpty() }
                if (sessionId == null) {
                    logger.d { "[start] no session id yet, nothing to read" }
                    continue
                }

                if (!poll(sessionId)) return@launch
            }
        }
    }

    /** Records that a ring event arrived, restarting the quiet period. */
    fun onRingEvent() {
        lastRingEventAt = now()
    }

    /** Stops polling. Safe to call when not started. */
    fun stop() {
        job?.cancel()
        job = null
    }

    /** @return false when polling should stop. */
    private suspend fun poll(callSessionId: String): Boolean {
        when (val result = fetch(callSessionId)) {
            is Result.Success -> {
                logger.d { "[poll] ${result.value}" }
                onRingState(result.value)
            }
            is Result.Failure -> {
                val error = result.value
                if (error is Error.NetworkError && error.statusCode in TERMINAL_STATUS_CODES) {
                    // The session is unknown, or belongs to another call. Retrying cannot change
                    // that, and every retry spends the app's rate limit on the endpoint.
                    logger.w { "[poll] giving up: $error" }
                    return false
                }
                // Anything else is the flaky connection this exists for. Keep reading.
                logger.d { "[poll] failed: $error" }
            }
        }
        return true
    }

    private companion object {
        private val TERMINAL_STATUS_CODES = setOf(400, 404)
    }
}
