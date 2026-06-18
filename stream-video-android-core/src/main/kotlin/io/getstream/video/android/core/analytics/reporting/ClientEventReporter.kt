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

import android.util.Log
import io.getstream.android.video.generated.apis.ProductvideoApi
import io.getstream.android.video.generated.models.ClientEvent
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.BuildConfig
import io.getstream.video.android.core.analytics.call.observer.model.JoinReason
import io.getstream.video.android.core.analytics.coordinator.CoordinatorAnalyticsStateHolder
import io.getstream.video.android.core.analytics.reporting.dispatcher.EventDispatcher
import io.getstream.video.android.core.analytics.reporting.dispatcher.ImmediateEventDispatcher
import io.getstream.video.android.core.analytics.reporting.model.CoordinatorFlightSession
import io.getstream.video.android.core.analytics.reporting.model.EventOutcome
import io.getstream.video.android.core.analytics.reporting.model.EventStage
import io.getstream.video.android.core.analytics.reporting.model.EventType
import io.getstream.video.android.core.analytics.reporting.model.InFlightSession
import io.getstream.video.android.core.analytics.reporting.model.PeerConnectionRole
import io.getstream.video.android.core.analytics.reporting.model.PostCallFlightSession
import io.getstream.video.android.core.analytics.reporting.model.StageId
import io.getstream.video.android.core.header.HeadersUtil
import io.getstream.video.android.core.socket.common.scope.UserScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import org.webrtc.PeerConnection
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal class ClientEventReporter(
    private val sender: EventDispatcher,
    private val userAgent: () -> String,
    private val sdkVersion: String,
    private val coordinatorAnalyticsStateHolder: CoordinatorAnalyticsStateHolder,
) {

    companion object {

        fun getDefault(
            api: ProductvideoApi,
            coordinatorAnalyticsStateHolder: CoordinatorAnalyticsStateHolder,
        ): ClientEventReporter {
            return ClientEventReporter(
                sender = ImmediateEventDispatcher(
                    api = api,
                    scope = CoroutineScope(
                        SupervisorJob(UserScope().coroutineContext[Job]) +
                            Dispatchers.IO +
                            CoroutineExceptionHandler(handler = { _, throwable ->
                                Log.e("ClientEvent", "Error in ClientEventReporter: $throwable")
                            }),
                    ),
                ),
                userAgent = { HeadersUtil().buildSdkTrackingHeaders() },
                sdkVersion = BuildConfig.STREAM_VIDEO_VERSION,
                coordinatorAnalyticsStateHolder = coordinatorAnalyticsStateHolder,
            )
        }
    }

    private val logger by taggedLogger("ClientEventReporter")

    private val clientEventFactory =
        ClientEventFactory(sdkVersion, userAgent, coordinatorAnalyticsStateHolder)

    private val postCallFlightSessions = ConcurrentHashMap<StageId, InFlightSession>()
    private val pcEverConnected = ConcurrentHashMap<PeerConnectionRole, PcConnected>()
    private val pcEventReporterStateHolder = PeerConnectionEventReporterStateHolder()

    // --- Coordinator WS ---
    internal fun reportCoordinatorWSInitiated(): String {
        val stageId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        postCallFlightSessions[stageId] = CoordinatorFlightSession(
            stageId = stageId,
            coordinatorConnectId = coordinatorAnalyticsStateHolder.coordinatorConnectId.value,
            stage = EventStage.CoordinatorWs,
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
        if (session is CoordinatorFlightSession) {
            val elapsedTime = System.currentTimeMillis() - session.startedAtMs
            sender.send(
                clientEventFactory.buildRequest(
                    stage = EventStage.CoordinatorWs,
                    stageId = stageId,
                    outcome = if (success) EventOutcome.SUCCESS else EventOutcome.FAILURE,
                    retryCountAttempt = retryCount,
                    retryFailureCode = failureCode,
                    elapsedTime = elapsedTime,
                    retryFailureReason = failureReason,
                    eventType = EventType.COMPLETED,
                ),
            )
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

    // --- Coordinator Join ---

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
        failureCode: String? = null,
        callSessionId: String? = null,
    ) = completePostCall(stageId) { session, elapsedTime ->
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

    // --- SFU Join ---

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
        peerConnectionHashCode: Int,
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
        when (peerConnectionState) {
            PeerConnection.PeerConnectionState.CONNECTING -> {
                handleOnPeerConnectionConnectingState(
                    peerConnectionHashCode,
                    callId,
                    callType,
                    joinStageAttemptId,
                    callSessionId,
                    sfuId,
                    joinReason,
                    role,
                    iceState,
                    peerConnectionState,
                )
            }
            PeerConnection.PeerConnectionState.CONNECTED -> {
                iceState?.let {
                    handleOnPeerConnectionConnectedState(
                        peerConnectionHashCode,
                        callId,
                        callType,
                        joinStageAttemptId,
                        callSessionId,
                        sfuId,
                        joinReason,
                        role,
                        iceState,
                        peerConnectionState,
                    )
                }
            }
            PeerConnection.PeerConnectionState.FAILED -> {
                iceState?.let {
                    handleOnPeerConnectionFailedState(
                        peerConnectionHashCode,
                        callId,
                        callType,
                        joinStageAttemptId,
                        callSessionId,
                        sfuId,
                        joinReason,
                        role,
                        iceState,
                        peerConnectionState,
                    )
                }
            }
            else -> {}
        }
    }

    fun handleOnPeerConnectionConnectingState(
        peerConnectionHashCode: Int,
        callId: String,
        callType: String,
        joinStageAttemptId: String,
        callSessionId: String,
        sfuId: String,
        joinReason: JoinReason,
        role: PeerConnectionRole,
        iceState: PeerConnection.IceConnectionState?,
        peerConnectionState: PeerConnection.PeerConnectionState,
    ) {
        val wasPrevConnected = pcEverConnected[role] != null
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
            wasPreviouslyConnected = wasPrevConnected,
            callSessionId = callSessionId,
            joinReason = joinReason,
            sfuId = sfuId,
        )
        pcEventReporterStateHolder.map[peerConnectionHashCode] =
            PeerConnectionEventReporterState(stageId, role)

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
                wasPreviouslyConnected = wasPrevConnected,
                callSessionId = callSessionId,
                iceState = iceState,
                peerConnectionState = peerConnectionState,
                joinReason = joinReason,
            ),
        )
    }

    fun handleOnPeerConnectionConnectedState(
        peerConnectionHashCode: Int,
        callId: String,
        callType: String,
        joinStageAttemptId: String,
        callSessionId: String,
        sfuId: String,
        joinReason: JoinReason,
        role: PeerConnectionRole,
        iceState: PeerConnection.IceConnectionState,
        peerConnectionState:
        PeerConnection.PeerConnectionState,
    ) {
        pcEverConnected[role] = PcConnected(System.currentTimeMillis())
        val pcState = pcEventReporterStateHolder.map.remove(peerConnectionHashCode) ?: return
        val stageId = pcState.stageId

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

    fun handleOnPeerConnectionFailedState(
        peerConnectionHashCode: Int,
        callId: String,
        callType: String,
        joinStageAttemptId: String,
        callSessionId: String,
        sfuId: String,
        joinReason: JoinReason,
        role: PeerConnectionRole,
        iceState: PeerConnection.IceConnectionState,
        peerConnectionState: PeerConnection.PeerConnectionState,
    ) {
        val pcState = pcEventReporterStateHolder.map.remove(peerConnectionHashCode) ?: return
        val stageId = pcState.stageId
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

    private inline fun completePostCall(
        stageId: String,
        build: (session: PostCallFlightSession, elapsedMs: Long) -> ClientEvent?,
    ) {
        val session = postCallFlightSessions.remove(stageId) ?: return
        if (session !is PostCallFlightSession) return
        val elapsedMs = System.currentTimeMillis() - session.startedAtMs
        build(session, elapsedMs)?.let(sender::send)
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

    internal fun reportFirstRemoteVideoFrameRendered(
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

    internal fun abortAllPostCallInFlight(
        publisherIceState: PeerConnection.IceConnectionState?,
        subscriberIceState: PeerConnection.IceConnectionState?,
        failCode: String,
        failMessage: String,
    ) {
        val snapshot = mutableListOf<PostCallFlightSession>()
        postCallFlightSessions.entries.forEach { (stageId, session) ->
            if (session is PostCallFlightSession && postCallFlightSessions.remove(stageId, session)) {
                snapshot += session
            }
        }
        val now = System.currentTimeMillis()
        val events = snapshot.map { session ->
            val iceState = when (session.peerConnectionRole) {
                PeerConnectionRole.PUBLISH -> {
                    publisherIceState
                }

                PeerConnectionRole.SUBSCRIBE -> {
                    subscriberIceState
                }

                else -> null
            }

            clientEventFactory.buildRequest(
                callId = session.callId,
                callType = session.callType,
                stage = session.stage,
                eventType = EventType.COMPLETED,
                stageId = session.stageId,
                elapsedTime = now - session.startedAtMs,
                outcome = EventOutcome.FAILURE,
                retryCountAttempt = 0,
                retryFailureReason = failMessage,
                retryFailureCode = failCode,
                sfuId = session.sfuId,
                callSessionId = session.callSessionId,
                peerConnection = session.peerConnectionRole,
                iceState = iceState,
                wasPreviouslyConnected = session.wasPreviouslyConnected,
                userSessionId = session.userSessionId,
                joinStageAttemptId = session.joinStageAttemptIdSnapshot,
                joinReason = session.joinReason,
            )
        }
        sender.sendAll(events)
    }
}

internal typealias ObjectHashCode = Int
internal class PeerConnectionEventReporterStateHolder {
    val map = ConcurrentHashMap<ObjectHashCode, PeerConnectionEventReporterState>()
}
internal class PeerConnectionEventReporterState(
    var stageId: String,
    val peerConnectionRole: PeerConnectionRole,
)
internal class PcConnected(val lastConnectedTime: Long)
