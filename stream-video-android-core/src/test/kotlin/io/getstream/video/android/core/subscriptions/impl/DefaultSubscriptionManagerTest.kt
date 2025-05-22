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
import io.getstream.video.android.core.api.SignalServerService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultSubscriptionManagerTest {
    private lateinit var signalingService: SignalServerService
    private lateinit var manager: DefaultSubscriptionManager
    private val sessionId = "session-1"

    @Before
    fun setup() {
        signalingService = mockk(relaxed = true)
        manager = DefaultSubscriptionManager(sessionId, signalingService)
    }

    private val defaultDimension = VideoDimension(width = 1280, height = 720)

    @Test
    fun `subscribe adds subscription and syncs`() = runTest {
        coEvery { signalingService.updateSubscriptions(any()) } returns mockk(relaxed = true)
        val result = manager.subscribe("user-1", TrackType.TRACK_TYPE_VIDEO, defaultDimension)
        assertTrue(result is Result.Success)
        assertEquals(1, manager.subscriptions().size)
        coVerify { signalingService.updateSubscriptions(any()) }
    }

    @Test
    fun `unsubscribe removes subscription and syncs`() = runTest {
        coEvery { signalingService.updateSubscriptions(any()) } returns mockk(relaxed = true) {
            every { error } returns null
        }
        manager.subscribe("user-1", TrackType.TRACK_TYPE_VIDEO, defaultDimension)
        val result = manager.unsubscribe("user-1", TrackType.TRACK_TYPE_VIDEO)
        assertTrue(result is Result.Success)
        assertEquals(0, manager.subscriptions().size)
        coVerify(exactly = 2) { signalingService.updateSubscriptions(any()) }
    }

    @Test
    fun `subscribe multiple sessions`() = runTest {
        coEvery { signalingService.updateSubscriptions(any()) } returns mockk(relaxed = true)
        val sessions = listOf(
            Triple("user-1", TrackType.TRACK_TYPE_VIDEO, defaultDimension),
            Triple("user-2", TrackType.TRACK_TYPE_AUDIO, defaultDimension),
        )
        val result = manager.subscribe(sessions)
        assertTrue(result is Result.Success)
        assertEquals(2, manager.subscriptions().size)
        coVerify { signalingService.updateSubscriptions(any()) }
    }

    @Test
    fun `unsubscribe multiple sessions`() = runTest {
        coEvery { signalingService.updateSubscriptions(any()) } returns mockk(relaxed = true) {
            every { error } returns null
        }
        val sessions = listOf(
            Triple("user-1", TrackType.TRACK_TYPE_VIDEO, defaultDimension),
            Triple("user-2", TrackType.TRACK_TYPE_AUDIO, defaultDimension),
        )
        manager.subscribe(sessions)
        val unsubSessions = listOf(
            Pair("user-1", TrackType.TRACK_TYPE_VIDEO),
            Pair("user-2", TrackType.TRACK_TYPE_AUDIO),
        )
        val result = manager.unsubscribe(unsubSessions)
        assertTrue(result is Result.Success)
        assertEquals(0, manager.subscriptions().size)
        coVerify(exactly = 2) { signalingService.updateSubscriptions(any()) }
    }

    @Test
    fun `clear removes all subscriptions`() = runTest {
        coEvery { signalingService.updateSubscriptions(any()) } returns mockk(relaxed = true)
        manager.subscribe("user-1", TrackType.TRACK_TYPE_VIDEO, defaultDimension)
        manager.subscribe("user-2", TrackType.TRACK_TYPE_AUDIO, defaultDimension)
        manager.clear()
        assertEquals(0, manager.subscriptions().size)
    }
}
