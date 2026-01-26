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

package io.getstream.video.android.core.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

private const val FREEZE_AT_TOP_TIME_MS = 50L

/**
 * Helper class that will gradually ram-up from 0 to the value supplied through [rampToValue] and
 * then will return gradually back to 0.
 */
internal class RampValueUpAndDownHelper(private val durationMillis: Long = 250L) {

    private val _value: MutableStateFlow<Float> = MutableStateFlow(0f)
    private var currentJob: Job? = null
    private var totalSteps = 0

    val currentLevel = _value.asStateFlow()

    /***
     * Sending a value will trigger update to the [currentLevel] StateFlow. The value will gradually
     * go from 0 to [targetValue] and then back to 0 over the duration time defined in [durationMillis].
     * Calling the function again while the job is running will only have an effect if the
     * new value is bigger than the previous one - in that case the output values will grow faster
     * to reach the new [targetValue] and go back again to 0.
     */
    suspend fun rampToValue(targetValue: Float) {
        // Check if we are not animating already. If yes then only change
        // change the animation if the new value is bigger. Otherwise the current
        // value will just ramp down after [durationMillis]
        if (totalSteps != 0) {
            if (targetValue == 0f || targetValue < currentLevel.value) {
                return
            }
        }

        currentJob?.cancel()
        val startValue = _value.value

        currentJob = CoroutineScope(coroutineContext).launch {
            totalSteps = 0
            rampValueInternal(startValue, targetValue)
            // stay a short while at the top value
            delay(FREEZE_AT_TOP_TIME_MS)
            rampValueInternal(targetValue, 0f) // Ramp down to zero
            // make sure we end up exactly at 0 again
            _value.value = 0f
        }
    }

    private suspend fun rampValueInternal(startValue: Float, targetValue: Float) {
        // Number of steps for the ramp
        val steps = 50
        val stepDuration = (durationMillis - FREEZE_AT_TOP_TIME_MS) / steps / 2

        for (i in 0..steps) {
            totalSteps += 1
            val value = startValue + (i * (targetValue - startValue) / steps)
            _value.value = value
            delay(stepDuration)
        }
    }
}
