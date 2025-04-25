/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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
import io.getstream.android.video.generated.models.CallSettingsRequest
import io.getstream.android.video.generated.models.CustomVideoEvent
import io.getstream.android.video.generated.models.MemberRequest
import io.getstream.android.video.generated.models.ScreensharingSettingsRequest
import io.getstream.result.Result
import io.getstream.video.android.core.base.IntegrationTestBase
import io.getstream.video.android.core.events.DominantSpeakerChangedEvent
import io.getstream.video.android.core.events.PinUpdate
import io.getstream.video.android.core.model.SortField
import io.getstream.video.android.core.pinning.PinType
import io.getstream.video.android.core.pinning.PinUpdateAtTime
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

        val sortedParticipants = call.state.sortedParticipants.stateIn(
            backgroundScope,
            SharingStarted.Eagerly,
            emptyList(),
        )

        val sorted1 = sortedParticipants.value
        assertThat(sorted1).isEmpty()

        call.state._localPins.value = mutableMapOf(
            "1" to PinUpdateAtTime(
                PinUpdate("1", "userId"), OffsetDateTime.now(Clock.systemUTC()), PinType.Local,
            ),
        )

        call.state.updateParticipant(
            ParticipantState("4", call, "4").apply { _videoEnabled.value = true },
        )
        call.state.updateParticipant(
            ParticipantState("5", call, "5").apply { _lastSpeakingAt.value = nowUtc },
        )
        call.state.updateParticipant(
            ParticipantState("6", call, "6").apply { _joinedAt.value = nowUtc },
        )

        call.state.updateParticipant(
            ParticipantState("1", call, "1"),
        )
        call.state.updateParticipant(
            ParticipantState("2", call, "2").apply { _screenSharingEnabled.value = true },
        )
        call.state.updateParticipant(
            ParticipantState("3", call, "3").apply { _dominantSpeaker.value = true },
        )

        val participants = call.state.participants.value
        println("emitSorted participants size is ${participants.size}")
        assertThat(participants.size).isEqualTo(6)
        delay(60)

        val sorted2 = sortedParticipants.value.map { it.sessionId }
        assertThat(sorted2).isEqualTo(listOf("1", "2", "3", "4", "5", "6"))

        clientImpl.fireEvent(DominantSpeakerChangedEvent("3", "3"), call.cid)
        assertThat(call.state.getParticipantBySessionId("3")?.dominantSpeaker?.value).isTrue()
        delay(60)

        val sorted3 = sortedParticipants.value.map { it.sessionId }
        assertThat(sorted3).isEqualTo(listOf("1", "2", "3", "4", "5", "6"))
    }

    @Test
    fun `Update sorting order`() = runTest {
        val call = client.call("default", randomUUID())
        val sortedParticipants = call.state.sortedParticipants.stateIn(
            this,
            SharingStarted.Eagerly,
            emptyList(),
        )
        call.state.updateParticipantSortingOrder(
            compareByDescending {
                it.sessionId
            },
        )

        call.state.updateParticipant(
            ParticipantState("1", call, "1"),
        )
        call.state.updateParticipant(
            ParticipantState("2", call, "2").apply { _screenSharingEnabled.value = true },
        )
        call.state.updateParticipant(
            ParticipantState("3", call, "3").apply { _dominantSpeaker.value = true },
        )

        delay(1000)
        val sorted = sortedParticipants.value.map { it.sessionId }
        assertThat(sorted).isEqualTo(listOf("3", "2", "1"))
    }

    @Test
    fun `Querying calls without watch should NOT populate the state`() = runTest {
        val filters = mutableMapOf("color" to "green")
        client.cleanup()
        val queryResult = client.queryCalls(filters, limit = 1)
        assertSuccess(queryResult)
        // verify the call has settings setup correctly
        queryResult.onSuccess {
            assertThat(it.calls.size).isGreaterThan(0)
            it.calls.forEach {
                val call = clientImpl.queriedCall(it.call.type, it.call.id)
                assertThat(call.state.settings.value).isNull()
            }
        }
    }

    @Test
    fun `Querying calls with watch true should populate the state`() = runTest {
        val filters = mutableMapOf("color" to "green")
        val queryResult = client.queryCalls(filters, limit = 1, watch = true)
        assertSuccess(queryResult)
        // verify the call has settings setup correctly
        queryResult.onSuccess {
            assertThat(it.calls.size).isGreaterThan(0)
            it.calls.forEach {
                val call = clientImpl.queriedCall(it.call.type, it.call.id)
                assertThat(call.state.settings.value).isNotNull()
            }
        }
    }

    @Test
    fun `Querying calls with watch true should return watched calls list in the result`() =
        runTest {
            val filters = mutableMapOf("color" to "green")
            val queryResult = client.queryCalls(filters, limit = 1, watch = true)
            assertSuccess(queryResult)
            // verify the call has settings setup correctly
            queryResult.onSuccess {
                assertThat(it.calls.size).isGreaterThan(0)
                assertEquals(1, it.watchedCalls.size)
                assertThat(it.watchedCalls.first().state.settings.value).isNotNull()
                val watchedCall = it.calls.first()
                val call = clientImpl.queriedCall(watchedCall.call.type, watchedCall.call.id)
                assertEquals(call.cid, watchedCall.call.cid)
                assertEquals(it.watchedCalls.first().cid, call.cid)
                assertEquals(it.watchedCalls.first().cid, watchedCall.call.cid)
            }
        }

    @Test
    fun `Querying calls with watch false should NOT return watched calls list in the result`() =
        runTest {
            val filters = mutableMapOf("color" to "green")
            client.cleanup()
            val queryResult = client.queryCalls(filters, limit = 1, watch = false)
            assertSuccess(queryResult)
            // verify the call has settings setup correctly
            queryResult.onSuccess {
                assertThat(it.calls.size).isGreaterThan(0)
                assertEquals(0, it.watchedCalls.size)
            }
        }

    @Test
    fun `Querying calls with watch true gets event updates`() =
        runTest {
            val filters = mutableMapOf("color" to "green")
            client.cleanup()
            val queryResult = client.queryCalls(filters, limit = 1, watch = true)
            assertSuccess(queryResult)
            queryResult.onSuccess {
                assertThat(it.calls.size).isGreaterThan(0)
                assertEquals(1, it.watchedCalls.size)
                val first = it.watchedCalls.first()
                val cid = first.cid
                first.subscribe { event ->
                    assertEquals("custom", event.getEventType())
                }
                val event = CustomVideoEvent(
                    callCid = cid,
                    createdAt = nowUtc,
                    custom = emptyMap(),
                    user = testData.users.values.first().toUserResponse(),
                    type = "custom",
                )
                clientImpl.fireEvent(event, cid)
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
