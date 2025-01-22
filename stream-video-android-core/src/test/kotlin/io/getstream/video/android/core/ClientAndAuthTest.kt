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

import com.google.common.truth.Truth.assertThat
import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.video.android.core.base.TestBase
import io.getstream.video.android.core.call.CallType
import io.getstream.video.android.core.errors.VideoErrorCode
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfigRegistry
import io.getstream.video.android.core.notifications.internal.service.callServiceConfig
import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserType
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.openapitools.client.models.ConnectedEvent
import org.openapitools.client.models.VideoEvent
import org.robolectric.RobolectricTestRunner

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
        val builder = StreamVideoBuilder(
            context = context,
            apiKey = authData!!.apiKey,
            token = authData!!.token,
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
        // we get the token from Stream's server in this case
        // the ID is generated, client side...
        // verify that we get the token
        // API call is getGuestUser or something like that
        val client = StreamVideoBuilder(
            context = context,
            apiKey = authData!!.apiKey,
            token = authData!!.token,
            geo = GEO.GlobalEdgeNetwork,
            user = User(
                id = "guest",
                type = UserType.Guest,
            ),
            ensureSingleInstance = false,
        ).build()
        client.cleanup()
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
        assertThat(client.state.connection.value).isEqualTo(ConnectionState.PreConnect)
        val clientImpl = client as StreamVideoClient

        val connectResultDeferred = clientImpl.connectAsync()

        val connectResult = connectResultDeferred.await()
        delay(100L)
        assertThat(client.state.connection.value).isEqualTo(ConnectionState.Connected)
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

    @Test
    fun build_withRegistryCallConfigAndNoLegacyCallConfig_usesRegistryInternally() {
        StreamVideo.removeClient()
        val client = StreamVideoBuilder(
            context = context,
            apiKey = authData!!.apiKey,
            user = testData.users["thierry"]!!,
            token = authData!!.token,
            callServiceConfigRegistry = CallServiceConfigRegistry().apply {
                register(CallType.Default.name) {
                    setRunCallServiceInForeground(testData.callConfigRegistryRunService)
                    setAudioUsage(testData.callConfigRegistryAudioUsage)
                }
            },
        ).build()

        val config = client.state.callConfigRegistry.get(CallType.Default.name)

        assertEquals(testData.callConfigRegistryRunService, config.runCallServiceInForeground)
        assertEquals(testData.callConfigRegistryAudioUsage, config.audioUsage)
    }

    @Test
    fun build_withLegacyCallConfigAndNoRegistryCallConfig_usesLegacyInternally() {
        StreamVideo.removeClient()
        val client = StreamVideoBuilder(
            context = context,
            apiKey = authData!!.apiKey,
            user = testData.users["thierry"]!!,
            token = authData!!.token,
            callServiceConfig = callServiceConfig().copy(
                runCallServiceInForeground = testData.callConfigLegacyRunService,
                audioUsage = testData.callConfigLegacyAudioUsage,
            ),
        ).build()

        val config = client.state.callConfigRegistry.get(CallType.Default.name)

        assertEquals(testData.callConfigLegacyRunService, config.runCallServiceInForeground)
        assertEquals(testData.callConfigLegacyAudioUsage, config.audioUsage)
    }

    @Test
    fun build_withBothRegistryCallConfigAndLegacyCallConfig_usesRegistryInternally() {
        StreamVideo.removeClient()
        val client = StreamVideoBuilder(
            context = context,
            apiKey = authData!!.apiKey,
            user = testData.users["thierry"]!!,
            token = authData!!.token,
            callServiceConfig = callServiceConfig().copy(
                runCallServiceInForeground = testData.callConfigLegacyRunService,
                audioUsage = testData.callConfigLegacyAudioUsage,
            ),
            callServiceConfigRegistry = CallServiceConfigRegistry().apply {
                register(CallType.Default.name) {
                    setRunCallServiceInForeground(testData.callConfigRegistryRunService)
                    setAudioUsage(testData.callConfigRegistryAudioUsage)
                }
            },
        ).build()

        val config = client.state.callConfigRegistry.get(CallType.Default.name)

        assertEquals(testData.callConfigRegistryRunService, config.runCallServiceInForeground)
        assertEquals(testData.callConfigRegistryAudioUsage, config.audioUsage)
    }
}
