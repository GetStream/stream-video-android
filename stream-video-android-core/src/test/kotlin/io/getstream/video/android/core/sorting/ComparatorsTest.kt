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
import io.getstream.video.android.core.model.VisibilityOnScreenState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.Test

class ComparatorsTest {

    private val scope = CoroutineScope(StandardTestDispatcher(TestCoroutineScheduler()))
    private val callActions = stubCallActions()

    @Test
    fun `combineComparators applies in declared order, first non-zero wins`() {
        val byA = Comparator<Pair<Int, Int>> { x, y -> x.first.compareTo(y.first) }
        val byB = Comparator<Pair<Int, Int>> { x, y -> x.second.compareTo(y.second) }

        val cmp = combineComparators(byA, byB)
        assertThat(cmp.compare(1 to 5, 2 to 3)).isLessThan(0) // A decides
        assertThat(cmp.compare(2 to 3, 1 to 5)).isGreaterThan(0)
        assertThat(cmp.compare(2 to 1, 2 to 5)).isLessThan(0) // A tied, B decides
        assertThat(cmp.compare(2 to 5, 2 to 5)).isEqualTo(0)
    }

    @Test
    fun `conditional with predicate-false returns 0 regardless of comparator`() {
        val alwaysMinusOne = Comparator<Int> { _, _ -> -1 }
        val never = conditional<Int>(predicate = { _, _ -> false }, comparator = alwaysMinusOne)
        assertThat(never.compare(1, 2)).isEqualTo(0)
        assertThat(never.compare(99, -99)).isEqualTo(0)
    }

    @Test
    fun `conditional with predicate-true delegates to inner comparator`() {
        val always = conditional<Int>(predicate = { _, _ -> true }, comparator = naturalOrder())
        assertThat(always.compare(1, 2)).isLessThan(0)
        assertThat(always.compare(2, 1)).isGreaterThan(0)
    }

    @Test
    fun `ifInvisible returns 0 for visible-visible pair`() {
        val a = participant(
            "a",
            scope,
            callActions,
            dominantSpeaker = true,
            visibility = VisibilityOnScreenState.VISIBLE,
        )
        val b = participant(
            "b",
            scope,
            callActions,
            dominantSpeaker = false,
            visibility = VisibilityOnScreenState.VISIBLE,
        )
        assertThat(ifInvisible(dominantSpeaker).compare(a, b)).isEqualTo(0)
        assertThat(ifInvisible(dominantSpeaker).compare(b, a)).isEqualTo(0)
    }

    @Test
    fun `ifInvisible delegates when at least one side is INVISIBLE`() {
        val visibleDominant = participant(
            "a",
            scope,
            callActions,
            dominantSpeaker = true,
            visibility = VisibilityOnScreenState.VISIBLE,
        )
        val invisible = participant(
            "b",
            scope,
            callActions,
            dominantSpeaker = false,
            visibility = VisibilityOnScreenState.INVISIBLE,
        )
        assertThat(ifInvisible(dominantSpeaker).compare(visibleDominant, invisible))
            .isEqualTo(-1)
        assertThat(ifInvisible(dominantSpeaker).compare(invisible, visibleDominant))
            .isEqualTo(1)
    }

    @Test
    fun `ifInvisible treats UNKNOWN as visible (does NOT fire comparator)`() {
        val unknownDominant = participant(
            "a",
            scope,
            callActions,
            dominantSpeaker = true,
            visibility = VisibilityOnScreenState.UNKNOWN,
        )
        val unknownOther = participant(
            "b",
            scope,
            callActions,
            dominantSpeaker = false,
            visibility = VisibilityOnScreenState.UNKNOWN,
        )
        assertThat(ifInvisible(dominantSpeaker).compare(unknownDominant, unknownOther))
            .isEqualTo(0)
    }

    @Test
    fun `ifInvisibleOrUnknown returns 0 only when both are VISIBLE`() {
        val visibleDominant = participant(
            "a",
            scope,
            callActions,
            dominantSpeaker = true,
            visibility = VisibilityOnScreenState.VISIBLE,
        )
        val visibleOther = participant(
            "b",
            scope,
            callActions,
            dominantSpeaker = false,
            visibility = VisibilityOnScreenState.VISIBLE,
        )
        assertThat(ifInvisibleOrUnknown(dominantSpeaker).compare(visibleDominant, visibleOther))
            .isEqualTo(0)
    }

    @Test
    fun `ifInvisibleOrUnknown delegates when at least one side is INVISIBLE`() {
        val visible = participant(
            "a",
            scope,
            callActions,
            dominantSpeaker = true,
            visibility = VisibilityOnScreenState.VISIBLE,
        )
        val invisible = participant(
            "b",
            scope,
            callActions,
            dominantSpeaker = false,
            visibility = VisibilityOnScreenState.INVISIBLE,
        )
        assertThat(ifInvisibleOrUnknown(dominantSpeaker).compare(visible, invisible))
            .isEqualTo(-1)
    }

    @Test
    fun `ifInvisibleOrUnknown delegates when at least one side is UNKNOWN`() {
        val visible = participant(
            "a",
            scope,
            callActions,
            dominantSpeaker = true,
            visibility = VisibilityOnScreenState.VISIBLE,
        )
        val unknown = participant(
            "b",
            scope,
            callActions,
            dominantSpeaker = false,
            visibility = VisibilityOnScreenState.UNKNOWN,
        )
        assertThat(ifInvisibleOrUnknown(dominantSpeaker).compare(visible, unknown))
            .isEqualTo(-1)
    }

    @Test
    fun `ifInvisibleOrUnknown handles UNKNOWN-UNKNOWN by firing comparator`() {
        val a = participant(
            "a",
            scope,
            callActions,
            dominantSpeaker = true,
            visibility = VisibilityOnScreenState.UNKNOWN,
        )
        val b = participant(
            "b",
            scope,
            callActions,
            dominantSpeaker = false,
            visibility = VisibilityOnScreenState.UNKNOWN,
        )
        assertThat(ifInvisibleOrUnknown(dominantSpeaker).compare(a, b)).isEqualTo(-1)
    }
}
