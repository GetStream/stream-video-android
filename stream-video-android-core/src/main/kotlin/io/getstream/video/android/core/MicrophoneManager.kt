/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core

import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import androidx.core.content.getSystemService
import com.twilio.audioswitch.AudioDevice
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.audio.AudioHandler
import io.getstream.video.android.core.audio.AudioSwitchHandler
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.core.audio.StreamAudioDevice.Companion.fromAudio
import io.getstream.video.android.core.audio.StreamAudioDevice.Companion.toAudioDevice
import io.getstream.video.android.core.utils.mapState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The Microphone manager makes it easy to use your microphone in a call
 *
 * @sample
 *
 * val call = client.call("default", "123")
 * val microphone = call.microphone
 *
 * microphone.enable() // enable the microphone
 * microphone.disable() // disable the microphone
 * microphone.setEnabled(true) // enable the microphone
 * microphone.setSpeaker(true) // enable the speaker
 *
 * microphone.listDevices() // return stateflow with the list of available devices
 * microphone.status // the status of the microphone
 * microphone.selectedDevice // the selected device
 * microphone.speakerPhoneEnabled // the status of the speaker. true/false
 */
class MicrophoneManager(
    val mediaManager: MediaManagerImpl,
    val audioUsage: Int,
) {
    // Internal data
    private val logger by taggedLogger("Media:MicrophoneManager")

    private lateinit var audioHandler: AudioHandler
    private var setupCompleted: Boolean = false
    internal var audioManager: AudioManager? = null
    internal var priorStatus: DeviceStatus? = null

    // Exposed state
    private val _status = MutableStateFlow<DeviceStatus>(DeviceStatus.NotSelected)

    /** The status of the audio */
    val status: StateFlow<DeviceStatus> = _status

    /** Represents whether the audio is enabled */
    public val isEnabled: StateFlow<Boolean> = _status.mapState { it is DeviceStatus.Enabled }

    private val _selectedDevice = MutableStateFlow<StreamAudioDevice?>(null)
    internal var nonHeadsetFallbackDevice: StreamAudioDevice? = null

    /** Currently selected device */
    val selectedDevice: StateFlow<StreamAudioDevice?> = _selectedDevice

    private val _devices = MutableStateFlow<List<StreamAudioDevice>>(emptyList())

    /** List of available devices. */
    val devices: StateFlow<List<StreamAudioDevice>> = _devices

    // API
    /** Enable the audio, the rtc engine will automatically inform the SFU */
    internal fun enable(fromUser: Boolean = true) {
        enforceSetup {
            if (fromUser) {
                _status.value = DeviceStatus.Enabled
            }
            mediaManager.audioTrack.trySetEnabled(true)
        }
    }

    fun pause(fromUser: Boolean = true) {
        enforceSetup {
            // pause the microphone, and when resuming switched back to the previous state
            priorStatus = _status.value
            disable(fromUser = fromUser)
        }
    }

    fun resume(fromUser: Boolean = true) {
        enforceSetup {
            priorStatus?.let {
                if (it == DeviceStatus.Enabled) {
                    enable(fromUser = fromUser)
                }
            }
        }
    }

    /** Disable the audio track. Audio is still captured, but not send.
     * This allows for the "you are muted" toast to indicate you are talking while muted */
    fun disable(fromUser: Boolean = true) {
        enforceSetup {
            if (fromUser) {
                _status.value = DeviceStatus.Disabled
            }
            mediaManager.audioTrack.trySetEnabled(false)
        }
    }

    /**
     * Enable or disable the microphone
     */
    fun setEnabled(enabled: Boolean, fromUser: Boolean = true) {
        enforceSetup {
            if (enabled) {
                enable(fromUser = fromUser)
            } else {
                disable(fromUser = fromUser)
            }
        }
    }

    /**
     * Select a specific device
     */
    fun select(device: StreamAudioDevice?) {
        logger.i { "selecting device $device" }
        ifAudioHandlerInitialized { it.selectDevice(device?.toAudioDevice()) }
        _selectedDevice.value = device

        if (device !is StreamAudioDevice.Speakerphone && mediaManager.speaker.isEnabled.value == true) {
            mediaManager.speaker._status.value = DeviceStatus.Disabled
        }

        if (device is StreamAudioDevice.Speakerphone) {
            mediaManager.speaker._status.value = DeviceStatus.Enabled
        }

        if (device !is StreamAudioDevice.BluetoothHeadset && device !is StreamAudioDevice.WiredHeadset) {
            nonHeadsetFallbackDevice = device
        }
    }

    /**
     * List the devices, returns a stateflow with audio devices
     */
    fun listDevices(): StateFlow<List<StreamAudioDevice>> {
        setup()
        return devices
    }

    fun cleanup() {
        ifAudioHandlerInitialized { it.stop() }
        setupCompleted = false
    }

    fun canHandleDeviceSwitch() = audioUsage != AudioAttributes.USAGE_MEDIA

    // Internal logic
    internal fun setup(preferSpeaker: Boolean = false, onAudioDevicesUpdate: (() -> Unit)? = null) {
        synchronized(this) {
            var capturedOnAudioDevicesUpdate = onAudioDevicesUpdate

            if (setupCompleted) {
                capturedOnAudioDevicesUpdate?.invoke()
                capturedOnAudioDevicesUpdate = null

                return
            }

            audioManager = mediaManager.context.getSystemService()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                audioManager?.allowedCapturePolicy = AudioAttributes.ALLOW_CAPTURE_BY_ALL
            }

            if (canHandleDeviceSwitch() && !::audioHandler.isInitialized) {
                audioHandler = AudioSwitchHandler(
                    context = mediaManager.context,
                    preferredDeviceList = listOf(
                        AudioDevice.BluetoothHeadset::class.java,
                        AudioDevice.WiredHeadset::class.java,
                    ) + if (preferSpeaker) {
                        listOf(
                            AudioDevice.Speakerphone::class.java,
                            AudioDevice.Earpiece::class.java,
                        )
                    } else {
                        listOf(
                            AudioDevice.Earpiece::class.java,
                            AudioDevice.Speakerphone::class.java,
                        )
                    },
                    audioDeviceChangeListener = { devices, selected ->
                        logger.i { "[audioSwitch] audio devices. selected $selected, available devices are $devices" }

                        _devices.value = devices.map { it.fromAudio() }
                        _selectedDevice.value = selected?.fromAudio()

                        setupCompleted = true

                        capturedOnAudioDevicesUpdate?.invoke()
                        capturedOnAudioDevicesUpdate = null
                    },
                )

                logger.d { "[setup] Calling start on instance $audioHandler" }
                audioHandler.start()
            } else {
                logger.d { "[MediaManager#setup] Usage is MEDIA or audioHandle is already initialized" }
            }
        }
    }

    internal fun enforceSetup(preferSpeaker: Boolean = false, actual: () -> Unit) = setup(
        preferSpeaker,
        onAudioDevicesUpdate = actual,
    )

    private fun ifAudioHandlerInitialized(then: (audioHandler: AudioSwitchHandler) -> Unit) {
        if (this::audioHandler.isInitialized) {
            then(this.audioHandler as AudioSwitchHandler)
        } else {
            logger.e { "Audio handler not initialized. Ensure calling setup(), before using the handler." }
        }
    }
}
