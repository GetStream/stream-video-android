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
import io.getstream.video.android.core.analytics.call.observer.model.JoinReason
import io.getstream.video.android.core.analytics.reporting.dispatcher.EventDispatcher
import io.getstream.video.android.core.analytics.reporting.model.AnalyticsCallAbortReason
import io.getstream.video.android.core.analytics.reporting.model.EventOutcome
import io.getstream.video.android.core.analytics.reporting.model.EventStage
import io.getstream.video.android.core.analytics.reporting.model.EventType
import io.getstream.video.android.core.analytics.reporting.model.PeerConnectionRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.webrtc.PeerConnection

class ClientEventReporterTest {

    private class RecordingEventDispatcher : EventDispatcher {
        val sent = mutableListOf<ClientEvent>()
        val batches = mutableListOf<List<ClientEvent>>()
        var retryPendingCount = 0
        var deleteAllCount = 0

        override fun send(event: ClientEvent) {
            sent += event
        }

        override fun sendAll(events: List<ClientEvent>) {
            batches += events
            sent += events
        }

        override fun retryPending() {
            retryPendingCount++
        }

        override fun deleteAll() {
            deleteAllCount++
        }
    }

    private lateinit var dispatcher: RecordingEventDispatcher
    private lateinit var reporter: ClientEventReporter

    @Before
    fun setup() {
        dispatcher = RecordingEventDispatcher()
        reporter = ClientEventReporter(
            sender = dispatcher,
            userAgent = { "test-agent" },
            sdkVersion = "1.0.0",
        )
    }

    private fun openCoordinatorJoin() = reporter.reportCoordinatorJoinInitiated(
        callId = "call-1",
        callType = "default",
        joinStageAttemptId = "attempt-1",
        joinReason = JoinReason.FirstAttempt,
    )

    private fun openSfuWsJoin() = reporter.reportSfuWsJoinInitiated(
        sfuId = "sfu-1",
        callId = "call-1",
        callType = "default",
        joinStageAttemptId = "attempt-1",
        callSessionId = "session-1",
        joinReason = JoinReason.FirstAttempt,
        wasPreviouslyConnected = false,
    )

    private fun pcStateChanged(
        role: PeerConnectionRole = PeerConnectionRole.PUBLISH,
        iceState: PeerConnection.IceConnectionState,
        pcState: PeerConnection.PeerConnectionState? = null,
    ) = reporter.onPeerConnectionStateChanged(
        callId = "call-1",
        callType = "default",
        joinStageAttemptId = "attempt-1",
        callSessionId = "session-1",
        sfuId = "sfu-1",
        joinReason = JoinReason.FirstAttempt,
        role = role,
        iceState = iceState,
        peerConnectionState = pcState,
    )

    // --- Coordinator WS ---

    @Test
    fun `coordinator ws initiated sends an initiated event and returns its stage id`() {
        val stageId = reporter.reportCoordinatorWSInitiated()

        assertEquals(1, dispatcher.sent.size)
        val event = dispatcher.sent.first()
        assertTrue(stageId.isNotEmpty())
        assertEquals(stageId, event.stageId)
        assertEquals(EventStage.CoordinatorWs.value, event.stage)
        assertEquals(EventType.INITIATED.value, event.eventType)
        assertEquals("test-agent", event.userAgent)
        assertEquals("1.0.0", event.sdkVersion)
        assertTrue(event.coordinatorConnectId.orEmpty().isNotEmpty())
    }

    @Test
    fun `coordinator ws completed with success reports the outcome and replays pending events`() {
        val stageId = reporter.reportCoordinatorWSInitiated()

        reporter.reportCoordinatorWSCompleted(stageId, success = true, retryCount = 2)

        assertEquals(2, dispatcher.sent.size)
        val completed = dispatcher.sent.last()
        assertEquals(EventType.COMPLETED.value, completed.eventType)
        assertEquals(EventOutcome.SUCCESS.value, completed.outcome)
        assertEquals(2, completed.retryCountAttempt)
        assertNotNull(completed.elapsedTime)
        assertEquals(1, dispatcher.retryPendingCount)
    }

    @Test
    fun `coordinator ws completed with failure does not replay pending events`() {
        val stageId = reporter.reportCoordinatorWSInitiated()

        reporter.reportCoordinatorWSCompleted(
            stageId,
            success = false,
            retryCount = 3,
            failureCode = "WS_FAILED",
            failureReason = "socket closed",
        )

        val completed = dispatcher.sent.last()
        assertEquals(EventOutcome.FAILURE.value, completed.outcome)
        assertEquals("WS_FAILED", completed.retryFailureCode)
        assertEquals("socket closed", completed.retryFailureReason)
        assertEquals(0, dispatcher.retryPendingCount)
    }

    @Test
    fun `coordinator ws completed with an unknown stage id sends no event`() {
        reporter.reportCoordinatorWSCompleted("unknown-stage", success = true)

        assertTrue(dispatcher.sent.isEmpty())
    }

    // --- SDK join + coordinator join ---

    @Test
    fun `sdk method join initiated sends a JoinInitiated event`() {
        reporter.reportSdkMethodJoinInitiated("call-1", "default", "attempt-1")

        val event = dispatcher.sent.single()
        assertEquals(EventStage.Call.JOIN_INITIATED.value, event.stage)
        assertEquals(EventType.INITIATED.value, event.eventType)
        assertEquals("call-1", event.id)
        assertEquals("default", event.type)
        assertEquals("attempt-1", event.joinAttemptId)
    }

    @Test
    fun `coordinator join initiated and completed are paired by stage id`() {
        val stageId = openCoordinatorJoin()

        reporter.reportCoordinatorJoinCompleted(
            stageId = stageId,
            success = true,
            retryCount = 1,
            callSessionId = "session-1",
        )

        assertEquals(2, dispatcher.sent.size)
        val completed = dispatcher.sent.last()
        assertEquals(EventStage.Call.COORDINATOR_JOIN.value, completed.stage)
        assertEquals(EventType.COMPLETED.value, completed.eventType)
        assertEquals(EventOutcome.SUCCESS.value, completed.outcome)
        assertEquals(stageId, completed.stageId)
        assertEquals("session-1", completed.callSessionId)
        assertEquals("attempt-1", completed.joinAttemptId)
        assertEquals(JoinReason.FirstAttempt.message, completed.joinReason)
        assertNotNull(completed.elapsedTime)
    }

    @Test
    fun `completing the same coordinator join twice sends only one completed event`() {
        val stageId = openCoordinatorJoin()

        reporter.reportCoordinatorJoinCompleted(stageId, success = true, retryCount = 0)
        reporter.reportCoordinatorJoinCompleted(stageId, success = true, retryCount = 0)

        assertEquals(2, dispatcher.sent.size)
    }

    @Test
    fun `coordinator join failure carries the failure reason`() {
        val stageId = openCoordinatorJoin()

        reporter.reportCoordinatorJoinCompleted(
            stageId = stageId,
            success = false,
            retryCount = 3,
            failureReason = "join exhausted",
        )

        val completed = dispatcher.sent.last()
        assertEquals(EventOutcome.FAILURE.value, completed.outcome)
        assertEquals("join exhausted", completed.retryFailureReason)
        assertEquals(3, completed.retryCountAttempt)
    }

    // --- SFU WS join ---

    @Test
    fun `sfu ws join initiated and completed are paired by stage id`() {
        val stageId = openSfuWsJoin()

        reporter.reportSfuWsJoinCompleted(
            stageId = stageId,
            joinStageAttemptId = "attempt-1",
            success = true,
            retryCount = 0,
        )

        assertEquals(2, dispatcher.sent.size)
        val completed = dispatcher.sent.last()
        assertEquals(EventStage.Call.WS_JOIN.value, completed.stage)
        assertEquals(EventOutcome.SUCCESS.value, completed.outcome)
        assertEquals(stageId, completed.stageId)
        assertEquals("sfu-1", completed.sfuId)
        assertEquals("session-1", completed.callSessionId)
    }

    // --- Peer connection ICE state machine ---

    @Test
    fun `ICE CHECKING opens a session and CONNECTED completes it as success`() {
        pcStateChanged(
            iceState = PeerConnection.IceConnectionState.CHECKING,
            pcState = PeerConnection.PeerConnectionState.CONNECTING,
        )

        assertEquals(1, dispatcher.sent.size)
        val initiated = dispatcher.sent.first()
        assertEquals(EventStage.Call.PEER_CONNECTION_CONNECT.value, initiated.stage)
        assertEquals(EventType.INITIATED.value, initiated.eventType)
        assertEquals(PeerConnectionRole.PUBLISH.value, initiated.peerConnection)
        assertEquals(false, initiated.wasPreviouslyConnected)

        pcStateChanged(
            iceState = PeerConnection.IceConnectionState.CONNECTED,
            pcState = PeerConnection.PeerConnectionState.CONNECTED,
        )

        assertEquals(2, dispatcher.sent.size)
        val completed = dispatcher.sent.last()
        assertEquals(EventType.COMPLETED.value, completed.eventType)
        assertEquals(EventOutcome.SUCCESS.value, completed.outcome)
        assertEquals(initiated.stageId, completed.stageId)
    }

    @Test
    fun `ICE FAILED completes the session as a connectivity failure`() {
        pcStateChanged(iceState = PeerConnection.IceConnectionState.CHECKING)

        pcStateChanged(iceState = PeerConnection.IceConnectionState.FAILED)

        val completed = dispatcher.sent.last()
        assertEquals(EventOutcome.FAILURE.value, completed.outcome)
        assertEquals("ICE_CONNECTIVITY_FAILED", completed.retryFailureCode)
        assertEquals("ICE connectivity checks failed", completed.retryFailureReason)
    }

    @Test
    fun `a second CHECKING supersedes the first in-flight session as failure`() {
        pcStateChanged(iceState = PeerConnection.IceConnectionState.CHECKING)

        pcStateChanged(iceState = PeerConnection.IceConnectionState.CHECKING)

        assertEquals(3, dispatcher.sent.size)
        val superseded = dispatcher.sent[1]
        assertEquals(EventType.COMPLETED.value, superseded.eventType)
        assertEquals(EventOutcome.FAILURE.value, superseded.outcome)
        assertEquals("ICE restart superseded previous attempt", superseded.retryFailureReason)
        assertEquals(EventType.INITIATED.value, dispatcher.sent[2].eventType)
    }

    @Test
    fun `a reconnect after CONNECTED is flagged as previously connected`() {
        pcStateChanged(iceState = PeerConnection.IceConnectionState.CHECKING)
        pcStateChanged(iceState = PeerConnection.IceConnectionState.CONNECTED)

        pcStateChanged(iceState = PeerConnection.IceConnectionState.CHECKING)

        val reconnectInitiated = dispatcher.sent.last()
        assertEquals(EventType.INITIATED.value, reconnectInitiated.eventType)
        assertEquals(true, reconnectInitiated.wasPreviouslyConnected)
    }

    @Test
    fun `CONNECTED without an open session sends nothing`() {
        pcStateChanged(iceState = PeerConnection.IceConnectionState.CONNECTED)

        assertTrue(dispatcher.sent.isEmpty())
    }

    @Test
    fun `publisher and subscriber sessions are tracked independently`() {
        pcStateChanged(
            role = PeerConnectionRole.PUBLISH,
            iceState = PeerConnection.IceConnectionState.CHECKING,
        )
        pcStateChanged(
            role = PeerConnectionRole.SUBSCRIBE,
            iceState = PeerConnection.IceConnectionState.CHECKING,
        )

        assertEquals(2, dispatcher.sent.size)
        assertTrue(dispatcher.sent.all { it.eventType == EventType.INITIATED.value })
    }

    // --- Abort flush ---

    @Test
    fun `abortAllPostCallInFlight flushes open call sessions as failures and clears them`() {
        reporter.reportCoordinatorWSInitiated()
        val joinStageId = openCoordinatorJoin()
        openSfuWsJoin()
        dispatcher.sent.clear()

        reporter.abortAllPostCallInFlight(AnalyticsCallAbortReason.CLIENT_ABORTED)

        assertEquals(1, dispatcher.batches.size)
        val aborted = dispatcher.batches.first()
        assertEquals(2, aborted.size)
        assertTrue(aborted.all { it.eventType == EventType.COMPLETED.value })
        assertTrue(aborted.all { it.outcome == EventOutcome.FAILURE.value })
        assertTrue(
            aborted.all { it.retryFailureCode == AnalyticsCallAbortReason.CLIENT_ABORTED.code },
        )
        assertTrue(
            aborted.all { it.retryFailureReason == AnalyticsCallAbortReason.CLIENT_ABORTED.message },
        )

        dispatcher.sent.clear()
        reporter.reportCoordinatorJoinCompleted(joinStageId, success = true, retryCount = 0)
        assertTrue(dispatcher.sent.isEmpty())
    }

    // --- Media readiness + permissions ---

    @Test
    fun `first video frame sends an initiated event with the track id`() {
        reporter.reportFirstVideoFrameRendered(
            sfuId = "sfu-1",
            callId = "call-1",
            callType = "default",
            joinStageAttemptId = "attempt-1",
            callSessionId = "session-1",
            joinReason = JoinReason.FirstAttempt,
            trackId = "track-1",
        )

        val event = dispatcher.sent.single()
        assertEquals(EventStage.Call.FIRST_VIDEO_FRAME_RENDERED.value, event.stage)
        assertEquals(EventType.INITIATED.value, event.eventType)
        assertEquals("track-1", event.trackId)
    }

    @Test
    fun `first audio frame sends an initiated event`() {
        reporter.reportFirstAudioFrameRendered(
            sfuId = "sfu-1",
            callId = "call-1",
            callType = "default",
            joinStageAttemptId = "attempt-1",
            callSessionId = "session-1",
            joinReason = JoinReason.FirstAttempt,
        )

        val event = dispatcher.sent.single()
        assertEquals(EventStage.Call.FIRST_AUDIO_FRAME_RENDERED.value, event.stage)
        assertEquals(EventType.INITIATED.value, event.eventType)
    }

    @Test
    fun `media permission status maps granted flags to permission text`() {
        reporter.reportMediaPermissionStatus(
            callId = "call-1",
            callType = "default",
            joinStageAttemptId = "attempt-1",
            joinReason = JoinReason.FirstAttempt,
            isCameraGranted = true,
            isMicrophoneGranted = false,
        )

        val event = dispatcher.sent.single()
        assertEquals(EventStage.Call.MEDIA_DEVICE_PERMISSION.value, event.stage)
        assertEquals("GRANTED", event.cameraPermissionStatus)
        assertEquals("NOT_GRANTED", event.microphonePermissionStatus)
    }

    @Test
    fun `deleteAll delegates to the dispatcher`() {
        reporter.deleteAll()

        assertEquals(1, dispatcher.deleteAllCount)
    }
}
