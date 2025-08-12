package com.twilio.audioswitch

import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.twilio.audioswitch.AudioDevice.Earpiece
import com.twilio.audioswitch.AudioDevice.Speakerphone
import com.twilio.audioswitch.AudioDevice.WiredHeadset
import com.twilio.audioswitch.bluetooth.BluetoothHeadsetConnectionListener
import junit.framework.TestCase.assertTrue
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@LargeTest
class AutomaticDeviceSelectionTest : AndroidTestBase() {

    @UiThreadTest
    @Test
    fun `it_should_select_the_bluetooth_audio_device_by_default`() {
        val context = getInstrumentationContext()
        val (audioSwitch, bluetoothHeadsetReceiver, wiredHeadsetReceiver) = setupFakeAudioSwitch(context)

        audioSwitch.start { _, _ -> }
        simulateBluetoothSystemIntent(context, bluetoothHeadsetReceiver)
        simulateWiredHeadsetSystemIntent(context, wiredHeadsetReceiver)

        assertThat(audioSwitch.selectedAudioDevice!! is AudioDevice.BluetoothHeadset, equalTo(true))
        audioSwitch.stop()
    }

    @UiThreadTest
    @Test
    fun `it_should_notify_callback_when_bluetooth_connects`() {
        val context = getInstrumentationContext()
        val bluetoothConnectedLatch = CountDownLatch(1)
        val bluetoothListener = object : BluetoothHeadsetConnectionListener {
            override fun onBluetoothHeadsetStateChanged(headsetName: String?, state: Int) {
                bluetoothConnectedLatch.countDown()
            }

            override fun onBluetoothScoStateChanged(state: Int) {}

            override fun onBluetoothHeadsetActivationError() {}
        }
        val (audioSwitch, bluetoothHeadsetReceiver, wiredHeadsetReceiver) = setupFakeAudioSwitch(context, bluetoothListener = bluetoothListener)

        audioSwitch.start { _, _ -> }
        simulateBluetoothSystemIntent(context, bluetoothHeadsetReceiver)
        simulateWiredHeadsetSystemIntent(context, wiredHeadsetReceiver)

        assertThat(audioSwitch.selectedAudioDevice!! is AudioDevice.BluetoothHeadset, equalTo(true))
        assertTrue(bluetoothConnectedLatch.await(5, TimeUnit.SECONDS))
        audioSwitch.stop()
    }

    @UiThreadTest
    @Test
    fun `it_should_select_the_wired_headset_by_default`() {
        val context = getInstrumentationContext()
        val (audioSwitch, bluetoothHeadsetReceiver, wiredHeadsetReceiver) =
            setupFakeAudioSwitch(context, listOf(WiredHeadset::class.java))

        audioSwitch.start { _, _ -> }
        simulateBluetoothSystemIntent(context, bluetoothHeadsetReceiver)
        simulateWiredHeadsetSystemIntent(context, wiredHeadsetReceiver)

        assertThat(audioSwitch.selectedAudioDevice!! is WiredHeadset, equalTo(true))
        audioSwitch.stop()
    }

    @UiThreadTest
    @Test
    fun `it_should_select_the_earpiece_audio_device_by_default`() {
        val context = getInstrumentationContext()
        val (audioSwitch, bluetoothHeadsetReceiver) =
            setupFakeAudioSwitch(context, listOf(Earpiece::class.java))
        audioSwitch.start { _, _ -> }
        simulateBluetoothSystemIntent(context, bluetoothHeadsetReceiver)

        assertThat(audioSwitch.selectedAudioDevice!! is Earpiece, equalTo(true))
        audioSwitch.stop()
    }

    @UiThreadTest
    @Test
    fun `it_should_select_the_speakerphone_audio_device_by_default`() {
        val context = getInstrumentationContext()
        val (audioSwitch, bluetoothHeadsetReceiver) =
            setupFakeAudioSwitch(context, listOf(Speakerphone::class.java))
        audioSwitch.start { _, _ -> }
        simulateBluetoothSystemIntent(context, bluetoothHeadsetReceiver)

        assertThat(audioSwitch.selectedAudioDevice!! is Speakerphone, equalTo(true))
        audioSwitch.stop()
    }
}
