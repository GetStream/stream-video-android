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
 * [RingStatePollingConfig.startAfterMs] without a participant's ring status changing, and stops at
 * the ring deadline, so a ring that resolves normally never issues a request.
 *
 * Reads are scheduled at a fixed delay, not a fixed rate:
 *
 * 1. polling waits until no participant's ring status has changed for
 *    [RingStatePollingConfig.startAfterMs];
 * 2. it then issues the first read;
 * 3. once a read and the state it produces have been applied, it waits
 *    [RingStatePollingConfig.intervalMs] before the next one;
 * 4. a status update arriving while it waits restarts the wait from step 1.
 *
 * The time a read takes is therefore not absorbed by the interval: with a five second interval and
 * a two second read, consecutive reads start about seven seconds apart. That is the intended
 * trade - a slow endpoint gets asked less often, not more.
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
     * When the current grace period began: the ring starting, or the last time a participant's
     * ring status proved the socket was still delivering.
     *
     * Polling waits for [RingStatePollingConfig.startAfterMs] measured from here, so an update
     * pushes the first read further out rather than cancelling the poller: in a group ring a
     * single rejection does not settle the outcome, and the ring may still go silent afterwards.
     *
     * This is a local observation time, not an event timestamp, and it is set when polling is
     * armed even though nothing has been observed yet.
     */
    @Volatile
    private var pollingGracePeriodStartedAtMs: Long = 0

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

        pollingGracePeriodStartedAtMs = now()

        job = scope.launch {
            // The ring window bounds polling so it resolves before the local auto-drop rather
            // than racing it, but that window is the app's own setting and can be minutes. The
            // SDK's own ceiling applies on top, so a ring that runs long cannot turn into an
            // unbounded read loop against the shard. A ring whose settings are not known yet
            // falls back to the length of a default ring rather than to the ceiling.
            val ringWindow = ringTimeoutMs()?.takeIf { it > 0 } ?: config.defaultRingWindowMs
            val deadline = now() + minOf(ringWindow, RingStatePollingConfig.MAX_DURATION_MS)
            var sessionId: String? = null

            logger.d { "[start] deadline in ${deadline - now()}ms" }
            while (isActive) {
                val quietFor = now() - pollingGracePeriodStartedAtMs
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
                if (now() - pollingGracePeriodStartedAtMs < config.startAfterMs) continue

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
    fun onRingParticipantStatusUpdate() {
        pollingGracePeriodStartedAtMs = now()
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
                val state = result.value
                // Counts rather than the response: the payload carries the call cid, the session
                // id and the participant maps, and this reads every few seconds for every
                // ringing caller. What a log needs to answer is whether the ring settled.
                logger.d {
                    "[poll] accepted=${state.acceptedBy.size} rejected=${state.rejectedBy.size} " +
                        "missed=${state.missedBy.size} " +
                        "ended=${state.callEndedAt != null || state.sessionEndedAt != null}"
                }
                onRingState(state)
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
