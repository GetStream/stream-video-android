package com.twilio.audioswitch.bluetooth

import android.os.Handler
import androidx.annotation.VisibleForTesting
import com.twilio.audioswitch.android.Logger
import com.twilio.audioswitch.android.SystemClockWrapper
import java.util.concurrent.TimeoutException

internal const val TIMEOUT = 5000L
private const val TAG = "BluetoothScoJob"

internal abstract class BluetoothScoJob(
    private val logger: Logger,
    private val bluetoothScoHandler: Handler,
    private val systemClockWrapper: SystemClockWrapper,
) {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var bluetoothScoRunnable: BluetoothScoRunnable? = null

    protected abstract fun scoAction()

    open fun scoTimeOutAction() {}

    fun executeBluetoothScoJob() {
        // cancel existing runnable
        bluetoothScoRunnable?.let { bluetoothScoHandler.removeCallbacks(it) }

        BluetoothScoRunnable().apply {
            bluetoothScoRunnable = this
            bluetoothScoHandler.post(this)
        }
        logger.d(TAG, "Scheduled bluetooth sco job")
    }

    fun cancelBluetoothScoJob() {
        bluetoothScoRunnable?.let {
            bluetoothScoHandler.removeCallbacks(it)
            bluetoothScoRunnable = null
            logger.d(TAG, "Canceled bluetooth sco job")
        }
    }

    inner class BluetoothScoRunnable : Runnable {

        private val startTime = systemClockWrapper.elapsedRealtime()
        private var elapsedTime = 0L

        override fun run() {
            if (elapsedTime < TIMEOUT) {
                scoAction()
                elapsedTime = systemClockWrapper.elapsedRealtime() - startTime
                bluetoothScoHandler.postDelayed(this, 500)
            } else {
                logger.e(TAG, "Bluetooth sco job timed out", TimeoutException())
                scoTimeOutAction()
                cancelBluetoothScoJob()
            }
        }
    }
}
