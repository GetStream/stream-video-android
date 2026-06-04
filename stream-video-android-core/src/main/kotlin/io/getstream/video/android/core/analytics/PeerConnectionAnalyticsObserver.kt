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

package io.getstream.video.android.core.analytics

import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.events.reporting.ClientEventReporter
import io.getstream.video.android.core.events.reporting.PeerConnectionRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import org.webrtc.PeerConnection

internal class PeerConnectionAnalyticsObserver(
    val callId: String,
    val callType: String,
    private val scope: CoroutineScope,
    val reporter: ClientEventReporter,
    val getJoinStageAttemptId: () -> String,
) {

    private var peerConnectionObserverJob: Job? = null
    private var publisherJob: Job? = null
    private var subscriberJob: Job? = null
    var publisherStage = Stage.NOT_STARTED
    var subscriberStage = Stage.NOT_STARTED

    fun observePeerConnections(session: StateFlow<RtcSession?>) {
        peerConnectionObserverJob?.cancel()
        peerConnectionObserverJob = scope.launch {
            publisherJob?.cancel()
            publisherJob = launch {
                session.filterNotNull()
                    .flatMapLatest { it.publisher.filterNotNull() }
                    .flatMapLatest { it.state.filterNotNull() }
                    .collect { state ->
                        publisherStage = getStage(state)
                        scope.launch {
                            onPeerConnectionStateChanged(
                                role = PeerConnectionRole.PUBLISH,
                                iceState = session.value?.publisher?.value?.iceState?.value,
                                peerConnectionState = state,
                            )
                        }
                    }
            }
            subscriberJob?.cancel()
            subscriberJob = launch {
                session.filterNotNull()
                    .flatMapLatest { it.subscriber.filterNotNull() }
                    .flatMapLatest { it.state.filterNotNull() }
                    .collect { state ->
                        subscriberStage = getStage(state)
                        scope.launch {
                            onPeerConnectionStateChanged(
                                role = PeerConnectionRole.SUBSCRIBE,
                                iceState = session.value?.subscriber?.value?.iceState?.value,
                                peerConnectionState = state,
                            )
                        }
                    }
            }
        }
    }

    private fun getStage(peerConnectionState: PeerConnection.PeerConnectionState): Stage {
        return when (peerConnectionState) {
            PeerConnection.PeerConnectionState.CONNECTING -> {
                Stage.IN_PROGRESS // initiated
            }

            PeerConnection.PeerConnectionState.FAILED,
            PeerConnection.PeerConnectionState.CONNECTED,
            -> {
                Stage.NOT_STARTED // completed
            }

            else -> {
                Stage.IN_PROGRESS
            }
        }
    }

    internal fun onPeerConnectionStateChanged(
        role: PeerConnectionRole,
        iceState: PeerConnection.IceConnectionState?,
        peerConnectionState: PeerConnection.PeerConnectionState?,
    ) {
        reporter.onPeerConnectionStateChanged(
            callId = callId,
            callType = callType,
            role = role,
            iceState = iceState,
            peerConnectionState = peerConnectionState,
            joinStageAttemptId = getJoinStageAttemptId.invoke(),
        )
    }

    fun stop() {
        peerConnectionObserverJob?.cancel()
        peerConnectionObserverJob = null
    }
}
