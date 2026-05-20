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
import io.getstream.video.android.core.socket.common.scope.ClientScope
import io.getstream.video.android.core.socket.common.scope.UserScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import org.webrtc.PeerConnection
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private typealias EventSessionId = String

internal class ClientEventReporter(
    private val api: ProductvideoApi,
    private val callType: String,
    private val callId: String,
    private val callCid: String,
    private val userId: String,
    private val userAgent: () -> String,
    private val sdkVersion: String,
    private val scope: CoroutineScope = UserScope(ClientScope()),
) {
    private val logger by taggedLogger("CallEventReporter:$callCid")

    @Volatile private var joinSuccessId: String = UUID.randomUUID().toString()

    private val inFlightSessions = ConcurrentHashMap<EventSessionId, InFlightSession>()

    // Active event_session_id per PC role — drives the ICE state machine
    private val activePcSessionIds = ConcurrentHashMap<PeerConnectionRole, String>()

    // Whether each PC role has ever reached CONNECTED (for was_previously_connected)
    private val pcEverConnected = ConcurrentHashMap<PeerConnectionRole, Boolean>()

    private data class InFlightSession(
        val eventSessionId: EventSessionId,
        val stage: CallEventStage,
        val startedAtMs: Long,
        val joinSuccessIdSnapshot: String,
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

    // --- join_success_id ---

    internal fun resetJoinSuccessId() {
        joinSuccessId = UUID.randomUUID().toString()
    }

    // --- CoordinatorJoin ---

    internal fun reportCoordinatorJoinInitiated(): String {
        val eventSessionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        inFlightSessions[eventSessionId] = InFlightSession(
            eventSessionId = eventSessionId,
            stage = CallEventStage.COORDINATOR_JOIN,
            startedAtMs = now,
            joinSuccessIdSnapshot = joinSuccessId,
        )
        sendEvent(
            buildRequest(
                stage = CallEventStage.COORDINATOR_JOIN,
                eventType = CallEventType.INITIATED,
                eventSessionId = eventSessionId,
            ),
        )
        return eventSessionId
    }

    internal fun reportCoordinatorJoinCompleted(
        eventSessionId: String,
        success: Boolean,
        retryCount: Int,
        failureReason: String? = null,
        failureCode: String? = null,
        callSessionId: String? = null,
    ) {
        val session = inFlightSessions.remove(eventSessionId) ?: return
        val elapsedTime = System.currentTimeMillis() - session.startedAtMs
        sendEvent(
            buildRequest(
                stage = CallEventStage.COORDINATOR_JOIN,
                eventType = CallEventType.COMPLETED,
                eventSessionId = eventSessionId,
                elapsedTime = elapsedTime,
                outcome = if (success) CallEventOutcome.SUCCESS else CallEventOutcome.FAILURE,
                retryCountAttempt = retryCount,
                retryFailureReason = if (!success) failureReason else null,
                retryFailureCode = if (!success) failureCode else null,
                callSessionId = callSessionId,
            ),
        )
    }

    // --- WSJoin ---

    internal fun reportWsJoinInitiated(
        sfuId: String,
        wasPreviouslyConnected: Boolean,
    ): String {
        val eventSessionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        inFlightSessions[eventSessionId] = InFlightSession(
            eventSessionId = eventSessionId,
            stage = CallEventStage.WS_JOIN,
            startedAtMs = now,
            joinSuccessIdSnapshot = joinSuccessId,
            sfuId = sfuId,
            wasPreviouslyConnected = wasPreviouslyConnected,
        )
        sendEvent(
            buildRequest(
                stage = CallEventStage.WS_JOIN,
                eventType = CallEventType.INITIATED,
                eventSessionId = eventSessionId,
                sfuId = sfuId,
                wasPreviouslyConnected = wasPreviouslyConnected,
            ),
        )
        return eventSessionId
    }

    internal fun reportWsJoinCompleted(
        eventSessionId: String,
        success: Boolean,
        retryCount: Int,
        failureReason: String? = null,
        failureCode: String? = null,
        callSessionId: String? = null,
    ) {
        val session = inFlightSessions.remove(eventSessionId) ?: return
        val elapsedTime = System.currentTimeMillis() - session.startedAtMs
        sendEvent(
            buildRequest(
                stage = CallEventStage.WS_JOIN,
                eventType = CallEventType.COMPLETED,
                eventSessionId = eventSessionId,
                elapsedTime = elapsedTime,
                outcome = if (success) CallEventOutcome.SUCCESS else CallEventOutcome.FAILURE,
                retryCountAttempt = retryCount,
                retryFailureReason = if (!success) failureReason else null,
                retryFailureCode = if (!success) failureCode else null,
                sfuId = session.sfuId,
                callSessionId = callSessionId,
            ),
        )
    }

    // --- PeerConnectionConnect (ICE state machine) ---

    internal fun onPeerConnectionIceStateChanged(
        role: PeerConnectionRole,
        iceState: PeerConnection.IceConnectionState,
        dtlsState: PeerConnection.PeerConnectionState?,
    ) {
        when (iceState) {
            PeerConnection.IceConnectionState.CHECKING -> {
                val wasPrev = pcEverConnected[role] == true
                // If an existing session is still in-flight, close it as failed first
                activePcSessionIds.remove(role)?.let { oldId ->
                    completePeerConnectionSession(
                        eventSessionId = oldId,
                        success = false,
                        iceState = iceState,
                        failureReason = "ICE restart superseded previous attempt",
                        failureCode = "ICE_CONNECTIVITY_FAILED",
                    )
                }
                val eventSessionId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                inFlightSessions[eventSessionId] = InFlightSession(
                    eventSessionId = eventSessionId,
                    stage = CallEventStage.PEER_CONNECTION_CONNECT,
                    startedAtMs = now,
                    joinSuccessIdSnapshot = joinSuccessId,
                    peerConnectionRole = role,
                    wasPreviouslyConnected = wasPrev,
                )
                activePcSessionIds[role] = eventSessionId
                sendEvent(
                    buildRequest(
                        stage = CallEventStage.PEER_CONNECTION_CONNECT,
                        eventType = CallEventType.INITIATED,
                        eventSessionId = eventSessionId,
                        peerConnection = role,
                        wasPreviouslyConnected = wasPrev,
                    ),
                )
            }

            PeerConnection.IceConnectionState.CONNECTED -> {
                val eventSessionId = activePcSessionIds.remove(role) ?: return
                pcEverConnected[role] = true
                completePeerConnectionSession(
                    eventSessionId = eventSessionId,
                    success = true,
                    iceState = iceState,
                )
            }

            PeerConnection.IceConnectionState.FAILED -> {
                val eventSessionId = activePcSessionIds.remove(role) ?: return
                completePeerConnectionSession(
                    eventSessionId = eventSessionId,
                    success = false,
                    iceState = iceState,
                    failureReason = "ICE connectivity checks failed",
                    failureCode = "ICE_CONNECTIVITY_FAILED",
                )
            }

            else -> { /* DISCONNECTED handled by ICE restart → CHECKING */ }
        }
    }

    private fun completePeerConnectionSession(
        eventSessionId: String,
        success: Boolean,
        iceState: PeerConnection.IceConnectionState,
        failureReason: String? = null,
        failureCode: String? = null,
    ) {
        val session = inFlightSessions.remove(eventSessionId) ?: return
        val elapsedTime = System.currentTimeMillis() - session.startedAtMs
        sendEvent(
            buildRequest(
                stage = CallEventStage.PEER_CONNECTION_CONNECT,
                eventType = CallEventType.COMPLETED,
                eventSessionId = eventSessionId,
                elapsedTime = elapsedTime,
                outcome = if (success) CallEventOutcome.SUCCESS else CallEventOutcome.FAILURE,
                retryCountAttempt = 0,
                retryFailureReason = if (!success) failureReason else null,
                retryFailureCode = if (!success) failureCode else null,
                peerConnection = session.peerConnectionRole,
                wasPreviouslyConnected = session.wasPreviouslyConnected,
                iceState = iceState,
            ),
        )
    }

    // --- Abort all in-flight sessions (user left or backend ended call) ---

    internal fun abortAllInFlight(reason: AbortReason) {
        val snapshot = inFlightSessions.values.toList()
        inFlightSessions.clear()
        activePcSessionIds.clear()
        val now = System.currentTimeMillis()
        for (session in snapshot) {
            sendEvent(
                buildRequest(
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
                ),
            )
        }
    }

    // --- Request builder ---

    private fun buildRequest(
        stage: CallEventStage,
        eventType: CallEventType,
        eventSessionId: String,
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
        userSessionId: String? = null,
    ): ClientEvent = ClientEvent(
        eventSessionId = eventSessionId,
        eventType = eventType.value,
        id = callId,
        sdkVersion = sdkVersion,
        stage = stage.value,
        timestamp = OffsetDateTime.now(ZoneOffset.UTC),
        type = callType,
        userAgent = userAgent.invoke().take(512),
        userId = userId,
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

//    private fun sendEvent(request: ReportClientEventRequest) {
//        scope.launch {
//            // TODO: wrap with StreamRetryPolicy when retries are added
//            runCatching {
//                api.reportClientCallEvent(request)
//            }.onFailure { e ->
//                logger.w { "[sendEvent] Failed to send client event: ${e.message}" }
//            }
//        }
//    }

    private fun sendEvent(request: ClientEvent) {
        scope.launch {
            // TODO: wrap with StreamRetryPolicy when retries are added
            runCatching {
                api.reportClientCallEvent(ReportClientEventRequest(arrayListOf(request)))
            }.onFailure { e ->
                logger.w { "[sendEvent] Failed to send client event: ${e.message}" }
            }
        }
    }
}