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

import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.base.IntegrationTestBase
import io.getstream.video.android.core.base.toResponse
import io.getstream.video.android.core.utils.toResponse
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import io.getstream.android.video.generated.models.CallAcceptedEvent
import io.getstream.android.video.generated.models.CallRejectedEvent
import io.getstream.android.video.generated.models.CallRingEvent
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
            members = emptyList(),
            video = true,
            type = "",
        )

        clientImpl.fireEvent(ringEvent)

        assertThat(client.state.ringingCall.value).isEqualTo(call)
    }

    @Test
    @Ignore
    fun `Outgoing call is automatically cancelled`() = runTest {
        client.state._ringingCall.value = null

        val call = client.call("default", randomUUID())
        call.create(memberIds = listOf("tommaso", "thierry"), ring = true)

        // We are in idle state
        assertThat(call.state.ringingState.value).isInstanceOf(RingingState.Idle::class.java)

        // We wait until cancel time passes
        val cancelTime = call.state.settings.value?.ring?.autoCancelTimeoutMs?.toLong() ?: 0
        advanceTimeBy(cancelTime + 500)

        // This is needed because we need to wait for call.reject() internally to finish
        // Is there a way we could wait for the suspend function?
        waitForNextEvent<CallRejectedEvent>()

        // We verify that the call doesn't ring anymore and the state is back to Idle
        assertThat(client.state.ringingCall.value).isNull()
        assertThat(call.state.ringingState.value is RingingState.Idle)
    }

    @Test
    @Ignore(
        "This doesn't work anymore because sending a fake CallAcceptedEvent triggers " +
            "an auto-join request in CallState and this results in CallRejected event",
    )
    fun `Accept a call`() = runTest {
        val call = client.call("default", randomUUID())
        val createResponse = call.create(memberIds = listOf("tommaso", "thierry"), ring = true)
        assertSuccess(createResponse)
        val userResponse = testData.users["tommaso"]!!.toResponse()

        val callAcceptedEvent = CallAcceptedEvent(
            call = call.toResponse(userResponse),
            callCid = call.cid,
            createdAt = nowUtc,
            user = userResponse,
            type = "",
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
            user = userResponse,
            type = "",
        )
        clientImpl.fireEvent(rejectEvent)

        // we need to fix this later, but rejecting an incoming call with real API calls is difficult
        advanceTimeBy(3000)

        // verify that call.state.rejectedBy is updated
        assertThat(call.state.rejectedBy.value).contains("tommaso")
    }
}
