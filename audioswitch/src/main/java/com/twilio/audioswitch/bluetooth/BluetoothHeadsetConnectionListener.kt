package com.twilio.audioswitch.bluetooth

import android.bluetooth.BluetoothHeadset
import android.media.AudioManager

/**
 * Notifies if Bluetooth device state has changed (connect, disconnect, audio connect, audio disconnect) or failed to connect.
 */
interface BluetoothHeadsetConnectionListener {

    /**
     * @param headsetName name of the headset
     * @param state provided by [BluetoothHeadset]
     */
    fun onBluetoothHeadsetStateChanged(headsetName: String? = null, state: Int = 0)

    /**
     * @param state provided by [AudioManager]
     */
    fun onBluetoothScoStateChanged(state: Int = 0)

    /**
     * Triggered when Bluetooth SCO job has timed out.
     */
    fun onBluetoothHeadsetActivationError()
}
