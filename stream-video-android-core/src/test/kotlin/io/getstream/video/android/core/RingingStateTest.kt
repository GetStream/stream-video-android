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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Structural equality of [RingingState] is load-bearing: the ringing state is published through a
 * [MutableStateFlow], and every consumer relies on the flow conflating unchanged values. Without
 * it, each recomputation publishes a fresh value and restarts side effects that are meant to run
 * once per transition — the auto-cancel ring timer, the outgoing ringtone, and the ongoing-call
 * notification.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RingingStateTest {

    @Test
    fun `outgoing states with the same acceptance are equal`() {
        assertEquals(
            RingingState.Outgoing(acceptedByCallee = false),
            RingingState.Outgoing(acceptedByCallee = false),
        )
        assertEquals(
            RingingState.Outgoing(acceptedByCallee = false).hashCode(),
            RingingState.Outgoing(acceptedByCallee = false).hashCode(),
        )
    }

    @Test
    fun `outgoing states with different acceptance are not equal`() {
        assertNotEquals(
            RingingState.Outgoing(acceptedByCallee = false),
            RingingState.Outgoing(acceptedByCallee = true),
        )
    }

    @Test
    fun `incoming states with the same acceptance are equal`() {
        assertEquals(
            RingingState.Incoming(acceptedByMe = false),
            RingingState.Incoming(acceptedByMe = false),
        )
        assertNotEquals(
            RingingState.Incoming(acceptedByMe = false),
            RingingState.Incoming(acceptedByMe = true),
        )
    }

    @Test
    fun `outgoing and incoming with the same acceptance are not equal`() {
        assertNotEquals<RingingState>(
            RingingState.Outgoing(acceptedByCallee = true),
            RingingState.Incoming(acceptedByMe = true),
        )
    }

    /**
     * `CallState.previousRingingStates` is a hash set. Without structural equality every
     * recomputation adds a new member and the set grows for the lifetime of the ring.
     */
    @Test
    fun `identical outgoing states collapse in a hash set`() {
        val states = ConcurrentHashMap.newKeySet<RingingState>()

        repeat(50) { states.add(RingingState.Outgoing(acceptedByCallee = false)) }
        states.add(RingingState.Outgoing(acceptedByCallee = true))

        assertEquals(2, states.size)
    }

    /**
     * The mechanism every side effect depends on: repeated identical outgoing states must not be
     * republished. `CallState.updateRingingState` allocates a new instance on every run, so
     * identity equality here would re-emit on each recomputation.
     */
    @Test
    fun `state flow does not republish identical outgoing states`() = runTest {
        val ringingState = MutableStateFlow<RingingState>(RingingState.Idle)
        val emissions = mutableListOf<RingingState>()

        // The collector has to observe every published value, so it runs eagerly on assignment.
        // A queued collector would only ever see the latest value and the assertion below would
        // hold even without structural equality.
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            ringingState.collect { emissions.add(it) }
        }

        // Three recomputations that all resolve to "outgoing, not yet accepted".
        repeat(3) { ringingState.value = RingingState.Outgoing(acceptedByCallee = false) }
        // A genuine transition must still be published.
        ringingState.value = RingingState.Outgoing(acceptedByCallee = true)

        collectJob.cancel()

        assertEquals(
            listOf(
                RingingState.Idle,
                RingingState.Outgoing(acceptedByCallee = false),
                RingingState.Outgoing(acceptedByCallee = true),
            ),
            emissions,
        )
    }
}
