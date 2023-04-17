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

package io.getstream.video.android.core.stories

import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.IntegrationTestBase
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.openapitools.client.models.CallAcceptedEvent
import org.openapitools.client.models.CallRejectedEvent
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RingTest : IntegrationTestBase() {

    @Test
    fun `Starting a ringing call, should add it to ringing calls`() = runTest {
        val call = client.call("default", randomUUID())
        val response = call.create(ring = true)
        assertSuccess(response)

        assertThat(client.state.ringingCall.value).isEqualTo(call)
    }

    @Test
    fun `Accepting or rejecting calls should be updated on members`() = runTest {
        val call = client.call("default", randomUUID())
        call.create(memberIds = listOf("tommaso", "thierry", "jaewoong"), ring = true)

        // tommaso accepts
        val userResponse = testData.users["tommaso"]!!.toResponse()
        clientImpl.fireEvent(
            CallAcceptedEvent(
                callCid = call.cid,
                createdAt = nowUtc,
                user = userResponse
            )
        )
        val tommaso = call.state.members.value.first { it.user.id == "tommaso" }
        assertThat(tommaso.acceptedAt).isNotNull()

        // jaewoong rejects
        val userResponse2 = testData.users["jaewoong"]!!.toResponse()
        clientImpl.fireEvent(
            CallRejectedEvent(
                callCid = call.cid,
                createdAt = nowUtc,
                user = userResponse2
            )
        )
        val jaewoong = call.state.members.value.first { it.user.id == "jaewoong" }
        assertThat(tommaso.rejectedAt).isNotNull()
    }

    @Test
    fun `Calls should drop if nobody answers within the timeout`() = runTest {
    }
}
