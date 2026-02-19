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

import kotlin.test.Test
import kotlin.test.assertEquals

class AudioValuePercentageNormaliserTest {

    @Test
    fun `returns 0 when decibel below lower bound`() {
        val result = AudioValuePercentageNormaliser.normalise(-100.0)
        assertEquals(0f, result)
    }

    @Test
    fun `returns 1 when decibel above upper bound`() {
        val result = AudioValuePercentageNormaliser.normalise(5.0)
        assertEquals(1f, result)
    }

    @Test
    fun `returns 0 when decibel equals lower bound`() {
        val result = AudioValuePercentageNormaliser.normalise(-50.0)
        assertEquals(0f, result)
    }

    @Test
    fun `returns 1 when decibel equals upper bound`() {
        val result = AudioValuePercentageNormaliser.normalise(0.0)
        assertEquals(1f, result)
    }

    @Test
    fun `returns 0_5 when decibel is halfway between min and max`() {
        val result = AudioValuePercentageNormaliser.normalise(-25.0)
        // halfway between -50 and 0 → 0.5
        assertEquals(0.5f, result, 0.0001f)
    }

    @Test
    fun `returns linear proportional values within range`() {
        // Between -50 and 0: linear interpolation
        val values = listOf(-50.0, -40.0, -30.0, -20.0, -10.0, 0.0)
        val expected = listOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f)

        values.zip(expected).forEach { (input, expectedValue) ->
            val result = AudioValuePercentageNormaliser.normalise(input)
            assertEquals(expectedValue, result, 0.001f, "Failed for input $input")
        }
    }

    @Test
    fun `delta should equal 50`() {
        assertEquals(50, AudioValuePercentageNormaliser.delta)
    }
}
