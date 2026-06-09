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

package io.getstream.video.android.core.analytics.reporting

import io.getstream.android.video.generated.models.ClientEvent
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.analytics.call.observer.model.JoinReason
import io.getstream.video.android.core.analytics.reporting.dispatcher.EventDispatcher
import io.getstream.video.android.core.analytics.reporting.model.AnalyticsCallAbortReason
import io.getstream.video.android.core.analytics.reporting.model.EventOutcome
import io.getstream.video.android.core.analytics.reporting.model.EventStage
import io.getstream.video.android.core.analytics.reporting.model.EventType
import io.getstream.video.android.core.analytics.reporting.model.InFlightSession
import io.getstream.video.android.core.analytics.reporting.model.PeerConnectionRole
import io.getstream.video.android.core.analytics.reporting.model.PostCallFlightSession
import io.getstream.video.android.core.analytics.reporting.model.PreCallInFlightSession
import io.getstream.video.android.core.analytics.reporting.model.StageId
import org.webrtc.PeerConnection
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

/**
 * TODO
 * [ClientEvent.previouslyConnectedTimestamp] : Ask clarification
 * [ClientEvent.retryFailureCode] : Ask clarification
 */

internal class ClientEventReporter(
    private val sender: EventDispatcher,
    private val userAgent: () -> String,
    private val sdkVersion: String,
) {
    private val logger by taggedLogger("ClientEventReporter")
    private val clientEventFactory = ClientEventFactory(sdkVersion, userAgent) {
        this.coordinatorConnectId
    }

    private val postCallFlightSessions = ConcurrentHashMap<StageId, InFlightSession>()

    // Active event_session_id per PC role — drives the ICE state machine
    private val activePcSessionIds = ConcurrentHashMap<PeerConnectionRole, String>()

    // Whether each PC role has ever reached CONNECTED (for was_previously_connected)
    private val pcEverConnected = ConcurrentHashMap<PeerConnectionRole, Boolean>()

    @Volatile
    private var coordinatorConnectId = ""

    private inline fun completePostCall(
        stageId: String,
        build: (session: PostCallFlightSession, elapsedMs: Long) -> ClientEvent?,
    ) {
        val session = postCallFlightSessions.remove(stageId) ?: return
        if (session !is PostCallFlightSession) return
        val elapsedMs = System.currentTimeMillis() - session.startedAtMs
        build(session, elapsedMs)?.let(sender::send)
    }

//    private fun callSessionId(callId: CallId): String? = callSessionIdMap[callId]
//
//    private fun rememberCallSession(callId: CallId, id: String?) {
//        callSessionIdMap[callId] = id ?: ""
//    }

    internal fun reportCoordinatorWSInitiated(): String {
        this.coordinatorConnectId = UUID.randomUUID().toString()
        val stageId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        postCallFlightSessions[stageId] = PreCallInFlightSession(
            stageId = stageId,
            coordinatorConnectId = coordinatorConnectId,
            stage = EventStage.Call.COORDINATOR_JOIN,
            startedAtMs = now,
        )
        sender.send(
            clientEventFactory.buildRequest(
                stageId = stageId,
                stage = EventStage.CoordinatorWs,
                eventType = EventType.INITIATED,
            ),
        )
        return stageId
    }

    internal fun reportCoordinatorWSCompleted(
        stageId: String,
        success: Boolean,
        retryCount: Int? = null,
        failureCode: String? = null,
        failureReason: String? = null,
    ) {
        val session = postCallFlightSessions.remove(stageId) ?: return
        if (session is PreCallInFlightSession) {
            val elapsedTime = System.currentTimeMillis() - session.startedAtMs
            sender.send(
                clientEventFactory.buildRequest(
                    stage = EventStage.CoordinatorWs,
                    outcome = if (success) EventOutcome.SUCCESS else EventOutcome.FAILURE,
                    retryCountAttempt = retryCount,
                    retryFailureCode = failureCode,
                    elapsedTime = elapsedTime,
                    retryFailureReason = failureReason,
                    eventType = EventType.COMPLETED,
                ),
            )
        }
        if (success) {
            sender.retryPending()
        }
    }

    internal fun reportSdkMethodJoinInitiated(
        callId: String,
        callType: String,
        joinStageAttemptId: String,
    ) {
        sender.send(
            clientEventFactory.buildRequest(
                callId,
                callType,
                stage = EventStage.Call.JOIN_INITIATED,
                eventType = EventType.INITIATED,
                joinStageAttemptId = joinStageAttemptId,
            ),
        )
    }

    // --- CoordinatorJoin ---

    internal fun reportCoordinatorJoinInitiated(
        callId: String,
        callType: String,
        joinStageAttemptId: String,
        joinReason: JoinReason,
    ): String {
        val stageId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        postCallFlightSessions[stageId] = PostCallFlightSession(
            stageId = stageId,
            callId = callId,
            callType = callType,
            stage = EventStage.Call.COORDINATOR_JOIN,
            startedAtMs = now,
            joinStageAttemptIdSnapshot = joinStageAttemptId,
            joinReason = joinReason,
        )
        sender.send(
            clientEventFactory.buildRequest(
                callId,
                callType,
                stage = EventStage.Call.COORDINATOR_JOIN,
                eventType = EventType.INITIATED,
                stageId = stageId,
                joinStageAttemptId = joinStageAttemptId,
                joinReason = joinReason,
            ),
        )
        return stageId
    }

    internal fun reportCoordinatorJoinCompleted(
        stageId: String,
        success: Boolean,
        retryCount: Int,
        failureReason: String? = null,
        failureCode: String? = null, // TODO Rahul, ask tomorrow
        callSessionId: String? = null,
    ) = completePostCall(stageId) { session, elapsedTime ->
//        rememberCallSession(session.callId, callSessionId)
        clientEventFactory.buildRequest(
            callId = session.callId,
            callType = session.callType,
            stage = EventStage.Call.COORDINATOR_JOIN,
            eventType = EventType.COMPLETED,
            stageId = stageId,
            elapsedTime = elapsedTime,
            outcome = if (success) EventOutcome.SUCCESS else EventOutcome.FAILURE,
            retryCountAttempt = retryCount,
            retryFailureReason = if (!success) failureReason else null,
            retryFailureCode = if (!success) failureCode else null,
            callSessionId = callSessionId,
            joinStageAttemptId = session.joinStageAttemptIdSnapshot,
            joinReason = session.joinReason,

        )
    }

    // --- WSJoin ---

    internal fun reportSfuWsJoinInitiated(
        sfuId: String,
        callId: String,
        callType: String,
        joinStageAttemptId: String,
        callSessionId: String,
        joinReason: JoinReason,
        wasPreviouslyConnected: Boolean,
    ): String {
        val stageId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        postCallFlightSessions[stageId] = PostCallFlightSession(
            callId = callId,
            callType = callType,
            stageId = stageId,
            stage = EventStage.Call.WS_JOIN,
            startedAtMs = now,
            joinStageAttemptIdSnapshot = joinStageAttemptId,
            sfuId = sfuId,
            wasPreviouslyConnected = wasPreviouslyConnected,
            callSessionId = callSessionId,
            joinReason = joinReason,
        )
        sender.send(
            clientEventFactory.buildRequest(
                callId = callId,
                callType = callType,
                stage = EventStage.Call.WS_JOIN,
                eventType = EventType.INITIATED,
                stageId = stageId,
                joinStageAttemptId = joinStageAttemptId,
                sfuId = sfuId,
                wasPreviouslyConnected = wasPreviouslyConnected,
                callSessionId = callSessionId,
                joinReason = joinReason,
            ),
        )
        return stageId
    }

    internal fun reportSfuWsJoinCompleted(
        stageId: String,
        joinStageAttemptId: String,
        success: Boolean,
        retryCount: Int,
        failureReason: String? = null,
        failureCode: String? = null,
    ) = completePostCall(stageId) { session, elapsedTime ->
        clientEventFactory.buildRequest(
            callId = session.callId,
            callType = session.callType,
            stage = EventStage.Call.WS_JOIN,
            eventType = EventType.COMPLETED,
            stageId = stageId,
            elapsedTime = elapsedTime,
            outcome = if (success) EventOutcome.SUCCESS else EventOutcome.FAILURE,
            retryCountAttempt = retryCount,
            retryFailureReason = if (!success) failureReason else null,
            retryFailureCode = if (!success) failureCode else null,
            sfuId = session.sfuId,
            callSessionId = session.callSessionId,
            joinStageAttemptId = joinStageAttemptId,
            joinReason = session.joinReason,
        )
    }

    // --- PeerConnectionConnect (ICE state machine) ---

    internal fun onPeerConnectionStateChanged(
        callId: String,
        callType: String,
        joinStageAttemptId: String,
        callSessionId: String,
        sfuId: String,
        joinReason: JoinReason,
        role: PeerConnectionRole,
        iceState: PeerConnection.IceConnectionState?,
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
                        stageId = oldId,
                        joinStageAttemptId = joinStageAttemptId,
                        success = false,
                        iceState = iceState,
                        peerConnectionState = peerConnectionState,
                        failureReason = "ICE restart superseded previous attempt",
                        failureCode = "ICE_CONNECTIVITY_FAILED",
                        joinReason = joinReason,
                        sfuId = sfuId,
                        callSessionId = callSessionId,
                    )
                }
                val stageId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                postCallFlightSessions[stageId] = PostCallFlightSession(
                    callId = callId,
                    callType = callType,
                    stageId = stageId,
                    stage = EventStage.Call.PEER_CONNECTION_CONNECT,
                    startedAtMs = now,
                    joinStageAttemptIdSnapshot = joinStageAttemptId,
                    peerConnectionRole = role,
                    wasPreviouslyConnected = wasPrev,
                    callSessionId = callSessionId,
                    joinReason = joinReason,
                    sfuId = sfuId,
                )
                activePcSessionIds[role] = stageId
                sender.send(
                    clientEventFactory.buildRequest(
                        callId = callId,
                        callType = callType,
                        stage = EventStage.Call.PEER_CONNECTION_CONNECT,
                        eventType = EventType.INITIATED,
                        stageId = stageId,
                        sfuId = sfuId,
                        joinStageAttemptId = joinStageAttemptId,
                        peerConnection = role,
                        wasPreviouslyConnected = wasPrev,
                        callSessionId = callSessionId,
                        iceState = iceState,
                        peerConnectionState = peerConnectionState,
                        joinReason = joinReason,
                    ),
                )
            }

            PeerConnection.IceConnectionState.CONNECTED -> {
                val stageId = activePcSessionIds.remove(role) ?: return
                pcEverConnected[role] = true
                completePeerConnectionSession(
                    callId = callId,
                    callType = callType,
                    stageId = stageId,
                    joinStageAttemptId = joinStageAttemptId,
                    success = true,
                    iceState = iceState,
                    peerConnectionState = peerConnectionState,
                    joinReason = joinReason,
                    sfuId = sfuId,
                    callSessionId = callSessionId,
                )
            }

            PeerConnection.IceConnectionState.FAILED -> {
                val stageId = activePcSessionIds.remove(role) ?: return
                completePeerConnectionSession(
                    callId = callId,
                    callType = callType,
                    stageId = stageId,
                    joinStageAttemptId = joinStageAttemptId,
                    success = false,
                    iceState = iceState,
                    peerConnectionState = peerConnectionState,
                    failureReason = "ICE connectivity checks failed",
                    failureCode = "ICE_CONNECTIVITY_FAILED",
                    joinReason = joinReason,
                    sfuId = sfuId,
                    callSessionId = callSessionId,
                )
            }

            else -> {
                /* DISCONNECTED handled by ICE restart → CHECKING */
            }
        }
    }

    private fun completePeerConnectionSession(
        callId: String,
        callType: String,
        stageId: String,
        sfuId: String,
        joinStageAttemptId: String,
        callSessionId: String,
        joinReason: JoinReason,
        success: Boolean,
        iceState: PeerConnection.IceConnectionState,
        peerConnectionState: PeerConnection.PeerConnectionState?,
        failureReason: String? = null,
        failureCode: String? = null,
    ) = completePostCall(stageId) { session, elapsedTime ->
        clientEventFactory.buildRequest(
            callId = callId,
            callSessionId = callSessionId,
            callType = callType,
            stage = EventStage.Call.PEER_CONNECTION_CONNECT,
            eventType = EventType.COMPLETED,
            stageId = stageId,
            sfuId = sfuId,
            joinStageAttemptId = joinStageAttemptId,
            elapsedTime = elapsedTime,
            outcome = if (success) EventOutcome.SUCCESS else EventOutcome.FAILURE,
            retryCountAttempt = 0,
            retryFailureReason = if (!success) failureReason else null,
            retryFailureCode = if (!success) failureCode else null,
            peerConnection = session.peerConnectionRole,
            wasPreviouslyConnected = session.wasPreviouslyConnected,
            iceState = iceState,
            peerConnectionState = peerConnectionState,
            joinReason = joinReason,
        )
    }

    internal fun reportFirstAudioFrameRendered(
        sfuId: String,
        callId: String,
        callType: String,
        joinStageAttemptId: String,
        callSessionId: String,
        joinReason: JoinReason,
    ): String {
        val stageId = UUID.randomUUID().toString()
        sender.send(
            clientEventFactory.buildRequest(
                callId = callId,
                callType = callType,
                stage = EventStage.Call.FIRST_AUDIO_FRAME_RENDERED,
                eventType = EventType.INITIATED,
                stageId = stageId,
                joinStageAttemptId = joinStageAttemptId,
                callSessionId = callSessionId,
                sfuId = sfuId,
                joinReason = joinReason,
            ),
        )
        return stageId
    }

    internal fun reportFirstVideoFrameRendered(
        sfuId: String,
        callId: String,
        callType: String,
        joinStageAttemptId: String,
        callSessionId: String,
        joinReason: JoinReason,
        trackId: String,
    ): String {
        val stageId = UUID.randomUUID().toString()
        sender.send(
            clientEventFactory.buildRequest(
                callId = callId,
                callType = callType,
                stage = EventStage.Call.FIRST_VIDEO_FRAME_RENDERED,
                eventType = EventType.INITIATED,
                stageId = stageId,
                joinStageAttemptId = joinStageAttemptId,
                callSessionId = callSessionId,
                sfuId = sfuId,
                trackId = trackId,
                joinReason = joinReason,
            ),
        )
        return stageId
    }

    internal fun reportMediaPermissionStatus(
        callId: String,
        callType: String,
        joinStageAttemptId: String,
        joinReason: JoinReason,
        isCameraGranted: Boolean,
        isMicrophoneGranted: Boolean,
    ): String {
        val stageId = UUID.randomUUID().toString()
        sender.send(
            clientEventFactory.buildRequest(
                callId = callId,
                callType = callType,
                stage = EventStage.Call.MEDIA_DEVICE_PERMISSION,
                eventType = EventType.INITIATED,
                stageId = stageId,
                joinStageAttemptId = joinStageAttemptId,
                cameraAllowed = isCameraGranted,
                microphoneAllowed = isMicrophoneGranted,
                joinReason = joinReason,
            ),
        )
        return stageId
    }

    internal fun abortAllPostCallInFlight(reason: AnalyticsCallAbortReason) {
        val snapshot: List<PostCallFlightSession> =
            postCallFlightSessions.values.filterIsInstance<PostCallFlightSession>().toList()
        postCallFlightSessions.clear()
        activePcSessionIds.clear()
        val now = System.currentTimeMillis()
        val events = snapshot.map { session ->
            clientEventFactory.buildRequest(
                callId = session.callId,
                callType = session.callType,
                stage = session.stage,
                eventType = EventType.COMPLETED,
                stageId = session.stageId,
                elapsedTime = now - session.startedAtMs,
                outcome = EventOutcome.FAILURE,
                retryCountAttempt = 0,
                retryFailureReason = reason.message,
                retryFailureCode = reason.code,
                sfuId = session.sfuId,
                callSessionId = session.callSessionId,
                peerConnection = session.peerConnectionRole,
                wasPreviouslyConnected = session.wasPreviouslyConnected,
                userSessionId = session.userSessionId,
                joinStageAttemptId = session.joinStageAttemptIdSnapshot,
                joinReason = session.joinReason,
            )
        }
        sender.sendAll(events)
    }

    fun deleteAll() {
        sender.deleteAll()
    }
}
