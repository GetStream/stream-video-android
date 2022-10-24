/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-video-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.video.android.audio

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.media.AudioManager
import io.getstream.logging.StreamLog
import io.getstream.video.android.audio.AudioDevice.BluetoothHeadset
import io.getstream.video.android.audio.AudioDevice.Earpiece
import io.getstream.video.android.audio.AudioDevice.Speakerphone
import io.getstream.video.android.audio.AudioDevice.WiredHeadset
import io.getstream.video.android.audio.AudioSwitch.State.ACTIVATED
import io.getstream.video.android.audio.AudioSwitch.State.STARTED
import io.getstream.video.android.audio.AudioSwitch.State.STOPPED

public class AudioSwitch internal constructor(
    context: Context,
    audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener,
    preferredDeviceList: List<Class<out AudioDevice>>,
    private val audioManager: AudioManagerAdapter = AudioManagerAdapterImpl(
        context,
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager,
        audioFocusChangeListener = audioFocusChangeListener
    ),
    private val wiredHeadsetReceiver: WiredHeadsetReceiver = WiredHeadsetReceiver(context),
    headsetManager: BluetoothHeadsetManager? = BluetoothHeadsetManager.newInstance(
        context,
        BluetoothAdapter.getDefaultAdapter(),
        audioManager
    )
) {

    private val logger = StreamLog.getLogger("Call:AudioSwitch")

    internal var audioDeviceChangeListener: AudioDeviceChangeListener? = null
    private var selectedDevice: AudioDevice? = null
    private var userSelectedDevice: AudioDevice? = null
    private var wiredHeadsetAvailable = false
    private val mutableAudioDevices = ArrayList<AudioDevice>()
    private var bluetoothHeadsetManager: BluetoothHeadsetManager? = headsetManager
    private val preferredDeviceList: List<Class<out AudioDevice>>

    internal var state: State = STOPPED

    internal enum class State {
        STARTED, ACTIVATED, STOPPED
    }

    internal val bluetoothDeviceConnectionListener = object : BluetoothHeadsetConnectionListener {
        override fun onBluetoothHeadsetStateChanged(headsetName: String?) {
            logger.d { "[onBluetoothHeadsetStateChanged] headsetName: $headsetName" }
            enumerateDevices(headsetName)
        }

        override fun onBluetoothHeadsetActivationError() {
            logger.e { "[onBluetoothHeadsetActivationError] no args" }
            if (userSelectedDevice is BluetoothHeadset) userSelectedDevice = null
            enumerateDevices()
        }
    }

    internal val wiredDeviceConnectionListener = object : WiredDeviceConnectionListener {
        override fun onDeviceConnected() {
            logger.d { "[onWiredDeviceConnected] no args" }
            wiredHeadsetAvailable = true
            enumerateDevices()
        }

        override fun onDeviceDisconnected() {
            logger.d { "[onWiredDeviceDisconnected] no args" }
            wiredHeadsetAvailable = false
            enumerateDevices()
        }
    }

    public val selectedAudioDevice: AudioDevice? get() = selectedDevice
    public val availableAudioDevices: List<AudioDevice> = mutableAudioDevices

    init {
        this.preferredDeviceList = getPreferredDeviceList(preferredDeviceList)
    }

    private fun getPreferredDeviceList(preferredDeviceList: List<Class<out AudioDevice>>):
        List<Class<out AudioDevice>> {
        require(hasNoDuplicates(preferredDeviceList))

        return if (preferredDeviceList.isEmpty() || preferredDeviceList == defaultPreferredDeviceList) {
            defaultPreferredDeviceList
        } else {
            val result = defaultPreferredDeviceList.toMutableList()
            result.removeAll(preferredDeviceList)
            preferredDeviceList.forEachIndexed { index, device ->
                result.add(index, device)
            }
            result
        }
    }

    /**
     * Starts listening for audio device changes and calls the [listener] upon each change.
     * **Note:** When audio device listening is no longer needed, [AudioSwitch.stop] should be
     * called in order to prevent a memory leak.
     */
    public fun start(listener: AudioDeviceChangeListener) {
        logger.d { "[start] state: $state" }
        audioDeviceChangeListener = listener
        when (state) {
            STOPPED -> {
                bluetoothHeadsetManager?.start(bluetoothDeviceConnectionListener)
                wiredHeadsetReceiver.start(wiredDeviceConnectionListener)
                enumerateDevices()
                state = STARTED
            }
            else -> {
            }
        }
    }

    /**
     * Stops listening for audio device changes if [AudioSwitch.start] has already been
     * invoked. [AudioSwitch.deactivate] will also get called if a device has been activated
     * with [AudioSwitch.activate].
     */
    public fun stop() {
        logger.d { "[stop] state: $state" }
        when (state) {
            ACTIVATED -> {
                deactivate()
                closeListeners()
            }
            STARTED -> {
                closeListeners()
            }
            STOPPED -> {
            }
        }
    }

    /**
     * Performs audio routing and unmuting on the selected device from
     * [AudioSwitch.selectDevice]. Audio focus is also acquired for the client application.
     * **Note:** [AudioSwitch.deactivate] should be invoked to restore the prior audio
     * state.
     */
    public fun activate() {
        logger.d { "[activate] state: $state" }
        when (state) {
            STARTED -> {
                audioManager.cacheAudioState()

                // Always set mute to false for WebRTC
                audioManager.mute(false)
                audioManager.setAudioFocus()
                selectedDevice?.let { activate(it) }
                state = ACTIVATED
            }
            ACTIVATED -> selectedDevice?.let { activate(it) }
            STOPPED -> throw IllegalStateException()
        }
    }

    /**
     * Restores the audio state prior to calling [AudioSwitch.activate] and removes
     * audio focus from the client application.
     */
    public fun deactivate() {
        logger.d { "[deactivate] state: $state" }
        when (state) {
            ACTIVATED -> {
                bluetoothHeadsetManager?.deactivate()

                // Restore stored audio state
                audioManager.restoreAudioState()
                state = STARTED
            }
            STARTED, STOPPED -> {
            }
        }
    }

    /**
     * Selects the desired [audioDevice]. If the provided [AudioDevice] is not
     * available, no changes are made. If the provided device is null, one is chosen based on the
     * specified preferred device list or the following default list:
     * [BluetoothHeadset], [WiredHeadset], [Earpiece], [Speakerphone].
     */
    public fun selectDevice(audioDevice: AudioDevice?) {
        logger.d { "[selectDevice] audioDevice: $audioDevice" }
        if (selectedDevice != audioDevice) {
            userSelectedDevice = audioDevice
            enumerateDevices()
        }
    }

    private fun hasNoDuplicates(list: List<Class<out AudioDevice>>) =
        list.groupingBy { it }.eachCount().filter { it.value > 1 }.isEmpty()

    private fun activate(audioDevice: AudioDevice) {
        logger.d { "[activate] audioDevice: $audioDevice" }
        when (audioDevice) {
            is BluetoothHeadset -> {
                audioManager.enableSpeakerphone(false)
                bluetoothHeadsetManager?.activate()
            }
            is Earpiece, is WiredHeadset -> {
                audioManager.enableSpeakerphone(false)
                bluetoothHeadsetManager?.deactivate()
            }
            is Speakerphone -> {
                audioManager.enableSpeakerphone(true)
                bluetoothHeadsetManager?.deactivate()
            }
        }
    }

    internal data class AudioDeviceState(
        val audioDeviceList: List<AudioDevice>,
        val selectedAudioDevice: AudioDevice?
    )

    private fun enumerateDevices(bluetoothHeadsetName: String? = null) {
        logger.d { "[enumerateDevices] bluetoothHeadsetName: $bluetoothHeadsetName" }
        // save off the old state and 'semi'-deep copy the list of audio devices
        val oldAudioDeviceState = AudioDeviceState(mutableAudioDevices.map { it }, selectedDevice)
        // update audio device list and selected device
        addAvailableAudioDevices(bluetoothHeadsetName)

        if (!userSelectedDevicePresent(mutableAudioDevices)) {
            userSelectedDevice = null
        }

        // Select the audio device
        selectedDevice = if (userSelectedDevice != null) {
            userSelectedDevice
        } else if (mutableAudioDevices.size > 0) {
            val firstAudioDevice = mutableAudioDevices.first()
            /*
             * If there was an error starting bluetooth sco, then the selected AudioDevice should
             * be the next valid device in the list.
             */
            if (firstAudioDevice is BluetoothHeadset &&
                bluetoothHeadsetManager?.hasActivationError() == true
            ) {
                mutableAudioDevices[1]
            } else {
                firstAudioDevice
            }
        } else {
            null
        }
        logger.v { "[enumerateDevices] selectedDevice: $selectedDevice" }

        // Activate the device if in the active state
        if (state == ACTIVATED) {
            activate()
        }
        // trigger audio device change listener if there has been a change
        val newAudioDeviceState = AudioDeviceState(mutableAudioDevices, selectedDevice)
        if (newAudioDeviceState != oldAudioDeviceState) {
            audioDeviceChangeListener?.invoke(mutableAudioDevices, selectedDevice)
        }
    }

    private fun addAvailableAudioDevices(bluetoothHeadsetName: String?) {
        logger.d {
            "[addAvailableAudioDevices] wiredHeadsetAvailable: $wiredHeadsetAvailable, " +
                "bluetoothHeadsetName: $bluetoothHeadsetName"
        }
        mutableAudioDevices.clear()
        preferredDeviceList.forEach { audioDevice ->
            logger.v { "[addAvailableAudioDevices] audioDevice: ${audioDevice.simpleName}" }
            when (audioDevice) {
                BluetoothHeadset::class.java -> {
                    /*
                     * Since the there is a delay between receiving the ACTION_ACL_CONNECTED event and receiving
                     * the name of the connected device from querying the BluetoothHeadset proxy class, the
                     * headset name received from the ACTION_ACL_CONNECTED intent needs to be passed into this
                     * function.
                     */
                    val headset = bluetoothHeadsetManager?.getHeadset(bluetoothHeadsetName)
                    logger.v {
                        "[addAvailableAudioDevices] #BluetoothHeadset; headset: $headset"
                    }
                    bluetoothHeadsetManager?.getHeadset(bluetoothHeadsetName)?.let {
                        mutableAudioDevices.add(it)
                    }
                }
                WiredHeadset::class.java -> {
                    logger.v {
                        "[addAvailableAudioDevices] #WiredHeadset; wiredHeadsetAvailable: $wiredHeadsetAvailable"
                    }
                    if (wiredHeadsetAvailable) {
                        mutableAudioDevices.add(WiredHeadset())
                    }
                }
                Earpiece::class.java -> {
                    val hasEarpiece = audioManager.hasEarpiece()
                    logger.v {
                        "[addAvailableAudioDevices] #Earpiece; hasEarpiece: $hasEarpiece, " +
                            "wiredHeadsetAvailable: $wiredHeadsetAvailable"
                    }
                    if (hasEarpiece && !wiredHeadsetAvailable) {
                        mutableAudioDevices.add(Earpiece())
                    }
                }
                Speakerphone::class.java -> {
                    val hasSpeakerphone = audioManager.hasSpeakerphone()
                    logger.v { "[addAvailableAudioDevices] #Speakerphone; hasSpeakerphone: $hasSpeakerphone" }
                    if (hasSpeakerphone) {
                        mutableAudioDevices.add(Speakerphone())
                    }
                }
            }
        }
    }

    private fun userSelectedDevicePresent(audioDevices: List<AudioDevice>) =
        userSelectedDevice?.let { selectedDevice ->
            if (selectedDevice is BluetoothHeadset) {
                // Match any bluetooth headset as a new one may have been connected
                audioDevices.find { it is BluetoothHeadset }?.let { newHeadset ->
                    userSelectedDevice = newHeadset
                    true
                } ?: false
            } else {
                audioDevices.contains(selectedDevice)
            }
        } ?: false

    private fun closeListeners() {
        bluetoothHeadsetManager?.stop()
        wiredHeadsetReceiver.stop()
        audioDeviceChangeListener = null
        state = STOPPED
    }

    public companion object {
        /**
         * The version of the AudioSwitch library.
         */
        public const val VERSION: String = "1.0"

        private val defaultPreferredDeviceList by lazy {
            listOf(
                BluetoothHeadset::class.java,
                WiredHeadset::class.java, Earpiece::class.java, Speakerphone::class.java
            )
        }
    }
}
