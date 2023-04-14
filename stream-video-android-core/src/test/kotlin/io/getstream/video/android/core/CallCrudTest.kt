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
import io.getstream.log.taggedLogger
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.openapitools.client.models.CallCreatedEvent
import org.robolectric.RobolectricTestRunner

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
        // Wait to receive the next event
        waitForNextEvent<CallCreatedEvent>()

        // verify that we received the create call event
        assertThat(events.size).isEqualTo(1)
        val event = assertEventReceived(CallCreatedEvent::class.java)

        // TODO: The structure of the event we receive is weird,
        //  seems out of sync with server, to investigate
    }

    @Test
    fun `update a call, verify it works and the event is fired`() = runTest {
        // create the call
        val callId = randomUUID()
        val call = client.call("default", callId)
        val createResult = call.create()
        assertSuccess(createResult)

        // set a custom field and update
        val secret = randomUUID()
        call.custom = mutableMapOf("secret" to secret)
        val updateResult = call.update()
        assertSuccess(updateResult)

        // get the call object
        val getResult = call.get()
        assertSuccess(getResult)

        // verify that we received the create call event
        assertThat(events.size).isEqualTo(1)
        val createEvent = assertEventReceived(CallCreatedEvent::class.java)
        val updateEvent = assertEventReceived(CallCreatedEvent::class.java)

        getResult.onSuccess {
            assertThat(it.call.custom["color"]).isEqualTo(secret)
        }
    }

    @Test
    fun `Create a call with members and custom data`() = runTest {
        val members = mutableListOf("thierry", "tommaso")
        // TODO: Rename participants to members
        val result = client.getOrCreateCall("default", "tommaso-thierry", members)
        assert(result.isSuccess)
    }

    @Test
    fun `Fail to update a call that doesn't exist`() = runTest {
        val result = client.call("default", randomUUID()).update()
        assert(result.isFailure)
        result.onError { logger.e { it.toString() } }
        // TODO: error is hidden, we need to fix that
    }

    @Test
    fun `Fail to create a call with a missing call type`() = runTest {
        val result = client.call("missing", "123").create()
        assert(result.isFailure)
        result.onError { logger.e { it.toString() } }
        // TODO: error is hidden, we need to fix that
    }

    @Test
    fun `Read call stats`() = runTest {
        // TODO
    }

    @Test
    fun `Paginate members`() = runTest {
        // TODO
    }
}
