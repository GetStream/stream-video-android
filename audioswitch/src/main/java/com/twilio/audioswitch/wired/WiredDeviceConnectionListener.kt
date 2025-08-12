package com.twilio.audioswitch.wired

internal interface WiredDeviceConnectionListener {
    fun onDeviceConnected()
    fun onDeviceDisconnected()
}
