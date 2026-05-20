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

package io.getstream.video.android.core.sorting

import com.google.common.truth.Truth.assertThat
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallActions
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.pinning.PinUpdateAtTime
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Regression test for the SortedParticipantsState channelFlow bug.
 *
 * Under StandardTestDispatcher (and any non-Unconfined dispatcher in production), the
 * existing channelFlow-based SortedParticipantsState closes its channel before the
 * launched producers can trySend. As a result, sortedParticipants never emits in
 * production — the OrientationVideoRenderer's `if (size > 6) sortedParticipants else
 * participants` always falls back to the unsorted participants map.
 *
 * The existing tests in CallStateTest pass because the global DispatcherRule installs
 * UnconfinedTestDispatcher, which runs launched coroutines inline up to first suspension
 * — masking the bug entirely.
 *
 * This test isolates SortedParticipantsState with a StandardTestDispatcher to surface the
 * real production behavior. It is EXPECTED TO FAIL on the current develop and to pass
 * after migrating SortedParticipantsState to a StateFlow-backed implementation.
 */
class SortedParticipantsChannelFlowRegressionTest {

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)

    @Test
    fun `sortedParticipants emits updates under StandardTestDispatcher (current bug fails this)`() =
        runTest(dispatcher) {
            val scope = CoroutineScope(dispatcher)
            val callActions = mockk<CallActions>(relaxed = true) {
                every { isLocalParticipant(any()) } returns false
            }
            val call = mockk<Call>(relaxed = true) {
                every { events } returns MutableSharedFlow<VideoEvent>(extraBufferCapacity = 150)
            }
            val participants =
                MutableStateFlow<Map<String, ParticipantState>>(emptyMap())
            val pinned: StateFlow<Map<String, PinUpdateAtTime>> =
                MutableStateFlow(emptyMap())

            val sut = SortedParticipantsState(scope, call, participants, pinned)

            val sortedFlow = sut.sortedParticipants

            advanceUntilIdle()
            assertThat(sortedFlow.value).isEmpty()

            participants.value = mapOf(
                "p1" to ParticipantState(
                    sessionId = "p1",
                    scope = scope,
                    callActions = callActions,
                    initialUserId = "u1",
                ),
                "p2" to ParticipantState(
                    sessionId = "p2",
                    scope = scope,
                    callActions = callActions,
                    initialUserId = "u2",
                ),
            )
            advanceUntilIdle()

            // BUG: under StandardTestDispatcher, channelFlow closes the channel before
            // the launched producers trySend, so nothing reaches the collector and the
            // sorted list stays empty. After the StateFlow migration, this becomes [p1, p2].
            assertThat(sortedFlow.value).hasSize(2)
            assertThat(sortedFlow.value.map { it.sessionId }).containsExactly("p1", "p2")
        }
}
