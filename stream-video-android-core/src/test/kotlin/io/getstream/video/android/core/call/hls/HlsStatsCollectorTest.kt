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

import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.call.hls.HlsPlaybackState.Buffering
import io.getstream.video.android.core.call.hls.HlsPlaybackState.Loading
import io.getstream.video.android.core.call.hls.HlsPlaybackState.Paused
import io.getstream.video.android.core.call.hls.HlsPlaybackState.Playing
import io.getstream.video.android.core.call.hls.HlsPlaybackState.Seeking
import org.junit.Test

class HlsStatsCollectorTest {

    private val clock = FakeHlsStatsClock()
    private val collector = HlsStatsCollector(clock)

    @Test
    fun `only the first play is reported as first`() {
        assertThat(collector.onState(Loading)).isFalse()
        assertThat(collector.onState(Playing)).isTrue()
        collector.onState(Paused)

        assertThat(collector.onState(Playing)).isFalse()
    }

    @Test
    fun `watch time counts playing only`() {
        collector.onState(Loading)
        clock.advance(500)
        collector.onState(Playing)
        clock.advance(3_000)
        collector.onState(Paused)
        clock.advance(10_000)
        collector.onState(Playing)
        clock.advance(1_000)

        assertThat(collector.stats(null).watchDurationMs).isEqualTo(4_000)
    }

    @Test
    fun `buffering after playing is a stall that ends when playback resumes`() {
        collector.onState(Playing)
        clock.advance(1_000)
        collector.onState(Buffering)
        clock.advance(700)
        collector.onState(Playing)

        val stats = collector.stats(null)
        assertThat(stats.stallCount).isEqualTo(1)
        assertThat(stats.stallDurationMs).isEqualTo(700)
        assertThat(stats.maxStallDurationMs).isEqualTo(700)
        assertThat(stats.watchDurationMs).isEqualTo(1_000)
        val events = collector.pendingEvents(10)
        assertThat(events.map { it.type })
            .containsExactly(HlsStatsCollector.EVENT_STALL_START, HlsStatsCollector.EVENT_STALL_END)
            .inOrder()
        assertThat(events[1].data).containsExactly("duration_ms", 700L)
    }

    @Test
    fun `initial loading and seeking are not stalls`() {
        collector.onState(Loading)
        clock.advance(2_000)
        collector.onState(Playing)
        collector.onState(Seeking)
        clock.advance(2_000)
        collector.onState(Playing)

        assertThat(collector.stats(null).stallCount).isEqualTo(0)
        assertThat(collector.pendingEvents(10)).isEmpty()
    }

    @Test
    fun `buffering after a pause is not a stall`() {
        collector.onState(Playing)
        collector.onState(Paused)
        collector.onState(Buffering)

        assertThat(collector.stats(null).stallCount).isEqualTo(0)
    }

    @Test
    fun `pausing during a stall ends it`() {
        collector.onState(Playing)
        collector.onState(Buffering)
        clock.advance(300)
        collector.onState(Paused)
        clock.advance(5_000)

        assertThat(collector.stats(null).stallDurationMs).isEqualTo(300)
    }

    @Test
    fun `an open stall is included in stats`() {
        collector.onState(Playing)
        collector.onState(Buffering)
        clock.advance(400)

        val stats = collector.stats(null)
        assertThat(stats.stallCount).isEqualTo(1)
        assertThat(stats.stallDurationMs).isEqualTo(400)
        assertThat(stats.maxStallDurationMs).isEqualTo(400)
    }

    @Test
    fun `finish closes the open stall and ignores later states`() {
        collector.onState(Playing)
        clock.advance(1_000)
        collector.onState(Buffering)
        clock.advance(200)
        collector.finish()
        clock.advance(5_000)
        collector.onState(Playing)
        clock.advance(5_000)

        val stats = collector.stats(null)
        assertThat(stats.watchDurationMs).isEqualTo(1_000)
        assertThat(stats.stallDurationMs).isEqualTo(200)
    }

    @Test
    fun `stats reuse the last metrics when none are given`() {
        collector.stats(HlsPlaybackMetrics(bitrateKbps = 2_500, width = 1280, height = 720))

        val stats = collector.stats(null)
        assertThat(stats.bitrateKbps).isEqualTo(2_500)
        assertThat(stats.width).isEqualTo(1280)
        assertThat(stats.height).isEqualTo(720)
    }

    @Test
    fun `the same error code in a row is recorded once`() {
        collector.onError("ERROR_CODE_IO_NETWORK_CONNECTION_FAILED")
        collector.onError("ERROR_CODE_IO_NETWORK_CONNECTION_FAILED")
        collector.onError("ERROR_CODE_BEHIND_LIVE_WINDOW")

        assertThat(collector.pendingEvents(10).map { it.data?.get("code") })
            .containsExactly(
                "ERROR_CODE_IO_NETWORK_CONNECTION_FAILED",
                "ERROR_CODE_BEHIND_LIVE_WINDOW",
            )
            .inOrder()
        assertThat(collector.errorCode).isEqualTo("ERROR_CODE_BEHIND_LIVE_WINDOW")
    }

    @Test
    fun `acknowledged events are dropped and newer events stay`() {
        collector.onError("a")
        val sent = collector.pendingEvents(10)
        collector.onError("b")

        collector.acknowledgeEvents(sent)

        assertThat(collector.pendingEvents(10).map { it.data?.get("code") }).containsExactly("b")
    }

    @Test
    fun `the oldest events are dropped once the buffer is full`() {
        repeat(HlsStatsCollector.MAX_BUFFERED_EVENTS + 1) { collector.onError("code-$it") }

        val events = collector.pendingEvents(Int.MAX_VALUE)
        assertThat(events).hasSize(HlsStatsCollector.MAX_BUFFERED_EVENTS)
        assertThat(events.first().data?.get("code")).isEqualTo("code-1")
    }
}

internal class FakeHlsStatsClock(
    private var elapsed: Long = 0L,
    private val wallOffset: Long = 1_700_000_000_000L,
) : HlsStatsClock {
    fun advance(ms: Long) {
        elapsed += ms
    }

    override fun elapsedMs(): Long = elapsed
    override fun wallMs(): Long = wallOffset + elapsed
}
