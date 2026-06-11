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

import com.google.common.truth.Truth.assertThat
import io.getstream.android.video.generated.models.CallParticipantResponse
import io.getstream.android.video.generated.models.CallSessionParticipantCountsUpdatedEvent
import io.getstream.android.video.generated.models.CallSessionParticipantJoinedEvent
import io.getstream.android.video.generated.models.CallSessionParticipantLeftEvent
import io.getstream.android.video.generated.models.CallSessionResponse
import io.getstream.android.video.generated.models.CallSettingsRequest
import io.getstream.android.video.generated.models.MemberRequest
import io.getstream.android.video.generated.models.ScreensharingSettingsRequest
import io.getstream.android.video.generated.models.UserResponse
import io.getstream.result.Result
import io.getstream.video.android.core.base.IntegrationTestBase
import io.getstream.video.android.core.events.DominantSpeakerChangedEvent
import io.getstream.video.android.core.events.ParticipantCount
import io.getstream.video.android.core.events.PinUpdate
import io.getstream.video.android.core.events.SFUHealthCheckEvent
import io.getstream.video.android.core.model.SortField
import io.getstream.video.android.core.pinning.PinEntry
import io.getstream.video.android.core.pinning.PinType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.threeten.bp.Clock
import org.threeten.bp.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class CallStateTest : IntegrationTestBase() {

    /**
     * State should be populated if you
     * - Create a call
     * - Get a call
     * - Join a call
     * - Query a call
     */

    @Test
    fun `Creating a call should populate the state`() = runTest {
        val call = client.call("default", randomUUID())
        // test with custom field, members and a settings overwrite
        val custom = mapOf("foo" to "bar")
        val response = call.create(
            custom = custom,
            members = listOf(MemberRequest("tommaso", custom = mutableMapOf("color" to "green"))),
            // block screensharing completely for this call
            settings = CallSettingsRequest(
                screensharing = ScreensharingSettingsRequest(
                    accessRequestEnabled = false,
                    enabled = false,
                ),
            ),
        )
        assertSuccess(response)

        // verify we can't screenshare
        call.state.settings.value?.apply {
            assertThat(this).isNotNull()
            assertThat(screensharing.enabled).isFalse()
            assertThat(screensharing.accessRequestEnabled).isFalse()
        }
        assertThat(call.state.members.value.size).isEqualTo(1)
        val memberNames = call.state.members.value.map { it.user.id }
        assertThat(memberNames).containsExactly("tommaso")
        val tommasoMember = call.state.members.value.first { it.user.id == "tommaso" }
        assertThat(tommasoMember.custom["color"]).isEqualTo("green")

        assertThat(call.state.custom.value["foo"]).isEqualTo("bar")
    }

    @Test
    fun `Getting a call should populate the state`() = runTest {
        val response = call.get()
        assertSuccess(response)
        assertThat(call.state.settings.value).isNotNull()
    }

    @Test
    fun `Joining a call should populate the state`() = runTest {
        val call = client.call("default", randomUUID())
        val response = call.joinRequest(
            create = CreateCallOptions(custom = mapOf("color" to "green")),
            location = "AMS",
        )
        assertSuccess(response)
        assertThat(call.state.settings.value).isNotNull()
        assertThat(call.state.custom.value["color"]).isEqualTo("green")
    }

    /**
     * * anyone who is pinned
     * * dominant speaker
     * * if you are screensharing
     * * last speaking at
     * * all other video participants by when they joined
     * * audio only participants by when they joined
     */
    @Test
    fun `Participants should be sorted`() = runTest {
        val call = client.call("default", randomUUID())

        val sortedParticipants = call.state.participants.stateIn(
            backgroundScope,
            SharingStarted.Eagerly,
            emptyList(),
        )

        val sorted1 = sortedParticipants.value
        assertThat(sorted1).isEmpty()

        call.state.pinManager.updateLocalPins(
            mutableMapOf(
                "1" to PinEntry(
                    PinUpdate("1", "userId"), OffsetDateTime.now(Clock.systemUTC()), PinType.Local,
                ),
            ),
        )

        call.state.updateParticipant(
            ParticipantState("4", call.state.scope, call.state.callActions, "4").apply {
                _videoEnabled.value = true
            },
        )
        call.state.updateParticipant(
            ParticipantState("5", call.state.scope, call.state.callActions, "5").apply {
                _lastSpeakingAt.value = nowUtc
            },
        )
        call.state.updateParticipant(
            ParticipantState("6", call.state.scope, call.state.callActions, "6").apply {
                _joinedAt.value = nowUtc
            },
        )

        call.state.updateParticipant(
            ParticipantState("1", call.state.scope, call.state.callActions, "1"),
        )
        call.state.updateParticipant(
            ParticipantState("2", call.state.scope, call.state.callActions, "2").apply {
                _screenSharingEnabled.value = true
            },
        )
        call.state.updateParticipant(
            ParticipantState("3", call.state.scope, call.state.callActions, "3").apply {
                _dominantSpeaker.value = true
            },
        )

        val participants = call.state.participants.value
        println("emitSorted participants size is ${participants.size}")
        assertThat(participants.size).isEqualTo(6)
        delay(60)

        val sorted2 = sortedParticipants.value.map { it.sessionId }
        // Default preset ordering with this setup:
        //   tier 1 (screenSharing): "2"
        //   tier 2 (pinned): "1"
        //   tier 3 (ifInvisibleOrUnknown chain): "3" (dominant) → "4" (publishing video)
        //     → "5", "6" (no signals; insertion order preserved)
        assertThat(sorted2).isEqualTo(listOf("2", "1", "3", "4", "5", "6"))

        clientImpl.fireEvent(DominantSpeakerChangedEvent("3", "3"), call.cid)
        assertThat(call.state.getParticipantBySessionId("3")?.dominantSpeaker?.value).isTrue()
        delay(60)

        val sorted3 = sortedParticipants.value.map { it.sessionId }
        assertThat(sorted3).isEqualTo(listOf("2", "1", "3", "4", "5", "6"))
    }

    @Test
    fun `Update sorting order`() = runTest {
        val call = client.call("default", randomUUID())
        val sortedParticipants = call.state.participants.stateIn(
            backgroundScope,
            SharingStarted.Eagerly,
            emptyList(),
        )
        call.state.updateParticipantSortingOrder(
            compareByDescending {
                it.sessionId
            },
        )

        call.state.updateParticipant(
            ParticipantState("1", call.state.scope, call.state.callActions, "1"),
        )
        call.state.updateParticipant(
            ParticipantState("2", call.state.scope, call.state.callActions, "2").apply {
                _screenSharingEnabled.value = true
            },
        )
        call.state.updateParticipant(
            ParticipantState("3", call.state.scope, call.state.callActions, "3").apply {
                _dominantSpeaker.value = true
            },
        )

        delay(1000)
        val sorted = sortedParticipants.value.map { it.sessionId }
        assertThat(sorted).isEqualTo(listOf("3", "2", "1"))
    }

    @Test
    fun `Querying calls should populate the state`() = runTest {
//        val createResult = client.call("default", randomUUID()).create(custom=mapOf("color" to "green"))
//        assertSuccess(createResult)
        val filters = mutableMapOf("color" to "green")
        val queryResult = client.queryCalls(filters, limit = 1)
        assertSuccess(queryResult)
        // verify the call has settings setup correctly
        queryResult.onSuccess {
            assertThat(it.calls.size).isGreaterThan(0)
            it.calls.forEach {
                val call = clientImpl.call(it.call.type, it.call.id)
                assertThat(call.state.settings.value).isNotNull()
            }
        }
    }

    @Test
    fun `Query calls pagination works`() = runTest {
        // get first page with one result
        val queryResult = client.queryCalls(emptyMap(), limit = 1)
        assertSuccess(queryResult)

        val successResponsePage1 = queryResult as Result.Success
        // verify the response has no previous page and a next page
        assertNotNull(successResponsePage1.value.next)
        assertNull(successResponsePage1.value.prev)

        // request next page
        val queryResultPage2 = client.queryCalls(
            emptyMap(),
            prev = successResponsePage1.value.prev,
            next = successResponsePage1.value.next,
            limit = 1,
        )
        assertSuccess(queryResultPage2)

        val successResultPage2 = queryResultPage2 as Result.Success
        // verify the response points to previous page and has a next page
//        assertEquals(queryResult.value.next, successResultPage2.value.prev)
        assertNotNull(successResultPage2.value.next)
    }

    @Test
    fun `Query members pagination works`() = runTest {
        val call = client.call("default", randomUUID())
        // create call
        val createResponse = call.create(memberIds = listOf("thierry", "tommaso"))
        assertSuccess(createResponse)

        // get first page with one result
        val queryResult1 = client.queryMembers(
            type = call.type,
            id = call.id,
            limit = 1,
            sort = mutableListOf(SortField.Desc("user_id")),
        )
        assertSuccess(queryResult1)
        assertEquals(queryResult1.getOrThrow().members.size, 1)
        assertEquals(queryResult1.getOrThrow().members[0].userId, "tommaso")

        // get second page with one result
        val queryResult2 = client.queryMembers(
            type = call.type,
            id = call.id,
            next = queryResult1.getOrThrow().next,
            limit = 1,
            sort = mutableListOf(SortField.Desc("user_id")),
        )

        assertSuccess(queryResult2)
        assertEquals(queryResult2.getOrThrow().members.size, 1)
        assertEquals(queryResult2.getOrThrow().members[0].userId, "thierry")
    }

    @Test
    fun `participantCount stays at SFU healthcheck value when coordinator participant_joined arrives post-join`() = runTest {
        // AND-926: post-join, only SFU healthcheck should drive participantCount.
        // Coordinator session events (counts_updated / participant_joined / participant_left)
        // must NOT clobber it — they carry a smaller, stale snapshot at scale.
        val call = client.call("default", randomUUID())

        // Steady post-join state: RTC is up. This is the state for ~all of an active call.
        call.state._connection.value = RealtimeConnection.Connected

        // Server gives us a tiny session snapshot (participants list is capped ~250)
        call.state._session.value = CallSessionResponse(
            anonymousParticipantCount = 0,
            id = "session-1",
            participants = emptyList(),
            participantsCountByRole = mapOf("user" to 1),
        )

        // SFU healthcheck delivers the authoritative live count (e.g. a livestream with 25k viewers)
        call.state.handleEvent(SFUHealthCheckEvent(ParticipantCount(total = 25_000, anonymous = 0)))
        assertThat(call.state.totalParticipants.value).isEqualTo(25_000)

        // A coordinator participant_joined event arrives. Today this recomputes the count
        // from the session snapshot and the display drops from 25k to ~2. That's the bug.
        val joinedEvent = CallSessionParticipantJoinedEvent(
            callCid = call.cid,
            createdAt = nowUtc,
            sessionId = "session-1",
            participant = CallParticipantResponse(
                joinedAt = nowUtc,
                role = "user",
                userSessionId = "u2-session",
                user = UserResponse(
                    createdAt = nowUtc,
                    id = "u2",
                    language = "en",
                    role = "user",
                    updatedAt = nowUtc,
                ),
            ),
            type = "call.session_participant_joined",
        )
        call.state.handleEvent(joinedEvent)

        assertThat(call.state.totalParticipants.value).isEqualTo(25_000)
    }

    @Test
    fun `participantCount stays at SFU healthcheck value when coordinator counts_updated event arrives post-join`() = runTest {
        // AND-926: same invariant, different entry point. CallSessionParticipantCountsUpdatedEvent
        // re-writes the role map on the session — it must not propagate to participantCount once joined.
        val call = client.call("default", randomUUID())
        call.state._connection.value = RealtimeConnection.Connected
        call.state._session.value = CallSessionResponse(
            anonymousParticipantCount = 0,
            id = "session-1",
            participants = emptyList(),
            participantsCountByRole = mapOf("user" to 1),
        )

        call.state.handleEvent(SFUHealthCheckEvent(ParticipantCount(total = 25_000, anonymous = 0)))
        assertThat(call.state.totalParticipants.value).isEqualTo(25_000)

        // Coordinator pushes a stale count snapshot
        call.state.handleEvent(
            CallSessionParticipantCountsUpdatedEvent(
                anonymousParticipantCount = 0,
                callCid = call.cid,
                createdAt = nowUtc,
                sessionId = "session-1",
                participantsCountByRole = mapOf("user" to 17),
                type = "call.session_participant_count_updated",
            ),
        )

        assertThat(call.state.totalParticipants.value).isEqualTo(25_000)
    }

    @Test
    fun `participantCount stays at SFU healthcheck value when coordinator participant_left arrives post-join`() = runTest {
        // AND-926: participant_left mutates the local session map too. Same guard must hold.
        val call = client.call("default", randomUUID())
        call.state._connection.value = RealtimeConnection.Connected

        val existingParticipant = CallParticipantResponse(
            joinedAt = nowUtc,
            role = "user",
            userSessionId = "u1-session",
            user = UserResponse(
                createdAt = nowUtc,
                id = "u1",
                language = "en",
                role = "user",
                updatedAt = nowUtc,
            ),
        )
        call.state._session.value = CallSessionResponse(
            anonymousParticipantCount = 0,
            id = "session-1",
            participants = listOf(existingParticipant),
            participantsCountByRole = mapOf("user" to 1),
        )

        call.state.handleEvent(SFUHealthCheckEvent(ParticipantCount(total = 25_000, anonymous = 0)))
        assertThat(call.state.totalParticipants.value).isEqualTo(25_000)

        call.state.handleEvent(
            CallSessionParticipantLeftEvent(
                callCid = call.cid,
                createdAt = nowUtc,
                sessionId = "session-1",
                participant = existingParticipant,
                durationSeconds = 0,
                type = "call.session_participant_left",
            ),
        )

        assertThat(call.state.totalParticipants.value).isEqualTo(25_000)
    }

    @Test
    fun `participantCount uses session snapshot with max(byRole, participants size) before joining the call`() = runTest {
        // Pre-join (lobby / ringing / inProgress join), SFU healthcheck hasn't arrived yet —
        // session-derived count IS the source of truth. Parity with React's
        // updateParticipantCountFromSession when callingState !== JOINED, including the
        // Math.max(byRoleCount, participants.length) guard for monotonicity during fast joins.
        val call = client.call("default", randomUUID())
        // Connection stays at the default PreJoin — we are NOT in the call yet.

        // byRoleSum=1 but the participants list already has 3 entries — the local list got
        // ahead of the role map (the case React's Math.max protects against). The displayed
        // count must follow the larger value, not the stale byRoleSum.
        val now = nowUtc
        val participantsList = listOf("a", "b", "c").map { id ->
            CallParticipantResponse(
                joinedAt = now,
                role = "user",
                userSessionId = "$id-session",
                user = UserResponse(
                    createdAt = now,
                    id = id,
                    language = "en",
                    role = "user",
                    updatedAt = now,
                ),
            )
        }
        call.state._session.value = CallSessionResponse(
            anonymousParticipantCount = 5,
            id = "session-1",
            participants = participantsList,
            participantsCountByRole = mapOf("user" to 1),
        )

        call.state.handleEvent(
            CallSessionParticipantCountsUpdatedEvent(
                anonymousParticipantCount = 5,
                callCid = call.cid,
                createdAt = now,
                sessionId = "session-1",
                participantsCountByRole = mapOf("user" to 1),
                type = "call.session_participant_count_updated",
            ),
        )

        // total = anonymous (5) + max(byRoleSum=1, participants.size=3) = 8
        // Without max(): would be 5 + 1 = 6.
        assertThat(call.state.totalParticipants.value).isEqualTo(8)
    }

    @Test
    fun `Setting the speaking while muted flag will reset itself after delay`() = runTest {
        // we can make multiple calls, this should have no impact on the reset logic or duration
        val speakingWhileMuted = call.state.speakingWhileMuted
        call.state.markSpeakingAsMuted()
        call.state.markSpeakingAsMuted()
        call.state.markSpeakingAsMuted()

        assertTrue(call.state.speakingWhileMuted.first())
        // The flag should automatically reset to false 2 seconds
        advanceTimeBy(3000)

        assertFalse(call.state.speakingWhileMuted.first())
    }
}
