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
import io.getstream.result.Result
import io.getstream.video.android.core.base.IntegrationTestBase
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.connection.StreamPeerConnectionFactory
import io.getstream.video.android.core.socket.sfu.state.SfuSocketState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import stream.video.sfu.signal.StartNoiseCancellationResponse
import stream.video.sfu.signal.StopNoiseCancellationResponse
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class NoiseCancellationSignalTest : IntegrationTestBase(connectCoordinatorWS = false) {

    private companion object {
        const val SIGNAL_TIMEOUT_MS = 5_000L
        const val SIGNAL_POLL_MS = 10L
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

    /**
     * Seeds the server state noise cancellation needs: capability granted and the call type's mode
     * set to available. Without both the policy gate refuses and nothing is signalled — see
     * [NoiseCancellationPolicyGateTest].
     */
    private fun Call.allowNoiseCancellation() {
        state.injectServerState(
            capabilities = listOf(OwnCapability.EnableNoiseCancellation),
            settings = noiseCancellationSettings(NoiseCancellationSettings.Mode.Available),
        )
    }

    /** A joined, allowed call whose audio processing can genuinely be turned on. */
    private fun callWithProcessor(): Pair<Call, RtcSession> {
        val call = client.call("default", randomUUID())
        call.allowNoiseCancellation()
        call.injectWorkingProcessor()
        val session = mockk<RtcSession>(relaxed = true)
        call.injectSession(session)
        return call to session
    }

    /** Records the state of every signal the SFU receives, in the order it receives them. */
    private fun record(session: RtcSession): List<Boolean> {
        val signalled = CopyOnWriteArrayList<Boolean>()
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

    /**
     * MockK records the mocked call before the answer body runs, and the append to the list
     * happens on the signal coroutine, so right after a passing coVerify the recorded signals
     * may not be visible to the test thread yet. Polls until they match instead of asserting
     * on a snapshot.
     */
    private fun awaitSignalled(signalled: List<Boolean>, expected: List<Boolean>) {
        val deadline = System.currentTimeMillis() + SIGNAL_TIMEOUT_MS
        while (signalled != expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(SIGNAL_POLL_MS)
        }
        assertEquals(expected, signalled.toList())
    }

    /** Same visibility caveat as [awaitSignalled], for a test where only the final state matters. */
    private fun awaitFinalSignal(signalled: List<Boolean>, expected: Boolean) {
        val deadline = System.currentTimeMillis() + SIGNAL_TIMEOUT_MS
        while (signalled.lastOrNull() != expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(SIGNAL_POLL_MS)
        }
        assertEquals(expected, signalled.lastOrNull())
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
        call.allowNoiseCancellation()
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
        call.allowNoiseCancellation()
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
    fun `start is not signalled until the SFU has accepted the join`() = runTest {
        val call = client.call("default", randomUUID())
        call.allowNoiseCancellation()
        call.injectWorkingProcessor()

        // Session exists as soon as joinInternal installs it, which is before the SFU has
        // delivered JoinCallResponseEvent. Signalling in Connecting or WebSocketConnected
        // is PARTICIPANT_NOT_FOUND.
        val session = mockk<RtcSession>(relaxed = true)
        val signalled = record(session)
        val sfuState = MutableStateFlow<SfuSocketState>(
            SfuSocketState.Connecting(mockk(relaxed = true)),
        )
        call.injectSession(session, sfuState)

        call.setAudioProcessingEnabled(true)

        coVerify(timeout = 500, exactly = 0) { session.startNoiseCancellation() }

        sfuState.value = SfuSocketState.WebSocketConnected
        coVerify(timeout = 500, exactly = 0) { session.startNoiseCancellation() }

        sfuState.value = SfuSocketState.Connected(mockk(relaxed = true))

        coVerify(timeout = SIGNAL_TIMEOUT_MS) { session.startNoiseCancellation() }
        assertEquals(listOf(true), signalled)
    }

    @Test
    fun `state changed before a session exists is signalled once one is installed`() = runTest {
        val call = client.call("default", randomUUID())
        call.allowNoiseCancellation()
        call.injectWorkingProcessor()

        // The call type's auto-on default and any pre-join change land while joining is still in
        // progress, so the signal has nowhere to go yet.
        call.setAudioProcessingEnabled(true)

        val session = mockk<RtcSession>(relaxed = true)
        val signalled = record(session)
        call.injectSession(session)

        coVerify(timeout = SIGNAL_TIMEOUT_MS) { session.startNoiseCancellation() }
        awaitSignalled(signalled, listOf(true))
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
        awaitSignalled(signalled, listOf(true))
    }

    @Test
    fun `a replacement session is not signalled until the SFU has accepted the join`() = runTest {
        val (call, first) = callWithProcessor()
        call.setAudioProcessingEnabled(true)
        coVerify(timeout = SIGNAL_TIMEOUT_MS) { first.startNoiseCancellation() }

        val replacement = mockk<RtcSession>(relaxed = true)
        val signalled = record(replacement)
        val sfuState = MutableStateFlow<SfuSocketState>(
            SfuSocketState.Connecting(mockk(relaxed = true)),
        )
        call.injectSession(replacement, sfuState)

        coVerify(timeout = 500, exactly = 0) { replacement.startNoiseCancellation() }

        sfuState.value = SfuSocketState.Connected(mockk(relaxed = true))

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
        awaitFinalSignal(signalled, expected = false)
    }

    @Test
    fun `a state applied only once the factory exists is signalled on join`() = runTest {
        val call = client.call("default", randomUUID())
        call.allowNoiseCancellation()

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
        awaitSignalled(signalled, listOf(true))
    }
}
