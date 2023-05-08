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
import io.getstream.video.android.core.model.QueryCallsData
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.openapitools.client.models.CallSettingsRequest
import org.openapitools.client.models.MemberRequest
import org.openapitools.client.models.ScreensharingSettingsRequest
import org.robolectric.RobolectricTestRunner

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
            settings = CallSettingsRequest(screensharing = ScreensharingSettingsRequest(accessRequestEnabled = false, enabled = false))
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
        val response = call.joinRequest(create = CreateCallOptions(custom = mapOf("color" to "green")))
        assertSuccess(response)
        assertThat(call.state.settings.value).isNotNull()
        assertThat(call.state.custom.value["color"]).isEqualTo("green")
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
}
