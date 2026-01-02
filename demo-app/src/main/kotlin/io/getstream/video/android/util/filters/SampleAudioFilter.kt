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

package io.getstream.video.android.util.filters

import java.nio.ByteBuffer
import java.nio.ByteOrder

object SampleAudioFilter {

    fun toRoboticVoice(audioBuffer: ByteBuffer, numChannels: Int, pitchShiftFactor: Float) {
        require(pitchShiftFactor > 0) { "Pitch shift factor must be greater than 0." }

        val inputBuffer = audioBuffer.duplicate()
        inputBuffer.order(
            ByteOrder.LITTLE_ENDIAN,
        ) // Set byte order for correct handling of PCM data

        val numSamples = inputBuffer.remaining() / 2 // Assuming 16-bit PCM audio

        val outputBuffer = ByteBuffer.allocate(inputBuffer.capacity())
        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        for (channel in 0 until numChannels) {
            val channelBuffer = ShortArray(numSamples)
            inputBuffer.asShortBuffer().get(channelBuffer)

            for (i in 0 until numSamples) {
                val originalIndex = (i * pitchShiftFactor).toInt()

                if (originalIndex >= 0 && originalIndex < numSamples) {
                    outputBuffer.putShort(channelBuffer[originalIndex])
                } else {
                    // Fill with silence if the index is out of bounds
                    outputBuffer.putShort(0)
                }
            }
        }

        outputBuffer.flip()
        audioBuffer.clear()
        audioBuffer.put(outputBuffer)
        audioBuffer.flip()
    }
}
