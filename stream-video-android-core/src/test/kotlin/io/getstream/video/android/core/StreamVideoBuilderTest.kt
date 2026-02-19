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

import io.getstream.video.android.core.base.TestBase
import io.getstream.video.android.core.call.CallType
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfigRegistry
import io.getstream.video.android.core.notifications.internal.service.callServiceConfig
import io.getstream.video.android.model.User
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

@Ignore(
    "Because it installs a anonymous user StreamVideo Client. However we will run it via isolatedTest (check build.gradle.kts)",
)
@RunWith(RobolectricTestRunner::class)
class StreamVideoBuilderTest : TestBase() {

    @Test
    fun createCallConfigurationRegistry_withRegistryCallConfigAndWithoutLegacyCallConfig_usesRegistry() {
        StreamVideo.removeClient()

        val builder = StreamVideoBuilder(context = context, apiKey = authData!!.apiKey)

        val config = builder.createCallConfigurationRegistry(
            callServiceConfigRegistry = CallServiceConfigRegistry().apply {
                register(CallType.Default.name) {
                    setRunCallServiceInForeground(testData.callConfigRegistryRunService)
                    setAudioUsage(testData.callConfigRegistryAudioUsage)
                }
            },
        ).get(CallType.Default.name)

        assertEquals(testData.callConfigRegistryRunService, config.runCallServiceInForeground)
        assertEquals(testData.callConfigRegistryAudioUsage, config.audioUsage)
    }

    @Test
    fun createCallConfigurationRegistry_withLegacyCallConfigAndWithoutRegistryCallConfig_usesLegacy() {
        val builder = StreamVideoBuilder(context = context, apiKey = authData!!.apiKey)

        val config = builder.createCallConfigurationRegistry(
            callServiceConfig = callServiceConfig().copy(
                runCallServiceInForeground = testData.callConfigLegacyRunService,
                audioUsage = testData.callConfigLegacyAudioUsage,
            ),
        ).get(CallType.Default.name)

        assertEquals(testData.callConfigLegacyRunService, config.runCallServiceInForeground)
        assertEquals(testData.callConfigLegacyAudioUsage, config.audioUsage)
    }

    @Test
    fun createCallConfigurationRegistry_withBothRegistryCallConfigAndLegacyCallConfig_usesRegistry() {
        val builder = StreamVideoBuilder(context = context, apiKey = authData!!.apiKey)

        val config = builder.createCallConfigurationRegistry(
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
        ).get(CallType.Default.name)

        assertEquals(testData.callConfigRegistryRunService, config.runCallServiceInForeground)
        assertEquals(testData.callConfigRegistryAudioUsage, config.audioUsage)
    }

    @Test
    fun customApiWssUrls() {
        StreamVideo.removeClient()

        val customApiUrl = "http://some-api-url.com:3030"
        val customWssUrl = "ws://some-wss-url.com:8800/video/connect"

        val client = StreamVideoBuilder(
            context = context,
            apiKey = authData!!.apiKey,
            user = User.anonymous(),
            token = "anonymous-token",
        ).apply {
            forceApiUrl(customApiUrl)
            forceWssUrl(customWssUrl)
        }.build() as StreamVideoClient

        assertEquals(client.coordinatorConnectionModule.apiUrl, customApiUrl)
        assertEquals(client.coordinatorConnectionModule.wssUrl, customWssUrl)
        StreamVideo.removeClient()
    }

    @Test
    fun build_afterRemovingClientDuringCleanup_doesNotThrow() {
        StreamVideo.removeClient()

        val cleanupStarted = CountDownLatch(1)
        val cleanupRelease = CountDownLatch(1)
        val blockingClient = mockk<StreamVideo>(relaxed = true)

        every { blockingClient.cleanup() } answers {
            cleanupStarted.countDown()
            cleanupRelease.await(5, TimeUnit.SECONDS)
        }

        StreamVideo.install(blockingClient)

        val removeThread = thread(start = true) {
            StreamVideo.removeClient()
        }

        val buildResult = try {
            assertTrue(
                "Timeout waiting for cleanup to start",
                cleanupStarted.await(5, TimeUnit.SECONDS),
            )

            runCatching {
                StreamVideoBuilder(
                    ensureSingleInstance = true,
                    context = context,
                    apiKey = authData!!.apiKey,
                    user = User.anonymous(),
                    token = "anonymous-token",
                ).build()
            }
        } catch (it: Exception) {
            it.printStackTrace()
            throw it
        } finally {
            cleanupRelease.countDown()
            removeThread.join(TimeUnit.SECONDS.toMillis(5))
            StreamVideo.removeClient()
        }

        println("buildResult: $buildResult")
        assertTrue("Expected builder to succeed after removeClient", buildResult.isSuccess)
    }
}
