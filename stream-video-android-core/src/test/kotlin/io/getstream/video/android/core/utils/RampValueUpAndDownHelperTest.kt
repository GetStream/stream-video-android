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

import io.getstream.video.android.core.base.IntegrationTestBase
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class RampValueUpAndDownHelperTest : IntegrationTestBase(connectCoordinatorWS = false) {

    @Test
    fun `values returns to zero`() = runTest {
        val totalDuration = 1000L
        val rampValueUpAndDownHelper = RampValueUpAndDownHelper(totalDuration)
        rampValueUpAndDownHelper.rampToValue(100f)
        advanceTimeBy(totalDuration)

        assertEquals(rampValueUpAndDownHelper.currentLevel.value, 0f)
    }

    @Test
    fun `value returns to zero even if multiple invocation`() = runTest {
        val totalDuration = 1000L
        val rampValueUpAndDownHelper = RampValueUpAndDownHelper(totalDuration)
        rampValueUpAndDownHelper.rampToValue(100f)
        rampValueUpAndDownHelper.rampToValue(100f)
        rampValueUpAndDownHelper.rampToValue(100f)
        advanceTimeBy(totalDuration)

        assertEquals(rampValueUpAndDownHelper.currentLevel.value, 0f)
    }

    @Test
    fun `value in middle of duration is close to targettarget value`() = runTest {
        val totalDuration = 1000L
        val rampValueUpAndDownHelper = RampValueUpAndDownHelper(totalDuration)

        rampValueUpAndDownHelper.rampToValue(100f)

        // advance time into the middle of the duration - the value now should be at the top
        advanceTimeBy(totalDuration / 2)

        assert(rampValueUpAndDownHelper.currentLevel.value >= (95f))
    }

    @Test
    fun `submitting a larger value will result in new target value`() = runTest {
        val totalDuration = 1000L
        val rampValueUpAndDownHelper = RampValueUpAndDownHelper(totalDuration)

        rampValueUpAndDownHelper.rampToValue(100f)
        rampValueUpAndDownHelper.rampToValue(200f)

        // advance time into the middle of the duration - the value now should be at the top
        advanceTimeBy(totalDuration / 2)

        assert(rampValueUpAndDownHelper.currentLevel.value >= 195f)

        // and also verify that the value goes back to 0
        advanceTimeBy(totalDuration / 2)

        assertEquals(rampValueUpAndDownHelper.currentLevel.value, 0f)
    }
}
