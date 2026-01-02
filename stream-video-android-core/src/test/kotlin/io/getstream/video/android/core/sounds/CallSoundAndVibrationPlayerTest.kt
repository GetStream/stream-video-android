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

package io.getstream.video.android.core.sounds

import android.content.Context
import android.os.Vibrator
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class CallSoundAndVibrationPlayerTest {

    private lateinit var context: Context
    private lateinit var vibrator: Vibrator
    private lateinit var player: CallSoundAndVibrationPlayer

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        vibrator = mockk(relaxed = true)

        every { context.getSystemService(any()) } returns vibrator

        player = CallSoundAndVibrationPlayer(context)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `vibrate starts vibration when not already active`() {
        val pattern = longArrayOf(0L, 200L, 400L)

        player.vibrate(pattern)

        verify(exactly = 1) { context.getSystemService(Context.VIBRATOR_SERVICE) }
        verify(exactly = 1) { vibrator.vibrate(pattern, 0) }
    }

    @Test
    fun `vibrate does not restart vibration when already active`() {
        val pattern = longArrayOf(50L, 150L, 250L)

        player.vibrate(pattern)
        player.vibrate(pattern)

        verify(exactly = 1) { vibrator.vibrate(pattern, 0) }
    }

    @Test
    fun `stopVibration does nothing when vibration not yet started`() {
        player.stopVibration()

        verify(exactly = 0) { vibrator.cancel() }
    }

    @Test
    fun `stopVibration cancels the active vibration`() {
        val pattern = longArrayOf(10L, 20L)

        player.vibrate(pattern)
        player.stopVibration()

        verify(exactly = 1) { vibrator.cancel() }
    }

    @Test
    fun `vibrate can restart after vibration has been stopped`() {
        val pattern = longArrayOf(100L, 200L)

        player.vibrate(pattern)
        player.stopVibration()
        clearMocks(vibrator)

        player.vibrate(pattern)

        verify(exactly = 1) { vibrator.vibrate(pattern, 0) }
    }
}
