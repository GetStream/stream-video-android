package com.twilio.audioswitch.android

import android.content.Intent

const val DEVICE_NAME = "DEVICE_NAME"

internal class FakeBluetoothIntentProcessor : BluetoothIntentProcessor {

    override fun getBluetoothDevice(intent: Intent): BluetoothDeviceWrapper? {
        return FakeBluetoothDevice(name = intent.getStringExtra(DEVICE_NAME)!!)
    }
}
