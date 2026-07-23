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

package io.getstream.video.android.core.call.components

import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallStatsReport
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Periodically collects WebRTC stats for a [Call], reports them to the SFU, and exposes
 * the latest report and latency history as observable flows.
 */
internal class CallStatsReporter(
    private val call: Call,
) {
    private val logger by taggedLogger("Call:StatsReporter:${call.type}:${call.id}")

    /** Contains stats events for observation. */
    val statsReport: MutableStateFlow<CallStatsReport?> = MutableStateFlow(null)

    /** Contains stats history. */
    val statLatencyHistory: MutableStateFlow<List<Int>> = MutableStateFlow(listOf(0, 0, 0))

    private var callStatsReportingJob: Job? = null

    fun start(reportingIntervalMs: Long = 10_000) {
        callStatsReportingJob?.cancel()
        callStatsReportingJob = call.scope.launch {
            // Wait a bit before we start capturing stats
            delay(reportingIntervalMs)

            while (isActive) {
                delay(reportingIntervalMs)
                call.session.value?.sendCallStats(
                    report = collectStats(),
                )
            }
        }
    }

    fun stop() {
        callStatsReportingJob?.cancel()
    }

    suspend fun collectStats(): CallStatsReport {
        val session = call.session.value
        val state = call.state
        val publisherStats = runCatching { session?.getPublisherStats() }.getOrNull()
        val subscriberStats = runCatching { session?.getSubscriberStats() }.getOrNull()
        runCatching {
            state.stats.updateFromRTCStats(publisherStats, isPublisher = true)
            state.stats.updateFromRTCStats(subscriberStats, isPublisher = false)
            state.stats.updateLocalStats()
        }.onFailure { logger.w { "[collectStats] Failed to update stats: ${it.message}" } }
        val local = state.stats._local.value

        val report = CallStatsReport(
            publisher = publisherStats,
            subscriber = subscriberStats,
            local = local,
            stateStats = state.stats,
        )

        statsReport.value = report
        statLatencyHistory.value += report.stateStats.publisher.latency.value
        if (statLatencyHistory.value.size > 20) {
            statLatencyHistory.value = statLatencyHistory.value.takeLast(20)
        }

        return report
    }
}
