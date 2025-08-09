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

import android.media.AudioManager
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.core.utils.mapState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SpeakerManager(
    val mediaManager: MediaManagerImpl,
    val microphoneManager: MicrophoneManager,
    val initialVolume: Int? = null,
) {

    private val logger by taggedLogger("Media:SpeakerManager")

    private var priorVolume: Int? = null
    private val _volume = MutableStateFlow(initialVolume)
    val volume: StateFlow<Int?> = _volume

    /** The status of the audio */
    internal val _status = MutableStateFlow<DeviceStatus>(DeviceStatus.NotSelected)
    val status: StateFlow<DeviceStatus> = _status

    /** Represents whether the speakerphone is enabled */
    public val isEnabled: StateFlow<Boolean> = _status.mapState { it is DeviceStatus.Enabled }

    val selectedDevice: StateFlow<StreamAudioDevice?> = microphoneManager.selectedDevice

    val devices: StateFlow<List<StreamAudioDevice>> = microphoneManager.devices

    private val _speakerPhoneEnabled = MutableStateFlow(true)
    val speakerPhoneEnabled: StateFlow<Boolean> = _speakerPhoneEnabled

    internal var selectedBeforeSpeaker: StreamAudioDevice? = null

    internal fun enable(fromUser: Boolean = true) {
        if (fromUser) {
            _status.value = DeviceStatus.Enabled
        }
        setSpeakerPhone(true)
    }

    fun disable(fromUser: Boolean = true) {
        if (fromUser) {
            _status.value = DeviceStatus.Disabled
        }
        setSpeakerPhone(false)
    }

    /**
     * Enable or disable the speakerphone.
     */
    fun setEnabled(enabled: Boolean, fromUser: Boolean = true) {
        logger.i { "setEnabled $enabled" }
        // TODO: what is fromUser?
        if (enabled) {
            enable(fromUser = fromUser)
        } else {
            disable(fromUser = fromUser)
        }
    }

    /**
     * Enables or disables the speakerphone.
     *
     * When the speaker is disabled the device that gets selected next is by default the first device
     * that is NOT a speakerphone. To override this use [defaultFallback].
     * If you want the earpice to be selected if the speakerphone is disabled do
     * ```kotlin
     * setSpeakerPhone(enable, StreamAudioDevice.Earpiece)
     * ```
     *
     * @param enable if true, enables the speakerphone, if false disables it and selects another device.
     * @param defaultFallback when [enable] is false this is used to select the next device after the speaker.
     * */
    fun setSpeakerPhone(enable: Boolean, defaultFallback: StreamAudioDevice? = null) {
        microphoneManager.enforceSetup(preferSpeaker = enable) {
            val devices = devices.value
            if (enable) {
                val speaker =
                    devices.filterIsInstance<StreamAudioDevice.Speakerphone>().firstOrNull()
                selectedBeforeSpeaker = selectedDevice.value.takeUnless {
                    it is StreamAudioDevice.Speakerphone
                } ?: devices.firstOrNull {
                    it !is StreamAudioDevice.Speakerphone
                }

                logger.d { "#deviceDebug; selectedBeforeSpeaker: $selectedBeforeSpeaker" }

                _speakerPhoneEnabled.value = true
                microphoneManager.select(speaker)
            } else {
                _speakerPhoneEnabled.value = false
                // swap back to the old one
                val defaultFallbackFromType = defaultFallback?.let {
                    devices.filterIsInstance(defaultFallback::class.java)
                }?.firstOrNull()

                val firstNonSpeaker = devices.firstOrNull { it !is StreamAudioDevice.Speakerphone }

                val fallback: StreamAudioDevice? = when {
                    defaultFallbackFromType != null -> defaultFallbackFromType
                    selectedBeforeSpeaker != null &&
                        selectedBeforeSpeaker !is StreamAudioDevice.Speakerphone &&
                        devices.contains(selectedBeforeSpeaker) -> selectedBeforeSpeaker

                    else -> firstNonSpeaker
                }

                microphoneManager.select(fallback)
            }
        }
    }

    /**
     * Set the volume as a percentage, 0-100
     */
    fun setVolume(volumePercentage: Int) {
        microphoneManager.enforceSetup {
            microphoneManager.audioManager?.let {
                val max = it.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
                val level = max / 100 * volumePercentage
                _volume.value = volumePercentage
                it.setStreamVolume(AudioManager.STREAM_VOICE_CALL, level, 0)
            }
        }
    }

    fun pause() {
        priorVolume = _volume.value
        setVolume(0)
    }

    fun resume() {
        priorVolume?.let {
            if (it > 0) {
                setVolume(it)
            }
        }
    }
}
