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

import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.analytics.call.observer.model.JoinReason
import io.getstream.video.android.core.analytics.reporting.ClientEventReporter
import io.getstream.video.android.core.call.RtcSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.webrtc.PeerConnection
import java.util.concurrent.atomic.AtomicBoolean

private typealias TrackId = String

internal class AudioAnalytics(
    private val callId: String,
    private val callType: String,
    private val clientEventReporter: ClientEventReporter,
    private val joinAnalyticsStateHolder: JoinAnalyticsStateHolder,
    private val sfuStateHolder: SfuAnalyticsStateHolder,
    private val observerScope: CoroutineScope,
) {

    var recordedFirstFrame: AtomicBoolean = AtomicBoolean(false)

    private var observeJob: Job? = null

    fun observeFirstRemoteParticipantAudioMuteState(
        session: StateFlow<RtcSession?>,
        participants: StateFlow<List<ParticipantState>>,
    ) {
        observeJob?.cancel()
        observeJob = observerScope.launch {
            session.filterNotNull()
                .flatMapLatest { it.subscriber.filterNotNull() }
                .flatMapLatest { subscriber ->
                    subscriber.state
                        .filter { it == PeerConnection.PeerConnectionState.CONNECTED }
                        .map { subscriber }
                }
                .flatMapLatest {
                    participants
                        .map { list -> list.firstOrNull { participant -> !participant.isLocal } }
                        .distinctUntilChangedBy { participant -> participant?.sessionId }
                        .flatMapLatest { participant ->
                            if (participant == null) {
                                flowOf(null)
                            } else {
                                participant.audioEnabled.map { enabled -> !enabled }
                            }
                        }
                }
                .distinctUntilChanged()
                .collect { isMuted ->
                    val notMuted = isMuted == false
                    if (notMuted && recordedFirstFrame.compareAndSet(false, true)) {
                        reportAndCleanup()
                    }
                }
        }
    }

    private fun reportAndCleanup() {
        clientEventReporter.reportFirstAudioFrameRendered(
            sfuStateHolder.sfuId.value,
            callId,
            callType,
            joinStageAttemptId = joinAnalyticsStateHolder.state.value.joinStageAttemptId ?: "unknown",
            joinReason = joinAnalyticsStateHolder.state.value.joinReason ?: JoinReason.Unknown,
            callSessionId = joinAnalyticsStateHolder.state.value.callSessionId,
        )
        observeJob?.cancel()
        observeJob = null
    }

    fun reset() {
        recordedFirstFrame.set(false)
        observeJob?.cancel()
        observeJob = null
    }
}
