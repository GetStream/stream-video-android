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
import io.getstream.log.Priority
import io.getstream.log.StreamLog
import io.getstream.log.StreamLogger
import io.getstream.video.android.BuildConfig
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.events.ConnectedEvent
import io.getstream.video.android.core.events.VideoEvent
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.utils.Result
import io.getstream.video.android.core.utils.onError
import io.mockk.internalSubstitute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.UUID
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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

/**
 * A logger that prints to stdout
 */
class StreamTestLogger: StreamLogger {
    var logLevel = Priority.VERBOSE

    override fun log(priority: Priority, tag: String, message: String, throwable: Throwable?) {
        if (priority > logLevel) {
            println("$priority $tag: $message")
            if (throwable != null) {
                println(throwable)
            }
        }
    }

}

open class TestBase {
    @get:Rule
    val dispatcherRule = DispatcherRule()
    /** Convenient helper with test data */
    val testData = IntegrationTestHelper()
    /** Android context */
    val context = testData.context
    /** API Key */
    val apiKey = "hd8szvscpxvd"

    private val testLogger = StreamTestLogger()

    init {
        StreamLog.install(logger = testLogger)
        println("test logger installed")
    }

    fun setLogLevel(priority: Priority) {
        testLogger.logLevel = priority
    }

    fun randomUUID(): String {
        return UUID.randomUUID().toString()
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
}

open class IntegrationTestBase(connectCoordinatorWS: Boolean = true): TestBase() {
    /** Client */
    val client: StreamVideo
    /** Implementation of the client for more access to interals */
    internal val clientImpl: StreamVideoImpl
    /** Tracks all events received by the client during a test */
    var events: MutableList<VideoEvent>
    /** The builder used for creating the client */
    val builder: StreamVideoBuilder

    public var nextEventContinuation: Continuation<VideoEvent>? = null
    public var nextEventCompleted: Boolean = false

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

        // Connect to the WS if needed
        if (connectCoordinatorWS) {
            clientImpl.connect()
        }

        // monitor for events
        events = mutableListOf()
        client.subscribe {
            println("sub received an event: $it")
            events.add(it)
            println("events in loop $events")

            nextEventContinuation?.let { continuation ->
                if (!nextEventCompleted) {
                    continuation.resume(value=it)
                }
                nextEventCompleted = true
            }
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
     * If we have already received an event of this type return immediately
     * Otherwise wait for the next event of this type
     * TODO: add a timeout
     */
    suspend inline fun <reified T : VideoEvent> waitForNextEvent(): VideoEvent = withTimeout(1000L) {
        suspendCoroutine { continuation ->
            client.subscribe {
                val matchingEvents = events.filter { it is T }
                if (matchingEvents.isNotEmpty()) {
                    continuation.resume(matchingEvents[0])
                }
                if (it is T) {
                    continuation.resume(it)
                }
            }

        }
    }
    @Before
    fun resetTestVars() {
        events = mutableListOf()
    }
}
