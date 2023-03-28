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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.events.CallCreatedEvent
import io.getstream.video.android.core.events.ConnectedEvent
import io.getstream.video.android.core.events.VideoEvent
import io.getstream.video.android.core.model.*
import io.getstream.video.android.core.utils.onError
import io.getstream.video.android.core.utils.onSuccess
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.*

/**
 * TODO: testing work
 *
 * - Livestream
 * - Audio room
 * - Video call
 *
 *
 * - Client initialization
 * - Call CRUD
 *
 */



@RunWith(RobolectricTestRunner::class)
public class CreateCallCrudTest : IntegrationTestBase() {
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
     * * Not having permission to create a call
     *
     * Get Success
     * * Validate basic stats work
     * * Validate pagination works on participants
     *
     */

    @Test
    fun newCallSyntax() = runTest {
        val call = client.call("default", "123", "mytoken")
        // TODO: Get or create call removes some of the fields, this is not ideal
        val result = call.get()

        result.onSuccess {
            println(it)
        }

        assert(result.isSuccess)
    }

    @Test
    fun `create a call and verify the event is fired`() = runTest {
        val result = client.getOrCreateCall("default", UUID.randomUUID().toString())

        // TODO: how to start listening for events using the websocket?

        assert(result.isSuccess)
        val events = mutableListOf<VideoEvent>()
        val eventTypes = mutableListOf<String>()
        client.subscribe {
            println("sub received an event: $it")
            events.add(it)
            eventTypes.add(it::class.java.toString())
        }
        clientImpl.fireEvent(ConnectedEvent("test123"))

        assertThat(events.size).isEqualTo(1)
        assertThat(events).contains(CallCreatedEvent::class.java.toString())


    }

    @Test
    fun createACallWith2Members() = runTest {
        val members = mutableListOf("thierry", "tommaso")
        // TODO: Rename participants to members
        val result = client.getOrCreateCall("default", "tommaso-thierry", members)
        assert(result.isSuccess)
    }

    @Test
    fun failToCreateACall() = runTest {
        val result = client.getOrCreateCall("missing", "123")
        assert(result.isFailure)
        result.onError { println(it) }
    }
}

@RunWith(AndroidJUnit4::class)
public class UpdateOrDeleteCallCrudTest : IntegrationTestBase() {
    /**
     * Alright so what do we need to test here:
     *
     * Success scenarios
     * * Call was successfully updated
     * * Call was successfully deleted
     *
     * Errors
     * * Call doesn't exist
     * * Not having permission to update/delete a call
     */

    @Test
    fun createACall() = runTest {
        val client = testData.client
        // TODO: update call needs to expose more..
        val result3 = client.updateCall("default", "abc", mutableMapOf("color" to "green"))

        val result2 = testData.client.getOrCreateCall("default", "123")

        System.out.println(
            result2.onError {
                System.out.println("abc")
                System.out.println(it.toString())
            }
        )
        System.out.println(
            result2.onSuccess {
                System.out.println("123")
                System.out.println(it)
            }
        )
    }
}
