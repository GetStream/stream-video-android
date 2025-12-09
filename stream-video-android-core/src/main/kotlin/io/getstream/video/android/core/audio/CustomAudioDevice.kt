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

package io.getstream.video.android.core.audio

import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Represents an audio device for the native Android audio switch implementation.
 * This class is used when [useCustomAudioSwitch] is true.
 *
 * Unlike [StreamAudioDevice], this class does not depend on Twilio's AudioDevice.
 * It uses Android's native [AudioDeviceInfo] for device identification and management.
 */
public sealed class CustomAudioDevice {

    /** The friendly name of the device.*/
    public abstract val name: String

    /**
     * The Android AudioDeviceInfo instance.
     * This provides device identification and capabilities.
     * @see android.media.AudioDeviceInfo
     */
    public abstract val audioDeviceInfo: AudioDeviceInfo?

    /** A [CustomAudioDevice] representing a Bluetooth Headset.*/
    public data class BluetoothHeadset constructor(
        override val name: String = "Bluetooth",
        override val audioDeviceInfo: AudioDeviceInfo? = null,
    ) : CustomAudioDevice()

    /** A [CustomAudioDevice] representing a Wired Headset.*/
    public data class WiredHeadset constructor(
        override val name: String = "Wired Headset",
        override val audioDeviceInfo: AudioDeviceInfo? = null,
    ) : CustomAudioDevice()

    /** A [CustomAudioDevice] representing the Earpiece.*/
    public data class Earpiece constructor(
        override val name: String = "Earpiece",
        override val audioDeviceInfo: AudioDeviceInfo? = null,
    ) : CustomAudioDevice()

    /** A [CustomAudioDevice] representing the Speakerphone.*/
    public data class Speakerphone constructor(
        override val name: String = "Speakerphone",
        override val audioDeviceInfo: AudioDeviceInfo? = null,
    ) : CustomAudioDevice()

    public companion object {
        /**
         * Converts an Android AudioDeviceInfo to a NativeStreamAudioDevice.
         * Returns null if the device type is not supported.
         * Available from API 23+ (always available since minSdk is 24).
         */
        @JvmStatic
        public fun fromAudioDeviceInfo(deviceInfo: AudioDeviceInfo): CustomAudioDevice? {
            return when (deviceInfo.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                -> {
                    CustomAudioDevice.BluetoothHeadset(
                        audioDeviceInfo = deviceInfo,
                    )
                }
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                -> {
                    CustomAudioDevice.WiredHeadset(
                        audioDeviceInfo = deviceInfo,
                    )
                }
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> {
                    CustomAudioDevice.Earpiece(
                        audioDeviceInfo = deviceInfo,
                    )
                }
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> {
                    CustomAudioDevice.Speakerphone(
                        audioDeviceInfo = deviceInfo,
                    )
                }
                else -> null
            }
        }

        /**
         * Converts a NativeStreamAudioDevice to an AudioDeviceInfo by finding a matching device
         * from the available communication devices.
         * Returns null if no matching device is found.
         */
        @RequiresApi(Build.VERSION_CODES.S)
        @JvmStatic
        public fun toAudioDeviceInfo(
            nativeDevice: CustomAudioDevice,
            audioManager: AudioManager,
        ): AudioDeviceInfo? {
            // If the device already has an AudioDeviceInfo, use it
            val existingInfo = when (nativeDevice) {
                is BluetoothHeadset -> nativeDevice.audioDeviceInfo?.id
                is WiredHeadset -> nativeDevice.audioDeviceInfo?.id
                is Earpiece -> nativeDevice.audioDeviceInfo?.id
                is Speakerphone -> nativeDevice.audioDeviceInfo?.id
            }

            // Otherwise, try to find a matching device from available devices
            // For API 31+: use communication devices, for API 24-30: use all output devices
            val availableDevices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return StreamAudioManager.getAvailableCommunicationDevices(audioManager).find {
                    it.id == existingInfo
                }
            } else {
                // For API < 31, use getDevices() to get all output devices
                try {
                    audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
                } catch (e: Exception) {
                    emptyList()
                }
            }

            return when (nativeDevice) {
                is BluetoothHeadset -> {
                    availableDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                    }
                }
                is WiredHeadset -> {
                    availableDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                            it.type == AudioDeviceInfo.TYPE_USB_HEADSET
                    }
                }
                is Earpiece -> {
                    availableDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                    }
                }
                is Speakerphone -> {
                    // For speakerphone, also check all devices if not found in communication devices
                    // Speakerphone might not always be in availableCommunicationDevices
                    availableDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    } ?: run {
                        // Fallback: try to get from all devices if not in communication devices
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            try {
                                audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull {
                                    it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                                }
                            } catch (e: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                    }
                }
            }
        }
    }
}
