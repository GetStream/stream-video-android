package com.twilio.audioswitch.android

import android.bluetooth.BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE

internal const val HEADSET_NAME = "Fake Headset"
internal const val HEADSET_2_NAME = "Fake Headset 2"

internal data class FakeBluetoothDevice(
    override val name: String,
    override val deviceClass: Int? = AUDIO_VIDEO_HANDSFREE,
) : BluetoothDeviceWrapper
