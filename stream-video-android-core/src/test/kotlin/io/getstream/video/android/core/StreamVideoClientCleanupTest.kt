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

import android.content.Context
import androidx.lifecycle.Lifecycle
import io.getstream.android.core.api.StreamClient
import io.getstream.android.core.api.model.connection.StreamConnectionState
import io.getstream.android.core.api.subscribe.StreamSubscription
import io.getstream.video.android.core.internal.module.CoordinatorConnectionModule
import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Runs under Robolectric so a real main looper exists and the test thread is the main thread,
 * which is exactly the situation an app is in when it calls StreamVideo.removeClient() from
 * a logout button handler.
 */
@RunWith(RobolectricTestRunner::class)
class StreamVideoClientCleanupTest {

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun buildClient(streamClient: StreamClient): StreamVideoClient = StreamVideoClient(
        context = mockk<Context>(relaxed = true),
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
}
