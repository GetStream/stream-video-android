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

package io.getstream.video.android.core.call.hls

import android.view.SurfaceView
import com.google.common.truth.Truth.assertThat
import io.getstream.android.video.generated.models.ReportHLSStatsRequest
import io.getstream.video.android.core.call.hls.HlsPlaybackState.Buffering
import io.getstream.video.android.core.call.hls.HlsPlaybackState.Ended
import io.getstream.video.android.core.call.hls.HlsPlaybackState.Loading
import io.getstream.video.android.core.call.hls.HlsPlaybackState.Playing
import io.getstream.video.android.core.call.hls.HlsStatsDeliveryResult.Delivered
import io.getstream.video.android.core.call.hls.HlsStatsDeliveryResult.Failed
import io.getstream.video.android.core.call.hls.HlsStatsDeliveryResult.Forbidden
import io.getstream.video.android.core.call.hls.HlsStatsDeliveryResult.Rejected
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HlsStatsReporterTest {

    companion object {
        private const val INTERVAL_MS = 10_000L
    }

    private val player = FakeReportingPlayer()
    private val delivery = FakeDelivery()

    private val TestScope.reporter: HlsStatsReporter
        get() = reporterOrNull ?: HlsStatsReporter(
            config = HlsStatsConfig(
                callSessionId = "call-session",
                statsToken = "token",
                unifiedSessionId = "unified-session",
                intervalMs = INTERVAL_MS,
            ),
            player = player,
            delivery = delivery,
            // Not backgroundScope: advanceUntilIdle skips its work, such as the ended report.
            scope = CoroutineScope(StandardTestDispatcher(testScheduler) + reporterJob),
            mainDispatcher = StandardTestDispatcher(testScheduler),
            clock = object : HlsStatsClock {
                override fun elapsedMs(): Long = testScheduler.currentTime
                override fun wallMs(): Long = 1_700_000_000_000L + testScheduler.currentTime
            },
            clientInfo = HlsStatsClientInfo("stream-android", "1.0.0", "Android", "15"),
            retryDelaysMs = listOf(1_000L, 2_000L),
        ).also { reporterOrNull = it }

    private var reporterOrNull: HlsStatsReporter? = null
    private val reporterJob = Job()

    /** Ends the periodic reports, which runTest would otherwise run forever once the body ends. */
    private fun runTest(body: suspend TestScope.() -> Unit) = kotlinx.coroutines.test.runTest {
        try {
            body()
        } finally {
            reporterJob.cancel()
        }
    }

    @Test
    fun `sends nothing until playback first plays`() = runTest {
        reporter
        emit(Loading)
        advanceTimeBy(INTERVAL_MS * 3)

        assertThat(delivery.requests).isEmpty()
    }

    @Test
    fun `sends started without stats on first play`() = runTest {
        reporter
        emit(Playing)
        runCurrent()

        val started = delivery.requests.single()
        assertThat(started.type).isEqualTo(HlsStatsReporter.TYPE_STARTED)
        assertThat(started.stats).isNull()
        assertThat(started.statsToken).isEqualTo("token")
        assertThat(started.callSessionId).isEqualTo("call-session")
        assertThat(started.unifiedSessionId).isEqualTo("unified-session")
        assertThat(started.userSessionId).isNotEmpty()
        assertThat(started.sdk).isEqualTo("stream-android")
        assertThat(started.os).isEqualTo("Android")
    }

    @Test
    fun `sends stats every interval with the same user session id`() = runTest {
        reporter
        emit(Playing)
        advanceTimeBy(INTERVAL_MS * 2 + 1)

        assertThat(delivery.requests.map { it.type }).containsExactly(
            HlsStatsReporter.TYPE_STARTED,
            HlsStatsReporter.TYPE_STATS,
            HlsStatsReporter.TYPE_STATS,
        ).inOrder()
        assertThat(delivery.requests.map { it.userSessionId }.toSet()).hasSize(1)
        assertThat(delivery.requests.last().stats?.watchDurationMs).isEqualTo(INTERVAL_MS * 2)
    }

    @Test
    fun `stats carry the player readings`() = runTest {
        player.metrics = HlsPlaybackMetrics(bitrateKbps = 3_000, width = 1920, height = 1080)
        reporter
        emit(Playing)
        advanceTimeBy(INTERVAL_MS + 1)

        val stats = delivery.requests.last().stats!!
        assertThat(stats.bitrateKbps).isEqualTo(3_000)
        assertThat(stats.width).isEqualTo(1920)
    }

    @Test
    fun `the stream ending sends ended and stops reporting`() = runTest {
        reporter
        emit(Playing)
        advanceTimeBy(INTERVAL_MS + 1)
        emit(Ended)
        advanceTimeBy(INTERVAL_MS * 3)

        assertThat(delivery.requests.map { it.type }).containsExactly(
            HlsStatsReporter.TYPE_STARTED,
            HlsStatsReporter.TYPE_STATS,
            HlsStatsReporter.TYPE_ENDED,
        ).inOrder()
        val ended = delivery.requests.last()
        assertThat(ended.endReason).isEqualTo(HlsStatsReporter.END_REASON_LEFT)
        assertThat(ended.stats?.watchDurationMs).isEqualTo(INTERVAL_MS + 1)
        assertThat(ended.events?.map { it.type })
            .containsExactly(HlsStatsCollector.EVENT_STREAM_ENDED)
    }

    @Test
    fun `the stream ending before the first frame sends nothing`() = runTest {
        reporter
        emit(Loading)
        emit(Ended)
        advanceTimeBy(INTERVAL_MS * 3)

        assertThat(delivery.requests).isEmpty()
    }

    @Test
    fun `delivered events are not sent again`() = runTest {
        reporter
        emit(Playing)
        runCurrent()
        stall()
        advanceTimeBy(INTERVAL_MS * 2)

        val stats = delivery.requests.filter { it.type == HlsStatsReporter.TYPE_STATS }
        assertThat(stats[0].events?.map { it.type }).containsExactly(
            HlsStatsCollector.EVENT_STALL_START,
            HlsStatsCollector.EVENT_STALL_END,
        )
        assertThat(stats[1].events).isNull()
    }

    @Test
    fun `a failed stats report keeps its events for the next one`() = runTest {
        reporter
        emit(Playing)
        runCurrent()
        stall()
        delivery.results.add(Failed)
        advanceTimeBy(INTERVAL_MS * 2)

        val stats = delivery.requests.filter { it.type == HlsStatsReporter.TYPE_STATS }
        assertThat(stats).hasSize(2)
        assertThat(stats[1].events?.map { it.type }).containsExactly(
            HlsStatsCollector.EVENT_STALL_START,
            HlsStatsCollector.EVENT_STALL_END,
        )
    }

    @Test
    fun `a rejected report drops its events`() = runTest {
        reporter
        emit(Playing)
        runCurrent()
        stall()
        delivery.results.add(Rejected)
        advanceTimeBy(INTERVAL_MS * 2)

        val stats = delivery.requests.filter { it.type == HlsStatsReporter.TYPE_STATS }
        assertThat(stats).hasSize(2)
        assertThat(stats[1].events).isNull()
    }

    @Test
    fun `a rejected stats token stops reporting, including ended`() = runTest {
        reporter
        delivery.results.add(Forbidden)
        emit(Playing)
        advanceTimeBy(INTERVAL_MS * 3)
        reporter.stop()
        advanceUntilIdle()

        assertThat(delivery.requests.map { it.type }).containsExactly(HlsStatsReporter.TYPE_STARTED)
    }

    @Test
    fun `started is retried until delivered`() = runTest {
        reporter
        delivery.results.addAll(listOf(Failed, Failed))
        emit(Playing)
        advanceTimeBy(3_001)

        assertThat(delivery.requests.map { it.type }).containsExactly(
            HlsStatsReporter.TYPE_STARTED,
            HlsStatsReporter.TYPE_STARTED,
            HlsStatsReporter.TYPE_STARTED,
        )
    }

    @Test
    fun `stop sends ended with reason left`() = runTest {
        reporter
        emit(Playing)
        advanceTimeBy(1_000)
        reporter.stop()
        advanceUntilIdle()

        val ended = delivery.requests.last()
        assertThat(ended.type).isEqualTo(HlsStatsReporter.TYPE_ENDED)
        assertThat(ended.endReason).isEqualTo(HlsStatsReporter.END_REASON_LEFT)
        assertThat(ended.errorCode).isNull()
        assertThat(ended.stats?.watchDurationMs).isEqualTo(1_000)
        assertThat(delivery.requests.count { it.type == HlsStatsReporter.TYPE_STATS }).isEqualTo(0)
    }

    @Test
    fun `ended after a playback error carries the error code`() = runTest {
        reporter
        emit(Playing)
        runCurrent()
        reporter.onPlaybackError("ERROR_CODE_BEHIND_LIVE_WINDOW")
        reporter.stop()
        advanceUntilIdle()

        val ended = delivery.requests.last()
        assertThat(ended.endReason).isEqualTo(HlsStatsReporter.END_REASON_ERROR)
        assertThat(ended.errorCode).isEqualTo("ERROR_CODE_BEHIND_LIVE_WINDOW")
        assertThat(ended.events?.map { it.type }).containsExactly(HlsStatsCollector.EVENT_ERROR)
    }

    @Test
    fun `ended is retried until delivered`() = runTest {
        reporter
        emit(Playing)
        runCurrent()
        delivery.results.addAll(listOf(Failed, Failed))
        reporter.stop()
        advanceUntilIdle()

        assertThat(delivery.requests.count { it.type == HlsStatsReporter.TYPE_ENDED }).isEqualTo(3)
    }

    @Test
    fun `an error before the first frame sends a single ended with the error`() = runTest {
        reporter
        emit(Loading)
        reporter.onPlaybackError("ERROR_CODE_PARSING_MANIFEST_MALFORMED")
        advanceTimeBy(INTERVAL_MS * 3)

        val ended = delivery.requests.single()
        assertThat(ended.type).isEqualTo(HlsStatsReporter.TYPE_ENDED)
        assertThat(ended.stats).isNull()
        assertThat(ended.endReason).isEqualTo(HlsStatsReporter.END_REASON_ERROR)
        assertThat(ended.errorCode).isEqualTo("ERROR_CODE_PARSING_MANIFEST_MALFORMED")
        assertThat(ended.events?.map { it.type }).containsExactly(HlsStatsCollector.EVENT_ERROR)
    }

    @Test
    fun `stop after an error before the first frame sends nothing more`() = runTest {
        reporter
        reporter.onPlaybackError("ERROR_CODE_PARSING_MANIFEST_MALFORMED")
        advanceUntilIdle()
        reporter.stop()
        emit(Playing)
        advanceUntilIdle()

        assertThat(delivery.requests).hasSize(1)
    }

    @Test
    fun `stop before playback plays sends nothing`() = runTest {
        reporter
        emit(Loading)
        reporter.stop()
        emit(Playing)
        advanceUntilIdle()

        assertThat(delivery.requests).isEmpty()
    }

    @Test
    fun `a second stop sends nothing more`() = runTest {
        reporter
        emit(Playing)
        runCurrent()
        reporter.stop()
        reporter.stop()
        advanceUntilIdle()

        assertThat(delivery.requests.count { it.type == HlsStatsReporter.TYPE_ENDED }).isEqualTo(1)
    }

    @Test
    fun `player events after stop are ignored`() = runTest {
        reporter
        emit(Playing)
        runCurrent()
        reporter.stop()
        emit(Buffering)
        reporter.onPlaybackError("ERROR_CODE_IO_NETWORK_CONNECTION_FAILED")
        advanceUntilIdle()

        val ended = delivery.requests.last()
        assertThat(ended.endReason).isEqualTo(HlsStatsReporter.END_REASON_LEFT)
        assertThat(ended.events).isNull()
    }

    private fun TestScope.emit(state: HlsPlaybackState) {
        reporter.onPlaybackStateChanged(state)
    }

    private fun TestScope.stall() {
        emit(Buffering)
        advanceTimeBy(500)
        emit(Playing)
    }

    private class FakeDelivery : HlsStatsDelivery {
        val requests = mutableListOf<ReportHLSStatsRequest>()
        val results = ArrayDeque<HlsStatsDeliveryResult>()

        override suspend fun deliver(request: ReportHLSStatsRequest): HlsStatsDeliveryResult {
            requests += request
            return results.removeFirstOrNull() ?: Delivered
        }
    }

    private class FakeReportingPlayer : HlsPlayer {
        var metrics: HlsPlaybackMetrics? = HlsPlaybackMetrics()

        override val playlistUrl: String = "https://example.com/playlist.m3u8"
        override val videoAspectRatio: StateFlow<Float?> = MutableStateFlow(null)
        override fun attach(surfaceView: SurfaceView) = Unit
        override fun detach(surfaceView: SurfaceView) = Unit
        override fun setAudioEnabled(enabled: Boolean) = Unit
        override fun release() = Unit
        override fun readPlaybackMetrics(): HlsPlaybackMetrics? = metrics
    }
}
