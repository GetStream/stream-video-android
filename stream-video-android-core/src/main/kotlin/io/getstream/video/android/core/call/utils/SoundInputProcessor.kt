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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * The average decibel value is calculated within the last X milliseconds defined
 * in this constant.
 */
private const val SAMPLING_TIME_MS = 600

/**
 * Use to detect if the volume of a sound sample reaches a specific decibel value threshold.
 * @param thresholdInDecibels - the threshold value in decibels. If the average decibel value goes
 * beyond this level then the [thresholdCrossedCallback] is invoked.
 * @param thresholdCrossedCallback - invoked when the threshold is met
 */
internal class SoundInputProcessor(
    private val thresholdInDecibels: Int = -45,
    val thresholdCrossedCallback: () -> Unit,
) {

    private val decibelSamples = LinkedHashMap<Long, Double>()

    private val _currentAudioLevels = MutableStateFlow(0f)
    val currentAudioLevel = _currentAudioLevels

    /**
     * Forward the input audio data from the microphone to this function.
     * Only 16bit PCM audio format is accepted.
     */
    fun processSoundInput(pcmByteArray: ByteArray) {
        val decibels = calculateAverageDecibelPower(pcmByteArray)

        val normalisedOutput = AudioValuePercentageNormaliser.normalise(decibels)
        _currentAudioLevels.value = normalisedOutput

        if (!decibels.isInfinite()) {
            val average = calculateDecibelAverage(decibels)
            if (average >= thresholdInDecibels) {
                thresholdCrossedCallback.invoke()
                // we can clear the average values to start again
                decibelSamples.clear()
            }
        }
    }

    private fun calculateAverageDecibelPower(soundData: ByteArray): Double {
        // For code clarity the following code expects the pcmData to be in PCM 16bit format.
        // And this is the default format used by the JavaAudioDeviceModule, so unless someone
        // in this class will override the default value then we are safe.
        val samples = ShortArray(soundData.size / 2) { i ->
            ((soundData[i * 2 + 1].toInt() and 0xFF) shl 8 or (soundData[i * 2].toInt() and 0xFF)).toShort()
        }

        // Calculate RMS value
        var sumSquared = 0.0
        for (sample in samples) {
            val sampleValue = sample.toDouble() / Short.MAX_VALUE
            sumSquared += sampleValue * sampleValue
        }
        val rmsValue = sqrt(sumSquared / samples.size)

        // Calculate decibel value (assumes a reference level of 1.0)
        return 20 * log10(rmsValue)
    }

    private fun calculateDecibelAverage(decibels: Double): Double {
        val receivedTime = System.currentTimeMillis()

        // Clear data if stale. Check the the latest recorded sample is not older than the overall
        // time window length. In this case all the data in the map is stale and we can drop it.
        if (decibelSamples.isNotEmpty() && System.currentTimeMillis() - decibelSamples.keys.last() > SAMPLING_TIME_MS) {
            decibelSamples.clear()
        }

        decibelSamples[receivedTime] = decibels

        // remove old entries from the end - we keep the last X seconds (defined in SAMPLING_TIME_MS)
        val iterator = decibelSamples.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if ((System.currentTimeMillis() - entry.key) > SAMPLING_TIME_MS) {
                iterator.remove()
            }
        }

        // calculate the average of the 1 second window
        if (decibelSamples.isNotEmpty()) {
            // calculate average
            var sumOfDecibels = 0.toDouble()
            decibelSamples.values.forEach {
                sumOfDecibels += it
            }

            return sumOfDecibels / decibelSamples.size.toDouble()
        } else {
            return 0.toDouble()
        }
    }
}
