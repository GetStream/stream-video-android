package com.twilio.audioswitch

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.twilio.audioswitch.AudioDevice.Earpiece
import com.twilio.audioswitch.AudioDevice.Speakerphone
import com.twilio.audioswitch.AudioDevice.WiredHeadset
import com.twilio.audioswitch.android.BuildWrapper
import com.twilio.audioswitch.android.DEVICE_NAME
import com.twilio.audioswitch.android.FakeBluetoothIntentProcessor
import com.twilio.audioswitch.android.HEADSET_NAME
import com.twilio.audioswitch.android.ProductionLogger
import com.twilio.audioswitch.bluetooth.BluetoothHeadsetConnectionListener
import com.twilio.audioswitch.bluetooth.BluetoothHeadsetManager
import com.twilio.audioswitch.wired.INTENT_STATE
import com.twilio.audioswitch.wired.STATE_PLUGGED
import com.twilio.audioswitch.wired.WiredHeadsetReceiver
import java.util.concurrent.TimeoutException

internal fun setupFakeAudioSwitch(
    context: Context,
    preferredDevicesList: List<Class<out AudioDevice>> =
        listOf(
            AudioDevice.BluetoothHeadset::class.java,
            WiredHeadset::class.java,
            Earpiece::class.java,
            Speakerphone::class.java,
        ),
    bluetoothListener: BluetoothHeadsetConnectionListener? = null,
): Triple<AudioSwitch, BluetoothHeadsetManager, WiredHeadsetReceiver> {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val logger = ProductionLogger(true)
    val audioDeviceManager =
        AudioDeviceManager(
            context,
            logger,
            audioManager,
            BuildWrapper(),
            AudioFocusRequestWrapper(),
            {},
        )
    val wiredHeadsetReceiver = WiredHeadsetReceiver(context, logger)
    val headsetManager = BluetoothAdapter.getDefaultAdapter()?.let { bluetoothAdapter ->
        BluetoothHeadsetManager(
            context,
            logger,
            bluetoothAdapter,
            audioDeviceManager,
            bluetoothIntentProcessor = FakeBluetoothIntentProcessor(),
        )
    } ?: run {
        null
    }
    return Triple(
        AudioSwitch(
            context,
            bluetoothListener,
            logger,
            {},
            preferredDevicesList,
            audioDeviceManager,
            wiredHeadsetReceiver,
            bluetoothHeadsetManager = headsetManager,
        ),
        headsetManager!!,
        wiredHeadsetReceiver,
    )
}

internal fun simulateBluetoothSystemIntent(
    context: Context,
    headsetManager: BluetoothHeadsetManager,
    deviceName: String = HEADSET_NAME,
    action: String = BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED,
    connectionState: Int = BluetoothHeadset.STATE_CONNECTED,
) {
    val intent = Intent(action).apply {
        putExtra(BluetoothHeadset.EXTRA_STATE, connectionState)
        putExtra(DEVICE_NAME, deviceName)
    }
    headsetManager.onReceive(context, intent)
}

internal fun simulateWiredHeadsetSystemIntent(
    context: Context,
    wiredHeadsetReceiver: WiredHeadsetReceiver,
) {
    val intent = Intent().apply {
        putExtra(INTENT_STATE, STATE_PLUGGED)
    }
    wiredHeadsetReceiver.onReceive(context, intent)
}

fun getTargetContext(): Context = getInstrumentation().targetContext

fun getInstrumentationContext(): Context = getInstrumentation().context

fun isSpeakerPhoneOn() =
    (getTargetContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager?)?.let {
        it.isSpeakerphoneOn
    } ?: false

fun retryAssertion(
    timeoutInMilliseconds: Long = 10000L,
    assertionAction: () -> Unit,
) {
    val startTime = System.currentTimeMillis()
    var currentTime = 0L
    while (currentTime <= timeoutInMilliseconds) {
        try {
            assertionAction()
            return
        } catch (error: AssertionError) {
            currentTime = System.currentTimeMillis() - startTime
            Thread.sleep(10)
        }
    }
    throw TimeoutException("Assertion timeout occurred")
}
