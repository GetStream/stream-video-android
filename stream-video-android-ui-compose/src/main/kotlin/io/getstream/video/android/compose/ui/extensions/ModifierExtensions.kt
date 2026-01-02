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

package io.getstream.video.android.compose.ui.extensions

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Add padding to the modifier based on the index and its relative position in the list.
 *
 *
 * If [first] equal to [index] then this boils down to:
 * ```
 * modifier.padding(start = value)
 * ```
 * If [last] equals to [index] then this is
 * ```
 * modifier.padding(end = value)
 * ```
 * Otherwise the modifier itself is returned.
 */
internal fun Modifier.startOrEndPadding(
    value: Dp,
    index: Int,
    first: Int = 0,
    last: Int,
): Modifier = when (index) {
    first -> this.padding(start = value)
    last -> this.padding(end = value)
    else -> this
}

/**
 * Add padding to the modifier based on the index and its relative position in the list.
 *
 *
 * If [first] equal to [index] then this boils down to:
 * ```
 * modifier.padding(top = value)
 * ```
 * If [last] equals to [index] then this is
 * ```
 * modifier.padding(bottom = value)
 * ```
 * Otherwise the modifier itself is returned.
 */
internal fun Modifier.topOrBottomPadding(
    value: Dp,
    index: Int,
    first: Int = 0,
    last: Int,
): Modifier = when (index) {
    first -> this.padding(top = value)
    last -> this.padding(bottom = value)
    else -> this
}
