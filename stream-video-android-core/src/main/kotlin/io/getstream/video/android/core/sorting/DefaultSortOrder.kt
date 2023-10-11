/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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
import io.getstream.video.android.core.utils.combineComparators

/**
 * Default comparator for the participants.
 * Returns a function that takes a set of the pinned participants before starting the sorting.
 */
val defaultComparator: (pinned: Set<String>) -> Comparator<ParticipantState> = { pinned ->
    combineComparators(
        onlyIfInvisibleOrUnknown(
            { it.userId.value },
            { it.joinedAt.value },
            { it.audioEnabled.value },
            { it.videoEnabled.value },
            { it.dominantSpeaker.value },
        ),
        compareBy(
            { it.screenSharingEnabled.value },
            { pinned.contains(it.sessionId) },
        ),
    )
}

/**
 * Conditional comparator for visibility.
 */
private fun onlyIfInvisibleOrUnknown(
    vararg selectors: (ParticipantState) -> Comparable<*>?,
): Comparator<ParticipantState> {
    return Comparator { p1, p2 ->
        if (p1.visibleOnScreen.value == VisibilityOnScreenState.INVISIBLE ||
            p1.visibleOnScreen.value == VisibilityOnScreenState.UNKNOWN ||
            p2.visibleOnScreen.value == VisibilityOnScreenState.INVISIBLE ||
            p2.visibleOnScreen.value == VisibilityOnScreenState.UNKNOWN
        ) {
            var comparisonResult = 0
            for (selector in selectors) {
                val valueToCompare1 = selector(p1)
                val valueToCompare2 = selector(p2)
                val diff = compareValues(valueToCompare1, valueToCompare2)
                if (diff != 0) {
                    comparisonResult = diff
                    break
                }
            }
            comparisonResult
        } else {
            0
        }
    }
}
