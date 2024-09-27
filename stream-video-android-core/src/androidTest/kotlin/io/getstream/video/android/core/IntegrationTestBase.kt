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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import io.getstream.log.Priority
import io.getstream.log.StreamLog
import io.getstream.log.android.AndroidStreamLogger
import io.getstream.log.streamLog
import io.getstream.result.Result
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.model.User
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Rule
import org.openapitools.client.models.VideoEvent
import java.util.UUID
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class IntegrationTestHelper {

    val users = mutableMapOf<String, User>()
    val tokens = mutableMapOf<String, String>()
    val context: Context

    val expiredToken =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoidGhpZXJyeUBnZXRzdHJlYW0uaW8iLCJpc3MiOiJwcm9udG8iLCJzdWIiOiJ1c2VyL3RoaWVycnlAZ2V0c3RyZWFtLmlvIiwiaWF0IjoxNjgxMjUxMDg4LCJleHAiOjE2ODEyNjE4OTN9.VinzXBwvT_AGXNBG8QTz9HJFSR6LhqIEtVpIlmY1aEc"

    val fakeSDP = """
        v=0
        o=Node 1 1 IN IP4 172.30.8.37
        s=Stream1
        t=0 0
        m=audio 5004 RTP/AVP 96
        c=IN IP4 239.30.22.1
        a=rtpmap:96 L24/48000/2
        a=recvonly
        a=ptime:1
        a=ts-refclk:ptp=IEEE1588-2008:01-23-45-67-89-AB-CD-EF:127
        a=mediaclk:direct=0
        a=sync-time:0
    """.trimIndent()

    init {
        // TODO: generate token from build vars
        val token =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoidGhpZXJyeSJ9._4aZL6BR0VGKfZsKYdscsBm8yKVgG-2LatYeHRJUq0g"

        val thierry = User(
            id = "thierry",
            role = "admin",
            name = "Thierry",
            image = "hello",
            teams = emptyList(),
            custom = mapOf(),
        )
        users["thierry"] = thierry
        tokens["thierry"] = token
        context = ApplicationProvider.getApplicationContext()
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

    private val testAndroidLogger = AndroidStreamLogger()

    init {
        if (!StreamLog.isInstalled) {
            StreamLog.setValidator { priority, _ -> priority > Priority.VERBOSE }
            StreamLog.install(logger = testAndroidLogger)
            testAndroidLogger.streamLog { "test logger installed" }
        }
    }

    fun setLogLevel(newPriority: Priority) {
        StreamLog.setValidator { priority, _ -> priority > newPriority }
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

object IntegrationTestState {
    var client: StreamVideo? = null
    var call: Call? = null
}

open class IntegrationTestBase(connectCoordinatorWS: Boolean = true) : TestBase() {
    /** Client */
    val client: StreamVideo

    /** Implementation of the client for more access to interals */
    internal val clientImpl: StreamVideoClient

    /** Tracks all events received by the client during a test */
    var events: MutableList<VideoEvent>

    /** The builder used for creating the client */
    val builder: StreamVideoBuilder

    var nextEventContinuation: Continuation<VideoEvent>? = null
    var nextEventCompleted: Boolean = false

    init {
        builder = StreamVideoBuilder(
            context = ApplicationProvider.getApplicationContext(),
            apiKey = "hd8szvscpxvd",
            geo = GEO.GlobalEdgeNetwork,
            testData.users["thierry"]!!,
            testData.tokens["thierry"]!!,
            loggingLevel = LoggingLevel(priority = Priority.VERBOSE),
        )
//        if (BuildConfig.CORE_TEST_LOCAL == "1") {
//            builder.videoDomain = "localhost"
//        }

        if (IntegrationTestState.client == null) {
            client = builder.build()
            clientImpl = client as StreamVideoClient
            // Connect to the WS if needed
            if (connectCoordinatorWS) {
                // wait for the connection/ avoids race conditions in tests
                runBlocking {
                    withTimeout(10000) {
                        val connectResultDeferred = clientImpl.connectAsync()
                        val connectResult = connectResultDeferred.await()
                    }
                }
            }
            IntegrationTestState.client = client
        } else {
            client = IntegrationTestState.client!!
            clientImpl = client as StreamVideoClient
        }

        // monitor for events
        events = mutableListOf()
        client.subscribe {

            events.add(it)

            nextEventContinuation?.let { continuation ->
                if (!nextEventCompleted) {
                    nextEventCompleted = true
                    continuation.resume(value = it)
                }
            }
        }
    }

    val call: Call by lazy {
        if (IntegrationTestState.call != null) {
            IntegrationTestState.call!!
        } else {
            val call = client.call("default", randomUUID())
            IntegrationTestState.call = call
            runBlocking {
                val result = call.create()
                assertSuccess(result)
            }
            call
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
    suspend inline fun <reified T : VideoEvent> waitForNextEvent(): T =
        suspendCoroutine { continuation ->
            var finished = false

            val matchingEvents = events.filterIsInstance<T>()
            if (matchingEvents.isNotEmpty()) {
                continuation.resume(matchingEvents[0])
                finished = true
            }

            client.subscribe {
                if (!finished) {
                    if (it is T) {
                        continuation.resume(it)
                        finished = true
                    }
                }
            }
        }

    @Before
    fun resetTestVars() {
        events = mutableListOf()
    }
}
