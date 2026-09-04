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

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.Lifecycle
import io.getstream.video.android.core.internal.module.CoordinatorConnectionModule
import io.getstream.video.android.core.notifications.internal.service.CallService
import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserType
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

/**
 * Runs under Robolectric so the stop intent inside cleanup() is a real Intent instead of a
 * stubbed one that returns null from every builder call.
 */
@RunWith(RobolectricTestRunner::class)
class StreamVideoClientCleanupTest {

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun mockContext(runningServices: List<ActivityManager.RunningServiceInfo>): Context {
        val context = mockk<Context>(relaxed = true)
        val activityManager = mockk<ActivityManager>()
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
        every { activityManager.getRunningServices(any()) } returns runningServices
        return context
    }

    private fun buildClient(context: Context): StreamVideoClient = StreamVideoClient(
        context = context,
        initialUser = User(id = "user-1", type = UserType.Authenticated),
        apiKey = "apikey",
        token = "token",
        lifecycle = mockk<Lifecycle>(relaxed = true),
        coordinatorConnectionModule = mockk<CoordinatorConnectionModule>(relaxed = true),
        tokenRepository = mockk(relaxed = true),
        streamNotificationManager = mockk(relaxed = true),
        enableCallNotificationUpdates = false,
        sounds = mockk(relaxed = true),
        vibrationConfig = mockk(relaxed = true),
        analytics = mockk(relaxed = true),
    )

    // buildStopIntent returns null when the call service is not running. Passing the null
    // through to stopService used to throw a NullPointerException, swallowed by safeCall,
    // on every logout without a running call service. AND-1466.
    @Test
    fun `cleanup does not call stopService when the call service is not running`() {
        val context = mockContext(runningServices = emptyList())
        val client = buildClient(context)

        client.cleanup()

        verify(exactly = 0) { context.stopService(any()) }
    }

    @Test
    fun `cleanup stops the call service when it is running`() {
        val runningService = ActivityManager.RunningServiceInfo().apply {
            service = ComponentName("io.getstream.video.android", CallService::class.java.name)
        }
        val context = mockContext(runningServices = listOf(runningService))
        val client = buildClient(context)

        client.cleanup()

        verify(exactly = 1) { context.stopService(any()) }
    }
}
