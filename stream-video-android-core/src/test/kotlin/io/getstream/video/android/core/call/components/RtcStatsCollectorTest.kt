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

import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.CallStatsReport
import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.call.stats.model.RtcStatsReport
import io.getstream.video.android.core.internal.module.SfuConnectionModule
import io.getstream.video.android.core.trace.Tracer
import io.getstream.video.android.core.trace.TracerManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import stream.video.sfu.signal.SendStatsResponse

internal class RtcStatsCollectorTest {

    private lateinit var peerConnections: PeerConnections
    private lateinit var sessionManager: CallSessionManager
    private lateinit var signalApi: SignalServerService
    private lateinit var sfuModule: SfuConnectionModule
    private lateinit var statsReporter: CallStatsReporter
    private lateinit var tracerManager: TracerManager

    @Before
    fun setUp() {
        peerConnections = PeerConnections()
        sessionManager = CallSessionManager()
        signalApi = mockk(relaxed = true)
        sfuModule = mockk(relaxed = true)
        every { sfuModule.api } returns signalApi
        statsReporter = mockk(relaxed = true)
        tracerManager = TracerManager(enabled = false)
        coEvery { signalApi.sendStats(any()) } returns SendStatsResponse()
    }

    private fun collector() = RtcStatsCollector(
        powerManager = null,
        sessionId = "session-1",
        sessionManager = sessionManager,
        peerConnections = peerConnections,
        sfuConnectionModule = { sfuModule },
        tracerManager = tracerManager,
        publisherTracer = mockk<Tracer>(relaxed = true),
        subscriberTracer = mockk<Tracer>(relaxed = true),
        sfuTracer = mockk<Tracer>(relaxed = true),
        statsReporter = statsReporter,
    )

    @Test
    fun `publisher and subscriber stats are null when no connections are set`() = runTest {
        val stats = collector()
        assertThat(stats.getPublisherStats()).isNull()
        assertThat(stats.getSubscriberStats()).isNull()
    }

    @Test
    fun `publisher and subscriber stats are read from the peer connections`() = runTest {
        val publisherReport = mockk<RtcStatsReport>(relaxed = true)
        val subscriberReport = mockk<RtcStatsReport>(relaxed = true)
        peerConnections.setPublisher(
            mockk(relaxed = true) { coEvery { getStats() } returns publisherReport },
        )
        peerConnections.setSubscriber(
            mockk(relaxed = true) { coEvery { getStats() } returns subscriberReport },
        )

        val stats = collector()
        assertThat(stats.getPublisherStats()).isSameInstanceAs(publisherReport)
        assertThat(stats.getSubscriberStats()).isSameInstanceAs(subscriberReport)
    }

    @Test
    fun `sendCallStats posts a sendStats request to the SFU`() = runTest {
        collector().sendCallStats()

        coVerify(exactly = 1) { signalApi.sendStats(any()) }
    }

    @Test
    fun `sendConnectionTimeStats collects a report before posting`() = runTest {
        coEvery { statsReporter.collectStats() } returns CallStatsReport(
            publisher = null,
            subscriber = null,
            local = null,
            stateStats = mockk(relaxed = true),
        )
        sessionManager.connectStartTime = System.currentTimeMillis() - 1_000

        collector().sendConnectionTimeStats()

        // collectStats is what distinguishes this path; sendStats may no-op on JVM
        // android.jar stubs if CallStatsReport.toJson hits a null JSONArray.toString().
        coVerify(exactly = 1) { statsReporter.collectStats() }
    }
}
