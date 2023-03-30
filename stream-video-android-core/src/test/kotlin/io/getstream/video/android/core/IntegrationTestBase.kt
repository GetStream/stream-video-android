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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import io.getstream.video.android.BuildConfig
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.events.VideoEvent
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.utils.Result
import io.getstream.video.android.core.utils.onError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.UUID

class DispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(TestCoroutineScheduler()),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
        DispatcherProvider.set(testDispatcher, testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
        DispatcherProvider.reset()
    }
}

public class IntegrationTestHelper {

    val users = mutableMapOf<String, User>()
    val tokens = mutableMapOf<String, String>()
    val context: Context

    init {
        // TODO: generate token from build vars
        val token =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoidGhpZXJyeSJ9._4aZL6BR0VGKfZsKYdscsBm8yKVgG-2LatYeHRJUq0g"

        val thierry = User(
            id = "thierry", role = "admin", name = "Thierry", imageUrl = "hello",
            teams = emptyList(), extraData = mapOf()
        )
        users["thierry"] = thierry
        tokens["thierry"] = token
        context = ApplicationProvider.getApplicationContext()
    }
}

open class IntegrationTestBase {
    @get:Rule
    val dispatcherRule = DispatcherRule()

    /** Convenient helper with test data */
    val testData = IntegrationTestHelper()
    /** Android context */
    val context = testData.context
    /** API Key */
    val apiKey = "hd8szvscpxvd"
    /** Client */
    val client: StreamVideo
    /** Implementation of the client for more access to interals */
    internal val clientImpl: StreamVideoImpl
    /** Tracks all events received by the client during a test */
    var events: MutableList<VideoEvent>
    /** The builder used for creating the client */
    val builder: StreamVideoBuilder

    init {
        builder = StreamVideoBuilder(
            context = ApplicationProvider.getApplicationContext(),
            apiKey = "hd8szvscpxvd",
            geo = GEO.GlobalEdgeNetwork,
            testData.users["thierry"]!!,
            testData.tokens["thierry"]!!,
            loggingLevel = LoggingLevel.BODY
        )
        if (BuildConfig.CORE_TEST_LOCAL == "1") {
            builder.videoDomain = "localhost"
        }
        client = builder.build()
        clientImpl = client as StreamVideoImpl

        // monitor for events
        events = mutableListOf()
        client.subscribe {
            println("sub received an event: $it")
            events.add(it)
            println("events in loop $events")
        }
    }

    /**
     * Assert that we received the event of this type, get the event as well for
     * further checks
     */
    fun assertEventReceived(eventClass: Class<*>?): VideoEvent {
        val eventTypes = events.map { it::class.java }
        Truth.assertThat(eventTypes).contains(eventClass)
        return events.filter { it::class.java == eventClass }[0]
    }

    /**
     * Verify the Result is a success and raise a nice error if it isn't
     */
    fun assertSuccess(result: Result<Any>) {
        assert(result.isSuccess) {
            result.onError {
                "result wasn't a success, got an error $it."
            }
        }
    }

    fun randomUUID(): String {
        return UUID.randomUUID().toString()
    }

    @Before
    fun resetTestVars() {
        events = mutableListOf()
    }
}
