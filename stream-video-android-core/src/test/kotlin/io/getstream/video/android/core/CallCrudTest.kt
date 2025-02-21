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

import app.cash.turbine.testIn
import com.google.common.truth.Truth.assertThat
import io.getstream.android.video.generated.models.MemberRequest
import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.video.android.core.base.IntegrationTestBase
import io.getstream.video.android.core.model.SortField
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
public class CallCrudTest : IntegrationTestBase() {

    private val logger by taggedLogger("Test:CreateCallCrudTest")

    /**
     * Alright so what do we need to test here:
     *
     * Success scenarios
     * * Create a call with a specific ID
     * * Create a call between 2 participants (no id)
     * * Setting custom fields on a call
     * * Create a ringing call
     *
     * Errors
     * * Try to create a call of a type that doesn't exist
     *
     * Get Success
     * * Validate basic stats work
     * * Validate pagination works on participants
     *
     */
    @Test
    fun `create a call and verify the event is fired`() = runTest {
        // create the call
        val call = client.call("default", randomUUID())
        val result = call.create()
        assertSuccess(result)
    }

    @Test
    fun `update a call, verify it works`() = runTest {
        // create the call
        val call = client.call("default", randomUUID())
        val createResult = call.create()
        assertSuccess(createResult)
        createResult.onSuccess {
            assertThat(it.call.cid).isEqualTo(call.cid)
        }

        // set a custom field and update
        val secret = randomUUID()
        val custom = mutableMapOf("secret" to secret)
        val updateResult = call.update(custom = custom)
        assertSuccess(updateResult)

        // get the call object
        val getResult = call.get()
        assertSuccess(getResult)

        getResult.onSuccess {
            assertThat(it.call.custom["secret"]).isEqualTo(secret)
        }
    }

    @Test
    fun `Create a call with members and custom data`() = runTest {
        val members = mutableListOf("thierry", "tommaso")
        val call = client.call("default", randomUUID())

        val result = call.create(memberIds = members, custom = mapOf("color" to "red"))
        assert(result.isSuccess)

        val customState = call.state.custom.testIn(backgroundScope)
        val customStateItem = customState.awaitItem()

        val memberState = call.state.members.testIn(backgroundScope)
        val memberStateItem = memberState.awaitItem()

        assertThat(customStateItem["color"]).isEqualTo("red")
        assertThat(memberStateItem).hasSize(2)
    }

    @Test
    fun `Create a call with different member roles`() = runTest {
        val call = client.call("default", randomUUID())

        val result = call.create(
            members = listOf(
                MemberRequest(userId = "thierry", role = "host"),
            ),
            custom = mapOf("color" to "red"),
        )
        assert(result.isSuccess)

        val customState = call.state.custom.testIn(backgroundScope)
        val customStateItem = customState.awaitItem()

        val memberState = call.state.members.testIn(backgroundScope)
        val memberStateItem = memberState.awaitItem()

        assertThat(customStateItem["color"]).isEqualTo("red")
        assertThat(memberStateItem.first().role).isEqualTo("host")
        assertThat(memberStateItem).hasSize(1)
    }

    @Test
    fun `Fail to update a call that doesn't exist`() = runTest {
        val result = client.call("default", randomUUID()).update()
        assert(result.isFailure)
        result.onError {
            assertTrue { it is Error.ThrowableError }
        }
    }

    @Test
    fun `Fail to create a call with a missing call type`() = runTest {
        val result = client.call("missing", "123").create()
        assert(result.isFailure)
        result.onError {
            assertTrue { it is Error.ThrowableError }
        }
    }

    @Test
    fun `Paginate members`() = runTest {
        val members = mutableListOf("thierry", "tommaso")
        val call = client.call("default", randomUUID())
        val result = call.create(memberIds = members, custom = mapOf("color" to "red"))
        assert(result.isSuccess)

        val filters = mapOf("user_id" to "thierry")
        val response = call.queryMembers(filters, listOf(SortField.Desc("created_at")), 5)
        assertSuccess(response)
        response.onSuccess {
            assertThat(it.members).hasSize(1)
        }
    }
}
