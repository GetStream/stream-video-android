package com.twilio.audioswitch

import android.content.Context
import android.media.AudioManager
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class AudioSwitchIntegrationTest : AndroidTestBase() {

    @Test
    @UiThreadTest
    fun it_should_disable_logging_by_default() {
        val audioSwitch = AudioSwitch(getInstrumentationContext())

        assertFalse(audioSwitch.loggingEnabled)
    }

    @Test
    @UiThreadTest
    fun it_should_allow_enabling_logging() {
        val audioSwitch = AudioSwitch(getInstrumentationContext())

        audioSwitch.loggingEnabled = true

        assertTrue(audioSwitch.loggingEnabled)
    }

    @Test
    @UiThreadTest
    fun it_should_allow_enabling_logging_at_construction() {
        val audioSwitch = AudioSwitch(getInstrumentationContext(), loggingEnabled = true)

        assertTrue(audioSwitch.loggingEnabled)
    }

    @Test
    @UiThreadTest
    fun it_should_allow_toggling_logging_while_in_use() {
        val audioSwitch = AudioSwitch(getInstrumentationContext())
        audioSwitch.loggingEnabled = true
        assertTrue(audioSwitch.loggingEnabled)
        audioSwitch.start { _, _ -> }
        val earpiece = audioSwitch.availableAudioDevices
            .find { it is AudioDevice.Earpiece }
        assertNotNull(earpiece)
        audioSwitch.selectDevice(earpiece!!)
        assertEquals(earpiece, audioSwitch.selectedAudioDevice)
        audioSwitch.stop()

        audioSwitch.loggingEnabled = false
        assertFalse(audioSwitch.loggingEnabled)

        audioSwitch.start { _, _ -> }
        audioSwitch.stop()
    }

    @Test
    @UiThreadTest
    fun `it_should_return_valid_semver_formatted_version`() {
        val semVerRegex = Regex(
            "^([0-9]+)\\.([0-9]+)\\.([0-9]+)(?:-([0-9A-" +
                "Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+[0-9A-Za-z-]+)?$",
        )
        val version: String = AudioSwitch.VERSION
        assertNotNull(version)
        assertTrue(version.matches(semVerRegex))
    }

    @Test
    fun it_should_receive_audio_focus_changes_if_configured() {
        val audioFocusLostLatch = CountDownLatch(1)
        val audioFocusGainedLatch = CountDownLatch(1)
        val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> audioFocusLostLatch.countDown()
                AudioManager.AUDIOFOCUS_GAIN -> audioFocusGainedLatch.countDown()
            }
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val audioSwitch = AudioSwitch(getTargetContext(), null, true, audioFocusChangeListener)
            audioSwitch.start { _, _ -> }
            audioSwitch.activate()
        }

        val audioManager = getInstrumentationContext()
            .getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val audioFocusUtil = AudioFocusUtil(audioManager, audioFocusChangeListener)
        audioFocusUtil.requestFocus()

        assertTrue(audioFocusLostLatch.await(5, TimeUnit.SECONDS))
        audioFocusUtil.abandonFocus()
        assertTrue(audioFocusGainedLatch.await(5, TimeUnit.SECONDS))
    }

    @Test
    fun it_should_acquire_audio_focus_if_it_is_already_acquired_in_the_system() {
        val audioFocusLostLatch = CountDownLatch(1)
        val audioFocusGainedLatch = CountDownLatch(1)
        val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> audioFocusLostLatch.countDown()
                AudioManager.AUDIOFOCUS_GAIN -> audioFocusGainedLatch.countDown()
            }
        }
        val audioManager = getInstrumentationContext()
            .getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val audioFocusUtil = AudioFocusUtil(audioManager, audioFocusChangeListener)
        audioFocusUtil.requestFocus()

        val audioSwitch = AudioSwitch(getTargetContext(), null, true)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            audioSwitch.start { _, _ -> }
            audioSwitch.activate()
        }

        assertTrue(audioFocusLostLatch.await(5, TimeUnit.SECONDS))
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            audioSwitch.stop()
        }
        assertTrue(audioFocusGainedLatch.await(5, TimeUnit.SECONDS))
    }
}
