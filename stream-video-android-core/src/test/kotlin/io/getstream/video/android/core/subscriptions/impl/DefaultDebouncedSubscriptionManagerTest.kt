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
import io.getstream.video.android.core.subscriptions.api.SubscriptionManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.signal.TrackSubscriptionDetails

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultDebouncedSubscriptionManagerTest {
    private lateinit var delegate: SubscriptionManager
    private lateinit var manager: DefaultDebouncedSubscriptionManager
    private lateinit var testScope: TestScope
    private lateinit var scheduler: TestCoroutineScheduler
    private val defaultDimension = VideoDimension(width = 1280, height = 720)

    @Before
    fun setup() {
        delegate = mockk(relaxed = true)
        testScope = TestScope()
        scheduler = testScope.testScheduler
        manager = DefaultDebouncedSubscriptionManager(testScope, delegate)
    }

    @Test
    fun `subscribe removes pending unsubscribe and delegates if not already subscribed`() = runTest {
        coEvery {
            delegate.subscriptions()
        } returns emptyMap() andThen mapOf("user-1" to TrackSubscriptionDetails("user-1", "user-1", TrackType.TRACK_TYPE_VIDEO))
        coEvery {
            delegate.subscribe("user-1", TrackType.TRACK_TYPE_VIDEO, defaultDimension)
        } returns Result.Success(TrackSubscriptionDetails("user-1", "user-1", TrackType.TRACK_TYPE_VIDEO))
        val result = manager.subscribe("user-1", TrackType.TRACK_TYPE_VIDEO, defaultDimension)
        assertTrue(result is Result.Success)
        coVerify { delegate.subscribe("user-1", TrackType.TRACK_TYPE_VIDEO, defaultDimension) }
    }

    @Test
    fun `subscribe does not delegate if already subscribed`() = runTest {
        coEvery {
            delegate.subscriptions()
        } returns mapOf("user-1" to TrackSubscriptionDetails("user-1", "user-1", TrackType.TRACK_TYPE_VIDEO))
        val result = manager.subscribe("user-1", TrackType.TRACK_TYPE_VIDEO, defaultDimension)
        assertTrue(result is Result.Success)
        coVerify(
            exactly = 0,
        ) { delegate.subscribe("user-1", TrackType.TRACK_TYPE_VIDEO, defaultDimension) }
    }

    @Test
    fun `unsubscribe delegates immediately`() = runTest {
        coEvery { delegate.unsubscribe("user-1", TrackType.TRACK_TYPE_VIDEO) } returns Result.Success(Unit)
        val result = manager.unsubscribe("user-1", TrackType.TRACK_TYPE_VIDEO)
        assertTrue(result is Result.Success)
        coVerify { delegate.unsubscribe("user-1", TrackType.TRACK_TYPE_VIDEO) }
    }

    @Test
    fun `unsubscribeDebounced schedules unsubscribe after 2 seconds`() = runTest {
        manager = DefaultDebouncedSubscriptionManager(testScope, delegate)
        coEvery {
            delegate.unsubscribe(listOf(Pair("user-1", TrackType.TRACK_TYPE_VIDEO)))
        } returns Result.Success(Unit)
        manager.unsubscribeDebounced("user-1", TrackType.TRACK_TYPE_VIDEO)
        // Should not call delegate yet
        coVerify(exactly = 0) { delegate.unsubscribe(any<List<Pair<String, TrackType>>>()) }
        val now = System.currentTimeMillis() + 2000
        (manager as DefaultDebouncedSubscriptionManager).processPendingUnsubscribes(now)
        // Now it should call delegate
        coVerify { delegate.unsubscribe(listOf("user-1" to TrackType.TRACK_TYPE_VIDEO)) }
    }

    @Test
    fun `unsubscribeDebounced batches multiple unsubscribes`() = runTest {
        coEvery { delegate.unsubscribe(any<List<Pair<String, TrackType>>>()) } returns Result.Success(Unit)
        manager.unsubscribeDebounced("user-1", TrackType.TRACK_TYPE_VIDEO)
        manager.unsubscribeDebounced("user-2", TrackType.TRACK_TYPE_AUDIO)
        val now = System.currentTimeMillis() + 2000
        (manager as DefaultDebouncedSubscriptionManager).processPendingUnsubscribes(now)
        coVerify {
            delegate.unsubscribe(
                listOf(
                    "user-1" to TrackType.TRACK_TYPE_VIDEO,
                    "user-2" to TrackType.TRACK_TYPE_AUDIO,
                ),
            )
        }
    }

    @Test
    fun `unsubscribeDebounced keeps subscription for at least 2 seconds`() = runTest {
        coEvery { delegate.unsubscribe(any<List<Pair<String, TrackType>>>()) } returns Result.Success(Unit)
        manager.unsubscribeDebounced("user-1", TrackType.TRACK_TYPE_VIDEO)
        // Should not call delegate yet
        val now = System.currentTimeMillis() + 1000
        (manager as DefaultDebouncedSubscriptionManager).processPendingUnsubscribes(now)
        coVerify(exactly = 0) { delegate.unsubscribe(any<List<Pair<String, TrackType>>>()) }
        // Now it should call delegate

        val now2 = System.currentTimeMillis() + 3000
        (manager as DefaultDebouncedSubscriptionManager).processPendingUnsubscribes(now2)
        coVerify { delegate.unsubscribe(listOf("user-1" to TrackType.TRACK_TYPE_VIDEO)) }
    }

    @Test
    fun `clear delegates to underlying manager`() = runTest {
        manager.clear()
        coVerify { delegate.clear() }
    }

    @Test
    fun `subscriptions delegates to underlying manager`() {
        val expected =
            mapOf(
                "user-1" to TrackSubscriptionDetails("user-1", "user-1", TrackType.TRACK_TYPE_VIDEO),
            )
        coEvery { delegate.subscriptions() } returns expected
        val result = manager.subscriptions()
        assertEquals(expected, result)
    }

    @Test
    fun `subscribe(List) delegates to underlying manager`() = runTest {
        val sessions = listOf(Triple("user-1", TrackType.TRACK_TYPE_VIDEO, defaultDimension))
        val expected =
            listOf(TrackSubscriptionDetails("user-1", "user-1", TrackType.TRACK_TYPE_VIDEO))
        coEvery { delegate.subscribe(sessions) } returns Result.Success(expected)
        val result = manager.subscribe(sessions)
        assertTrue(result is Result.Success)
        coVerify { delegate.subscribe(sessions) }
    }

    @Test
    fun `unsubscribe(List) delegates to underlying manager`() = runTest {
        val sessions = listOf("user-1" to TrackType.TRACK_TYPE_VIDEO)
        coEvery { delegate.unsubscribe(sessions) } returns Result.Success(Unit)
        val result = manager.unsubscribe(sessions)
        assertTrue(result is Result.Success)
        coVerify { delegate.unsubscribe(sessions) }
    }

    @Test
    fun `pulseJob is started after unsubscribeDebounced`() = runTest {
        assertNull(manager.pulsingJob())
        manager.unsubscribeDebounced("user-1", TrackType.TRACK_TYPE_VIDEO)
        assertNotNull(manager.pulsingJob())
    }

    @Test
    fun `unsubscribeDebounced then subscribe removes pending unsubscribe`() = runTest {
        // Schedule a debounced unsubscribe
        manager.unsubscribeDebounced("user-1", TrackType.TRACK_TYPE_VIDEO)
        // There should be a pending unsubscribe
        val internalManager = manager as DefaultDebouncedSubscriptionManager
        assertTrue(
            internalManager.pendingUnsubscribes().any {
                it.first == "user-1" && it.second == TrackType.TRACK_TYPE_VIDEO
            },
        )
        // Now subscribe, which should remove the pending unsubscribe
        coEvery { delegate.subscriptions() } returns emptyMap()
        coEvery {
            delegate.subscribe("user-1", TrackType.TRACK_TYPE_VIDEO, defaultDimension)
        } returns Result.Success(TrackSubscriptionDetails("user-1", "user-1", TrackType.TRACK_TYPE_VIDEO))
        manager.subscribe("user-1", TrackType.TRACK_TYPE_VIDEO, defaultDimension)
        // The pending unsubscribe should be removed
        assertTrue(
            internalManager.pendingUnsubscribes().none {
                it.first == "user-1" && it.second == TrackType.TRACK_TYPE_VIDEO
            },
        )
    }
}
