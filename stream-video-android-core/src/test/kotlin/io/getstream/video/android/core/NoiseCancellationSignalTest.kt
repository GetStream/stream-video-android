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

import io.getstream.result.Result
import io.getstream.video.android.core.base.IntegrationTestBase
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.connection.StreamPeerConnectionFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import stream.video.sfu.signal.StartNoiseCancellationResponse
import stream.video.sfu.signal.StopNoiseCancellationResponse
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class NoiseCancellationSignalTest : IntegrationTestBase(connectCoordinatorWS = false) {

    private companion object {
        const val SIGNAL_TIMEOUT_MS = 5_000L
    }

    /**
     * Installs a factory that actually tracks the audio-processing state, so what the SFU is told
     * follows what was applied locally rather than a fixed answer.
     */
    private fun Call.injectWorkingProcessor() {
        var processing = false
        val factory = mockk<StreamPeerConnectionFactory>(relaxed = true)
        every { factory.isAudioProcessingEnabled() } answers { processing }
        every { factory.setAudioProcessingEnabled(any()) } answers { processing = firstArg() }
        every { factory.toggleAudioProcessing() } answers {
            processing = !processing
            processing
        }
        injectPeerConnectionFactory(factory)
    }

    /** A joined call whose audio processing can genuinely be turned on. */
    private fun callWithProcessor(): Pair<Call, RtcSession> {
        val call = client.call("default", randomUUID())
        call.injectWorkingProcessor()
        val session = mockk<RtcSession>(relaxed = true)
        call.injectSession(session)
        return call to session
    }

    /** Records the state of every signal the SFU receives, in the order it receives them. */
    private fun record(session: RtcSession): MutableList<Boolean> {
        val signalled = mutableListOf<Boolean>()
        coEvery { session.startNoiseCancellation() } coAnswers {
            signalled += true
            Result.Success(mockk<StartNoiseCancellationResponse>(relaxed = true))
        }
        coEvery { session.stopNoiseCancellation() } coAnswers {
            signalled += false
            Result.Success(mockk<StopNoiseCancellationResponse>(relaxed = true))
        }
        return signalled
    }

    @Test
    fun `enabling audio processing signals the SFU that noise cancellation started`() = runTest {
        val (call, session) = callWithProcessor()

        call.setAudioProcessingEnabled(true)

        coVerify(timeout = SIGNAL_TIMEOUT_MS) { session.startNoiseCancellation() }
    }

    @Test
    fun `disabling audio processing signals the SFU that noise cancellation stopped`() = runTest {
        val (call, session) = callWithProcessor()

        call.setAudioProcessingEnabled(false)

        coVerify(timeout = SIGNAL_TIMEOUT_MS) { session.stopNoiseCancellation() }
    }

    @Test
    fun `nothing is signalled as started when no processor is running`() = runTest {
        val call = client.call("default", randomUUID())
        val session = mockk<RtcSession>(relaxed = true)
        call.injectSession(session)

        // No ManagedAudioProcessingFactory is configured, so the request cannot take effect
        // locally. The SFU must be told what is actually running, not what was asked for.
        call.setAudioProcessingEnabled(true)

        coVerify(timeout = SIGNAL_TIMEOUT_MS) { session.stopNoiseCancellation() }
        coVerify(exactly = 0) { session.startNoiseCancellation() }
    }

    @Test
    fun `toggle reports the wanted state but signals what is running`() = runTest {
        val call = client.call("default", randomUUID())
        val session = mockk<RtcSession>(relaxed = true)
        call.injectSession(session)

        // No factory exists yet, so the toggle records what the call wants and reports that —
        // building one here would capture the pre-join audio bitrate profile. Nothing is
        // processing, so the SFU is told noise cancellation is off.
        val wanted = call.toggleAudioProcessing()

        assertEquals(true, wanted)
        coVerify(timeout = SIGNAL_TIMEOUT_MS) { session.stopNoiseCancellation() }
        coVerify(exactly = 0) { session.startNoiseCancellation() }
    }

    @Test
    fun `state changed before a session exists is signalled once one is installed`() = runTest {
        val call = client.call("default", randomUUID())
        call.injectWorkingProcessor()

        // The call type's auto-on default and any pre-join change land while joining is still in
        // progress, so the signal has nowhere to go yet.
        call.setAudioProcessingEnabled(true)

        val session = mockk<RtcSession>(relaxed = true)
        val signalled = record(session)
        call.injectSession(session)

        coVerify(timeout = SIGNAL_TIMEOUT_MS) { session.startNoiseCancellation() }
        assertEquals(listOf(true), signalled)
    }

    @Test
    fun `a replacement session is told the current state`() = runTest {
        val (call, first) = callWithProcessor()
        call.setAudioProcessingEnabled(true)
        coVerify(timeout = SIGNAL_TIMEOUT_MS) { first.startNoiseCancellation() }

        // A rejoin or migration installs a session that was never told, which would leave the SFU
        // out of step with what is still running locally.
        val replacement = mockk<RtcSession>(relaxed = true)
        val signalled = record(replacement)
        call.injectSession(replacement)

        coVerify(timeout = SIGNAL_TIMEOUT_MS) { replacement.startNoiseCancellation() }
        assertEquals(listOf(true), signalled)
    }

    @Test
    fun `rapid changes leave the SFU holding the final state`() = runTest {
        val (call, session) = callWithProcessor()
        val signalled = record(session)

        // Signals are queued and each sends whatever was requested most recently. Intermediate
        // signals are legitimate — the state really was on for a moment — so what matters is
        // the last one the SFU is left holding.
        call.setAudioProcessingEnabled(true)
        call.setAudioProcessingEnabled(false)

        // Waiting on the stop means every signal before it has already landed.
        coVerify(timeout = SIGNAL_TIMEOUT_MS) { session.stopNoiseCancellation() }
        assertEquals(false, signalled.last())
    }

    @Test
    fun `a state applied only once the factory exists is signalled on join`() = runTest {
        val call = client.call("default", randomUUID())

        // Wanted before any factory exists, so nothing is applied yet and the signal finds no
        // session to send at.
        call.setAudioProcessingEnabled(true)

        // The factory is built while joining and applies the wanted state, so by the time a
        // session appears noise cancellation really is running.
        call.injectWorkingProcessor()
        call.mediaAppliesWantedState()

        val session = mockk<RtcSession>(relaxed = true)
        val signalled = record(session)
        call.injectSession(session)

        coVerify(timeout = SIGNAL_TIMEOUT_MS) { session.startNoiseCancellation() }
        assertEquals(listOf(true), signalled)
    }
}
