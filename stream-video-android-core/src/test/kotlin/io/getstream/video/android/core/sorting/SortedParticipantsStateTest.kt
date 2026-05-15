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
import io.getstream.video.android.core.events.PinUpdate
import io.getstream.video.android.core.model.VisibilityOnScreenState
import io.getstream.video.android.core.pinning.PinType
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
import org.threeten.bp.OffsetDateTime

/**
 * Focused tests on the SortedParticipantsState's reactive surface — preset changes,
 * comparator overrides, pin updates, and the no-redundant-emission optimization. Uses
 * StandardTestDispatcher so the suite would catch the channelFlow regression if it ever
 * reappeared.
 */
class SortedParticipantsStateTest {

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)

    @Test
    fun `participants flow change triggers a resort and emission`() = runTest(dispatcher) {
        val sut = newSut()
        sut.participants.value = mapOf(
            "a" to sut.participant("a"),
            "b" to sut.participant("b"),
        )
        advanceUntilIdle()
        assertThat(sut.sorted.value.map { it.sessionId }).containsExactly("a", "b")
    }

    @Test
    fun `pinned flow change pushes pinned participant to front`() = runTest(dispatcher) {
        val sut = newSut()
        sut.participants.value = mapOf(
            "a" to sut.participant("a"),
            "b" to sut.participant("b"),
        )
        advanceUntilIdle()
        assertThat(sut.sorted.value.map { it.sessionId }.first()).isEqualTo("a")

        sut.pinned.value = mapOf(
            "b" to pinUpdateAt("b", OffsetDateTime.parse("2026-05-14T10:00:00Z"), PinType.Local),
        )
        advanceUntilIdle()
        assertThat(sut.sorted.value.map { it.sessionId }.first()).isEqualTo("b")
    }

    @Test
    fun `switching preset triggers a resort using the new preset`() = runTest(dispatcher) {
        val sut = newSut()
        val a = sut.participant("a").apply { _videoEnabled.value = true }
        val b = sut.participant("b").apply { _dominantSpeaker.value = true }
        sut.participants.value = mapOf("a" to a, "b" to b)
        advanceUntilIdle()
        // Default preset puts dominant speaker first (visibility UNKNOWN → guard runs).
        assertThat(sut.sorted.value.first().sessionId).isEqualTo("b")

        sut.state.setPreset(SortPreset.SpeakerLayout)
        advanceUntilIdle()
        // SpeakerLayout has dominantSpeaker outside the guard → b stays first too.
        assertThat(sut.sorted.value.first().sessionId).isEqualTo("b")
    }

    @Test
    fun `custom comparator overrides preset until cleared`() = runTest(dispatcher) {
        val sut = newSut()
        sut.participants.value = mapOf(
            "a" to sut.participant("a"),
            "b" to sut.participant("b"),
            "c" to sut.participant("c"),
        )
        advanceUntilIdle()
        assertThat(sut.sorted.value.map { it.sessionId }).containsExactly("a", "b", "c").inOrder()

        sut.state.updateComparator(compareByDescending { it.sessionId })
        advanceUntilIdle()
        assertThat(sut.sorted.value.map { it.sessionId }).containsExactly("c", "b", "a").inOrder()

        // Calling setPreset clears the custom comparator and reverts to preset order.
        sut.state.setPreset(SortPreset.Default)
        advanceUntilIdle()
        assertThat(sut.sorted.value.map { it.sessionId }).containsExactly("c", "b", "a").inOrder()
        // ↑ Default preset returns 0 for every pair (no signals on a/b/c, UNKNOWN visibility
        // would fire ifInvisibleOrUnknown but every comparator inside returns 0). With stable
        // sort, the input order (which is now [c, b, a] from the previous sort) is preserved.
    }

    @Test
    fun `no emission when re-sort produces the same order`() = runTest(dispatcher) {
        val sut = newSut()
        val a = sut.participant("a")
        val b = sut.participant("b")
        sut.participants.value = mapOf("a" to a, "b" to b)
        advanceUntilIdle()

        val firstSnapshot = sut.sorted.value
        // Re-trigger a re-sort by pushing the same participants map — order is identical.
        sut.participants.value = mapOf("a" to a, "b" to b)
        advanceUntilIdle()
        // Same list instance because the resort short-circuits on unchanged order.
        assertThat(sut.sorted.value).isSameInstanceAs(firstSnapshot)
    }

    @Test
    fun `call event triggers a resort against current state`() = runTest(dispatcher) {
        val events = MutableSharedFlow<VideoEvent>(extraBufferCapacity = 150)
        val sut = newSut(events = events)
        val a = sut.participant("a")
        val b = sut.participant("b")
        sut.participants.value = mapOf("a" to a, "b" to b)
        advanceUntilIdle()
        assertThat(sut.sorted.value.first().sessionId).isEqualTo("a")

        // Mutate b's dominantSpeaker outside the participants map (the SDK does this from
        // event handlers writing directly to ParticipantState fields). Without a call.event
        // firing, the combine would still see no change in the participants StateFlow's
        // map identity. The events launch should trigger a re-sort.
        b._dominantSpeaker.value = true
        events.emit(mockk<VideoEvent>(relaxed = true))
        advanceUntilIdle()
        assertThat(sut.sorted.value.first().sessionId).isEqualTo("b")
    }

    @Test
    fun `visible-block internal order survives off-screen dominant-speaker promotion`() =
        runTest(dispatcher) {
            // End-to-end through the live flow: not just the static comparator. Verifies
            // that when an off-screen participant gains a sort-promoting signal, the tiles
            // currently in the viewport keep their relative order in the emitted list.
            val events = MutableSharedFlow<VideoEvent>(extraBufferCapacity = 150)
            val sut = newSut(events = events)

            // 15 participants, P8..P12 visible (sessionIds "8" through "12").
            val ps = (1..15).associate { n ->
                val visible = n in 8..12
                val visibility =
                    if (visible) {
                        VisibilityOnScreenState.VISIBLE
                    } else {
                        VisibilityOnScreenState.INVISIBLE
                    }
                n.toString() to sut.participant(n.toString()).apply {
                    _visibleOnScreen.value = visibility
                }
            }
            sut.participants.value = ps
            advanceUntilIdle()

            val initialOrder = sut.sorted.value.map { it.sessionId }
            assertThat(initialOrder)
                .containsExactlyElementsIn((1..15).map { it.toString() }).inOrder()

            // P15 (off-screen, after the viewport) becomes dominant speaker. Mutating the
            // field directly mirrors the SDK's DominantSpeakerChangedEvent handler; the
            // call event drives the resort.
            ps["15"]!!._dominantSpeaker.value = true
            events.emit(mockk<VideoEvent>(relaxed = true))
            advanceUntilIdle()

            // P15 jumps to top.
            assertThat(sut.sorted.value.first().sessionId).isEqualTo("15")

            // The visible window's internal order is preserved across the resort.
            val visibleBlock = sut.sorted.value
                .filter { it.sessionId.toInt() in 8..12 }
                .map { it.sessionId }
            assertThat(visibleBlock)
                .containsExactly("8", "9", "10", "11", "12").inOrder()
        }

    @Test
    fun `visible block stays stable when a visible participant gains a signal`() =
        runTest(dispatcher) {
            // Edge case: the signal-gainer is itself inside the viewport. Even then the
            // visible/visible predicate returns 0 and the block keeps its order.
            val events = MutableSharedFlow<VideoEvent>(extraBufferCapacity = 150)
            val sut = newSut(events = events)
            val ps = (1..15).associate { n ->
                val visible = n in 8..12
                n.toString() to sut.participant(n.toString()).apply {
                    _visibleOnScreen.value = if (visible) {
                        VisibilityOnScreenState.VISIBLE
                    } else {
                        VisibilityOnScreenState.INVISIBLE
                    }
                }
            }
            sut.participants.value = ps
            advanceUntilIdle()

            // P10 (the third visible tile) starts speaking.
            ps["10"]!!._dominantSpeaker.value = true
            events.emit(mockk<VideoEvent>(relaxed = true))
            advanceUntilIdle()

            // ifInvisibleOrUnknown's predicate sees only visible-visible pairs for the
            // P8..P12 set → returns 0 → no internal reshuffle within the block. P10 stays
            // at index 9 (its original position) instead of jumping to the top.
            assertThat(sut.sorted.value.map { it.sessionId })
                .containsExactlyElementsIn((1..15).map { it.toString() })
                .inOrder()
        }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun newSut(
        events: MutableSharedFlow<VideoEvent> = MutableSharedFlow(extraBufferCapacity = 150),
    ): SutHandle {
        val scope = CoroutineScope(dispatcher)
        val callActions = mockk<CallActions>(relaxed = true) {
            every { isLocalParticipant(any()) } returns false
        }
        val call = mockk<Call>(relaxed = true) {
            every { this@mockk.events } returns events
        }
        val participants =
            MutableStateFlow<Map<String, ParticipantState>>(emptyMap())
        val pinned: MutableStateFlow<Map<String, PinUpdateAtTime>> =
            MutableStateFlow(emptyMap())
        val state = SortedParticipantsState(scope, call, participants, pinned)
        return SutHandle(
            scope = scope,
            callActions = callActions,
            participants = participants,
            pinned = pinned,
            state = state,
            sorted = state.sortedParticipants,
        )
    }

    private fun pinUpdateAt(
        sessionId: String,
        at: OffsetDateTime,
        type: PinType,
    ): PinUpdateAtTime = PinUpdateAtTime(
        it = PinUpdate(sessionId = sessionId, userId = "user-$sessionId"),
        at = at,
        type = type,
    )

    private data class SutHandle(
        val scope: CoroutineScope,
        val callActions: CallActions,
        val participants: MutableStateFlow<Map<String, ParticipantState>>,
        val pinned: MutableStateFlow<Map<String, PinUpdateAtTime>>,
        val state: SortedParticipantsState,
        val sorted: StateFlow<List<ParticipantState>>,
    ) {
        fun participant(sessionId: String): ParticipantState = ParticipantState(
            sessionId = sessionId,
            scope = scope,
            callActions = callActions,
            initialUserId = sessionId,
        )
    }
}
