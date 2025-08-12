package com.twilio.audioswitch

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import com.twilio.audioswitch.AudioDevice.Earpiece
import com.twilio.audioswitch.AudioDevice.Speakerphone
import com.twilio.audioswitch.AudioDevice.WiredHeadset
import com.twilio.audioswitch.AudioSwitch.State.ACTIVATED
import com.twilio.audioswitch.AudioSwitch.State.STARTED
import com.twilio.audioswitch.AudioSwitch.State.STOPPED
import com.twilio.audioswitch.wired.INTENT_STATE
import com.twilio.audioswitch.wired.STATE_PLUGGED
import com.twilio.audioswitch.wired.STATE_UNPLUGGED
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@RunWith(JUnitParamsRunner::class)
class AudioSwitchTest : BaseTest() {

    internal val packageManager = mock<PackageManager> {
        whenever(mock.hasSystemFeature(any())).thenReturn(true)
    }

    @Before
    fun setUp() {
        whenever(context.packageManager).thenReturn(packageManager)
    }

    @Test
    fun `start should start the bluetooth and wired headset listeners`() {
        audioSwitch.start(audioDeviceChangeListener)

        assertBluetoothHeadsetSetup()

        assertThat(wiredHeadsetReceiver.deviceListener, equalTo(audioSwitch.wiredDeviceConnectionListener))
        verify(context).registerReceiver(eq(wiredHeadsetReceiver), isA())
    }

    @Test
    fun `start should transition to the started state if the current state is stopped`() {
        audioSwitch.start(audioDeviceChangeListener)

        assertThat(audioSwitch.state, equalTo(STARTED))
    }

    @Test
    fun `start can be called without audio device change listener`() {
        audioSwitch.start()

        assertThat(audioSwitch.state, equalTo(STARTED))
        verifyNoInteractions(audioDeviceChangeListener)
    }

    @Test
    fun `should be able to add audio device change listener`() {
        audioSwitch.start()
        audioSwitch.setAudioDeviceChangeListener(audioDeviceChangeListener)
        audioSwitch.selectDevice(Speakerphone())
        verify(audioDeviceChangeListener).invoke(
            listOf(Earpiece(), Speakerphone()),
            Speakerphone(),
        )
    }

    @Test
    fun `should be able to remove audio device change listener`() {
        audioSwitch.start(audioDeviceChangeListener)
        audioSwitch.setAudioDeviceChangeListener(null)
        audioSwitch.selectDevice(Speakerphone())
        audioSwitch.selectDevice(Earpiece())
        audioSwitch.selectDevice(Speakerphone())
        verify(audioDeviceChangeListener, times(1)).invoke(
            listOf(Earpiece(), Speakerphone()),
            Earpiece(),
        )
    }

    @Test
    fun `start should cache the default audio devices and the default selected audio device`() {
        audioSwitch.start(audioDeviceChangeListener)

        audioSwitch.availableAudioDevices.let { audioDevices ->
            assertThat(audioDevices.size, equalTo(2))
            assertThat(audioDevices[0] is Earpiece, equalTo(true))
            assertThat(audioDevices[1] is Speakerphone, equalTo(true))
        }
        assertThat(audioSwitch.selectedAudioDevice is Earpiece, equalTo(true))
    }

    @Test
    fun `start should invoke the audio device change listener with the default audio devices`() {
        audioSwitch.start(audioDeviceChangeListener)

        verify(audioDeviceChangeListener).invoke(
            listOf(Earpiece(), Speakerphone()),
            Earpiece(),
        )
    }

    @Test
    fun `start should not start the HeadsetManager if it is null`() {
        audioSwitch = AudioSwitch(
            context = context,
            bluetoothHeadsetConnectionListener = bluetoothListener,
            logger = logger,
            audioDeviceManager = audioDeviceManager,
            wiredHeadsetReceiver = wiredHeadsetReceiver,
            audioFocusChangeListener = defaultAudioFocusChangeListener,
            preferredDeviceList = preferredDeviceList,
            permissionsCheckStrategy = permissionsStrategyProxy,
            bluetoothHeadsetManager = null,
        )

        audioSwitch.start(audioDeviceChangeListener)

        verify(bluetoothAdapter, times(0)).getProfileProxy(
            context,
            headsetManager,
            BluetoothProfile.HEADSET,
        )
        verify(context, times(0)).registerReceiver(eq(headsetManager), isA())
    }

    @Test
    fun `start should do nothing if the current state is started`() {
        audioSwitch.start(audioDeviceChangeListener)

        try {
            audioSwitch.start(audioDeviceChangeListener)
        } catch (e: Exception) {
            fail("Exception should not have been thrown")
        }
    }

    @Test
    fun `start should do nothing if the current state is activated`() {
        audioSwitch.start(audioDeviceChangeListener)
        audioSwitch.activate()
        audioSwitch.start(audioDeviceChangeListener)

        try {
            audioSwitch.start(audioDeviceChangeListener)
        } catch (e: Exception) {
            fail("Exception should not have been thrown")
        }
    }

    @Test
    fun `stop should transition to the stopped state if the current state is started`() {
        audioSwitch.start(audioDeviceChangeListener)
        audioSwitch.stop()

        assertThat(audioSwitch.state, equalTo(STOPPED))
    }

    @Test
    fun `stop should stop the bluetooth and wired headset listeners if the current state is started`() {
        headsetManager.onServiceConnected(0, headsetProxy)

        audioSwitch.start(audioDeviceChangeListener)
        audioSwitch.stop()

        // Verify bluetooth behavior
        assertBluetoothHeadsetTeardown()

        // Verify wired headset behavior
        assertThat(wiredHeadsetReceiver.deviceListener, `is`(nullValue()))
        verify(context).unregisterReceiver(wiredHeadsetReceiver)
    }

    @Test
    fun `stop should transition to the stopped state if the current state is activated`() {
        val bluetoothProfile = mock<BluetoothHeadset>()
        headsetManager.onServiceConnected(0, bluetoothProfile)

        audioSwitch.start(audioDeviceChangeListener)
        audioSwitch.activate()
        audioSwitch.stop()

        assertThat(audioSwitch.state, equalTo(STOPPED))
    }

    @Ignore("Finish as part of https://issues.corp.twilio.com/browse/AHOYAPPS-588")
    @Test
    fun `stop should stop the bluetooth and wired headset listeners if the current state is activated`() {
        TODO("Implement after deactivate tests are complete")
    }

    @Test
    fun `stop should do nothing if the current state is stopped`() {
        audioSwitch.start(audioDeviceChangeListener)
        audioSwitch.stop()

        try {
            audioSwitch.stop()
        } catch (e: Exception) {
            fail("Exception should not have been thrown")
        }
    }

    @Test
    fun `stop should unassign the audio device change listener`() {
        audioSwitch.start(audioDeviceChangeListener)
        audioSwitch.stop()

        assertThat(audioSwitch.audioDeviceChangeListener, `is`(nullValue()))
    }

    @Test
    fun `stop should not stop the BluetoothHeadsetManager if it is null and if transitioning from the started state`() {
        audioSwitch = AudioSwitch(
            context = context,
            bluetoothHeadsetConnectionListener = bluetoothListener,
            logger = logger,
            audioDeviceManager = audioDeviceManager,
            wiredHeadsetReceiver = wiredHeadsetReceiver,
            audioFocusChangeListener = defaultAudioFocusChangeListener,
            preferredDeviceList = preferredDeviceList,
            permissionsCheckStrategy = permissionsStrategyProxy,
            bluetoothHeadsetManager = null,
        )
        audioSwitch.start(audioDeviceChangeListener)
        audioSwitch.stop()

        verifyNoInteractions(bluetoothAdapter)
        verify(context, times(0)).unregisterReceiver(headsetManager)
    }

    @Test
    fun `stop should not stop the BluetoothHeadsetManager if it is null and if transitioning from the activated state`() {
        audioSwitch = AudioSwitch(
            context = context,
            bluetoothHeadsetConnectionListener = bluetoothListener,
            logger = logger,
            audioDeviceManager = audioDeviceManager,
            audioFocusChangeListener = defaultAudioFocusChangeListener,
            preferredDeviceList = preferredDeviceList,
            permissionsCheckStrategy = permissionsStrategyProxy,
            bluetoothHeadsetManager = null,
        )
        audioSwitch.start(audioDeviceChangeListener)
        audioSwitch.activate()
        audioSwitch.stop()

        verifyNoInteractions(bluetoothAdapter)
        verify(context, times(0)).unregisterReceiver(headsetManager)
    }

    @Test
    fun `activate should transition to the activated state if the current state is started`() {
        audioSwitch.start(audioDeviceChangeListener)
        audioSwitch.activate()

        assertThat(audioSwitch.state, equalTo(ACTIVATED))
    }

    @Test
    fun `activate should set audio focus using Android O method if api version is 26`() {
        whenever(buildWrapper.getVersion()).thenReturn(Build.VERSION_CODES.O)
        val audioFocusRequest = mock<AudioFocusRequest>()
        whenever(this.audioFocusRequest.buildRequest(defaultAudioFocusChangeListener)).thenReturn(audioFocusRequest)
        audioSwitch.start(audioDeviceChangeListener)
        audioSwitch.activate()

        verify(audioManager).requestAudioFocus(audioFocusRequest)
    }

    @Test
    fun `activate should set audio focus using Android O method if api version is 27`() {
        whenever(buildWrapper.getVersion()).thenReturn(Build.VERSION_CODES.O_MR1)
        val audioFocusRequest = mock<AudioFocusRequest>()
        whenever(this.audioFocusRequest.buildRequest(defaultAudioFocusChangeListener)).thenReturn(audioFocusRequest)
        audioSwitch.start(audioDeviceChangeListener)
        audioSwitch.activate()

        verify(audioManager).requestAudioFocus(audioFocusRequest)
    }

    @Test
    fun `activate should set audio focus using pre Android O method if api version is 25`() {
        whenever(buildWrapper.getVersion()).thenReturn(Build.VERSION_CODES.N_MR1)
        audioSwitch.start(audioDeviceChangeListener)
        audioSwitch.activate()

        verify(audioManager).requestAudioFocus(
            defaultAudioFocusChangeListener,
            AudioManager.STREAM_VOICE_CALL,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
        )
    }

    @Test
    fun `deactivate should abandon audio focus using pre Android O method if api version is 26`() {
        whenever(buildWrapper.getVersion()).thenReturn(Build.VERSION_CODES.O)
        val audioFocusRequest = mock<AudioFocusRequest>()
        whenever(this.audioFocusRequest.buildRequest(defaultAudioFocusChangeListener)).thenReturn(audioFocusRequest)
        audioSwitch.start(audioDeviceChangeListener)
        audioSwitch.activate()
        audioSwitch.stop()

        verify(audioManager).abandonAudioFocusRequest(audioFocusRequest)
    }

    @Test
    fun `deactivate should abandon audio focus using pre Android O method if api version is 27`() {
        whenever(buildWrapper.getVersion()).thenReturn(Build.VERSION_CODES.O_MR1)
        val audioFocusRequest = mock<AudioFocusRequest>()
        whenever(this.audioFocusRequest.buildRequest(defaultAudioFocusChangeListener)).thenReturn(audioFocusRequest)
        audioSwitch.start(audioDeviceChangeListener)
        audioSwitch.activate()
        audioSwitch.stop()

        verify(audioManager).abandonAudioFocusRequest(audioFocusRequest)
    }

    @Test
    fun `deactivate should abandon audio focus using pre Android O method if api version is 25`() {
        whenever(buildWrapper.getVersion()).thenReturn(Build.VERSION_CODES.N_MR1)
        audioSwitch.start(audioDeviceChangeListener)
        audioSwitch.activate()
        audioSwitch.stop()

        verify(audioManager).abandonAudioFocus(defaultAudioFocusChangeListener)
    }

    @Test
    fun `activate should enable audio routing to the earpiece`() {
        audioSwitch.start(audioDeviceChangeListener)
        val earpiece = audioSwitch.availableAudioDevices.find { it.name == "Earpiece" }
        audioSwitch.selectDevice(earpiece)
        audioSwitch.activate()

        verify(audioManager).isSpeakerphoneOn = false
    }

    @Test
    fun `activate should enable audio routing to the speakerphone device`() {
        audioSwitch.start(audioDeviceChangeListener)
        audioSwitch.selectDevice(Speakerphone())
        audioSwitch.activate()

        verify(audioManager).isSpeakerphoneOn = true
    }

    @Test
    fun `activate should enable audio routing to the bluetooth device`() {
        audioSwitch.start(audioDeviceChangeListener)
        simulateNewBluetoothHeadsetConnection()
        audioSwitch.activate()

        verify(audioManager).isSpeakerphoneOn = false
        verify(audioManager).startBluetoothSco()
    }

    @Test
    fun `activate should enable audio routing to the wired headset device`() {
        audioSwitch.start(audioDeviceChangeListener)
        audioSwitch.wiredDeviceConnectionListener.onDeviceConnected()
        audioSwitch.activate()

        verify(audioManager).isSpeakerphoneOn = false
    }

    @Ignore("Finish as part of https://issues.corp.twilio.com/browse/AHOYAPPS-588")
    @Test
    fun `test stopBluetooth sco scenarios when activating other audio devices`() {
        TODO("Not yet implemented")
    }

    @Ignore("Finish as part of https://issues.corp.twilio.com/browse/AHOYAPPS-588")
    @Test
    fun `activate should do nothing if the current state is activated`() {
        TODO("Not yet implemented")
    }

    @Ignore("Finish as part of https://issues.corp.twilio.com/browse/AHOYAPPS-588")
    @Test
    fun `activate should throw an IllegalStateException if the current state is stopped`() {
        TODO("Not yet implemented")
    }

    @Ignore("Finish as part of https://issues.corp.twilio.com/browse/AHOYAPPS-588")
    @Test
    fun `deactivate should transition to the started state if the current state is activated`() {
        TODO("Not yet implemented")
    }

    @Ignore("Finish as part of https://issues.corp.twilio.com/browse/AHOYAPPS-588")
    @Test
    fun `deactivate should do nothing if the current state is stopped`() {
        TODO("Assert cached audio state from activate() -> deactivate")
    }

    @Ignore("Finish as part of https://issues.corp.twilio.com/browse/AHOYAPPS-588")
    @Test
    fun `deactivate should throw an IllegalStateException if the current state is started`() {
        TODO("Not yet implemented")
    }

    @Ignore("Finish as part of https://issues.corp.twilio.com/browse/AHOYAPPS-588")
    @Test
    fun `selectDevice should throw an IllegalStateException if the current state is stopped`() {
        TODO("Not yet implemented")
    }

    @Ignore("Finish as part of https://issues.corp.twilio.com/browse/AHOYAPPS-588")
    @Test
    fun `selectDevice should do nothing if the current state is activated`() {
        TODO("Not yet implemented")
    }

    @Ignore("Finish as part of https://issues.corp.twilio.com/browse/AHOYAPPS-588")
    @Test
    fun `TODO test all permutations of possible audio devices and their priorities`() {
        TODO("Not yet implemented")
    }

    @Test
    fun `onBluetoothDeviceStateChanged should enumerate devices`() {
        audioSwitch.start(audioDeviceChangeListener)
        simulateNewBluetoothHeadsetConnection()
        audioSwitch.activate()

        audioSwitch.bluetoothDeviceConnectionListener.onBluetoothHeadsetStateChanged()

        verify(audioManager, times(2)).isSpeakerphoneOn = false
        verify(audioManager).startBluetoothSco()
    }

    @Test
    fun `selectDevice should not re activate the bluetooth device if the same device has been selected`() {
        audioSwitch.start(audioDeviceChangeListener)
        simulateNewBluetoothHeadsetConnection()
        audioSwitch.activate()

        audioSwitch.selectDevice(AudioDevice.BluetoothHeadset(DEVICE_NAME))

        verify(audioManager).isSpeakerphoneOn = false
        verify(audioManager).startBluetoothSco()
    }

    @Test
    fun `getVersion should return valid semver formatted version`() {
        val semVerRegex = Regex(
            "^([0-9]+)\\.([0-9]+)\\.([0-9]+)(?:-([0-9A-" +
                "Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+[0-9A-Za-z-]+)?$",
        )
        assertNotNull(AudioSwitch.VERSION)
        assertTrue(AudioSwitch.VERSION.matches(semVerRegex))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor should throw an IllegalArgumentException given duplicate preferred devices`() {
        audioSwitch = AudioSwitch(
            context = context,
            bluetoothHeadsetConnectionListener = bluetoothListener,
            logger = logger,
            audioDeviceManager = audioDeviceManager,
            wiredHeadsetReceiver = wiredHeadsetReceiver,
            audioFocusChangeListener = defaultAudioFocusChangeListener,
            preferredDeviceList = listOf(
                Speakerphone::class.java,
                AudioDevice.BluetoothHeadset::class.java,
                Earpiece::class.java,
                Speakerphone::class.java,
            ),
            permissionsCheckStrategy = permissionsStrategyProxy,
            bluetoothHeadsetManager = headsetManager,
        )
    }

    @Test
    fun `a new bluetooth headset should automatically connect if it is the user's selection`() {
        // Switch selection order so BT isn't automatically selected
        audioSwitch = AudioSwitch(
            context = context,
            bluetoothHeadsetConnectionListener = bluetoothListener,
            logger = logger,
            audioDeviceManager = audioDeviceManager,
            wiredHeadsetReceiver = wiredHeadsetReceiver,
            audioFocusChangeListener = defaultAudioFocusChangeListener,
            preferredDeviceList = listOf(
                Earpiece::class.java,
                WiredHeadset::class.java,
                Speakerphone::class.java,
                AudioDevice.BluetoothHeadset::class.java,
            ),
            permissionsCheckStrategy = permissionsStrategyProxy,
            bluetoothHeadsetManager = headsetManager,
        )
        val secondBluetoothDevice = mock<BluetoothDevice> {
            whenever(mock.name).thenReturn("$DEVICE_NAME 2")
            whenever(mock.bluetoothClass).thenReturn(bluetoothClass)
        }

        audioSwitch.run {
            start(this@AudioSwitchTest.audioDeviceChangeListener)
            simulateNewBluetoothHeadsetConnection()
            val bluetoothDevice = availableAudioDevices.find { it is AudioDevice.BluetoothHeadset }
            selectDevice(bluetoothDevice)
            activate()
            simulateNewBluetoothHeadsetConnection(secondBluetoothDevice)

            assertThat(
                selectedAudioDevice,
                equalTo(AudioDevice.BluetoothHeadset(secondBluetoothDevice.name)),
            )
        }
    }

    @Parameters(source = EarpieceAndSpeakerParams::class)
    @Test
    fun `when configuring a new preferred device list, the correct device should be automatically selected and activated`(
        preferredDeviceList: List<Class<out AudioDevice>>,
        expectedDevice: AudioDevice,
    ) {
        audioSwitch = AudioSwitch(
            context = context,
            bluetoothHeadsetConnectionListener = bluetoothListener,
            logger = logger,
            audioDeviceManager = audioDeviceManager,
            wiredHeadsetReceiver = wiredHeadsetReceiver,
            audioFocusChangeListener = defaultAudioFocusChangeListener,
            preferredDeviceList = preferredDeviceList,
            permissionsCheckStrategy = permissionsStrategyProxy,
            bluetoothHeadsetManager = headsetManager,
        )

        audioSwitch.run {
            start(this@AudioSwitchTest.audioDeviceChangeListener)
            activate()

            assertThat(selectedAudioDevice, equalTo(expectedDevice))
        }
    }

    @Parameters(source = WiredHeadsetParams::class)
    @Test
    fun `when configuring a new preferred device list, the correct device should be automatically selected and activated with a wired headset connected`(
        preferredDeviceList: List<Class<out AudioDevice>>,
        expectedDevice: AudioDevice,
    ) {
        audioSwitch = AudioSwitch(
            context = context,
            bluetoothHeadsetConnectionListener = bluetoothListener,
            logger = logger,
            audioDeviceManager = audioDeviceManager,
            wiredHeadsetReceiver = wiredHeadsetReceiver,
            audioFocusChangeListener = defaultAudioFocusChangeListener,
            preferredDeviceList = preferredDeviceList,
            permissionsCheckStrategy = permissionsStrategyProxy,
            bluetoothHeadsetManager = headsetManager,
        )

        audioSwitch.run {
            start(this@AudioSwitchTest.audioDeviceChangeListener)
            activate()
            simulateNewWiredHeadsetConnection()

            assertThat(selectedAudioDevice, equalTo(expectedDevice))
        }
    }

    @Parameters(source = BluetoothHeadsetParams::class)
    @Test
    fun `when configuring a new preferred device list, the correct device should be automatically selected and activated with a bluetooth headset connected`(
        preferredDeviceList: List<Class<out AudioDevice>>,
        expectedDevice: AudioDevice,
    ) {
        audioSwitch = AudioSwitch(
            context = context,
            bluetoothHeadsetConnectionListener = bluetoothListener,
            logger = logger,
            audioDeviceManager = audioDeviceManager,
            wiredHeadsetReceiver = wiredHeadsetReceiver,
            audioFocusChangeListener = defaultAudioFocusChangeListener,
            preferredDeviceList = preferredDeviceList,
            permissionsCheckStrategy = permissionsStrategyProxy,
            bluetoothHeadsetManager = headsetManager,
        )

        audioSwitch.run {
            start(this@AudioSwitchTest.audioDeviceChangeListener)
            activate()
            simulateNewBluetoothHeadsetConnection()

            assertThat(selectedAudioDevice, equalTo(expectedDevice))
        }
    }

    @Parameters(source = DefaultDeviceParams::class)
    @Test
    fun `when configuring a new preferred device list, all connected devices should be available but earpiece when a wired headset is connected`(
        preferredDeviceList: List<Class<out AudioDevice>>,
    ) {
        audioSwitch = AudioSwitch(
            context = context,
            bluetoothHeadsetConnectionListener = bluetoothListener,
            logger = logger,
            audioDeviceManager = audioDeviceManager,
            wiredHeadsetReceiver = wiredHeadsetReceiver,
            audioFocusChangeListener = defaultAudioFocusChangeListener,
            preferredDeviceList = preferredDeviceList,
            permissionsCheckStrategy = permissionsStrategyProxy,
            bluetoothHeadsetManager = headsetManager,
        )

        audioSwitch.run {
            start(this@AudioSwitchTest.audioDeviceChangeListener)
            activate()
            simulateNewBluetoothHeadsetConnection()
            simulateNewWiredHeadsetConnection()

            assertThat(availableAudioDevices.size, equalTo(3))
            assertThat(
                availableAudioDevices.containsAll(
                    listOf(
                        AudioDevice.BluetoothHeadset(),
                        WiredHeadset(),
                        Speakerphone(),
                    ),
                ),
                equalTo(true),
            )
        }
    }

    @Parameters(source = DefaultDeviceParams::class)
    @Test
    fun `when configuring a new preferred device list, all connected devices should be available but the wired headset`(
        preferredDeviceList: List<Class<out AudioDevice>>,
    ) {
        audioSwitch = AudioSwitch(
            context = context,
            bluetoothHeadsetConnectionListener = bluetoothListener,
            logger = logger,
            audioDeviceManager = audioDeviceManager,
            wiredHeadsetReceiver = wiredHeadsetReceiver,
            audioFocusChangeListener = defaultAudioFocusChangeListener,
            preferredDeviceList = preferredDeviceList,
            permissionsCheckStrategy = permissionsStrategyProxy,
            bluetoothHeadsetManager = headsetManager,
        )

        audioSwitch.run {
            start(this@AudioSwitchTest.audioDeviceChangeListener)
            activate()
            simulateNewBluetoothHeadsetConnection()

            assertThat(availableAudioDevices.size, equalTo(3))
            assertThat(
                availableAudioDevices.containsAll(
                    listOf(
                        AudioDevice.BluetoothHeadset(),
                        Earpiece(),
                        Speakerphone(),
                    ),
                ),
                equalTo(true),
            )
        }
    }

    @Test
    fun `Upon receiving redundant system events, redundant onAudioChanged events shall not be triggered`() {
        audioSwitch.start(audioDeviceChangeListener)
        simulateNewBluetoothHeadsetConnection()
        audioSwitch.activate()
        // additional disconnects should not invoke the listener, after the first disconnect,
        // device list and selected device should not change
        simulateDisconnectedBluetoothHeadsetConnection()
        simulateDisconnectedBluetoothHeadsetConnection()
        verify(audioDeviceChangeListener, times(2)).invoke(
            listOf(Earpiece(), Speakerphone()),
            Earpiece(),
        )
    }

    private fun simulateNewWiredHeadsetConnection() {
        val intent = mock<Intent> {
            whenever(mock.getIntExtra(INTENT_STATE, STATE_UNPLUGGED))
                .thenReturn(STATE_PLUGGED)
        }
        wiredHeadsetReceiver.onReceive(context, intent)
    }
}
