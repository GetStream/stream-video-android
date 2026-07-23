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

package io.getstream.video.android.core.call.components

import com.google.common.truth.Truth.assertThat
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.base.DispatcherRule
import io.getstream.video.android.core.events.GoAwayEvent
import io.getstream.video.android.core.events.SFUConnectedEvent
import io.getstream.video.android.core.events.VideoEventListener
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [CallEventManager] which owns the event pipeline for a [Call]:
 * the shared events flow, legacy subscriptions and dispatch of incoming events.
 */
class CallEventManagerTest {

    @get:Rule
    val dispatcherRule = DispatcherRule()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val call = mockk<Call>(relaxed = true).also {
        every { it.type } returns "default"
        every { it.id } returns "call-id"
        every { it.scope } returns testScope
    }

    private fun manager() = CallEventManager(call)

    @Test
    fun `subscribe without filter receives every fired event`() {
        val manager = manager()
        val received = mutableListOf<VideoEvent>()
        manager.subscribe { received.add(it) }

        val event = mockk<VideoEvent>(relaxed = true)
        manager.fireEvent(event)

        assertThat(received).containsExactly(event)
    }

    @Test
    fun `subscribeFor only receives matching event types`() {
        val manager = manager()
        val received = mutableListOf<VideoEvent>()
        manager.subscribeFor(GoAwayEvent::class.java) { received.add(it) }

        val matching = mockk<GoAwayEvent>(relaxed = true)
        val nonMatching = mockk<SFUConnectedEvent>(relaxed = true)

        manager.fireEvent(nonMatching)
        assertThat(received).isEmpty()

        manager.fireEvent(matching)
        assertThat(received).containsExactly(matching)
    }

    @Test
    fun `unsubscribe stops future delivery`() {
        val manager = manager()
        val received = mutableListOf<VideoEvent>()
        val listener = VideoEventListener<VideoEvent> { received.add(it) }
        val subscription = manager.subscribe(listener)

        manager.unsubscribe(subscription)
        manager.fireEvent(mockk(relaxed = true))

        assertThat(received).isEmpty()
    }

    @Test
    fun `disposed subscriptions do not receive events`() {
        val manager = manager()
        val received = mutableListOf<VideoEvent>()
        val subscription = manager.subscribe { received.add(it) }
        subscription.dispose()

        manager.fireEvent(mockk(relaxed = true))

        assertThat(received).isEmpty()
    }

    @Test
    fun `fireEvent emits to the shared events flow`() = runTest(testDispatcher) {
        val manager = manager()
        val event = mockk<VideoEvent>(relaxed = true)

        val emitted = mutableListOf<VideoEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.events.collect { emitted.add(it) }
        }

        manager.fireEvent(event)
        advanceUntilIdle()

        assertThat(emitted).contains(event)
    }

    @Test
    fun `handleEvent triggers migrate on GoAwayEvent`() = runTest(testDispatcher) {
        val manager = manager()

        manager.handleEvent(mockk<GoAwayEvent>(relaxed = true))
        advanceUntilIdle()

        coVerify(exactly = 1) { call.migrate() }
    }

    @Test
    fun `handleEvent ignores unrelated events`() = runTest(testDispatcher) {
        val manager = manager()

        manager.handleEvent(mockk<SFUConnectedEvent>(relaxed = true))
        advanceUntilIdle()

        coVerify(exactly = 0) { call.migrate() }
    }
}
