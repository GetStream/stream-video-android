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

package io.getstream.video.android.core.notifications.internal

import android.content.Context
import io.getstream.android.video.generated.apis.ProductvideoApi
import io.getstream.video.android.core.notifications.NotificationConfig
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.core.notifications.internal.storage.DeviceTokenStorage
import io.getstream.video.android.model.Device
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import kotlin.test.Test
import kotlin.test.assertTrue

class StreamNotificationManagerTest {

    private val device = Device(
        id = "fcm-token-abc",
        pushProvider = "firebase",
        pushProviderName = "firebase",
    )

    @After
    fun tearDown() {
        unmockkAll()
    }

    // Regression: existing happy-path. Local store is cleared when the API call succeeds.
    @Test
    fun `deleteDevice clears local store when API succeeds`() = runTest {
        val api = mockk<ProductvideoApi>(relaxed = true)
        val storage = mockk<DeviceTokenStorage>(relaxed = true) {
            every { userDevice } returns flowOf(device)
        }
        val manager = makeManager(api = api, storage = storage)

        val result = manager.deleteDevice(device)

        assertTrue(result is io.getstream.result.Result.Success, "expected Success, got $result")
        coVerify { storage.updateUserDevice(null) }
    }

    // AND-1214: the bug — server 404 or network failure left the cache populated,
    // making the next createDevice short-circuit on token equality.
    @Test
    fun `deleteDevice clears local store when API throws`() = runTest {
        val api = mockk<ProductvideoApi>(relaxed = true) {
            coEvery { deleteDevice(device.id) } throws RuntimeException("boom")
        }
        val storage = mockk<DeviceTokenStorage>(relaxed = true) {
            every { userDevice } returns flowOf(device)
        }
        val manager = makeManager(api = api, storage = storage)

        val result = manager.deleteDevice(device)

        assertTrue(result is io.getstream.result.Result.Failure, "expected Failure, got $result")
        coVerify { storage.updateUserDevice(null) }
    }

    // The local-cleanup must not mask the API-call outcome.
    @Test
    fun `deleteDevice surfaces API failure even when local cleanup throws`() = runTest {
        val api = mockk<ProductvideoApi>(relaxed = true) {
            coEvery { deleteDevice(device.id) } throws RuntimeException("boom")
        }
        val storage = mockk<DeviceTokenStorage>(relaxed = true) {
            every { userDevice } returns flowOf(device)
            coEvery { updateUserDevice(any()) } throws RuntimeException("storage failed")
        }
        val manager = makeManager(api = api, storage = storage)

        val result = manager.deleteDevice(device)

        // The API call failed; that's what the caller needs to know — local-cleanup
        // throwing must not turn this into a different exception or a Success.
        assertTrue(result is io.getstream.result.Result.Failure, "expected Failure, got $result")
        // And the cleanup must have been attempted — otherwise this test would still
        // pass if a future refactor removed the cleanup path entirely.
        coVerify { storage.updateUserDevice(null) }
    }

    private fun makeManager(
        api: ProductvideoApi,
        storage: DeviceTokenStorage,
    ): StreamNotificationManager {
        val notificationHandler = mockk<NotificationHandler>(relaxed = true)
        val notificationConfig = mockk<NotificationConfig>(relaxed = true) {
            every { this@mockk.notificationHandler } returns notificationHandler
        }
        // Private primary constructor — instantiate via reflection. Pick the 6-arg
        // overload (Kotlin emits extra synthetic constructors for default values).
        val ctor = StreamNotificationManager::class.java.declaredConstructors
            .first { it.parameterCount == 6 }
        ctor.isAccessible = true
        return ctor.newInstance(
            mockk<Context>(relaxed = true),
            TestScope(),
            notificationConfig,
            api,
            storage,
            null,
        ) as StreamNotificationManager
    }
}
