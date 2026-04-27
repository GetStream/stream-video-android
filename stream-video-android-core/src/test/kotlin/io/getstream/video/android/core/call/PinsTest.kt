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

package io.getstream.video.android.core.call

import app.cash.turbine.test
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.events.ParticipantJoinedEvent
import io.getstream.video.android.core.events.PinUpdate
import io.getstream.video.android.core.pinning.PinType
import io.getstream.video.android.core.pinning.PinUpdateAtTime
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.threeten.bp.Clock
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PinsTest {

    private val fixedClock = Clock.fixed(
        Instant.parse("2024-01-01T00:00:00Z"),
        ZoneOffset.UTC,
    )

    @Before
    fun before() {
        org.threeten.bp.zone.ZoneRulesProvider.getAvailableZoneIds()
    }

    private fun getPins(scope: CoroutineScope): Pins {
        val pins = Pins(scope, fixedClock)
        pins.updateServerPins(emptyMap())
        return pins
    }

    @Test
    fun `pin adds session to localPins`() = runTest {
        val pins = getPins(backgroundScope)
        pins.pin("user1", "session1")

        val local = pins.localPins.value
        assertTrue(local.containsKey("session1"))
        assertEquals("user1", local["session1"]!!.it.userId)
        assertEquals("session1", local["session1"]!!.it.sessionId)
        assertEquals(PinType.Local, local["session1"]!!.type)
    }

    @Test
    fun `pin multiple sessions adds all to localPins`() = runTest {
        val pins = getPins(backgroundScope)

        pins.pin("user1", "session1")
        pins.pin("user2", "session2")

        val local = pins.localPins.value
        assertEquals(2, local.size)
        assertTrue(local.containsKey("session1"))
        assertTrue(local.containsKey("session2"))
    }

    @Test
    fun `unpin removes session from localPins`() = runTest {
        val pins = getPins(backgroundScope)

        pins.pin("user1", "session1")
        pins.unpin("session1")

        assertFalse(pins.localPins.value.containsKey("session1"))
    }

    @Test
    fun `unpin on non-existent session does not throw`() = runTest {
        val pins = getPins(backgroundScope)

        pins.unpin("no-such-session")

        assertTrue(pins.localPins.value.isEmpty())
    }

    @Test
    fun `unpin only removes the targeted session`() = runTest {
        val pins = getPins(backgroundScope)

        pins.pin("user1", "session1")
        pins.pin("user2", "session2")
        pins.unpin("session1")

        val local = pins.localPins.value
        assertFalse(local.containsKey("session1"))
        assertTrue(local.containsKey("session2"))
    }

    @Test
    fun `updateLocalPins replaces existing localPins`() = runTest {
        val pins = getPins(backgroundScope)

        pins.pin("user1", "session1")

        val newPins = mapOf(
            "session2" to PinUpdateAtTime(
                PinUpdate("user2", "session2"),
                OffsetDateTime.now(fixedClock),
                PinType.Local,
            ),
        )
        pins.updateLocalPins(newPins)

        val local = pins.localPins.value
        assertEquals(1, local.size)
        assertTrue(local.containsKey("session2"))
        assertFalse(local.containsKey("session1"))
    }

    // endregion

    // region serverPins

    @Test
    fun `updateServerPins with map stores server pins`() = runTest {
        val pins = getPins(backgroundScope)

        val serverMap = mapOf(
            "session1" to PinUpdateAtTime(
                PinUpdate("user1", "session1"),
                OffsetDateTime.now(fixedClock),
                PinType.Server,
            ),
        )
        pins.updateServerPins(serverMap)

        val server = pins.serverPins.value
        assertTrue(server.containsKey("session1"))
        assertEquals(PinType.Server, server["session1"]!!.type)
    }

    @Test
    fun `updateServerPins with participants and pin list only keeps participants in call`() = runTest {
        val pins = getPins(backgroundScope)

        val participantInCall = mockk<ParticipantState>()
        val internalParticipants = mapOf("session1" to participantInCall)

        val pinList = listOf(
            PinUpdate("user1", "session1"),
            PinUpdate("user2", "session2"),
        )
        pins.updateServerPins(internalParticipants, pinList)

        val server = pins.serverPins.value
        assertEquals(1, server.size)
        assertTrue(server.containsKey("session1"))
        assertFalse(server.containsKey("session2"))
    }

    @Test
    fun `updateServerPins with empty participant list results in empty server pins`() = runTest {
        val pins = getPins(backgroundScope)

        val pinList = listOf(PinUpdate("user1", "session1"))
        pins.updateServerPins(emptyMap(), pinList)

        assertTrue(pins.serverPins.value.isEmpty())
    }

    @Test
    fun `updateServerPins with event adds participant when not already pinned`() = runTest {
        val pins = getPins(backgroundScope)

        val participantState = mockk<ParticipantState>()
        val internalParticipants = mapOf("session1" to participantState)

        val participant = mockk<stream.video.sfu.models.Participant>()
        every { participant.session_id } returns "session1"
        every { participant.user_id } returns "user1"

        val event = ParticipantJoinedEvent(participant = participant, callCid = "call:cid")

        pins.updateServerPins(internalParticipants, event)

        val server = pins.serverPins.value
        assertTrue(server.containsKey("session1"))
    }

    @Test
    fun `updateServerPins with event does not add participant when already pinned`() = runTest {
        val pins = getPins(backgroundScope)

        val participantState = mockk<ParticipantState>()
        val internalParticipants = mapOf("session1" to participantState)

        val existing = mapOf(
            "session1" to PinUpdateAtTime(
                PinUpdate("user1", "session1"),
                OffsetDateTime.now(fixedClock),
                PinType.Server,
            ),
        )
        pins.updateServerPins(existing)

        val participant = mockk<stream.video.sfu.models.Participant>()
        every { participant.session_id } returns "session1"
        every { participant.user_id } returns "user1"

        val event = ParticipantJoinedEvent(participant = participant, callCid = "call:cid")
        pins.updateServerPins(internalParticipants, event)

        assertEquals(1, pins.serverPins.value.size)
    }

    @Test
    fun `updateServerPins with event ignores participant not in internalParticipants`() = runTest {
        val pins = getPins(backgroundScope)

        val participant = mockk<stream.video.sfu.models.Participant>()
        every { participant.session_id } returns "session99"
        every { participant.user_id } returns "user99"

        val event = ParticipantJoinedEvent(participant = participant, callCid = "call:cid")
        pins.updateServerPins(emptyMap(), event)

        assertTrue(pins.serverPins.value.isEmpty())
    }

    // endregion

    // region pinnedParticipants

    @Test
    fun `pinnedParticipants is empty when no pins exist`() = runTest {
        val pins = getPins(backgroundScope)

        advanceUntilIdle()

        assertTrue(pins.pinnedParticipants.value.isEmpty())
    }

    @Test
    fun `pinnedParticipants contains local pin after pin()`() = runTest {
        val pins = getPins(backgroundScope)

        pins.pinnedParticipants.test {
            // 1. initial emission
            val initial = awaitItem()
            assertTrue(initial.isEmpty())

            // 2. trigger pin
            pins.pin("user1", "session1")

            // 3. assert update
            val updated = awaitItem()
            assertTrue(updated.containsKey("session1"))

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pinnedParticipants removes entry after unpin()`() = runTest {
        val pins = getPins(backgroundScope)

        pins.pin("user1", "session1")
        advanceUntilIdle()

        pins.unpin("session1")
        advanceUntilIdle()

        assertFalse(pins.pinnedParticipants.value.containsKey("session1"))
    }

    @Test
    fun `pinnedParticipants merges local and server pins`() = runTest {
        val pins = getPins(backgroundScope)

        pins.pinnedParticipants.test {
            // 1. Initial emission from stateIn
            val initial = awaitItem()
            assertTrue(initial.isEmpty())

            // 2. Trigger local update
            pins.pin("user1", "session1")

            // 3. Expect update from local
            val afterLocal = awaitItem()
            assertEquals(1, afterLocal.size)
            assertTrue(afterLocal.containsKey("session1"))

            // 4. Trigger server update
            val serverMap = mapOf(
                "session2" to PinUpdateAtTime(
                    PinUpdate("user2", "session2"),
                    OffsetDateTime.now(fixedClock),
                    PinType.Server,
                ),
            )
            pins.updateServerPins(serverMap)

            // 5. Expect merged result
            val final = awaitItem()
            assertEquals(2, final.size)
            assertTrue(final.containsKey("session1"))
            assertTrue(final.containsKey("session2"))

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pinnedParticipants server pin overwrites local pin for same session`() = runTest {
        val pins = getPins(backgroundScope)

        val localTime = OffsetDateTime.now(fixedClock).minusMinutes(5)
        val serverTime = OffsetDateTime.now(fixedClock)

        pins.pinnedParticipants.test {
            // 1) initial emission from stateIn
            val initial = awaitItem()
            assertTrue(initial.isEmpty())

            // 2) apply local pin
            pins.updateLocalPins(
                mapOf(
                    "session1" to PinUpdateAtTime(
                        PinUpdate("user1", "session1"),
                        localTime,
                        PinType.Local,
                    ),
                ),
            )

            val afterLocal = awaitItem()
            assertEquals(1, afterLocal.size)
            assertEquals(localTime, afterLocal["session1"])

            // 3) apply server pin (should overwrite)
            pins.updateServerPins(
                mapOf(
                    "session1" to PinUpdateAtTime(
                        PinUpdate("user1", "session1"),
                        serverTime,
                        PinType.Server,
                    ),
                ),
            )

            val afterServer = awaitItem()
            assertEquals(1, afterServer.size)
            assertEquals(serverTime, afterServer["session1"]) // overwrite verified

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pinnedParticipants emits updates when localPins change`() = runTest {
        val pins = getPins(backgroundScope)

        pins.pinnedParticipants.test {
            // 1. initial
            val initial = awaitItem()
            assertTrue(initial.isEmpty())

            // 2. after pin
            pins.pin("user1", "session1")
            val afterPin = awaitItem()
            assertTrue(afterPin.containsKey("session1"))

            // 3. after unpin
            pins.unpin("session1")
            val afterUnpin = awaitItem()
            assertTrue(afterUnpin.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion
}
