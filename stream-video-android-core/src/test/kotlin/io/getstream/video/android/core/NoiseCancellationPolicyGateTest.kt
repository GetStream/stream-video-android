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

import io.getstream.android.video.generated.models.NoiseCancellationSettings
import io.getstream.android.video.generated.models.OwnCapability
import io.getstream.video.android.core.base.IntegrationTestBase
import io.getstream.video.android.core.call.RtcSession
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers the core-level gate: noise cancellation may only be turned on when the server both
 * grants the capability and leaves it enabled on the call type.
 */
@RunWith(RobolectricTestRunner::class)
class NoiseCancellationPolicyGateTest : IntegrationTestBase(connectCoordinatorWS = false) {

    private companion object {
        const val TIMEOUT_MS = 2_000L
    }

    private fun call(
        capabilities: List<OwnCapability>,
        mode: NoiseCancellationSettings.Mode?,
    ): Pair<Call, RtcSession> {
        val call = client.call("default", randomUUID())
        call.state.injectServerState(
            capabilities = capabilities,
            settings = mode?.let { noiseCancellationSettings(it) },
        )
        val session = mockk<RtcSession>(relaxed = true)
        call.injectSession(session)
        return call to session
    }

    @Test
    fun `enabling is refused without the capability`() = runTest {
        val (call, session) = call(emptyList(), NoiseCancellationSettings.Mode.Available)

        call.setAudioProcessingEnabled(true)

        assertFalse(call.isAudioProcessingEnabled())
        coVerify(exactly = 0) { session.startNoiseCancellation() }
    }

    @Test
    fun `enabling is refused when the call type disables noise cancellation`() = runTest {
        val (call, session) = call(
            listOf(OwnCapability.EnableNoiseCancellation),
            NoiseCancellationSettings.Mode.Disabled,
        )

        call.setAudioProcessingEnabled(true)

        assertFalse(call.isAudioProcessingEnabled())
        coVerify(exactly = 0) { session.startNoiseCancellation() }
    }

    @Test
    fun `enabling is refused before settings resolve`() = runTest {
        val (call, session) = call(listOf(OwnCapability.EnableNoiseCancellation), mode = null)

        call.setAudioProcessingEnabled(true)

        coVerify(exactly = 0) { session.startNoiseCancellation() }
    }

    @Test
    fun `toggling on is refused when not allowed`() = runTest {
        val (call, session) = call(emptyList(), NoiseCancellationSettings.Mode.Available)

        assertFalse(call.toggleAudioProcessing())
        coVerify(exactly = 0) { session.startNoiseCancellation() }
    }

    @Test
    fun `disabling is always permitted so a withdrawal can never be blocked`() = runTest {
        val (call, session) = call(emptyList(), NoiseCancellationSettings.Mode.Disabled)

        call.setAudioProcessingEnabled(false)

        coVerify(timeout = TIMEOUT_MS) { session.stopNoiseCancellation() }
    }

    @Test
    fun `withdrawal clears a state wanted before any factory existed`() = runTest {
        val (call, _) = call(
            listOf(OwnCapability.EnableNoiseCancellation),
            NoiseCancellationSettings.Mode.Available,
        )

        // Wanted while no factory exists, so nothing is processing yet and a withdrawal that only
        // looks at what is running would find nothing to do.
        call.setAudioProcessingEnabled(true)
        assertTrue(call.isAudioProcessingWanted())

        // The server then withholds it. The wanted state has to go, or the next factory applies
        // it after the server said no.
        call.state.injectServerState(capabilities = emptyList())

        withTimeout(TIMEOUT_MS) {
            while (call.isAudioProcessingWanted()) {
                delay(10)
            }
        }
        assertFalse(call.isAudioProcessingWanted())
    }

    @Test
    fun `the toggle gate does not build a peer-connection factory`() = runTest {
        val (call, _) = call(emptyList(), NoiseCancellationSettings.Mode.Available)

        // Refused by the gate, and must not have created a factory on the way: one built before
        // join captures the pre-join audio bitrate profile.
        assertFalse(call.toggleAudioProcessing())
        assertFalse(call.hasPeerConnectionFactory())
    }
}
