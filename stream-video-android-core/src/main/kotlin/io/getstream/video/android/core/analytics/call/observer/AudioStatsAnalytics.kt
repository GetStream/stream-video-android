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

import io.getstream.video.android.core.CallStatsReport
import io.getstream.video.android.core.analytics.call.observer.model.JoinReason
import io.getstream.video.android.core.analytics.reporting.ClientEventReporter
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.stats.model.RtcInboundRtpAudioStreamStats
import io.getstream.video.android.core.call.stats.model.discriminator.RtcReportType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.webrtc.PeerConnection
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Receipt-based first-audio-frame detector built on the subscriber's inbound-rtp counters.
 *
 * The [AudioTrackSink][AudioAnalytics] PCM tap observes NetEq *output*, which keeps
 * producing energetic frames during packet-loss concealment — it can fire while the
 * device is offline. The counters used here cannot:
 *
 *  - `totalSamplesReceived` includes `concealedSamples` (W3C webrtc-stats), so
 *    `realSamples = totalSamplesReceived - concealedSamples` only grows when samples
 *    actually arrived from the network and were decoded. During concealment both
 *    counters grow in lockstep and the difference stays flat.
 *  - As a second gate, detection is suppressed unless the subscriber peer connection
 *    is currently CONNECTED.
 *
 * Counters are cumulative per peer connection, so [observe] must be (re)invoked at the
 * same call site where the subscriber peer connection is recreated (next to
 * [PeerConnectionAnalytics.stopAndObservePeerConnections]).
 *
 * Trade-off vs the PCM tap: detection latency is bounded by the stats reporting
 * interval, so the PCM tap remains the precise timestamp source while this detector
 * is the receipt-proof one.
 */
internal class AudioStatsAnalytics(
    private val callId: String,
    private val callType: String,
    private val clientEventReporter: ClientEventReporter,
    private val joinAnalyticsStateHolder: JoinAnalyticsStateHolder,
    private val sfuStateHolder: SfuAnalyticsStateHolder,
    private val isEnabled: Boolean = false,
) {

    var recordedFirstFrame: AtomicBoolean = AtomicBoolean(false)

    private var observeJob: Job? = null

    fun observe(
        statsReports: StateFlow<CallStatsReport?>,
        session: StateFlow<RtcSession?>,
        scope: CoroutineScope,
    ) {
        if (!isEnabled) return
        observeJob?.cancel()
        observeJob = scope.launch {
            statsReports
                .filterNotNull()
                .collect { report -> onStatsReport(report, session.value) }
        }
    }

    internal fun onStatsReport(report: CallStatsReport, session: RtcSession?) {
        if (!isEnabled) return
        if (recordedFirstFrame.get()) return

        val realSamples = report.subscriberRealAudioSamples() ?: return
        if (realSamples <= BigInteger.ZERO) return

        // Offline gate: a first frame is only credible while the media path is up.
        if (!isSubscriberConnected(session)) return

        if (recordedFirstFrame.compareAndSet(false, true)) {
            clientEventReporter.reportFirstAudioFrameRendered(
                sfuId = sfuStateHolder.sfuId.value,
                callId = callId,
                callType = callType,
                joinStageAttemptId = joinAnalyticsStateHolder.state.value.joinStageAttemptId
                    ?: "unknown",
                joinReason = joinAnalyticsStateHolder.state.value.joinReason
                    ?: JoinReason.Unknown,
                callSessionId = joinAnalyticsStateHolder.state.value.callSessionId,
            )
            stop()
        }
    }

    private fun isSubscriberConnected(session: RtcSession?): Boolean =
        session?.subscriber?.value?.state?.value == PeerConnection.PeerConnectionState.CONNECTED

    fun stop() {
        observeJob?.cancel()
        observeJob = null
    }

    /**
     * Re-arms detection for a new join attempt. Intentionally does not stop the
     * observer job — [observe] and this are invoked from independent points of the
     * join flow, so neither may rely on the other's ordering.
     */
    fun reset() {
        if (!isEnabled) return
        recordedFirstFrame.set(false)
    }
}

/**
 * Samples actually received from the network and decoded, summed across all inbound
 * audio streams of the subscriber. Concealed (synthesized) samples are excluded.
 * Returns null when the report carries no inbound audio stats.
 */
private fun CallStatsReport.subscriberRealAudioSamples(): BigInteger? {
    val inboundAudio = subscriber?.parsed?.get(RtcReportType.INBOUND_RTP)
        ?.filterIsInstance<RtcInboundRtpAudioStreamStats>()
        ?.takeIf { it.isNotEmpty() }
        ?: return null
    return inboundAudio.fold(BigInteger.ZERO) { acc, stats ->
        val total = stats.totalSamplesReceived ?: BigInteger.ZERO
        val concealed = stats.concealedSamples ?: BigInteger.ZERO
        acc + (total - concealed).max(BigInteger.ZERO)
    }
}
