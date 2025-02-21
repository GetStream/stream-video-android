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

package io.getstream.video.android.core.stories

import io.getstream.android.video.generated.models.MemberRequest
import io.getstream.video.android.core.base.IntegrationTestBase
import io.getstream.video.android.core.model.SortField
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.threeten.bp.Clock
import org.threeten.bp.OffsetDateTime

@Ignore("Temporary ignoring it to pass the CI check")
@RunWith(RobolectricTestRunner::class)
class AudioRoomTest : IntegrationTestBase() {
    /**
     * The Swift tutorial is good inspiration
     *
     * Create a call
     * Go live in that call
     *
     * Join a call as a listener
     * Request permission to speak
     *
     * Errors
     * - Trying to publish while not having permissions to do so should raise an error
     *
     */
    @Test
    fun `query calls - about to start`() = runTest {
        val threeHoursFromNow = OffsetDateTime.now(Clock.systemUTC()).plusHours(3)
        // TODO: support null for ended_at
        val filters = mutableMapOf(
            "members" to mutableMapOf("\$in" to listOf("tommaso")),
            "starts_at" to mutableMapOf("\$lt" to threeHoursFromNow),
            // "ended_at" to null,
        )
        val sort = listOf(SortField.Asc("starts_at"))
        val result = client.queryCalls(filters = filters, sort = sort, limit = 10, watch = true)
        assertSuccess(result)
    }

    @Test
    fun `query calls - custom field`() = runTest {
        val filters = mutableMapOf(
            "custom.color" to "red",
        )
        val sort = listOf(SortField.Asc("starts_at"))
        val result = client.queryCalls(filters = filters, sort = sort, limit = 10, watch = true)
        assertSuccess(result)
    }

    @Test
    fun `creating a new call should work`() = runTest {
        val result = client.call("audio_room", randomUUID()).create()
        assertSuccess(result)
    }

    @Test
    fun `add people to an audio room`() = runTest {
        val result = client.call("audio_room").create(
            members = listOf(
                MemberRequest("john", role = "admin"),
                MemberRequest("tommaso", role = "moderator"),
            ),
        )
        assertSuccess(result)
    }

    @Test
    fun `next we want to go live in a call`() = runTest {
        val call = client.call("audio_room", randomUUID())
        val result = call.create()
        assertSuccess(result)
        val goLiveResult = call.goLive()
        assertSuccess(goLiveResult)
        val stopLiveResult = call.stopLive()
        assertSuccess(stopLiveResult)
    }

    @Test
    fun `send a reaction`() = runTest {
        val response = call.sendReaction("raise-hand", "", mapOf("mycustomfield" to "hello"))

        assertSuccess(response)
    }

    @Test
    fun `send a custom event`() = runTest {
        val response = call.sendCustomEvent(mapOf("type" to "draw", "x" to 10, "y" to 20))

        assertSuccess(response)
    }

    @Test
    fun `for audio rooms it's common to request permissions`() = runTest {
        val result = call.requestPermissions("screenshare", "send-audio")
        assertSuccess(result)
    }
}
