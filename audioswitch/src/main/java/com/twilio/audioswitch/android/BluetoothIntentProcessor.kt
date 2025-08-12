package com.twilio.audioswitch.android

import android.content.Intent

internal interface BluetoothIntentProcessor {

    fun getBluetoothDevice(intent: Intent): BluetoothDeviceWrapper?
}
