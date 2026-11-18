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

package io.getstream.video.android.core

import io.getstream.android.video.generated.models.CallSettingsResponse
import io.getstream.android.video.generated.models.JoinCallResponse
import io.getstream.android.video.generated.models.NoiseCancellationSettings
import io.getstream.android.video.generated.models.OwnCapability
import io.getstream.video.android.core.base.IntegrationTestBase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A response writes [CallState.ownCapabilities] and [CallState.settings] one after the other.
 * The noise-cancellation policy reads the snapshot published after both, so a read during the
 * gap still sees the previous pair.
 */
@RunWith(RobolectricTestRunner::class)
class CallStateServerSnapshotTest : IntegrationTestBase(connectCoordinatorWS = false) {

    private companion object {
        const val CONCURRENT_READ_WAIT_MS = 500L
        const val RESULT_TIMEOUT_MS = 5_000L
    }

    @Test
    fun `a snapshot taken while a join response is applied stays on the previous pair`() {
        val state = client.call("default", randomUUID()).state
        val oldSettings = noiseCancellationSettings(NoiseCancellationSettings.Mode.Available)
        state.injectServerState(capabilities = emptyList(), settings = oldSettings)

        val newCapabilities = listOf(OwnCapability.EnableNoiseCancellation)
        val newSettings = noiseCancellationSettings(NoiseCancellationSettings.Mode.AutoOn)
        var concurrentRead: CompletableFuture<Pair<CallSettingsResponse?, List<OwnCapability>>>? =
            null
        val response = mockk<JoinCallResponse>(relaxed = true) {
            every { ownCapabilities } returns newCapabilities
            every { members } returns emptyList()
            every { call.settings } answers {
                // Capabilities are already written and settings are not. The published snapshot
                // must still be the previous pair, including for a reader on another thread.
                if (concurrentRead == null) {
                    val read = CompletableFuture.supplyAsync { state.settingsAndCapabilities() }
                    concurrentRead = read
                    try {
                        read.get(CONCURRENT_READ_WAIT_MS, TimeUnit.MILLISECONDS)
                    } catch (_: TimeoutException) {
                        // The read shares no lock with the writer, so this is unexpected.
                    }
                }
                newSettings
            }
        }

        state.updateFromResponse(response)

        val during = concurrentRead!!.get(RESULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertEquals(oldSettings to emptyList(), during)
        assertEquals(newSettings to newCapabilities, state.settingsAndCapabilities())
    }

    @Test
    fun `auto-on applies when a join response grants the capability and the mode together`() =
        runTest {
            val call = client.call("default", randomUUID())
            call.state.injectServerState(
                capabilities = emptyList(),
                settings = noiseCancellationSettings(NoiseCancellationSettings.Mode.Available),
            )
            assertFalse(call.isAudioProcessingWanted())

            // The policy collector shares the test dispatcher with this write, so it resumes
            // inside the snapshot assignment. A decision taken on the half-applied response
            // would latch auto-on against the previous Available mode and never start it.
            val response = mockk<JoinCallResponse>(relaxed = true)
            every { response.ownCapabilities } returns listOf(OwnCapability.EnableNoiseCancellation)
            every { response.members } returns emptyList()
            every { response.call.settings } returns noiseCancellationSettings(
                NoiseCancellationSettings.Mode.AutoOn,
            )
            call.state.updateFromResponse(response)

            assertTrue(call.isAudioProcessingWanted())
        }
}
