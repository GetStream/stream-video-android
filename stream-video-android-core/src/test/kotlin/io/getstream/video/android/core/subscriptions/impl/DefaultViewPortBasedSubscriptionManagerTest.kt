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

package io.getstream.video.android.core.subscriptions.impl

import io.getstream.result.Result
import io.getstream.video.android.core.subscriptions.api.DebouncedSubscriptionManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.signal.TrackSubscriptionDetails

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultViewPortBasedSubscriptionManagerTest {
    private lateinit var debouncedManager: DebouncedSubscriptionManager
    private lateinit var manager: DefaultViewPortBasedSubscriptionManager
    private lateinit var testScope: TestScope
    private val defaultDimension = VideoDimension(width = 1280, height = 720)

    @Before
    fun setup() {
        debouncedManager = mockk(relaxed = true)
        testScope = TestScope()
        manager = DefaultViewPortBasedSubscriptionManager(debouncedManager)
    }

    @Test
    fun `updateViewport subscribes on first visible viewport`() = runTest {
        coEvery {
            debouncedManager.subscribe(listOf(Triple("user-1", TrackType.TRACK_TYPE_VIDEO, defaultDimension)))
        } returns Result.Success(listOf(TrackSubscriptionDetails("user-1", "user-1", TrackType.TRACK_TYPE_VIDEO)))
        manager.updateViewport(
            "viewport-1",
            "user-1",
            TrackType.TRACK_TYPE_VIDEO,
            true,
            defaultDimension,
        )
        coVerify {
            debouncedManager.subscribe(
                listOf(Triple("user-1", TrackType.TRACK_TYPE_VIDEO, defaultDimension)),
            )
        }
    }

    @Test
    fun `updateViewport does not subscribe again for additional visible viewports`() = runTest {
        coEvery { debouncedManager.subscribe(any()) } returns Result.Success(listOf())
        manager.updateViewport(
            "viewport-1",
            "user-1",
            TrackType.TRACK_TYPE_VIDEO,
            true,
            defaultDimension,
        )
        manager.updateViewport(
            "viewport-2",
            "user-1",
            TrackType.TRACK_TYPE_VIDEO,
            true,
            defaultDimension,
        )
        coVerify(exactly = 1) {
            debouncedManager.subscribe(
                listOf(Triple("user-1", TrackType.TRACK_TYPE_VIDEO, defaultDimension)),
            )
        }
    }

    @Test
    fun `updateViewport unsubscribes when last viewport goes invisible`() = runTest {
        manager.updateViewport(
            "viewport-1",
            "user-1",
            TrackType.TRACK_TYPE_VIDEO,
            true,
            defaultDimension,
        )
        manager.updateViewport(
            "viewport-1",
            "user-1",
            TrackType.TRACK_TYPE_VIDEO,
            false,
            defaultDimension,
        )
        coVerify { debouncedManager.unsubscribeDebounced("user-1", TrackType.TRACK_TYPE_VIDEO) }
    }

    @Test
    fun `updateViewport does not unsubscribe if other viewports are still visible`() = runTest {
        manager.updateViewport(
            "viewport-1",
            "user-1",
            TrackType.TRACK_TYPE_VIDEO,
            true,
            defaultDimension,
        )
        manager.updateViewport(
            "viewport-2",
            "user-1",
            TrackType.TRACK_TYPE_VIDEO,
            true,
            defaultDimension,
        )
        manager.updateViewport(
            "viewport-1",
            "user-1",
            TrackType.TRACK_TYPE_VIDEO,
            false,
            defaultDimension,
        )
        coVerify(exactly = 0) { debouncedManager.unsubscribeDebounced(any(), any()) }
    }

    @Test
    fun `subscriptions delegates to debounced manager`() {
        val expected =
            mapOf(
                "user-1" to TrackSubscriptionDetails("user-1", "user-1", TrackType.TRACK_TYPE_VIDEO),
            )
        coEvery { debouncedManager.subscriptions() } returns expected
        val result = manager.subscriptions()
        assertEquals(expected, result)
    }

    @Test
    fun `subscribe delegates to debounced manager`() = runTest {
        val sessionId = "user-1"
        val trackType = TrackType.TRACK_TYPE_VIDEO
        val sessions = listOf(Triple(sessionId, trackType, defaultDimension))
        val expected = listOf(TrackSubscriptionDetails(sessionId, sessionId, trackType))
        coEvery { debouncedManager.subscribe(sessions) } returns Result.Success(expected)
        val result = manager.subscribe(sessions)
        assertTrue(result is Result.Success)
        coVerify { debouncedManager.subscribe(sessions) }
    }

    @Test
    fun `unsubscribe delegates to debounced manager`() = runTest {
        coEvery {
            debouncedManager.unsubscribe("user-1", TrackType.TRACK_TYPE_VIDEO)
        } returns Result.Success(Unit)
        val result = manager.unsubscribe("user-1", TrackType.TRACK_TYPE_VIDEO)
        assertTrue(result is Result.Success)
        coVerify { debouncedManager.unsubscribe("user-1", TrackType.TRACK_TYPE_VIDEO) }
    }

    @Test
    fun `subscribe(List) delegates to debounced manager`() = runTest {
        val sessions = listOf(Triple("user-1", TrackType.TRACK_TYPE_VIDEO, defaultDimension))
        val expected =
            listOf(TrackSubscriptionDetails("user-1", "user-1", TrackType.TRACK_TYPE_VIDEO))
        coEvery { debouncedManager.subscribe(sessions) } returns Result.Success(expected)
        val result = manager.subscribe(sessions)
        assertTrue(result is Result.Success)
        coVerify { debouncedManager.subscribe(sessions) }
    }

    @Test
    fun `unsubscribe(List) delegates to debounced manager`() = runTest {
        val sessions = listOf(Pair("user-1", TrackType.TRACK_TYPE_VIDEO))
        coEvery { debouncedManager.unsubscribe(sessions) } returns Result.Success(Unit)
        val result = manager.unsubscribe(sessions)
        assertTrue(result is Result.Success)
        coVerify { debouncedManager.unsubscribe(sessions) }
    }

    @Test
    fun `clear delegates to debounced manager`() {
        manager.clear()
        coVerify { debouncedManager.clear() }
    }

    @Test
    fun `setDebouncePolicy delegates to debounced manager`() {
        val policy = mockk<io.getstream.video.android.core.subscriptions.api.DebouncePolicy>()
        manager.setDebouncePolicy(policy)
        coVerify { debouncedManager.setDebouncePolicy(policy) }
    }

    @Test
    fun `unsubscribeDebounced delegates to debounced manager`() {
        manager.unsubscribeDebounced("user-1", TrackType.TRACK_TYPE_VIDEO)
        coVerify { debouncedManager.unsubscribeDebounced("user-1", TrackType.TRACK_TYPE_VIDEO) }
    }
}
