package io.getstream.video.android.core.sounds

import android.content.Context
import android.os.Looper
import android.os.Vibrator
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
