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
import io.getstream.video.android.core.toResponse
import io.getstream.video.android.core.utils.toResponse
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.openapitools.client.models.CallAcceptedEvent
import org.openapitools.client.models.CallRejectedEvent
import org.openapitools.client.models.CallRingEvent
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
    fun `Ringing call event should add a call to the ringing call`() = runTest {
        client.state._ringingCall.value = null
        val userResponse = testData.users["tommaso"]!!.toResponse()

        val ringEvent = CallRingEvent(
            call = call.toResponse(userResponse),
            callCid = call.cid,
            createdAt = nowUtc,
            sessionId = "",
            user = userResponse,
            members = emptyList()
        )

        clientImpl.fireEvent(ringEvent)

        assertThat(client.state.ringingCall.value).isEqualTo(call)
    }

    @Test
    fun `Accept a call`() = runTest {
        val call = client.call("default")
        val createResponse = call.create(memberIds = listOf("tommaso", "thierry"), ring = true)
        assertSuccess(createResponse)
        val userResponse = testData.users["tommaso"]!!.toResponse()

        val callAcceptedEvent = CallAcceptedEvent(
            call = call.toResponse(userResponse),
            callCid = call.cid,
            createdAt = nowUtc,
            user = userResponse
        )
        clientImpl.fireEvent(callAcceptedEvent)

        // verify that call.state.acceptedBy is updated
        assertThat(call.state.acceptedBy.value).contains("tommaso")
    }

    @Test
    fun `Reject a call`() = runTest {
        val call = client.call("default")
        val createResponse = call.create(memberIds = listOf("tommaso", "thierry"), ring = true)
        assertSuccess(createResponse)
        val userResponse = testData.users["tommaso"]!!.toResponse()

        val rejectEvent = CallRejectedEvent(
            call = call.toResponse(userResponse),
            callCid = call.cid,
            createdAt = nowUtc,
            user = userResponse
        )
        clientImpl.fireEvent(rejectEvent)

        // verify that call.state.rejectedBy is updated
        assertThat(call.state.rejectedBy.value).contains("tommaso")
    }
}
