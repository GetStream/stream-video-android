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

package io.getstream.video.android.core.pinning

import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.SessionId
import io.getstream.video.android.core.events.ParticipantJoinedEvent
import io.getstream.video.android.core.events.PinUpdate
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import stream.video.sfu.models.Participant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class PinManagerTest {
    private lateinit var participants: MutableMap<SessionId, ParticipantState>
    private lateinit var timeProvider: TimeProvider
    private lateinit var manager: PinManager

    @Before
    fun setup() {
        participants = mutableMapOf()
        timeProvider = FakeTimeProvider()

        manager = PinManager(
            timeProvider = timeProvider,
            participants = { participants },
        )
    }

    private fun fakeParticipant(
        sessionId: String,
        userId: String,
    ): ParticipantState {
        return mockk {
            every { this@mockk.sessionId } returns sessionId
            every { this@mockk.userId.value } returns userId
        }
    }

    @Test
    fun `pin should add local pin`() {
        manager.pin(
            userId = "user-1",
            sessionId = "session-1",
        )

        val result = manager.localPins.value["session-1"]

        assertNotNull(result)
        assertEquals(PinType.Local, result.type)
        assertEquals("user-1", result.pinTarget.userId)
        assertEquals("session-1", result.pinTarget.sessionId)
        assertEquals(timeProvider.now(), result.at)
    }

    @Test
    fun `unpin should remove local pin`() {
        manager.pin("user-1", "session-1")
        manager.unpin("session-1")
        assertFalse(manager.localPins.value.containsKey("session-1"))
    }

    @Test
    fun `setServerPins should map server pin to connected participant by sessionId`() {
        participants["session-1"] = fakeParticipant(
            sessionId = "session-1",
            userId = "user-1",
        )
        manager.setServerPins(
            listOf(
                PinUpdate(
                    userId = "user-1",
                    sessionId = "session-1",
                ),
            ),
        )

        val result = manager.serverPins.value["session-1"]
        assertNotNull(result)
        assertEquals("session-1", result.pinTarget.sessionId)
    }

    @Test
    fun `setServerPins should fallback to userId when session changes`() {
        participants["new-session"] = fakeParticipant(
            sessionId = "new-session",
            userId = "user-1",
        )

        manager.setServerPins(
            listOf(
                PinUpdate(
                    userId = "user-1",
                    sessionId = "old-session",
                ),
            ),
        )

        val result = manager.serverPins.value["new-session"]

        assertNotNull(result)
        assertEquals("old-session", result.pinTarget.sessionId)
        assertEquals("user-1", result.pinTarget.userId)
    }

    @Test
    fun `setServerPins should disable fallback for duplicate userIds`() {
        participants["new-session"] = fakeParticipant(
            sessionId = "new-session",
            userId = "user-1",
        )
        manager.setServerPins(
            listOf(
                PinUpdate("user-1", "session-1"),
                PinUpdate("user-1", "session-2"),
            ),
        )
        assertTrue(manager.serverPins.value.isEmpty())
    }

    @Test
    fun `setServerPins should prefer exact session match over userId fallback`() {
        participants["session-1"] = fakeParticipant(
            sessionId = "session-1",
            userId = "user-1",
        )

        manager.setServerPins(
            listOf(
                PinUpdate("user-1", "session-1"),
                PinUpdate("user-1", "other-session"),
            ),
        )

        val result = manager.serverPins.value["session-1"]
        assertEquals("session-1", result!!.pinTarget.sessionId)
    }

    @Test
    fun `setServerPins should preserve existing timestamp`() {
        val existingTime =
            OffsetDateTime.parse("2025-01-01T00:00:00Z")

        manager.updateServerPins(
            mapOf(
                "session-1" to PinEntry(
                    pinTarget = PinUpdate("user-1", "session-1"),
                    at = existingTime,
                    type = PinType.Server,
                ),
            ),
        )

        participants["session-1"] = fakeParticipant(
            sessionId = "session-1",
            userId = "user-1",
        )

        manager.setServerPins(
            listOf(
                PinUpdate("user-1", "session-1"),
            ),
        )

        assertEquals(
            existingTime,
            manager.serverPins.value["session-1"]!!.at,
        )
    }

    @Test
    fun `setServerPins should preserve server pin order`() {
        participants["s1"] = fakeParticipant("s1", "u1")
        participants["s2"] = fakeParticipant("s2", "u2")

        manager.setServerPins(
            listOf(
                PinUpdate("u1", "s1"), // highest priority
                PinUpdate("u2", "s2"),
            ),
        )

        val first = manager.serverPins.value["s1"]!!
        val second = manager.serverPins.value["s2"]!!

        assertTrue(first.at.isAfter(second.at))
    }

    @Test
    fun `onParticipantJoined should add server pin`() {
        val sessionId = "session-1"
        val userId = "user-1"

        val participant = mockk<Participant> {
            every { session_id } returns sessionId
            every { user_id } returns userId
        }

        val event = mockk<ParticipantJoinedEvent> {
            every { isPinned } returns true
            every { this@mockk.participant } returns participant
        }
        participants = mutableMapOf(
            sessionId to fakeParticipant(
                sessionId,
                userId,
            ),
        )
        manager.onParticipantJoined(event)
        assertTrue(manager.serverPins.value.containsKey(sessionId))
    }

    @Test
    fun `onParticipantJoined should ignore unpinned participant`() {
        val sessionId = "session-1"
        val userId = "user-1"

        val participant = mockk<Participant> {
            every { session_id } returns sessionId
            every { user_id } returns userId
        }

        val event = mockk<ParticipantJoinedEvent> {
            every { isPinned } returns false
            every { this@mockk.participant } returns participant
        }
        participants = mutableMapOf(
            "session-1" to fakeParticipant(
                "session-1",
                "user-1",
            ),
        )
        manager.onParticipantJoined(event = event)

        assertTrue(manager.serverPins.value.isEmpty())
    }

    @Test
    fun `onParticipantsJoined should add server pins for pinned participants`() {
        participants["session-1"] = fakeParticipant(
            sessionId = "session-1",
            userId = "user-1",
        )

        participants["session-2"] = fakeParticipant(
            sessionId = "session-2",
            userId = "user-2",
        )

        val participant1 = mockk<Participant> {
            every { session_id } returns "session-1"
            every { user_id } returns "user-1"
        }

        val participant2 = mockk<Participant> {
            every { session_id } returns "session-2"
            every { user_id } returns "user-2"
        }

        manager.onParticipantsJoined(
            listOf(
                participant1 to true,
                participant2 to true,
            ),
        )

        assertEquals(2, manager.serverPins.value.size)

        assertTrue(manager.serverPins.value.containsKey("session-1"))
        assertTrue(manager.serverPins.value.containsKey("session-2"))
    }

    @Test
    fun `onParticipantsJoined should ignore unpinned participants`() {
        participants["session-1"] = fakeParticipant(
            sessionId = "session-1",
            userId = "user-1",
        )

        val participant = mockk<Participant> {
            every { session_id } returns "session-1"
            every { user_id } returns "user-1"
        }

        manager.onParticipantsJoined(
            listOf(
                participant to false,
            ),
        )

        assertTrue(manager.serverPins.value.isEmpty())
    }

    @Test
    fun `onParticipantsJoined should ignore participants not present in participant map`() {
        val participant = mockk<Participant> {
            every { session_id } returns "missing-session"
            every { user_id } returns "user-1"
        }

        manager.onParticipantsJoined(
            listOf(
                participant to true,
            ),
        )

        assertTrue(manager.serverPins.value.isEmpty())
    }

    @Test
    fun `onParticipantsJoined should only pin eligible participants`() {
        participants["session-1"] = fakeParticipant(
            sessionId = "session-1",
            userId = "user-1",
        )

        participants["session-2"] = fakeParticipant(
            sessionId = "session-2",
            userId = "user-2",
        )

        val pinnedParticipant = mockk<Participant> {
            every { session_id } returns "session-1"
            every { user_id } returns "user-1"
        }

        val unpinnedParticipant = mockk<Participant> {
            every { session_id } returns "session-2"
            every { user_id } returns "user-2"
        }

        val missingParticipant = mockk<Participant> {
            every { session_id } returns "missing-session"
            every { user_id } returns "user-3"
        }

        manager.onParticipantsJoined(
            listOf(
                pinnedParticipant to true,
                unpinnedParticipant to false,
                missingParticipant to true,
            ),
        )

        assertEquals(1, manager.serverPins.value.size)
        assertTrue(manager.serverPins.value.containsKey("session-1"))
    }
}

internal class FakeTimeProvider(
    private var currentTime: OffsetDateTime =
        OffsetDateTime.parse("2026-01-01T00:00:00Z"),
) : TimeProvider {

    override fun now(): OffsetDateTime = currentTime
    override fun currentTimeMillis(): Long = 100L

    override fun fromMillis(millis: Long): OffsetDateTime {
        return OffsetDateTime.ofInstant(
            Instant.ofEpochMilli(millis),
            ZoneOffset.UTC,
        )
    }
}
