package com.twilio.audioswitch

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.media.AudioManager.OnAudioFocusChangeListener
import com.twilio.audioswitch.AudioDevice.Earpiece
import com.twilio.audioswitch.AudioDevice.Speakerphone
import com.twilio.audioswitch.AudioDevice.WiredHeadset
import com.twilio.audioswitch.android.BuildWrapper
import com.twilio.audioswitch.bluetooth.BluetoothHeadsetConnectionListener
import com.twilio.audioswitch.bluetooth.BluetoothHeadsetManager
import com.twilio.audioswitch.wired.WiredHeadsetReceiver
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

open class BaseTest {
    internal val bluetoothClass = mock<BluetoothClass> {
        whenever(mock.deviceClass).thenReturn(BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE)
    }
    internal val expectedBluetoothDevice = mock<BluetoothDevice> {
        whenever(mock.name).thenReturn(DEVICE_NAME)
        whenever(mock.bluetoothClass).thenReturn(bluetoothClass)
    }
    internal val application = mock<Application>()
    internal val context = mock<Context>() {
        whenever(mock.applicationContext).thenReturn(application)
    }
    internal val bluetoothListener = mock<BluetoothHeadsetConnectionListener>()
    internal val logger = UnitTestLogger()
    internal val audioManager = setupAudioManagerMock()
    internal val bluetoothManager = mock<BluetoothManager>()
    internal val bluetoothAdapter = mock<BluetoothAdapter>()

    internal val audioDeviceChangeListener = mock<AudioDeviceChangeListener>()
    internal val buildWrapper = mock<BuildWrapper>()
    internal val audioFocusRequest = mock<AudioFocusRequestWrapper>()
    internal val defaultAudioFocusChangeListener = mock<OnAudioFocusChangeListener>()
    internal val audioDeviceManager = AudioDeviceManager(
        context,
        logger,
        audioManager,
        buildWrapper,
        audioFocusRequest,
        defaultAudioFocusChangeListener,
    )
    internal val wiredHeadsetReceiver = WiredHeadsetReceiver(context, logger)
    internal var handler = setupScoHandlerMock()
    internal var systemClockWrapper = setupSystemClockMock()
    internal val headsetProxy = mock<BluetoothHeadset>()
    internal val preferredDeviceList = listOf(
        AudioDevice.BluetoothHeadset::class.java,
        WiredHeadset::class.java,
        Earpiece::class.java,
        Speakerphone::class.java,
    )
    internal val permissionsStrategyProxy = setupPermissionsCheckStrategy()
    internal var headsetManager: BluetoothHeadsetManager = BluetoothHeadsetManager(
        context,
        logger,
        bluetoothAdapter,
        audioDeviceManager,
        bluetoothScoHandler = handler,
        systemClockWrapper = systemClockWrapper,
        headsetProxy = headsetProxy,
    )

    internal var audioSwitch = AudioSwitch(
        context = context,
        bluetoothHeadsetConnectionListener = bluetoothListener,
        logger = logger,
        audioDeviceManager = audioDeviceManager,
        wiredHeadsetReceiver = wiredHeadsetReceiver,
        audioFocusChangeListener = defaultAudioFocusChangeListener,
        preferredDeviceList = preferredDeviceList,
        permissionsCheckStrategy = permissionsStrategyProxy,
        bluetoothManager = bluetoothManager,
        bluetoothHeadsetManager = headsetManager,
    )

    internal fun assertBluetoothHeadsetTeardown() {
        assertThat(headsetManager.headsetListener, CoreMatchers.`is`(CoreMatchers.nullValue()))
        verify(bluetoothAdapter).closeProfileProxy(BluetoothProfile.HEADSET, headsetProxy)
        verify(context).unregisterReceiver(headsetManager)
    }

    internal fun simulateNewBluetoothHeadsetConnection(
        bluetoothDevice: BluetoothDevice = expectedBluetoothDevice,
    ) {
        val intent = mock<Intent> {
            whenever(mock.action).thenReturn(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)
            whenever(mock.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED))
                .thenReturn(BluetoothHeadset.STATE_CONNECTED)
            whenever(mock.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE))
                .thenReturn(bluetoothDevice)
        }
        headsetManager.onReceive(context, intent)
    }

    internal fun simulateDisconnectedBluetoothHeadsetConnection(
        bluetoothDevice: BluetoothDevice = expectedBluetoothDevice,
    ) {
        val intent = mock<Intent> {
            whenever(mock.action).thenReturn(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)
            whenever(mock.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED))
                .thenReturn(BluetoothHeadset.STATE_DISCONNECTED)
            whenever(mock.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE))
                .thenReturn(bluetoothDevice)
        }
        headsetManager.onReceive(context, intent)
    }
}
