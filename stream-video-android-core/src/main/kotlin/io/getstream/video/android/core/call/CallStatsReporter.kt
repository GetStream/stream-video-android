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

package io.getstream.video.android.core.call

import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallStatsReport
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class CallStatsReporter(private val call: Call) {

    private var callStatsReportingJob: Job? = null

    internal suspend fun collectStats(session: RtcSession?): CallStatsReport {
        val publisherStats = session?.getPublisherStats()
        val subscriberStats = session?.getSubscriberStats()
        call.state.stats.updateFromRTCStats(publisherStats, isPublisher = true)
        call.state.stats.updateFromRTCStats(subscriberStats, isPublisher = false)
        call.state.stats.updateLocalStats()
        val local = call.state.stats._local.value

        val report = CallStatsReport(
            publisher = publisherStats,
            subscriber = subscriberStats,
            local = local,
            stateStats = call.state.stats,
        )

        call.statsReport.value = report
        call.statLatencyHistory.value += report.stateStats.publisher.latency.value
        if (call.statLatencyHistory.value.size > 20) {
            call.statLatencyHistory.value = call.statLatencyHistory.value.takeLast(20)
        }
        return report
    }

    internal fun startCallStatsReporting(session: RtcSession?, reportingIntervalMs: Long = 10_000) {
        cancelJobs()
        callStatsReportingJob = call.scope.launch {
            // Wait a bit before we start capturing stats
            delay(reportingIntervalMs)

            while (isActive) {
                delay(reportingIntervalMs)
                session?.sendCallStats(
                    report = collectStats(session),
                )
            }
        }
    }

    internal fun cancelJobs() {
        callStatsReportingJob?.cancel()
    }
}
