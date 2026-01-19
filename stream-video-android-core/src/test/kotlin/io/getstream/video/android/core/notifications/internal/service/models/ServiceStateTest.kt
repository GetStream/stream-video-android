/*
 * Copyright (c) 2014-2025 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.notifications.internal.service.models

import android.app.Service
import android.content.IntentFilter
import io.getstream.video.android.core.notifications.internal.receivers.ToggleCameraBroadcastReceiver
import io.getstream.video.android.core.notifications.internal.service.controllers.ServiceStateController
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class ServiceStateTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val service: Service = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `registerToggleCameraBroadcastReceiver registers receiver`() {
        val sut = ServiceStateController()
        sut.registerToggleCameraBroadcastReceiver(service, testScope)

        verify(exactly = 1) {
            service.registerReceiver(
                any<ToggleCameraBroadcastReceiver>(),
                any<IntentFilter>(),
            )
        }
    }

    @Test
    fun `registerToggleCameraBroadcastReceiver does not re-register if already registered`() {
        val sut = ServiceStateController()
        sut.registerToggleCameraBroadcastReceiver(service, testScope)
        sut.registerToggleCameraBroadcastReceiver(service, testScope)

        verify(exactly = 1) {
            service.registerReceiver(any(), any())
        }
    }

    @Test
    fun `registerToggleCameraBroadcastReceiver swallows exception`() {
        val sut = ServiceStateController()
        every {
            service.registerReceiver(any(), any())
        } throws RuntimeException("boom")

        sut.registerToggleCameraBroadcastReceiver(service, testScope)

        // Should not crash
    }

    @Test
    fun `unregisterToggleCameraBroadcastReceiver does nothing if not registered`() {
        val sut = ServiceStateController()
        sut.unregisterToggleCameraBroadcastReceiver(service)

        verify {
            service wasNot Called
        }
    }

    /**
     * Ignored because of unknown reason of failure, it will fail when running all tests
     */
    @Test
    fun `unregisterToggleCameraBroadcastReceiver unregisters receiver`() {
        val sut = ServiceStateController()
        sut.registerToggleCameraBroadcastReceiver(service, testScope)
        sut.unregisterToggleCameraBroadcastReceiver(service)

        verify {
            service.unregisterReceiver(any())
        }
    }

    @Test
    fun `unregisterToggleCameraBroadcastReceiver swallows exception`() {
        val sut = ServiceStateController()
        sut.registerToggleCameraBroadcastReceiver(service, testScope)

        every {
            service.unregisterReceiver(any())
        } throws IllegalArgumentException("not registered")

        sut.unregisterToggleCameraBroadcastReceiver(service)

        // Should not crash
    }
}
