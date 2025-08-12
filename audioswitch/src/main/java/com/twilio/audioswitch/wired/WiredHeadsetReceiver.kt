package com.twilio.audioswitch.wired

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.twilio.audioswitch.android.Logger

private const val TAG = "WiredHeadsetReceiver"
internal const val STATE_UNPLUGGED = 0
internal const val STATE_PLUGGED = 1
internal const val INTENT_STATE = "state"
internal const val INTENT_NAME = "name"

internal class WiredHeadsetReceiver(
    private val context: Context,
    private val logger: Logger,
) : BroadcastReceiver() {

    internal var deviceListener: WiredDeviceConnectionListener? = null

    override fun onReceive(context: Context, intent: Intent) {
        intent.getIntExtra(INTENT_STATE, STATE_UNPLUGGED).let { state ->
            if (state == STATE_PLUGGED) {
                intent.getStringExtra(INTENT_NAME).let { name ->
                    logger.d(TAG, "Wired headset device ${name ?: ""} connected")
                }
                deviceListener?.onDeviceConnected()
            } else {
                intent.getStringExtra(INTENT_NAME).let { name ->
                    logger.d(TAG, "Wired headset device ${name ?: ""} disconnected")
                }
                deviceListener?.onDeviceDisconnected()
            }
        }
    }

    fun start(deviceListener: WiredDeviceConnectionListener) {
        this.deviceListener = deviceListener
        context.registerReceiver(this, IntentFilter(Intent.ACTION_HEADSET_PLUG))
    }

    fun stop() {
        deviceListener = null
        context.unregisterReceiver(this)
    }
}
