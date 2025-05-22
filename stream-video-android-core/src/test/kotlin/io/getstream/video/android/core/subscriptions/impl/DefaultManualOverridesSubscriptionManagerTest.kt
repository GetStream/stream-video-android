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
import io.getstream.video.android.core.subscriptions.api.DebouncePolicy
import io.getstream.video.android.core.subscriptions.api.ViewportBasedSubscriptionManager
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
class DefaultManualOverridesSubscriptionManagerTest {
    private lateinit var delegate: ViewportBasedSubscriptionManager
    private lateinit var manager: DefaultManualOverridesSubscriptionManager
    private lateinit var testScope: TestScope
    private val defaultDimension = VideoDimension(width = 1280, height = 720)

    @Before
    fun setup() {
        delegate = mockk(relaxed = true)
        testScope = TestScope()
        manager = DefaultManualOverridesSubscriptionManager(delegate)
    }

    @Test
    fun `updateViewport delegates with manual overrides`() = runTest {
        coEvery { delegate.updateViewport(any(), any(), any(), any(), any()) } returns Unit
        manager.setVisibilityOverride("user-1", false)
        manager.setDimensionOverride("user-1", defaultDimension)
        manager.updateViewport(
            "viewport-1",
            "user-1",
            TrackType.TRACK_TYPE_VIDEO,
            true,
            VideoDimension(640, 480),
        )
        coVerify {
            delegate.updateViewport(
                "viewport-1",
                "user-1",
                TrackType.TRACK_TYPE_VIDEO,
                false,
                defaultDimension,
            )
        }
    }

    @Test
    fun `remove overrides falls back to provided values`() = runTest {
        coEvery { delegate.updateViewport(any(), any(), any(), any(), any()) } returns Unit
        manager.setVisibilityOverride("user-1", false)
        manager.setDimensionOverride("user-1", defaultDimension)
        manager.removeVisibilityOverride("user-1")
        manager.removeDimensionOverride("user-1")
        manager.updateViewport(
            "viewport-1",
            "user-1",
            TrackType.TRACK_TYPE_VIDEO,
            true,
            defaultDimension,
        )
        coVerify {
            delegate.updateViewport(
                "viewport-1",
                "user-1",
                TrackType.TRACK_TYPE_VIDEO,
                true,
                defaultDimension,
            )
        }
    }

    @Test
    fun `clearOverrides removes all overrides`() = runTest {
        coEvery { delegate.updateViewport(any(), any(), any(), any(), any()) } returns Unit
        manager.setVisibilityOverride("user-1", false)
        manager.setDimensionOverride("user-1", defaultDimension)
        manager.clearOverrides("user-1")
        manager.updateViewport(
            "viewport-1",
            "user-1",
            TrackType.TRACK_TYPE_VIDEO,
            true,
            defaultDimension,
        )
        coVerify {
            delegate.updateViewport(
                "viewport-1",
                "user-1",
                TrackType.TRACK_TYPE_VIDEO,
                true,
                defaultDimension,
            )
        }
    }

    @Test
    fun `setDebouncePolicy delegates to underlying manager`() {
        val policy = mockk<DebouncePolicy>()
        manager.setDebouncePolicy(policy)
        coVerify { delegate.setDebouncePolicy(policy) }
    }

    @Test
    fun `unsubscribeDebounced delegates to underlying manager`() {
        manager.unsubscribeDebounced("user-1", TrackType.TRACK_TYPE_VIDEO)
        coVerify { delegate.unsubscribeDebounced("user-1", TrackType.TRACK_TYPE_VIDEO) }
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
    fun `subscribe delegates to underlying manager`() = runTest {
        val dimension = defaultDimension
        coEvery {
            delegate.subscribe("user-1", TrackType.TRACK_TYPE_VIDEO, dimension)
        } returns Result.Success(TrackSubscriptionDetails("user-1", "user-1", TrackType.TRACK_TYPE_VIDEO, dimension = dimension))
        val result = manager.subscribe("user-1", TrackType.TRACK_TYPE_VIDEO, dimension)
        assertTrue(result is Result.Success)
        coVerify { delegate.subscribe("user-1", TrackType.TRACK_TYPE_VIDEO, dimension) }
    }

    @Test
    fun `unsubscribe delegates to underlying manager`() = runTest {
        coEvery { delegate.unsubscribe("user-1", TrackType.TRACK_TYPE_VIDEO) } returns Result.Success(Unit)
        val result = manager.unsubscribe("user-1", TrackType.TRACK_TYPE_VIDEO)
        assertTrue(result is Result.Success)
        coVerify { delegate.unsubscribe("user-1", TrackType.TRACK_TYPE_VIDEO) }
    }

    @Test
    fun `subscribe(List) delegates to underlying manager`() = runTest {
        val sessions = listOf(Triple("user-1", TrackType.TRACK_TYPE_VIDEO, defaultDimension))
        val expected =
            listOf(
                TrackSubscriptionDetails(
                    "user-1",
                    "user-1",
                    TrackType.TRACK_TYPE_VIDEO,
                    dimension = defaultDimension,
                ),
            )
        coEvery { delegate.subscribe(sessions) } returns Result.Success(expected)
        val result = manager.subscribe(sessions)
        assertTrue(result is Result.Success)
        coVerify { delegate.subscribe(sessions) }
    }

    @Test
    fun `subscribe batch delegates to underlying manager with dimension`() = runTest {
        val dimension = VideoDimension(320, 240)
        val sessions = listOf(Triple("user-1", TrackType.TRACK_TYPE_VIDEO, dimension))
        val expected =
            listOf(
                TrackSubscriptionDetails(
                    "user-1",
                    "user-1",
                    TrackType.TRACK_TYPE_VIDEO,
                    dimension = dimension,
                ),
            )
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
    fun `clear delegates to underlying manager`() {
        manager.clear()
        coVerify { delegate.clear() }
    }

    @Test
    fun `global visibility override takes precedence over session and provided value`() = runTest {
        coEvery { delegate.updateViewport(any(), any(), any(), any(), any()) } returns Unit
        manager.setVisibilityOverride("user-1", false)
        manager.setGlobalVisibilityOverride(true)
        manager.updateViewport(
            "viewport-1",
            "user-1",
            TrackType.TRACK_TYPE_VIDEO,
            false,
            defaultDimension,
        )
        coVerify {
            delegate.updateViewport(
                "viewport-1",
                "user-1",
                TrackType.TRACK_TYPE_VIDEO,
                true,
                defaultDimension,
            )
        }
    }

    @Test
    fun `global dimension override takes precedence over session and provided value`() = runTest {
        coEvery { delegate.updateViewport(any(), any(), any(), any(), any()) } returns Unit
        manager.setDimensionOverride("user-1", VideoDimension(320, 240))
        manager.setGlobalDimensionOverride(defaultDimension)
        manager.updateViewport(
            "viewport-1",
            "user-1",
            TrackType.TRACK_TYPE_VIDEO,
            true,
            defaultDimension,
        )
        coVerify {
            delegate.updateViewport(
                "viewport-1",
                "user-1",
                TrackType.TRACK_TYPE_VIDEO,
                true,
                defaultDimension,
            )
        }
    }

    @Test
    fun `clearGlobalOverrides falls back to session or provided value`() = runTest {
        coEvery { delegate.updateViewport(any(), any(), any(), any(), any()) } returns Unit
        manager.setGlobalVisibilityOverride(false)
        manager.setGlobalDimensionOverride(defaultDimension)
        manager.clearGlobalOverrides()
        manager.setVisibilityOverride("user-1", true)
        manager.setDimensionOverride("user-1", VideoDimension(320, 240))
        manager.updateViewport(
            "viewport-1",
            "user-1",
            TrackType.TRACK_TYPE_VIDEO,
            false,
            defaultDimension,
        )
        coVerify {
            delegate.updateViewport(
                "viewport-1",
                "user-1",
                TrackType.TRACK_TYPE_VIDEO,
                true,
                VideoDimension(320, 240),
            )
        }
    }
}
