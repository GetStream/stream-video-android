/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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
import io.getstream.android.core.api.model.connection.StreamConnectionState
import io.getstream.android.video.generated.models.ConnectedEvent
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.video.android.core.base.TestBase
import io.getstream.video.android.core.errors.VideoErrorCode
import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserType
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@Ignore(
    "Because it installs a anonymous user StreamVideo Client. However we will run it via isolatedTest (check build.gradle.kts)",
)
@RunWith(RobolectricTestRunner::class)
class ClientAndAuthTest : TestBase() {

    private val logger by taggedLogger("Test:ClientAndAuthTest")

    @Test
    fun regularUser() = runTest {
        val builder = StreamVideoBuilder(
            context = context,
            apiKey = authData!!.apiKey,
            geo = GEO.GlobalEdgeNetwork,
            user = testData.users["thierry"]!!,
            token = testData.tokens["thierry"]!!,
            ensureSingleInstance = false,
        )
        val client = builder.build()
        client.cleanup()
    }

    @Test
    fun anonymousUser() = runTest {
        // Anonymous users do not require a token — no server-side credential is needed.
        val builder = StreamVideoBuilder(
            context = context,
            apiKey = authData!!.apiKey,
            geo = GEO.GlobalEdgeNetwork,
            user = User(
                type = UserType.Anonymous,
            ),
            ensureSingleInstance = false,
        )
        val client = builder.build()
        client.cleanup()
    }

    @Test
    fun guestUser() = runTest {
        // Guest users do not need an initial token.
        // The SDK automatically calls POST /video/guest to obtain a short-lived JWT.
        val client = StreamVideoBuilder(
            context = context,
            apiKey = authData!!.apiKey,
            geo = GEO.GlobalEdgeNetwork,
            user = User(
                id = "guest-${System.currentTimeMillis()}",
                type = UserType.Guest,
            ),
            ensureSingleInstance = false,
            connectOnInit = false,
        ).build()
        client.cleanup()
    }

    /**
     * Verifies the core guest authentication flow:
     * 1. SDK calls POST /video/guest to obtain a JWT (no initial token needed)
     * 2. Guest user can establish a WebSocket connection
     *
     * Note: guest users on the test environment do not have permission to create
     * new calls — that is expected backend behaviour, not a bug.
     */
    @Test
    fun `guest user can connect`() = runTest {
        val client = StreamVideoBuilder(
            context = context,
            apiKey = authData!!.apiKey,
            geo = GEO.GlobalEdgeNetwork,
            user = User(
                id = "guest-${System.currentTimeMillis()}",
                type = UserType.Guest,
            ),
            ensureSingleInstance = false,
            connectOnInit = false,
        ).build()

        // connect() internally awaits guestUserJob (which fetches the JWT from POST /video/guest)
        // and then establishes the WebSocket connection with that token.
        val connectResult = withContext(Dispatchers.Default) {
            withTimeout(15_000) {
                client.connect()
            }
        }
        assertSuccess(connectResult)

        client.cleanup()
    }

    /**
     * Verifies that a guest user can join a call created by an authenticated user.
     * WebRTC peer connection components are mocked because real WebRTC cannot run in unit tests.
     *
     * Ignored: Call.join() triggers protobuf classloading inside Robolectric's sandbox
     * (ClassCastException in ensureFactoryMatchesAudioProfile) — a known limitation for
     * WebRTC join tests, tracked in JoinCallTest.kt.
     */
    @Test
    @Ignore("Robolectric classloader incompatibility with protobuf in Call.join()")
    fun `guest user can join a call`() = runTest {
        // Mock WebRTC components — real peer connections cannot run in Robolectric/unit tests.
        Call.testInstanceProvider.mediaManagerCreator = { mockk(relaxed = true) }
        Call.testInstanceProvider.rtcSessionCreator = { mockk(relaxed = true) }

        // Step 1: create the call as an authenticated user.
        val authClient = StreamVideoBuilder(
            context = context,
            apiKey = authData!!.apiKey,
            geo = GEO.GlobalEdgeNetwork,
            user = testData.users["thierry"]!!,
            token = authData!!.token,
            ensureSingleInstance = false,
            connectOnInit = false,
        ).build()
        val callId = randomUUID()
        val authCall = authClient.call("default", callId)
        val createResult = withContext(Dispatchers.Default) {
            authCall.create()
        }
        assertSuccess(createResult)
        authClient.cleanup()

        // Step 2: connect as a guest and join the existing call.
        val guestClient = StreamVideoBuilder(
            context = context,
            apiKey = authData!!.apiKey,
            geo = GEO.GlobalEdgeNetwork,
            user = User(
                id = "guest-${System.currentTimeMillis()}",
                type = UserType.Guest,
            ),
            ensureSingleInstance = false,
            connectOnInit = false,
        ).build()

        val connectResult = withContext(Dispatchers.Default) {
            withTimeout(15_000) {
                guestClient.connect()
            }
        }
        assertSuccess(connectResult)

        val guestCall = guestClient.call("default", callId)
        guestCall.peerConnectionFactory = mockedPCFactory

        val joinResult = guestCall.join()
        assertSuccess(joinResult)

        guestClient.cleanup()
    }

    @Test
    @Ignore
    fun subscribeToAllEvents() = runTest {
        val client = StreamVideoBuilder(
            context = context,
            apiKey = authData!!.apiKey,
            geo = GEO.GlobalEdgeNetwork,
            user = User(
                id = "guest",
                type = UserType.Guest,
            ),
        ).build()
        val sub = client.subscribe { event: VideoEvent ->
            logger.d { event.toString() }
        }
        sub.dispose()
    }

    @Test
    @Ignore
    fun subscribeToSpecificEvents() = runTest {
        val client = StreamVideoBuilder(
            context = context,
            apiKey = authData!!.apiKey,
            geo = GEO.GlobalEdgeNetwork,
            user = User(
                id = "guest",
                type = UserType.Guest,
            ),
        ).build()
        // Subscribe for new message events
        val sub = client.subscribeFor<ConnectedEvent> { newMessageEvent ->
            logger.d { newMessageEvent.toString() }
        }
        sub.dispose()
    }

    @Test
    @Ignore
    fun waitForWSConnection() = runTest {
        val client = StreamVideoBuilder(
            context = context,
            apiKey = authData!!.apiKey,
            geo = GEO.GlobalEdgeNetwork,
            user = testData.users["thierry"]!!,
            token = authData!!.token,
        ).build()
        assertThat(client.state.connection.value).isEqualTo(StreamConnectionState.Idle)
        val clientImpl = client as StreamVideoClient

        val connectResultDeferred = clientImpl.connectAsync()

        val connectResult = connectResultDeferred.await()
        delay(100L)
        assertThat(
            client.state.connection.value,
        ).isInstanceOf(StreamConnectionState.Connected::class.java)
    }

    @Test
    @Ignore
    fun testInvalidAPIKey() = runTest {
        StreamVideoBuilder(
            context = context,
            apiKey = "notvalid",
            geo = GEO.GlobalEdgeNetwork,
            user = testData.users["thierry"]!!,
            token = authData!!.token,
        ).build()
    }

    @Test
    @Ignore
    fun `test an expired token, no provider set`() = runTest {
        val client = StreamVideoBuilder(
            context = context,
            apiKey = authData!!.apiKey,
            geo = GEO.GlobalEdgeNetwork,
            user = testData.users["thierry"]!!,
            token = authData!!.token,
        ).build()

        val result = client.call("default", "123").create()
        assertError(result)
        result.onError {
            it as Error.NetworkError
            assertThat(it.serverErrorCode).isEqualTo(VideoErrorCode.TOKEN_EXPIRED.code)
        }
        client.cleanup()
    }

    @Test
    @Ignore("Throws exception: Token signature is invalid")
    fun `test an expired token, with token provider set`() = runTest {
        StreamVideo.removeClient()
        val client = StreamVideoBuilder(
            context = context,
            apiKey = authData!!.apiKey,
            geo = GEO.GlobalEdgeNetwork,
            user = testData.users["thierry"]!!,
            token = testData.expiredToken,
            legacyTokenProvider = { error ->
                testData.tokens["thierry"]!!
            },
        ).build()

        val result = client.call("default").create()
        assertSuccess(result)

        client.cleanup()
    }

    @Test
    fun testEmptyAPIKey() = runTest {
//        assertFailsWith<java.lang.IllegalArgumentException> {
//            StreamVideoBuilder(
//                context = context,
//                apiKey = "",
//                geo = GEO.GlobalEdgeNetwork,
//                testData.users["thierry"]!!,
//                testData.tokens["thierry"]!!,
//            ).build()
//        }
    }

    @Test(expected = RuntimeException::class)
    fun `two clients is not allowed`() = runTest {
        val builder = StreamVideoBuilder(
            context = context,
            apiKey = authData!!.apiKey,
            geo = GEO.GlobalEdgeNetwork,
            user = testData.users["thierry"]!!,
            token = authData!!.token,
        )
        val client = builder.build()
        val client2 = builder.build()
    }

    @Test
    @Ignore("Throws exception: Token signature is invalid")
    fun testWaitingForConnection() = runTest {
        // often you'll want to run the connection task in the background and not wait for it
        val client = StreamVideoBuilder(
            context = context,
            apiKey = authData!!.apiKey,
            geo = GEO.GlobalEdgeNetwork,
            user = testData.users["thierry"]!!,
            token = authData!!.token,
        ).build()
        val clientImpl = client as StreamVideoClient
        client.subscribe {
        }
        val deferred = clientImpl.connectAsync()
        deferred.join()
    }
}
