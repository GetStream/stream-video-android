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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
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

    val allowedStates = listOf(
        PeerConnection.PeerConnectionState.CONNECTING,
        PeerConnection.PeerConnectionState.FAILED,
        PeerConnection.PeerConnectionState.CONNECTED,
    )

    fun observePeerConnections(session: StateFlow<RtcSession?>) {
        stateHolder.state.value.peerConnectionObserverJob?.cancel()
        val peerConnectionObserverJob = observerScope.launch {
            stateHolder.state.value.publisherJob?.cancel()
            val publisherJob = launch {
                session.filterNotNull()
                    .flatMapLatest { it.publisher.filterNotNull() }
                    .flatMapLatest {
                        it.state.filter { it ->
                            allowedStates.contains(
                                it,
                            )
                        }.filterNotNull()
                    }.filter {
                        val existingStage = stateHolder.state.value.publisherStage
                        val newStage = getStage(it)
                        val isExistingStageAndNewStageAreCompleted = (existingStage == Stage.COMPLETED && newStage == Stage.COMPLETED)
                        !isExistingStageAndNewStageAreCompleted
                    }
                    .collect { state ->
                        val publisherStage = getStage(state)
                        publisherStage?.let {
                            stateHolder.updatePublisherStage(publisherStage)
                            observerScope.launch {
                                session.value?.publisher?.value?.let { publisher ->
                                    onPeerConnectionStateChanged(
                                        publisher.hashCode(),
                                        role = PeerConnectionRole.PUBLISH,
                                        iceState = publisher.iceState.value,
                                        peerConnectionState = state,
                                    )
                                }
                            }
                        }
                    }
            }
            stateHolder.updatePublisherJob(publisherJob)
            stateHolder.state.value.subscriberJob?.cancel()
            val subscriberJob = launch {
                session.filterNotNull()
                    .flatMapLatest { it.subscriber.filterNotNull() }
                    .flatMapLatest {
                        it.state.filter { it -> allowedStates.contains(it) }.filterNotNull()
                    }.filter {
                        val existingStage = stateHolder.state.value.subscriberStage
                        val newStage = getStage(it)
                        val isExistingStageAndNewStageAreCompleted = (existingStage == Stage.COMPLETED && newStage == Stage.COMPLETED)
                        !isExistingStageAndNewStageAreCompleted
                    }
                    .collect { state ->
                        val stage = getStage(state)
                        stage?.let {
                            stateHolder.updateSubscriberStage(stage)
                            observerScope.launch {
                                session.value?.subscriber?.value?.let { subscriber ->
                                    onPeerConnectionStateChanged(
                                        subscriber.hashCode(),
                                        role = PeerConnectionRole.SUBSCRIBE,
                                        iceState = subscriber.iceState.value,
                                        peerConnectionState = state,
                                    )
                                }
                            }
                        }
                    }
            }
            stateHolder.updateSubscriberJob(subscriberJob)
        }
        stateHolder.updatePeerConnectionObserverJob(peerConnectionObserverJob)
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
        iceState: PeerConnection.IceConnectionState?,
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
