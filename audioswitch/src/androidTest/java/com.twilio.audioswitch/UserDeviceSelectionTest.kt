package com.twilio.audioswitch

import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.twilio.audioswitch.AudioDevice.BluetoothHeadset
import com.twilio.audioswitch.AudioDevice.Earpiece
import com.twilio.audioswitch.AudioDevice.Speakerphone
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class UserDeviceSelectionTest : AndroidTestBase() {

    private val context = InstrumentationRegistry.getInstrumentation().context

    @UiThreadTest
    @Test
    fun `it_should_select_the_earpiece_audio_device_when_the_user_selects_it`() {
        val audioSwitch = AudioSwitch(context)
        audioSwitch.start { _, _ -> }
        val earpiece = audioSwitch.availableAudioDevices
            .find { it is Earpiece }
        assertThat(earpiece, `is`(notNullValue()))

        audioSwitch.selectDevice(earpiece!!)

        assertThat(audioSwitch.selectedAudioDevice, equalTo(earpiece))
        audioSwitch.stop()
    }

    @UiThreadTest
    @Test
    fun `it_should_select_the_speakerphone_audio_device_when_the_user_selects_it`() {
        val audioSwitch = AudioSwitch(context)
        audioSwitch.start { _, _ -> }
        val speakerphone = audioSwitch.availableAudioDevices
            .find { it is Speakerphone }
        assertThat(speakerphone, `is`(notNullValue()))

        audioSwitch.selectDevice(speakerphone!!)

        assertThat(audioSwitch.selectedAudioDevice, equalTo(speakerphone))
        audioSwitch.stop()
    }

    @UiThreadTest
    @Test
    fun `it_should_select_the_bluetooth_audio_device_when_the_user_selects_it`() {
        val (audioSwitch, bluetoothHeadsetReceiver) = setupFakeAudioSwitch(context)
        audioSwitch.start { _, _ -> }
        simulateBluetoothSystemIntent(context, bluetoothHeadsetReceiver)
        val bluetoothDevice = audioSwitch.availableAudioDevices
            .find { it is BluetoothHeadset }
        assertThat(bluetoothDevice, `is`(notNullValue()))

        audioSwitch.selectDevice(bluetoothDevice!!)

        assertThat(audioSwitch.selectedAudioDevice, equalTo(bluetoothDevice))
        audioSwitch.stop()
    }
}
