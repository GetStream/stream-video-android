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

package io.getstream.video.android.core.call.utils

import kotlin.math.abs

/**
 * The normaliser computes the percentage values or value of the provided value.
 */
internal object AudioValuePercentageNormaliser {

    /**
     * -50 is usually considered to be the background noise level - we register anything
     * above that.
     */
    private val valueRange: ClosedRange<Int> = IntRange(-50, 0)

    // / Compute the range between the min and max values
    internal val delta: Int = valueRange.endInclusive - valueRange.start

    fun normalise(decibelValue: Double): Float {
        return if (decibelValue < valueRange.start) {
            0.toFloat()
        } else if (decibelValue > valueRange.endInclusive) {
            1f
        } else {
            abs((decibelValue - valueRange.start) / delta).toFloat()
        }
    }
}
