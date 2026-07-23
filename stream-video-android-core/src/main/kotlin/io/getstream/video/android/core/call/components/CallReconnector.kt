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

package io.getstream.video.android.core.call.components

import io.getstream.log.taggedLogger
import io.getstream.result.Result.Success
import io.getstream.video.android.core.BackendCause
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallLeaveReason
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.analytics.call.observer.model.JoinAnalyticsModel
import io.getstream.video.android.core.analytics.call.observer.model.JoinReason
import io.getstream.video.android.core.analytics.reporting.model.AnalyticsCallAbortReason
import io.getstream.video.android.core.call.FastReconnectResult
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.SfuConnectionResult
import io.getstream.video.android.core.model.toIceServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import stream.video.sfu.event.ReconnectDetails
import stream.video.sfu.models.WebsocketReconnectStrategy
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Outcome of a single reconnect attempt. Each reconnect method returns one of
 * these instead of throwing, making the control flow in the reconnect loop
 * explicit and exhaustively checked by the compiler.
 */
private sealed class ReconnectOutcome {
    /** Reconnect succeeded — exit the loop. */
    object Success : ReconnectOutcome()

    /** A required precondition is missing (no session, no location). Terminal — don't retry. */
    data class PreconditionNotMet(val reason: String) : ReconnectOutcome()

    /** Peer connections are stale and can't be reused. Should escalate to REJOIN. */
    object PeerConnectionStale : ReconnectOutcome()

    /** Server-initiated disconnect — leave the call cleanly. */
    object Disconnect : ReconnectOutcome()

    /** A transient failure occurred. The loop should retry with escalation. */
    data class Failed(val error: Exception) : ReconnectOutcome()
}

/**
 * Owns the unified reconnection state machine for a [Call]: the FAST / REJOIN / MIGRATE
 * strategies, escalation logic, the single-flight reconnect mutex and the set of failed
 * SFU edge names used to populate `migrating_from_list`.
 */
internal class CallReconnector(
    private val call: Call,
) {
    private val logger by taggedLogger("Call:Reconnector:${call.type}:${call.id}")

    private val clientImpl get() = call.clientImpl
    private val state get() = call.state
    private val session get() = call.session
    private val callAnalytics get() = call.callAnalytics

    private val reconnectMutex = Mutex()

    /**
     * SFU IDs (edge names) we failed to connect to (e.g. SFU_FULL). Sent in migrating_from_list
     * when requesting new credentials so the coordinator can exclude them.
     */
    private val failedSfuIds: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * Unified reconnection entry point.
     *
     * All callers (stateJob, NetworkStateListener, error handlers) funnel through
     * this method. It acquires [reconnectMutex] so only one flow runs at a time,
     * and implements a retry loop that **escalates** the strategy on failure:
     *
     * - **FAST** → **REJOIN** after [MAX_FAST_RECONNECT_ATTEMPTS] failures *or*
     *   when the elapsed time exceeds the reconnect deadline.
     * - **MIGRATE** → **REJOIN** if the migration attempt fails.
     * - **DISCONNECT** → leaves the call (server-initiated).
     * - **UNSPECIFIED** → treated as FAST (HealthMonitor already attempted the WS).
     *
     * The loop exits when the connection state becomes [RealtimeConnection.Connected],
     * [RealtimeConnection.ReconnectingFailed], or [RealtimeConnection.Disconnected].
     *
     * @param strategy the initial reconnection strategy requested by the caller.
     * @param reason a human-readable reason for logging / tracing.
     */
    suspend fun reconnect(
        strategy: WebsocketReconnectStrategy,
        reason: String,
    ) {
        val conn = state.connection.value
        logger.d { "[reconnect] Entry — strategy=$strategy reason=$reason connection=$conn" }

        if (call.isDestroyed || conn is RealtimeConnection.Disconnected) {
            logger.d {
                "[reconnect] Call already left/destroyed (isDestroyed=${call.isDestroyed}, conn=$conn) — skipping ($reason)"
            }
            return
        }

        // Use tryLock so concurrent triggers (stateJob, NetworkStateListener,
        // SfuSocket errors) don't queue up. If a reconnect loop is already
        // running it will handle recovery; redundant callers return immediately.
        if (!reconnectMutex.tryLock()) {
            logger.d { "[reconnect] Active reconnect loop running — skipping ($reason)" }
            return
        }
        var currentStrategy = strategy
        try {
            // Re-check after acquiring the lock — bail only if the user left.
            // We deliberately allow reconnect from Connected (SFU/network may
            // request it) and from ReconnectingFailed (fresh trigger like
            // network recovery should be retried).
            val currentConn = state.connection.value
            if (currentConn is RealtimeConnection.Disconnected) {
                logger.d { "[reconnect] State is $currentConn — no reconnect needed ($reason)" }
                return
            }

            val isMigrate = strategy ==
                WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_MIGRATE
            state._connection.value = if (isMigrate) {
                RealtimeConnection.Migrating
            } else {
                RealtimeConnection.Reconnecting
            }

            val loopStartTime = System.currentTimeMillis()
            // Local iteration counter for this reconnect() invocation only.
            // Controls MAX_RECONNECT_ATTEMPTS cap and FAST→REJOIN escalation.
            // Distinct from the class-level reconnectAttempts which is cumulative.
            var loopIteration = 0

            while (true) {
                // EARLY EXIT CASE 1 - State based
                val connectionState = state.connection.value
                if (connectionState is RealtimeConnection.Connected ||
                    connectionState is RealtimeConnection.ReconnectingFailed ||
                    connectionState is RealtimeConnection.Disconnected
                ) {
                    logger.i { "[reconnect] Loop finished — state=$connectionState" }
                    break
                }

                // EARLY EXIT CASE 2 - count based
                if (loopIteration >= MAX_RECONNECT_ATTEMPTS) {
                    logger.w { "[reconnect] Max reconnect attempts ($MAX_RECONNECT_ATTEMPTS) reached — giving up" }
                    state._connection.value = RealtimeConnection.ReconnectingFailed
                    break
                }

                // EARLY EXIT CASE 3 - time based
                val elapsedMs = System.currentTimeMillis() - loopStartTime
                if (clientImpl.leaveAfterDisconnectSeconds > 0 &&
                    elapsedMs / 1000 > clientImpl.leaveAfterDisconnectSeconds
                ) {
                    logger.w { "[reconnect] Disconnection timeout reached — giving up" }
                    state._connection.value = RealtimeConnection.ReconnectingFailed
                    break
                }

                // Wait for network before doing anything else. Polls without
                // consuming the attempt budget — the elapsed-time guard below
                // will still fire if we wait too long.
                if (!call.isNetworkConnected()) {
                    logger.d {
                        "[reconnect] Network unavailable — waiting for connectivity (loopIteration=$loopIteration)"
                    }
                    delay(RECONNECT_DELAY_MS)
                    continue
                }

                val currentTimeInMillis = System.currentTimeMillis()
                if (currentTimeInMillis - loopStartTime >= call.reconnectDeadlineMillis) {
                    currentStrategy = when (currentStrategy) {
                        WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
                        WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_UNSPECIFIED,
                        -> {
                            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN
                        }

                        else -> currentStrategy
                    }
                }

                logger.i {
                    "[reconnect] loopIteration=$loopIteration strategy=$currentStrategy reason=$reason"
                }

                val outcome = when (currentStrategy) {
                    WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
                    WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_UNSPECIFIED,
                    -> reconnectFast(reason)

                    WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN -> {
                        call.nonFastReconnectAttempts++
                        reconnectRejoin(
                            reason,
                            JoinAnalyticsModel(call.nonFastReconnectAttempts, JoinReason.ReJoin),
                        )
                    }

                    WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_MIGRATE -> {
                        call.nonFastReconnectAttempts++
                        reconnectMigrate(
                            JoinAnalyticsModel(call.nonFastReconnectAttempts, JoinReason.Migrate),
                        )
                    }

                    WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_DISCONNECT ->
                        ReconnectOutcome.Disconnect
                }

                when (outcome) {
                    is ReconnectOutcome.Success -> break

                    is ReconnectOutcome.Disconnect -> {
                        logger.w { "[reconnect] DISCONNECT requested — leaving call" }
                        call.leave(
                            CallLeaveReason.Backend(BackendCause.SFU_DISCONNECT),
                        )
                        break
                    }

                    is ReconnectOutcome.PreconditionNotMet -> {
                        logger.w { "[reconnect] Precondition not met — giving up: ${outcome.reason}" }
                        state._connection.value = RealtimeConnection.ReconnectingFailed
                        break
                    }

                    is ReconnectOutcome.PeerConnectionStale -> {
                        logger.w { "[reconnect] Peer connections stale — escalating to REJOIN" }
                        delay(RECONNECT_DELAY_MS)
                        loopIteration++
                        currentStrategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN
                    }

                    is ReconnectOutcome.Failed -> {
                        logger.w {
                            "[reconnect] $currentStrategy (${call.nonFastReconnectAttempts}) failed: ${outcome.error.message}"
                        }

                        delay(RECONNECT_DELAY_MS)
                        loopIteration++

                        val wasMigrating = currentStrategy ==
                            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_MIGRATE
                        val pastFastReconnectDeadline = (System.currentTimeMillis() - loopStartTime) >
                            call.reconnectDeadlineMillis
                        val shouldEscalateToRejoin = wasMigrating ||
                            pastFastReconnectDeadline

                        if (shouldEscalateToRejoin) {
                            currentStrategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN
                        }
                        logger.i { "[reconnect] Next strategy: $currentStrategy (loopIteration=$loopIteration)" }
                    }
                }
            }

            if (state.connection.value is RealtimeConnection.ReconnectingFailed) {
                val message = "[reconnect] All recovery attempts exhausted — leaving call ($reason)"
                logger.w { message }
                callAnalytics.joinAnalytics.onJoinRequestRetryExhausted(
                    loopIteration,
                    AnalyticsCallAbortReason.RETRY_EXHAUSTED.name,
                    message,
                )
                call.leave(
                    CallLeaveReason.RetryExhausted(
                        loopIteration,
                        "reconnect-failed",
                        message,
                    ),
                )
            }
        } finally {
            // Always release the mutex — even on exceptions or coroutine
            // cancellation — so future reconnect() calls aren't permanently blocked.
            reconnectMutex.unlock()
            logger.d {
                "[reconnect] Free reconnectMutex, initialStrategy: $strategy, finalStrategy: $currentStrategy"
            }
        }
    }

    /**
     * Fast reconnect to the same SFU with the same participant session.
     * Reuses the existing session ID — no previous_session_id needed since the
     * SFU already knows this participant.
     */
    private suspend fun reconnectFast(reason: String): ReconnectOutcome {
        logger.d { "[reconnectFast] reconnectAttempts=${call.nonFastReconnectAttempts}" }
        val currentSession = session.value
            ?: return ReconnectOutcome.PreconditionNotMet("No active session for fast reconnect")

        val stats = call.collectStats()
        currentSession.sendCallStats(stats)

        currentSession.prepareReconnect()
        state._connection.value = RealtimeConnection.Reconnecting
        call.reconnectStartTime = System.currentTimeMillis()

        val (_, subscriptionsInfo, publishingInfo) = currentSession.currentSfuInfo()
        val reconnectDetails = ReconnectDetails(
            previous_session_id = "",
            strategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
            announced_tracks = publishingInfo,
            subscriptions = subscriptionsInfo,
            reconnect_attempt = call.nonFastReconnectAttempts,
            reason = reason,
        )
        return when (val result = currentSession.fastReconnect(reconnectDetails)) {
            is FastReconnectResult.Connected -> ReconnectOutcome.Success
            is FastReconnectResult.PeerConnectionStale -> ReconnectOutcome.PeerConnectionStale
            is FastReconnectResult.Failed -> ReconnectOutcome.Failed(result.error)
        }
    }

    /**
     * Rejoin a call. Creates a new session ID and joins as a new participant.
     * previous_session_id is set so the SFU can transfer state (tracks,
     * subscriptions) from the old session to the new one.
     */
    private suspend fun reconnectRejoin(
        reason: String,
        joinAnalyticsModel: JoinAnalyticsModel,
    ): ReconnectOutcome {
        logger.d { "[reconnectRejoin] reconnectAttempts=${call.nonFastReconnectAttempts}" }
        state._connection.value = RealtimeConnection.Reconnecting
        val loc = call.location
            ?: return ReconnectOutcome.PreconditionNotMet("No location available for rejoin")
        val oldSession = session.value
            ?: return ReconnectOutcome.PreconditionNotMet("No active session for rejoin")
        call.reconnectStartTime = System.currentTimeMillis()

        val joinResponse = call.joinRequest(location = loc, joinAnalyticsModel = joinAnalyticsModel)
        if (joinResponse !is Success) {
            return ReconnectOutcome.Failed(
                Exception("Failed to get join response: ${joinResponse.errorOrNull()}"),
            )
        }

        val cred = joinResponse.value.credentials
        val currentOptions = oldSession.publisher.value?.currentOptions()
        logger.i { "Rejoin SFU ${oldSession.sfuUrl} to ${cred.server.url}" }

        call.sessionId = UUID.randomUUID().toString()
        val (prevSessionId, subscriptionsInfo, publishingInfo) = oldSession.currentSfuInfo()
        val reconnectDetails = ReconnectDetails(
            previous_session_id = prevSessionId,
            strategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN,
            announced_tracks = publishingInfo,
            subscriptions = subscriptionsInfo,
            reconnect_attempt = call.nonFastReconnectAttempts,
            reason = reason,
        )
        call.state.removeParticipant(prevSessionId)
        oldSession.prepareRejoin("rejoin")
        val newSession = call.unitTestRtcSessionFactory?.invoke() ?: RtcSession(
            clientImpl,
            call.nonFastReconnectAttempts,
            call.powerManager,
            call,
            call.sessionId,
            clientImpl.apiKey,
            clientImpl.coordinatorConnectionModule.lifecycle,
            cred.server.url,
            cred.server.wsEndpoint,
            cred.token,
            cred.server.edgeName,
            cred.iceServers.map { ice -> ice.toIceServer() },
            sfuAnalytics = callAnalytics.sfuAnalytics.apply {
                sfuAnalyticsStateHolder.updateSfuId(
                    cred.server.edgeName,
                )
            },
        )
        session.value = newSession

        return when (
            val result = newSession.connectInternal(
                reconnectDetails,
                currentOptions,
            )
        ) {
            is SfuConnectionResult.Success -> {
                newSession.sfuTracer.trace("rejoin", reason)
                call.monitorSession(joinResponse.value)
                ReconnectOutcome.Success
            }
            is SfuConnectionResult.Failure -> ReconnectOutcome.Failed(result.error)
        }
    }

    /**
     * Migrate to another SFU. Reuses the same session ID — the SFU
     * identifies the participant via from_sfu_id, not previous_session_id.
     */
    private suspend fun reconnectMigrate(joinAnalyticsModel: JoinAnalyticsModel): ReconnectOutcome {
        logger.d { "[reconnectMigrate] Migrating" }
        state._connection.value = RealtimeConnection.Migrating
        val loc = call.location
            ?: return ReconnectOutcome.PreconditionNotMet("No location available for migrate")
        val oldSession = session.value
            ?: return ReconnectOutcome.PreconditionNotMet("No active session for migrate")
        call.reconnectStartTime = System.currentTimeMillis()
        addFailedSfuId(oldSession.sfuName)

        val joinResponse =
            call.joinRequest(
                location = loc,
                migratingFrom = oldSession.sfuName,
                joinAnalyticsModel = joinAnalyticsModel,
            )
        if (joinResponse !is Success) {
            return ReconnectOutcome.Failed(
                Exception(
                    "Failed to get join response during migration: ${joinResponse.errorOrNull()}",
                ),
            )
        }

        val cred = joinResponse.value.credentials
        val currentOptions = oldSession.publisher.value?.currentOptions()
        val oldSfuName = oldSession.sfuName
        logger.i { "[reconnectMigrate] Migrate SFU $oldSfuName to ${cred.server.edgeName}" }

        val (_, subscriptionsInfo, publishingInfo) = oldSession.currentSfuInfo()
        val reconnectDetails = ReconnectDetails(
            previous_session_id = "",
            strategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_MIGRATE,
            announced_tracks = publishingInfo,
            subscriptions = subscriptionsInfo,
            from_sfu_id = oldSfuName,
            reconnect_attempt = call.nonFastReconnectAttempts,
        )

        val stats = call.collectStats()
        oldSession.sendCallStats(stats)
        oldSession.enterMigration()

        val newSession = call.unitTestRtcSessionFactory?.invoke() ?: RtcSession(
            clientImpl,
            call.nonFastReconnectAttempts,
            call.powerManager,
            call,
            call.sessionId,
            clientImpl.apiKey,
            clientImpl.coordinatorConnectionModule.lifecycle,
            cred.server.url,
            cred.server.wsEndpoint,
            cred.token,
            cred.server.edgeName,
            cred.iceServers.map { ice -> ice.toIceServer() },
            sfuAnalytics = callAnalytics.sfuAnalytics.apply {
                sfuAnalyticsStateHolder.updateSfuId(
                    cred.server.edgeName,
                )
            },
        )
        session.value = newSession

        return try {
            val result = newSession.connectInternal(
                reconnectDetails,
                currentOptions,
            )
            when (result) {
                is SfuConnectionResult.Success -> {
                    call.monitorSession(joinResponse.value)
                    ReconnectOutcome.Success
                }
                is SfuConnectionResult.Failure -> ReconnectOutcome.Failed(result.error)
            }
        } finally {
            oldSession.finalizeMigration()
        }
    }

    suspend fun fastReconnect(reason: String = "unknown") {
        reconnect(WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST, reason)
    }

    suspend fun rejoin(reason: String = "unknown") {
        reconnect(WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN, reason)
    }

    suspend fun migrate() {
        reconnect(WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_MIGRATE, "migrate")
    }

    /** Adds the given SFU ID (edge name) to the failed set (for migrating_from_list). */
    private fun addFailedSfuId(sfuId: String) {
        if (sfuId.isBlank()) return
        failedSfuIds.add(sfuId)
    }

    /** Returns a snapshot of failed SFU IDs to send as migrating_from_list. */
    fun getFailedSfuIdsSnapshot(): List<String> = failedSfuIds.toList()

    /** Clears the failed SFU list (e.g. after a successful join). */
    fun clearFailedSfuIds() {
        failedSfuIds.clear()
    }

    companion object {
        /** How many consecutive FAST reconnect failures are allowed before
         *  escalating to a full REJOIN. Kept small because each failed FAST
         *  attempt can cost up to the socket connection deadline before it gives
         *  up: OkHttp's WebSocket-upgrade timeout, followed by the join-response
         *  wait — both driven by StreamVideoBuilder.connectionTimeoutInMs. */
        private const val MAX_FAST_RECONNECT_ATTEMPTS = 3

        /** Absolute upper bound on loop iterations across all strategies
         *  (FAST + REJOIN + MIGRATE combined). Prevents infinite retries
         *  when every strategy keeps failing. */
        private const val MAX_RECONNECT_ATTEMPTS = 10

        /** Delay between consecutive reconnect attempts (both after a
         *  failed attempt and while polling for network availability
         *  during FAST reconnect). Kept short so the SDK reacts quickly
         *  once conditions improve. */
        private const val RECONNECT_DELAY_MS = 500L
    }
}
