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

package io.getstream.video.android.ui.common.util

import app.cash.turbine.test
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.RealtimeConnection
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Regression tests for the [lastParticipantSignal] gating (AND-1455): with
 * `leaveWhenLastInCall = true`, a leave triggered while an SFU join or reconnect is running
 * cancels the work in the call scope and leaves the UI stuck on "Connecting...". The signal
 * must only act while the connection is connected, re-evaluate the roster once the connection
 * settles, and not repeat an unchanged roster across connection transitions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class LastParticipantSignalTest {

    private val debounceMs = 1_000L
    private val local = mockk<ParticipantState>()
    private val remote = mockk<ParticipantState>()

    @Test
    fun `emits when the last participant remains while connected`() = runTest {
        val participants = MutableStateFlow(listOf(local, remote))
        val connection = MutableStateFlow<RealtimeConnection>(RealtimeConnection.Connected)

        lastParticipantSignal(participants, connection, debounceMs).test {
            participants.value = listOf(local)

            advanceTimeBy(debounceMs + 1)
            assertEquals(1, awaitItem().size)
        }
    }

    @Test
    fun `does not emit while more than one participant is in the call`() = runTest {
        val participants = MutableStateFlow(listOf(local, remote))
        val connection = MutableStateFlow<RealtimeConnection>(RealtimeConnection.Connected)

        lastParticipantSignal(participants, connection, debounceMs).test {
            advanceTimeBy(debounceMs + 1)
            expectNoEvents()
        }
    }

    @Test
    fun `does not emit while the connection is reconnecting`() = runTest {
        val participants = MutableStateFlow(listOf(local, remote))
        val connection = MutableStateFlow<RealtimeConnection>(RealtimeConnection.Connected)

        lastParticipantSignal(participants, connection, debounceMs).test {
            connection.value = RealtimeConnection.Reconnecting
            participants.value = listOf(local)

            advanceTimeBy(debounceMs + 1)
            expectNoEvents()
        }
    }

    @Test
    fun `does not emit while the connection is migrating`() = runTest {
        val participants = MutableStateFlow(listOf(local, remote))
        val connection = MutableStateFlow<RealtimeConnection>(RealtimeConnection.Connected)

        lastParticipantSignal(participants, connection, debounceMs).test {
            connection.value = RealtimeConnection.Migrating
            participants.value = listOf(local)

            advanceTimeBy(debounceMs + 1)
            expectNoEvents()
        }
    }

    @Test
    fun `does not emit while the join is in progress`() = runTest {
        val participants = MutableStateFlow(listOf(local))
        val connection = MutableStateFlow<RealtimeConnection>(RealtimeConnection.PreJoin)

        lastParticipantSignal(participants, connection, debounceMs).test {
            advanceTimeBy(debounceMs + 1)
            expectNoEvents()

            connection.value = RealtimeConnection.InProgress
            advanceTimeBy(debounceMs + 1)
            expectNoEvents()
        }
    }

    @Test
    fun `does not emit when the reconnect fails terminally`() = runTest {
        val participants = MutableStateFlow(listOf(local))
        val connection = MutableStateFlow<RealtimeConnection>(RealtimeConnection.Reconnecting)

        lastParticipantSignal(participants, connection, debounceMs).test {
            connection.value = RealtimeConnection.ReconnectingFailed

            advanceTimeBy(debounceMs + 1)
            expectNoEvents()
        }
    }

    @Test
    fun `does not emit the same roster again after a reconnect flap`() = runTest {
        val roster = listOf(local)
        val participants = MutableStateFlow(roster)
        val connection = MutableStateFlow<RealtimeConnection>(RealtimeConnection.Connected)

        lastParticipantSignal(participants, connection, debounceMs).test {
            advanceTimeBy(debounceMs + 1)
            assertEquals(1, awaitItem().size)

            connection.value = RealtimeConnection.Reconnecting
            advanceTimeBy(debounceMs + 1)
            connection.value = RealtimeConnection.Connected
            advanceTimeBy(debounceMs + 1)
            expectNoEvents()
        }
    }

    @Test
    fun `emits again when a remote participant joins and leaves again`() = runTest {
        val participants = MutableStateFlow(listOf(local))
        val connection = MutableStateFlow<RealtimeConnection>(RealtimeConnection.Connected)

        lastParticipantSignal(participants, connection, debounceMs).test {
            advanceTimeBy(debounceMs + 1)
            assertEquals(1, awaitItem().size)

            participants.value = listOf(local, remote)
            advanceTimeBy(debounceMs + 1)
            expectNoEvents()

            participants.value = listOf(local)
            advanceTimeBy(debounceMs + 1)
            assertEquals(1, awaitItem().size)
        }
    }

    @Test
    fun `emits after the reconnect settles with the local user still alone`() = runTest {
        val participants = MutableStateFlow(listOf(local, remote))
        val connection = MutableStateFlow<RealtimeConnection>(RealtimeConnection.Connected)

        lastParticipantSignal(participants, connection, debounceMs).test {
            connection.value = RealtimeConnection.Reconnecting
            participants.value = listOf(local)
            advanceTimeBy(debounceMs + 1)
            expectNoEvents()

            connection.value = RealtimeConnection.Connected
            advanceTimeBy(debounceMs + 1)
            assertEquals(1, awaitItem().size)
        }
    }

    @Test
    fun `does not emit after the reconnect settles with the roster restored`() = runTest {
        val participants = MutableStateFlow(listOf(local, remote))
        val connection = MutableStateFlow<RealtimeConnection>(RealtimeConnection.Connected)

        lastParticipantSignal(participants, connection, debounceMs).test {
            connection.value = RealtimeConnection.Reconnecting
            participants.value = listOf(local)
            advanceTimeBy(debounceMs + 1)
            expectNoEvents()

            participants.value = listOf(local, remote)
            connection.value = RealtimeConnection.Connected
            advanceTimeBy(debounceMs + 1)
            expectNoEvents()
        }
    }

    @Test
    fun `debounce absorbs a quick roster flap`() = runTest {
        val participants = MutableStateFlow(listOf(local, remote))
        val connection = MutableStateFlow<RealtimeConnection>(RealtimeConnection.Connected)

        lastParticipantSignal(participants, connection, debounceMs).test {
            participants.value = listOf(local)
            advanceTimeBy(debounceMs - 1)
            participants.value = listOf(local, remote)

            advanceTimeBy(debounceMs + 1)
            expectNoEvents()
        }
    }

    @Test
    fun `invokes onEvaluated for suppressed evaluations`() = runTest {
        val participants = MutableStateFlow(listOf(local))
        val connection = MutableStateFlow<RealtimeConnection>(RealtimeConnection.Reconnecting)
        val evaluations = mutableListOf<Pair<Int, RealtimeConnection>>()

        lastParticipantSignal(
            participants = participants,
            connection = connection,
            debounceMs = debounceMs,
            onEvaluated = { roster, connectionState ->
                evaluations += roster.size to connectionState
            },
        ).test {
            advanceTimeBy(debounceMs + 1)
            expectNoEvents()
        }

        assertEquals(
            listOf(1 to RealtimeConnection.Reconnecting as RealtimeConnection),
            evaluations,
        )
    }
}
