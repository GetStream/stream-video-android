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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.test.assertEquals

/**
 * A response writes [CallState.ownCapabilities] and [CallState.settings] one after the other.
 * Policy decisions read both, so they must never see a response half applied.
 */
@RunWith(RobolectricTestRunner::class)
class CallStateServerSnapshotTest : IntegrationTestBase(connectCoordinatorWS = false) {

    private companion object {
        const val CONCURRENT_READ_WAIT_MS = 500L
        const val RESULT_TIMEOUT_MS = 5_000L
    }

    @Test
    fun `a snapshot taken while a join response is applied sees it whole`() {
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
                // The first read is the one that writes settings, so capabilities are already
                // written and settings are not: read from another thread right in that gap, and
                // give it time to return the mixed pair if it can.
                if (concurrentRead == null) {
                    val read = CompletableFuture.supplyAsync { state.settingsAndCapabilities() }
                    concurrentRead = read
                    try {
                        read.get(CONCURRENT_READ_WAIT_MS, TimeUnit.MILLISECONDS)
                    } catch (_: TimeoutException) {
                        // Blocked behind the response being applied, as it should be.
                    }
                }
                newSettings
            }
        }

        state.updateFromResponse(response)

        val snapshot = concurrentRead!!.get(RESULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertEquals(newSettings to newCapabilities, snapshot)
    }
}
