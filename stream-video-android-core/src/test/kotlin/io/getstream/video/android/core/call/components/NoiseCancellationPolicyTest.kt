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

import io.getstream.android.video.generated.models.CallSettingsResponse
import io.getstream.android.video.generated.models.NoiseCancellationSettings
import io.getstream.android.video.generated.models.OwnCapability
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoiseCancellationPolicyTest {

    private val policy = NoiseCancellationPolicy()
    private val granted = listOf(OwnCapability.EnableNoiseCancellation)

    private fun settings(mode: NoiseCancellationSettings.Mode): CallSettingsResponse =
        mockk(relaxed = true) {
            every { audio.noiseCancellation } returns NoiseCancellationSettings(mode)
        }

    private fun settingsWithoutNoiseCancellation(): CallSettingsResponse = mockk(relaxed = true) {
        every { audio.noiseCancellation } returns null
    }

    @Test
    fun `allowed when the capability is granted and the mode is available`() {
        assertTrue(
            policy.isAllowed(granted, settings(NoiseCancellationSettings.Mode.Available)),
        )
    }

    @Test
    fun `allowed when the capability is granted and the mode is auto-on`() {
        assertTrue(policy.isAllowed(granted, settings(NoiseCancellationSettings.Mode.AutoOn)))
    }

    @Test
    fun `not allowed without the capability, whatever the mode says`() {
        assertFalse(
            policy.isAllowed(emptyList(), settings(NoiseCancellationSettings.Mode.AutoOn)),
        )
    }

    @Test
    fun `not allowed when the call type disables it, even with the capability`() {
        assertFalse(
            policy.isAllowed(granted, settings(NoiseCancellationSettings.Mode.Disabled)),
        )
    }

    @Test
    fun `not allowed for an unrecognised mode`() {
        assertFalse(
            policy.isAllowed(granted, settings(NoiseCancellationSettings.Mode.Unknown("future"))),
        )
    }

    @Test
    fun `not allowed when the call type carries no noise-cancellation settings`() {
        assertFalse(policy.isAllowed(granted, settingsWithoutNoiseCancellation()))
    }

    @Test
    fun `not decided before settings resolve`() {
        // Null settings mean the server has not told us yet. Callers must wait rather than treat
        // this as a refusal, or noise cancellation flaps off and back on during join.
        assertFalse(policy.isAllowed(granted, null))
        assertFalse(policy.isAutoOn(granted, null))
    }

    @Test
    fun `auto-on only for the auto-on mode`() {
        assertTrue(policy.isAutoOn(granted, settings(NoiseCancellationSettings.Mode.AutoOn)))
        assertFalse(policy.isAutoOn(granted, settings(NoiseCancellationSettings.Mode.Available)))
    }

    @Test
    fun `auto-on requires the capability too`() {
        assertFalse(
            policy.isAutoOn(emptyList(), settings(NoiseCancellationSettings.Mode.AutoOn)),
        )
    }
}
