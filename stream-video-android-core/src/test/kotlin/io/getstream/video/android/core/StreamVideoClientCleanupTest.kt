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
import io.getstream.android.core.api.StreamClient
import io.getstream.android.core.api.model.connection.StreamConnectionState
import io.getstream.android.core.api.subscribe.StreamSubscription
import io.getstream.video.android.core.internal.module.CoordinatorConnectionModule
import io.getstream.video.android.core.notifications.internal.service.CallService
import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Runs under Robolectric so a real main looper exists and the test thread is the main thread,
 * which is exactly the situation an app is in when it calls StreamVideo.removeClient() from
 * a logout button handler. The stop intent inside cleanup() is also a real Intent instead of
 * a stubbed one that returns null from every builder call.
 */
@RunWith(RobolectricTestRunner::class)
class StreamVideoClientCleanupTest {

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun mockStreamClient(): StreamClient {
        val streamClient = mockk<StreamClient>(relaxed = true)
        every { streamClient.subscribe(any()) } returns
            Result.success(mockk<StreamSubscription>(relaxed = true))
        every { streamClient.connectionState } returns
            MutableStateFlow(StreamConnectionState.Idle)
        return streamClient
    }

    private fun awaitScopeCancelled(client: StreamVideoClient) {
        val deadline = System.currentTimeMillis() + 5_000
        while (client.scope.isActive && System.currentTimeMillis() < deadline) {
            Thread.sleep(20)
        }
        assertFalse(
            client.scope.isActive,
            "the scope must be cancelled after the disconnect step",
        )
    }

    private fun mockContext(runningServices: List<ActivityManager.RunningServiceInfo>): Context {
        val context = mockk<Context>(relaxed = true)
        val activityManager = mockk<ActivityManager>()
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
        every { activityManager.getRunningServices(any()) } returns runningServices
        return context
    }

    private fun buildClient(
        streamClient: StreamClient,
        context: Context = mockk(relaxed = true),
        cleanupDisconnectTimeoutMs: Long = 10_000L,
    ): StreamVideoClient = StreamVideoClient(
        context = context,
        initialUser = User(id = "user-1", type = UserType.Authenticated),
        apiKey = "apikey",
        token = "token",
        lifecycle = mockk<Lifecycle>(relaxed = true),
        coordinatorConnectionModule = mockk<CoordinatorConnectionModule>(relaxed = true),
        streamClient = streamClient,
        tokenRepository = mockk(relaxed = true),
        streamNotificationManager = mockk(relaxed = true),
        enableCallNotificationUpdates = false,
        sounds = mockk(relaxed = true),
        vibrationConfig = mockk(relaxed = true),
        analytics = mockk(relaxed = true),
        cleanupDisconnectTimeoutMs = cleanupDisconnectTimeoutMs,
    )

    // Regression for AND-1466: cleanup() used to bridge streamClient.disconnect() with
    // runBlocking even on the main thread. StreamClient.disconnect() stops its lifecycle
    // monitor by posting to the main looper and blocking on it with a 5 second safety
    // timeout, so a logout from the main thread froze the UI for those 5 seconds.
    @Test
    fun `cleanup on the main thread returns without waiting for the disconnect`() {
        val streamClient = mockk<StreamClient>(relaxed = true)
        every { streamClient.subscribe(any()) } returns
            Result.success(mockk<StreamSubscription>(relaxed = true))
        every { streamClient.connectionState } returns
            MutableStateFlow(StreamConnectionState.Idle)
        coEvery { streamClient.disconnect() } coAnswers {
            delay(6_000)
            Result.success(Unit)
        }
        val client = buildClient(streamClient)

        val elapsed = measureTimeMillis { client.cleanup() }

        assertTrue(
            elapsed < 3_000,
            "cleanup must not park the main thread on the disconnect; took ${elapsed}ms",
        )
        // The disconnect must still happen, detached from the calling thread.
        coVerify(timeout = 3_000, exactly = 1) { streamClient.disconnect() }
    }

    // The StreamClient executes its internals, the disconnect included, on the same scope the
    // video client owns, so cancelling that scope before the disconnect finishes turns the
    // disconnect into a silent no-op and leaks the coordinator socket.
    @Test
    fun `cleanup on the main thread cancels the client scope only after the disconnect finishes`() {
        val streamClient = mockk<StreamClient>(relaxed = true)
        every { streamClient.subscribe(any()) } returns
            Result.success(mockk<StreamSubscription>(relaxed = true))
        every { streamClient.connectionState } returns
            MutableStateFlow(StreamConnectionState.Idle)
        coEvery { streamClient.disconnect() } coAnswers {
            delay(2_000)
            Result.success(Unit)
        }
        val client = buildClient(streamClient)

        client.cleanup()

        assertTrue(
            client.scope.isActive,
            "the scope must stay alive while the disconnect is still in flight",
        )
        coVerify(timeout = 3_000, exactly = 1) { streamClient.disconnect() }
        val deadline = System.currentTimeMillis() + 5_000
        while (client.scope.isActive && System.currentTimeMillis() < deadline) {
            Thread.sleep(20)
        }
        assertFalse(
            client.scope.isActive,
            "the scope must be cancelled once the disconnect has completed",
        )
    }

    // A failed disconnect is logged but must not stop the teardown: the scope is cancelled
    // regardless, otherwise a transient socket error would leak the whole client.
    @Test
    fun `cleanup still cancels the scope when the disconnect fails`() {
        val streamClient = mockStreamClient()
        coEvery { streamClient.disconnect() } returns Result.failure(RuntimeException("socket down"))
        val client = buildClient(streamClient)

        client.cleanup()

        coVerify(timeout = 3_000, exactly = 1) { streamClient.disconnect() }
        awaitScopeCancelled(client)
    }

    // Off the main thread there is no deadlock risk, so cleanup keeps the synchronous
    // contract: the disconnect has completed and the scope is cancelled by the time it returns.
    @Test
    fun `cleanup off the main thread disconnects before returning`() {
        val streamClient = mockStreamClient()
        coEvery { streamClient.disconnect() } returns Result.success(Unit)
        val client = buildClient(streamClient)

        val worker = thread { client.cleanup() }
        worker.join(10_000)

        assertFalse(worker.isAlive, "cleanup must complete on a background thread")
        coVerify(exactly = 1) { streamClient.disconnect() }
        assertFalse(
            client.scope.isActive,
            "the scope must already be cancelled when cleanup returns off the main thread",
        )
    }

    // A disconnect that never completes must not hold the teardown hostage: after the
    // configured bound the scope is cancelled anyway.
    @Test
    fun `cleanup gives up on a disconnect that exceeds the timeout`() {
        val streamClient = mockStreamClient()
        coEvery { streamClient.disconnect() } coAnswers {
            delay(60_000)
            Result.success(Unit)
        }
        val client = buildClient(streamClient, cleanupDisconnectTimeoutMs = 200)

        client.cleanup()

        coVerify(timeout = 3_000, exactly = 1) { streamClient.disconnect() }
        // The mock is still suspended in its delay, so reaching cancellation here proves the
        // timeout branch ran instead of waiting for the disconnect.
        awaitScopeCancelled(client)
    }

    // buildStopIntent returns null when the call service is not running. Passing the null
    // through to stopService used to throw a NullPointerException, swallowed by safeCall,
    // on every logout without a running call service. AND-1466 / #1794.
    @Test
    fun `cleanup does not call stopService when the call service is not running`() {
        val context = mockContext(runningServices = emptyList())
        val client = buildClient(mockStreamClient(), context)

        client.cleanup()

        verify(exactly = 0) { context.stopService(any()) }
    }

    @Test
    fun `cleanup stops the call service when it is running`() {
        val runningService = ActivityManager.RunningServiceInfo().apply {
            service = ComponentName("io.getstream.video.android", CallService::class.java.name)
        }
        val context = mockContext(runningServices = listOf(runningService))
        val client = buildClient(mockStreamClient(), context)

        client.cleanup()

        verify(exactly = 1) { context.stopService(any()) }
    }
}
