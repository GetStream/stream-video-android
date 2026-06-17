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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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

    companion object {
        /**
         * How long a CONNECTED peer connection waits for its ICE state to reach
         * [PeerConnection.IceConnectionState.CONNECTED] before reporting the current ICE state.
         */
        private const val ICE_CONNECTED_GRACE_MILLIS = 2_000L
    }
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
     * The peer-connection [StreamPeerConnection.state] drives the stage tracking: whenever it
     * enters one of the [allowedPcStates], the stage is updated (this is independent of the ICE
     * state, since the stage is also consumed by the call-leave flow).
     *
     * Once the peer connection enters an allowed state, the ICE state reported alongside it depends
     * on the peer-connection state:
     * - [PeerConnection.PeerConnectionState.CONNECTING] / [PeerConnection.PeerConnectionState.FAILED]:
     *   report the current ICE state immediately.
     * - [PeerConnection.PeerConnectionState.CONNECTED]: wait up to [ICE_CONNECTED_GRACE_MILLIS] for
     *   the ICE state to become [PeerConnection.IceConnectionState.CONNECTED]; if it does not, report
     *   whatever the ICE state is when the grace period elapses.
     *
     * [mapLatest] makes the ICE resolution follow the latest peer-connection state (a newer
     * peer-connection state cancels an in-progress grace wait), and [distinctUntilChanged] avoids
     * emitting the same combination twice.
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
                    .onEach { pcState ->
                        val existingStage = currentStage()
                        val newStage = getStage(pcState)
                        val isExistingStageAndNewStageAreCompleted = (existingStage == Stage.COMPLETED && newStage == Stage.COMPLETED)
                        if (newStage != null && !isExistingStageAndNewStageAreCompleted) {
                            updateStage(newStage)
                        }
                    }
                    .mapLatest { pcState ->
                        val iceState = if (pcState == PeerConnection.PeerConnectionState.CONNECTED) {
                            // Give ICE a grace period to reach CONNECTED before reporting the
                            // connected peer connection; otherwise report the current ICE state.
                            withTimeoutOrNull(ICE_CONNECTED_GRACE_MILLIS) {
                                connection.iceState.first {
                                    it == PeerConnection.IceConnectionState.CONNECTED
                                }
                            } ?: connection.iceState.value
                        } else {
                            // CONNECTING / FAILED: report the current ICE state immediately.
                            connection.iceState.value
                        }
                        PeerConnectionSnapshot(
                            connection.hashCode(),
                            pcState,
                            iceState.toVideoAnalyticsIceState(),
                        )
                    }
            }
            .distinctUntilChanged()
            .collect { snapshot ->
                onPeerConnectionStateChanged(
                    peerConnectionHashCode = snapshot.peerConnectionHashCode,
                    role = role,
                    iceState = snapshot.iceState,
                    peerConnectionState = snapshot.peerConnectionState,
                )
            }
    }

    private data class PeerConnectionSnapshot(
        val peerConnectionHashCode: Int,
        val peerConnectionState: PeerConnection.PeerConnectionState,
        val iceState: VideoAnalyticsIceState,
    )

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

internal enum class VideoAnalyticsIceState(val text: String) {
    CONNECTED("CONNECTED"), FAILED("FAILED"), NOT_CONNECTED("NOT_CONNECTED")
}
