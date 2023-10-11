/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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
import io.getstream.result.Result
import io.getstream.video.android.core.base.IntegrationTestBase
import io.getstream.video.android.core.model.SortField
import io.getstream.video.android.core.sorting.SortedParticipantsState
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.createTestCoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.openapitools.client.models.CallSettingsRequest
import org.openapitools.client.models.MemberRequest
import org.openapitools.client.models.ScreensharingSettingsRequest
import org.robolectric.RobolectricTestRunner
import org.threeten.bp.OffsetDateTime
import stream.video.sfu.models.Participant
import java.util.SortedMap
import java.util.UUID
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
            members = listOf(MemberRequest("tommaso", mutableMapOf("color" to "green"))),
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

    @Test
    fun `Participants should be sorted`() = runTest {
        // Given
        val mockCall = mockk<Call>(relaxed = true)
        val mockParticipantsFlow =
            MutableStateFlow<SortedMap<String, ParticipantState>>(sortedMapOf())
        val mockPinnedParticipantsFlow = MutableStateFlow<Map<String, OffsetDateTime>>(mapOf())
        val sortedParticipantsState = SortedParticipantsState(
            scope = this,
            call = mockCall,
            participants = mockParticipantsFlow,
            pinnedParticipants = mockPinnedParticipantsFlow
        )
        val participants: MutableMap<String, ParticipantState> = mutableMapOf()
        val size = 10
        for (i in 1..size) {
            val id = UUID.randomUUID().toString()
            participants[id] = ParticipantState(id, mockCall, id, id)
        }

        // When
        mockParticipantsFlow.value = participants.toSortedMap()
        mockPinnedParticipantsFlow.value = mapOf()
        val sortedParticipants = sortedParticipantsState.lastSortOrder()

        // Then
        assertEquals(size, sortedParticipants.size)
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
        assertEquals(queryResult.value.next, successResultPage2.value.prev)
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
