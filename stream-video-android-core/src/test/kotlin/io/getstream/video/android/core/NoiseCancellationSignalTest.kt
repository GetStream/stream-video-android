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

import io.getstream.video.android.core.base.IntegrationTestBase
import io.getstream.video.android.core.call.RtcSession
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse

@RunWith(RobolectricTestRunner::class)
class NoiseCancellationSignalTest : IntegrationTestBase(connectCoordinatorWS = false) {

    private companion object {
        const val SIGNAL_TIMEOUT_MS = 5_000L
    }

    @Test
    fun `enabling audio processing signals the SFU that noise cancellation started`() = runTest {
        val call = client.call("default", randomUUID())
        val session = mockk<RtcSession>(relaxed = true)
        call.injectSession(session)

        call.setAudioProcessingEnabled(true)

        coVerify(timeout = SIGNAL_TIMEOUT_MS) { session.startNoiseCancellation() }
    }

    @Test
    fun `disabling audio processing signals the SFU that noise cancellation stopped`() = runTest {
        val call = client.call("default", randomUUID())
        val session = mockk<RtcSession>(relaxed = true)
        call.injectSession(session)

        call.setAudioProcessingEnabled(false)

        coVerify(timeout = SIGNAL_TIMEOUT_MS) { session.stopNoiseCancellation() }
    }

    @Test
    fun `toggle signals the resolved state, not the requested one`() = runTest {
        val call = client.call("default", randomUUID())
        val session = mockk<RtcSession>(relaxed = true)
        call.injectSession(session)

        // No ManagedAudioProcessingFactory is supplied in tests, so the toggle cannot turn
        // anything on — and the signal must follow what actually happened locally.
        val enabled = call.toggleAudioProcessing()

        assertFalse(enabled)
        coVerify(timeout = SIGNAL_TIMEOUT_MS) { session.stopNoiseCancellation() }
    }

    @Test
    fun `toggling with no active session does not signal or throw`() = runTest {
        val call = client.call("default", randomUUID())
        val session = mockk<RtcSession>(relaxed = true)
        call.injectSession(null)

        call.setAudioProcessingEnabled(true)
        call.toggleAudioProcessing()

        coVerify(exactly = 0) { session.startNoiseCancellation() }
        coVerify(exactly = 0) { session.stopNoiseCancellation() }
    }
}
