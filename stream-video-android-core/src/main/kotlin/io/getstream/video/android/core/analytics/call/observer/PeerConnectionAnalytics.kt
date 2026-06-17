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

package io.getstream.video.android.core.analytics.call.observer

import io.getstream.video.android.core.analytics.call.observer.model.JoinReason
import io.getstream.video.android.core.analytics.call.observer.model.Stage
import io.getstream.video.android.core.analytics.reporting.ClientEventReporter
import io.getstream.video.android.core.analytics.reporting.model.PeerConnectionRole
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.connection.StreamPeerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.webrtc.PeerConnection

internal class PeerConnectionAnalytics(
    val callId: String,
    val callType: String,
    private val observerScope: CoroutineScope,
    val reporter: ClientEventReporter,
    val joinAnalyticsStateHolder: JoinAnalyticsStateHolder,
    val sfuAnalyticsStateHolder: SfuAnalyticsStateHolder,
    val stateHolder: PeerConnectionAnalyticsStateHolder = PeerConnectionAnalyticsStateHolder(),
) {

    val allowedPcStates = listOf(
        PeerConnection.PeerConnectionState.CONNECTING,
        PeerConnection.PeerConnectionState.FAILED,
        PeerConnection.PeerConnectionState.CONNECTED,
    )

    fun observePeerConnections(session: StateFlow<RtcSession?>) {
        stateHolder.state.value.peerConnectionObserverJob?.cancel()
        val peerConnectionObserverJob = observerScope.launch {
            stateHolder.state.value.publisherJob?.cancel()
            val publisherJob = observeConnection(
                session = session,
                role = PeerConnectionRole.PUBLISH,
                connectionOf = { it.publisher },
                currentStage = { stateHolder.state.value.publisherStage },
                updateStage = stateHolder::updatePublisherStage,
            )
            stateHolder.updatePublisherJob(publisherJob)

            stateHolder.state.value.subscriberJob?.cancel()
            val subscriberJob = observeConnection(
                session = session,
                role = PeerConnectionRole.SUBSCRIBE,
                connectionOf = { it.subscriber },
                currentStage = { stateHolder.state.value.subscriberStage },
                updateStage = stateHolder::updateSubscriberStage,
            )
            stateHolder.updateSubscriberJob(subscriberJob)
        }
        stateHolder.updatePeerConnectionObserverJob(peerConnectionObserverJob)
    }

    /**
     * Observes a single peer connection ([connectionOf]) for analytics.
     *
     * Each allowed peer-connection state ([allowedPcStates]) is reported together with the ICE
     * state as it stands at that moment ([StreamPeerConnection.iceState] value, mapped via
     * [toVideoAnalyticsIceState]). So a CONNECTED peer connection reports CONNECTED only when its
     * ICE is already connected, otherwise NOT_CONNECTED. The upstream filter drops a transition
     * that would merely repeat an already-completed stage (e.g. CONNECTED after FAILED), and
     * [distinctUntilChanged] avoids emitting the same combination twice.
     *
     * The stage is updated in lockstep with the reported event (in the terminal collector), not
     * when the raw peer-connection state changes. It therefore stays IN_PROGRESS while the
     * peer-connection session is open and only flips to COMPLETED once the completed event is
     * actually emitted, which keeps the call-leave flow's "is a stage still in progress?" check
     * correct.
     */
    private fun CoroutineScope.observeConnection(
        session: StateFlow<RtcSession?>,
        role: PeerConnectionRole,
        connectionOf: (RtcSession) -> StateFlow<StreamPeerConnection?>,
        currentStage: () -> Stage,
        updateStage: (Stage) -> Unit,
    ): Job = launch {
        session.filterNotNull()
            .flatMapLatest { connectionOf(it).filterNotNull() }
            .flatMapLatest { connection ->
                connection.state
                    .filter { allowedPcStates.contains(it) }
                    .filterNotNull()
                    .filter {
                        // Skip a peer-connection state that would only repeat an already-completed
                        // stage (e.g. a CONNECTED after a FAILED). Gating here — before the ICE read
                        // and the collector — means we don't do any work for such transitions.
                        val existingStage = currentStage()
                        val newStage = getStage(it)
                        val isExistingStageAndNewStageAreCompleted = (existingStage == Stage.COMPLETED && newStage == Stage.COMPLETED)
                        !isExistingStageAndNewStageAreCompleted
                    }
                    .map { pcState ->
                        PeerConnectionSnapshot(
                            connection.hashCode(),
                            pcState,
                            connection.iceState.value.toVideoAnalyticsIceState(),
                        )
                    }
            }
            .distinctUntilChanged()
            .collect { snapshot ->
                // Update the stage in lockstep with the reported event (not when the raw
                // peer-connection state changed), so it only flips to COMPLETED once the completed
                // event is actually emitted. The COMPLETED -> COMPLETED case is already dropped by
                // the upstream filter, so no extra guard is needed here.
                val pcAnalyticsStage = getStage(snapshot.peerConnectionState)
                pcAnalyticsStage?.let {
                    updateStage(pcAnalyticsStage)
                    onPeerConnectionStateChanged(
                        peerConnectionHashCode = snapshot.peerConnectionHashCode,
                        role = role,
                        iceState = snapshot.iceState,
                        peerConnectionState = snapshot.peerConnectionState,
                    )
                }
            }
    }

    private fun getStage(peerConnectionState: PeerConnection.PeerConnectionState): Stage? {
        return when (peerConnectionState) {
            PeerConnection.PeerConnectionState.CONNECTING -> {
                Stage.IN_PROGRESS // initiated
            }

            PeerConnection.PeerConnectionState.FAILED,
            PeerConnection.PeerConnectionState.CONNECTED,
            -> {
                Stage.COMPLETED // completed
            }

            else -> { null }
        }
    }

    internal fun onPeerConnectionStateChanged(
        peerConnectionHashCode: Int,
        role: PeerConnectionRole,
        iceState: VideoAnalyticsIceState,
        peerConnectionState: PeerConnection.PeerConnectionState?,
    ) {
        reporter.onPeerConnectionStateChanged(
            peerConnectionHashCode = peerConnectionHashCode,
            callId = callId,
            callType = callType,
            role = role,
            iceState = iceState,
            peerConnectionState = peerConnectionState,
            joinStageAttemptId = joinAnalyticsStateHolder.state.value.joinStageAttemptId ?: "unknown",
            joinReason = joinAnalyticsStateHolder.state.value.joinReason ?: JoinReason.Unknown,
            sfuId = sfuAnalyticsStateHolder.sfuId.value,
            callSessionId = joinAnalyticsStateHolder.state.value.callSessionId,
        )
    }

    fun stop() {
        stateHolder.state.value.peerConnectionObserverJob?.cancel()
        stateHolder.updatePeerConnectionObserverJob(null)
    }

    fun stopAndObservePeerConnections(session: StateFlow<RtcSession?>) {
        stop()
        observePeerConnections(session)
    }
}
internal fun PeerConnection.IceConnectionState?.toVideoAnalyticsIceState(): VideoAnalyticsIceState {
    return when (this) {
        PeerConnection.IceConnectionState.CONNECTED,
        PeerConnection.IceConnectionState.COMPLETED,
        -> VideoAnalyticsIceState.CONNECTED

        PeerConnection.IceConnectionState.FAILED -> VideoAnalyticsIceState.FAILED

        else -> VideoAnalyticsIceState.NOT_CONNECTED
    }
}

private data class PeerConnectionSnapshot(
    val peerConnectionHashCode: Int,
    val peerConnectionState: PeerConnection.PeerConnectionState,
    val iceState: VideoAnalyticsIceState,
)

internal enum class VideoAnalyticsIceState(val text: String) {
    CONNECTED("CONNECTED"), FAILED("FAILED"), NOT_CONNECTED("NOT_CONNECTED")
}
