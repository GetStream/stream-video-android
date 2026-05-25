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

import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.model.VisibilityOnScreenState

/**
 * Combines multiple comparators in priority order. The first comparator that returns a
 * non-zero result determines the ordering; subsequent comparators are skipped for that
 * pair. Mirrors `combineComparators` from stream-video-js.
 */
public fun <T> combineComparators(vararg comparators: Comparator<T>): Comparator<T> =
    Comparator { a, b ->
        for (comparator in comparators) {
            val result = comparator.compare(a, b)
            if (result != 0) return@Comparator result
        }
        0
    }

/**
 * Returns a comparator that delegates to [comparator] only when [predicate] returns true
 * for the pair, otherwise returns 0 (preserving order). Mirrors `conditional` from
 * stream-video-js.
 */
public fun <T> conditional(
    predicate: (T, T) -> Boolean,
    comparator: Comparator<T>,
): Comparator<T> = Comparator { a, b ->
    if (predicate(a, b)) comparator.compare(a, b) else 0
}

/**
 * Wraps [comparator] so it only applies when at least one participant in a pair has
 * visibility state [VisibilityOnScreenState.INVISIBLE]. When both are
 * [VisibilityOnScreenState.VISIBLE] (or [VisibilityOnScreenState.UNKNOWN]) the pair is
 * left in place (returns 0). This is the speaker-layout / default-grid stability
 * guarantee.
 */
public fun ifInvisible(
    comparator: Comparator<ParticipantState>,
): Comparator<ParticipantState> = conditional(
    predicate = { a, b ->
        a.visibleOnScreen.value == VisibilityOnScreenState.INVISIBLE ||
            b.visibleOnScreen.value == VisibilityOnScreenState.INVISIBLE
    },
    comparator = comparator,
)

/**
 * Wraps [comparator] so it applies when at least one participant has visibility state
 * [VisibilityOnScreenState.INVISIBLE] or [VisibilityOnScreenState.UNKNOWN]. Stricter
 * than [ifInvisible] — useful for continuous-scroll grids where off-screen tiles may
 * report UNKNOWN before they ever render. Visible/visible pairs return 0.
 */
public fun ifInvisibleOrUnknown(
    comparator: Comparator<ParticipantState>,
): Comparator<ParticipantState> = conditional(
    predicate = { a, b ->
        a.visibleOnScreen.value != VisibilityOnScreenState.VISIBLE ||
            b.visibleOnScreen.value != VisibilityOnScreenState.VISIBLE
    },
    comparator = comparator,
)
