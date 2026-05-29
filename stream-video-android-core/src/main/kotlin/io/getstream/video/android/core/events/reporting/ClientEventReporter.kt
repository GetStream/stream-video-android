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

package io.getstream.video.android.core.events.reporting

import io.getstream.android.video.generated.apis.ProductvideoApi
import io.getstream.android.video.generated.models.ClientEvent
import io.getstream.android.video.generated.models.ReportClientEventRequest
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.socket.common.scope.ClientScope
import io.getstream.video.android.core.socket.common.scope.UserScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import org.webrtc.PeerConnection
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

internal typealias EventSessionId = String
internal typealias CallId = String

/**
 * TODO
 * [ClientEvent.previouslyConnectedTimestamp] : Ask clarification
 * [ClientEvent.retryFailureCode] : Ask clarification
 */

internal enum class TelemetrySendingStrategy { IN_PLACE, BATCH }

internal class ClientEventReporter(
    private val api: ProductvideoApi,
    private val userAgent: () -> String,
    private val sdkVersion: String,
    private val sendingStrategy: TelemetrySendingStrategy = TelemetrySendingStrategy.IN_PLACE,
    private val scope: CoroutineScope = UserScope(ClientScope()),
) {
    private val logger by taggedLogger("ClientEventReporter")

    private val inFlightSessions = ConcurrentHashMap<EventSessionId, InFlightSession>()
    private val joinStageAttemptIdMap = ConcurrentHashMap<CallId, String>()
    private val callSessionIdMap = ConcurrentHashMap<CallId, String>()

    // Active event_session_id per PC role — drives the ICE state machine
    private val activePcSessionIds = ConcurrentHashMap<PeerConnectionRole, String>()

    // Whether each PC role has ever reached CONNECTED (for was_previously_connected)
    private val pcEverConnected = ConcurrentHashMap<PeerConnectionRole, Boolean>()

    internal data class InFlightSession(
        val eventSessionId: EventSessionId,
        val callId: String,
        val callType: String,
        val stage: CallEventStage,
        val startedAtMs: Long,
        val joinStageAttemptIdSnapshot: String,
        val sfuId: String? = null,
        val callSessionId: String? = null,
        val userSessionId: String? = null,
        val peerConnectionRole: PeerConnectionRole? = null,
        val wasPreviouslyConnected: Boolean = false,
    )

    enum class AbortReason(val code: String, val message: String) {
        CLIENT_ABORTED("CLIENT_ABORTED", "Aborted: user left during retry"),
        BACKEND_LEAVE("BACKEND_LEAVE", "Aborted: backend ended call during connect"),
    }

    enum class FailureCodes(val code: String, val message: String) {
        CLIENT_ABORTED("CLIENT_ABORTED", "Aborted: user left during retry"),
        BACKEND_LEAVE("BACKEND_LEAVE", "Aborted: backend ended call during connect"),
        NETWORK_OFFLINE("NETWORK_OFFLINE", "Device offline"),
        ICE_GATHERING_FAILED("ICE_GATHERING_FAILED", "ICE gathering failed"),
        ICE_CONNECTIVITY_FAILED("ICE_CONNECTIVITY_FAILED", "ICE connectivity failed"),
        REQUEST_TIMEOUT("REQUEST_TIMEOUT", "Device offline"),
        SFU_REQUEST_TIMEOUT("REQUEST_TIMEOUT", "SFU connection timed out"),
    }

    // --- CoordinatorJoin ---

    internal fun reportCoordinatorJoinInitiated(
        callId: String,
        callType: String,
        joinStageAttemptId:String
    ): String {

        val eventSessionId = UUID.randomUUID().toString()
        joinStageAttemptIdMap[callId] = joinStageAttemptId
        val now = System.currentTimeMillis()
        inFlightSessions[eventSessionId] = InFlightSession(
            eventSessionId = eventSessionId,
            callId = callId,
            callType = callType,
            stage = CallEventStage.COORDINATOR_JOIN,
            startedAtMs = now,
            joinStageAttemptIdSnapshot = joinStageAttemptId,
        )
        sendEvent(
            buildRequest(
                callId,
                callType,
                stage = CallEventStage.COORDINATOR_JOIN,
                eventType = CallEventType.INITIATED,
                eventSessionId = eventSessionId,
                joinStageAttemptId = joinStageAttemptId,
            ),
        )
        return eventSessionId
    }

    internal fun reportCoordinatorJoinCompleted(
        eventSessionId: String,
        success: Boolean,
        retryCount: Int,
        failureReason: String? = null,
        failureCode: String? = null, // TODO Rahul, ask tomorrow
        callSessionId: String? = null,
    ) {
        val session = inFlightSessions.remove(eventSessionId) ?: return
        val elapsedTime = System.currentTimeMillis() - session.startedAtMs
        callSessionIdMap[session.callId] = callSessionId ?: ""
        sendEvent(
            buildRequest(
                callId = session.callId,
                callType = session.callType,
                stage = CallEventStage.COORDINATOR_JOIN,
                eventType = CallEventType.COMPLETED,
                eventSessionId = eventSessionId,
                elapsedTime = elapsedTime,
                outcome = if (success) CallEventOutcome.SUCCESS else CallEventOutcome.FAILURE,
                retryCountAttempt = retryCount,
                retryFailureReason = if (!success) failureReason else null,
                retryFailureCode = if (!success) failureCode else null,
                callSessionId = callSessionId,
                joinStageAttemptId = session.joinStageAttemptIdSnapshot,
            ),
        )
    }

    // --- WSJoin ---

    internal fun reportWsJoinInitiated(
        sfuId: String,
        callId: String,
        callType: String,
        joinStageAttemptId: String,
        wasPreviouslyConnected: Boolean,
    ): String {
        val eventSessionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val callSessionId = callSessionIdMap[callId]
        inFlightSessions[eventSessionId] = InFlightSession(
            callId = callId,
            callType = callType,
            eventSessionId = eventSessionId,
            stage = CallEventStage.WS_JOIN,
            startedAtMs = now,
            joinStageAttemptIdSnapshot = joinStageAttemptIdMap[callId] ?: "",
            sfuId = sfuId,
            wasPreviouslyConnected = wasPreviouslyConnected,
            callSessionId = callSessionId,
        )
        sendEvent(
            buildRequest(
                callId = callId,
                callType = callType,
                stage = CallEventStage.WS_JOIN,
                eventType = CallEventType.INITIATED,
                eventSessionId = eventSessionId,
                joinStageAttemptId = joinStageAttemptId,
                sfuId = sfuId,
                wasPreviouslyConnected = wasPreviouslyConnected,
            ),
        )
        return eventSessionId
    }

    internal fun reportWsJoinCompleted(
        eventSessionId: String,
        joinStageAttemptId: String,
        success: Boolean,
        retryCount: Int,
        failureReason: String? = null,
        failureCode: String? = null,
    ) {
        val session = inFlightSessions.remove(eventSessionId) ?: return
        val elapsedTime = System.currentTimeMillis() - session.startedAtMs
        sendEvent(
            buildRequest(
                callId = session.callId,
                callType = session.callType,
                stage = CallEventStage.WS_JOIN,
                eventType = CallEventType.COMPLETED,
                eventSessionId = eventSessionId,
                elapsedTime = elapsedTime,
                outcome = if (success) CallEventOutcome.SUCCESS else CallEventOutcome.FAILURE,
                retryCountAttempt = retryCount,
                retryFailureReason = if (!success) failureReason else null,
                retryFailureCode = if (!success) failureCode else null,
                sfuId = session.sfuId,
                callSessionId = session.callSessionId,
                joinStageAttemptId = joinStageAttemptId,
            ),
        )
    }

    // --- PeerConnectionConnect (ICE state machine) ---

    internal fun onPeerConnectionIceStateChanged(
        callId: String,
        callType: String,
        joinStageAttemptId: String,
        role: PeerConnectionRole,
        iceState: PeerConnection.IceConnectionState,
        peerConnectionState: PeerConnection.PeerConnectionState?,
    ) {
        when (iceState) {
            PeerConnection.IceConnectionState.CHECKING -> {
                val wasPrev = pcEverConnected[role] == true
                // TODO Rahul, maybe this `completePeerConnectionSession` is not needed
                // If an existing session is still in-flight, close it as failed first
                activePcSessionIds.remove(role)?.let { oldId ->
                    completePeerConnectionSession(
                        callId = callId,
                        callType = callType,
                        eventSessionId = oldId,
                        joinStageAttemptId = joinStageAttemptId,
                        success = false,
                        iceState = iceState,
                        peerConnectionState = peerConnectionState,
                        failureReason = "ICE restart superseded previous attempt",
                        failureCode = "ICE_CONNECTIVITY_FAILED",
                    )
                }
                val eventSessionId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                inFlightSessions[eventSessionId] = InFlightSession(
                    callId = callId,
                    callType = callType,
                    eventSessionId = eventSessionId,
                    stage = CallEventStage.PEER_CONNECTION_CONNECT,
                    startedAtMs = now,
                    joinStageAttemptIdSnapshot = joinStageAttemptIdMap[callId] ?: "",
                    peerConnectionRole = role,
                    wasPreviouslyConnected = wasPrev,
                    callSessionId = callSessionIdMap[callId],
                )
                activePcSessionIds[role] = eventSessionId
                sendEvent(
                    buildRequest(
                        callId = callId,
                        callType = callType,
                        stage = CallEventStage.PEER_CONNECTION_CONNECT,
                        eventType = CallEventType.INITIATED,
                        eventSessionId = eventSessionId,
                        joinStageAttemptId = joinStageAttemptId,
                        peerConnection = role,
                        wasPreviouslyConnected = wasPrev,
                        callSessionId = callSessionIdMap[callId],
                        iceState = iceState,
                        peerConnectionState = peerConnectionState,
                    ),
                )
            }

            PeerConnection.IceConnectionState.CONNECTED -> {
                val eventSessionId = activePcSessionIds.remove(role) ?: return
                pcEverConnected[role] = true
                completePeerConnectionSession(
                    callId = callId,
                    callType = callType,
                    eventSessionId = eventSessionId,
                    joinStageAttemptId = joinStageAttemptId,
                    success = true,
                    iceState = iceState,
                    peerConnectionState = peerConnectionState,
                )
            }

            PeerConnection.IceConnectionState.FAILED -> {
                val eventSessionId = activePcSessionIds.remove(role) ?: return
                completePeerConnectionSession(
                    callId = callId,
                    callType = callType,
                    eventSessionId = eventSessionId,
                    joinStageAttemptId = joinStageAttemptId,
                    success = false,
                    iceState = iceState,
                    peerConnectionState = peerConnectionState,
                    failureReason = "ICE connectivity checks failed",
                    failureCode = "ICE_CONNECTIVITY_FAILED",
                )
            }

            else -> { /* DISCONNECTED handled by ICE restart → CHECKING */ }
        }
    }

    private fun completePeerConnectionSession(
        callId: String,
        callType: String,
        eventSessionId: String,
        joinStageAttemptId: String,
        success: Boolean,
        iceState: PeerConnection.IceConnectionState,
        peerConnectionState: PeerConnection.PeerConnectionState?,
        failureReason: String? = null,
        failureCode: String? = null,
    ) {
        val session = inFlightSessions.remove(eventSessionId) ?: return
        val elapsedTime = System.currentTimeMillis() - session.startedAtMs
        sendEvent(
            buildRequest(
                callId = callId,
                callType = callType,
                stage = CallEventStage.PEER_CONNECTION_CONNECT,
                eventType = CallEventType.COMPLETED,
                eventSessionId = eventSessionId,
                joinStageAttemptId = joinStageAttemptId,
                elapsedTime = elapsedTime,
                outcome = if (success) CallEventOutcome.SUCCESS else CallEventOutcome.FAILURE,
                retryCountAttempt = 0,
                retryFailureReason = if (!success) failureReason else null,
                retryFailureCode = if (!success) failureCode else null,
                peerConnection = session.peerConnectionRole,
                wasPreviouslyConnected = session.wasPreviouslyConnected,
                iceState = iceState,
                peerConnectionState = peerConnectionState,
            ),
        )
    }

    internal fun abortAllInFlight(reason: AbortReason) {
        val snapshot = inFlightSessions.values.toList()
        inFlightSessions.clear()
        activePcSessionIds.clear()
        val now = System.currentTimeMillis()
        val events = snapshot.map { session ->
            buildRequest(
                callId = session.callId,
                callType = session.callType,
                stage = session.stage,
                eventType = CallEventType.COMPLETED,
                eventSessionId = session.eventSessionId,
                elapsedTime = now - session.startedAtMs,
                outcome = CallEventOutcome.FAILURE,
                retryCountAttempt = 0,
                retryFailureReason = reason.message,
                retryFailureCode = reason.code,
                sfuId = session.sfuId,
                callSessionId = session.callSessionId,
                peerConnection = session.peerConnectionRole,
                wasPreviouslyConnected = session.wasPreviouslyConnected,
                userSessionId = session.userSessionId,
                joinStageAttemptId = session.joinStageAttemptIdSnapshot,
            )
        }
        sendEvents(events)
    }

    // --- Request builder ---

    private fun buildRequest(
        callId: String,
        callType: String,
        stage: CallEventStage,
        eventType: CallEventType,
        eventSessionId: String,
        joinStageAttemptId: String,
        elapsedTime: Long? = null,
        outcome: CallEventOutcome? = null,
        retryCountAttempt: Int? = null,
        retryFailureReason: String? = null,
        retryFailureCode: String? = null,
        callSessionId: String? = null,
        sfuId: String? = null,
        peerConnection: PeerConnectionRole? = null,
        wasPreviouslyConnected: Boolean? = null,
        iceState: PeerConnection.IceConnectionState? = null,
        peerConnectionState: PeerConnection.PeerConnectionState? = null,
        userSessionId: String? = null,
    ): ClientEvent = ClientEvent(
        eventSessionId = eventSessionId,
        joinSuccessId = joinStageAttemptId,
        eventType = eventType.value,
        id = callId,
        sdkVersion = sdkVersion,
        stage = stage.value,
        timestamp = OffsetDateTime.now(ZoneOffset.UTC),
        type = callType,
        userAgent = userAgent.invoke().take(512),
        userId = StreamVideo.instanceOrNull()?.userId,
        callSessionId = callSessionId,
        elapsedTime = elapsedTime?.toInt(),
        iceState = iceState?.name,
        outcome = outcome?.value,
        peerConnection = peerConnection?.value,
        previouslyConnectedTimestamp = null,
        retryCountAttempt = retryCountAttempt,
        retryFailureCode = retryFailureCode,
        retryFailureReason = retryFailureReason,
        sfuId = sfuId,
        userSessionId = userSessionId,
        wasPreviouslyConnected = wasPreviouslyConnected,
    )

    // --- Delivery ---

    private fun sendEvent(event: ClientEvent) {
        when (sendingStrategy) {
            TelemetrySendingStrategy.BATCH -> { }
            TelemetrySendingStrategy.IN_PLACE -> {
                scope.launch {
                    // TODO: wrap with StreamRetryPolicy when retries are added
                    runCatching {
                        logger.d { event.toLog() }
                        api.reportClientCallEvent(ReportClientEventRequest(arrayListOf(event)))
                    }.onFailure { e ->
                        logger.w { "[sendEvent] Failed to send client event: ${e.message}" }
                    }
                }
            }
        }
    }

    private fun sendEvents(events: List<ClientEvent>) {
        scope.launch {
            // TODO: wrap with StreamRetryPolicy when retries are added
            runCatching {
                logger.d { events.map { it.toLog() }.joinToString { "," } }
                api.reportClientCallEvent(ReportClientEventRequest(events))
            }.onFailure { e ->
                logger.w { "[sendEvent] Failed to send client event: ${e.message}" }
            }
        }
    }
}
