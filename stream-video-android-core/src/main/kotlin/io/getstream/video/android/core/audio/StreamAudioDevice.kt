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
import com.twilio.audioswitch.AudioDevice

// Lazy initialization of default Twilio AudioDevice instances
private val defaultBluetoothHeadsetAudioDevice: AudioDevice by lazy {
    StreamAudioDevice.Companion.createTwilioBluetoothHeadset()
}
private val defaultWiredHeadsetAudioDevice: AudioDevice by lazy {
    StreamAudioDevice.Companion.createTwilioWiredHeadset()
}
private val defaultEarpieceAudioDevice: AudioDevice by lazy {
    StreamAudioDevice.Companion.createTwilioEarpiece()
}
private val defaultSpeakerphoneAudioDevice: AudioDevice by lazy {
    StreamAudioDevice.Companion.createTwilioSpeakerphone()
}

/**
 * Represents an audio device for audio switching.
 * Supports both Twilio's AudioSwitch implementation and native Android audio device management.
 *
 * @see AudioDevice
 * @see AudioDeviceInfo
 */
public sealed class StreamAudioDevice {

    /** The friendly name of the device.*/
    public abstract val name: String

    /**
     * The Twilio AudioDevice instance.
     * Used when using Twilio's AudioSwitch implementation.
     */
    public abstract val audio: AudioDevice

    /**
     * The Android AudioDeviceInfo instance.
     * This provides device identification and capabilities when using native Android audio management.
     * @see android.media.AudioDeviceInfo
     */
    public abstract val audioDeviceInfo: AudioDeviceInfo?

    /** A [StreamAudioDevice] representing a Bluetooth Headset.*/
    public data class BluetoothHeadset constructor(
        override val name: String = "Bluetooth",
        override val audio: AudioDevice = defaultBluetoothHeadsetAudioDevice,
        override val audioDeviceInfo: AudioDeviceInfo? = null,
    ) : StreamAudioDevice()

    /** A [StreamAudioDevice] representing a Wired Headset.*/
    public data class WiredHeadset constructor(
        override val name: String = "Wired Headset",
        override val audio: AudioDevice = defaultWiredHeadsetAudioDevice,
        override val audioDeviceInfo: AudioDeviceInfo? = null,
    ) : StreamAudioDevice()

    /** A [StreamAudioDevice] representing the Earpiece.*/
    public data class Earpiece constructor(
        override val name: String = "Earpiece",
        override val audio: AudioDevice = defaultEarpieceAudioDevice,
        override val audioDeviceInfo: AudioDeviceInfo? = null,
    ) : StreamAudioDevice()

    /** A [StreamAudioDevice] representing the Speakerphone.*/
    public data class Speakerphone constructor(
        override val name: String = "Speakerphone",
        override val audio: AudioDevice = defaultSpeakerphoneAudioDevice,
        override val audioDeviceInfo: AudioDeviceInfo? = null,
    ) : StreamAudioDevice()

    public companion object {
        @JvmStatic
        public fun StreamAudioDevice.toAudioDevice(): AudioDevice {
            return this.audio
        }

        @JvmStatic
        public fun AudioDevice.fromAudio(): StreamAudioDevice {
            return when (this) {
                is AudioDevice.BluetoothHeadset -> BluetoothHeadset(audio = this)
                is AudioDevice.WiredHeadset -> WiredHeadset(audio = this)
                is AudioDevice.Earpiece -> Earpiece(audio = this)
                is AudioDevice.Speakerphone -> Speakerphone(audio = this)
            }
        }

        /**
         * Converts an Android AudioDeviceInfo to a StreamAudioDevice.
         * Returns null if the device type is not supported.
         * Available from API 23+ (always available since minSdk is 24).
         */
        @JvmStatic
        public fun fromAudioDeviceInfo(deviceInfo: AudioDeviceInfo): StreamAudioDevice? {
            return when (deviceInfo.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                -> {
                    BluetoothHeadset(
                        audioDeviceInfo = deviceInfo,
                    )
                }
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                -> {
                    WiredHeadset(
                        audioDeviceInfo = deviceInfo,
                    )
                }
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> {
                    Earpiece(
                        audioDeviceInfo = deviceInfo,
                    )
                }
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> {
                    Speakerphone(
                        audioDeviceInfo = deviceInfo,
                    )
                }
                else -> null
            }
        }

        /**
         * Converts a StreamAudioDevice to an AudioDeviceInfo by finding a matching device
         * from the available communication devices.
         * Returns null if no matching device is found.
         */
        @RequiresApi(Build.VERSION_CODES.S)
        @JvmStatic
        public fun toAudioDeviceInfo(
            streamDevice: StreamAudioDevice,
            audioManager: AudioManager,
        ): AudioDeviceInfo? {
            // If the device already has an AudioDeviceInfo, use it
            val existingInfo = streamDevice.audioDeviceInfo?.id

            // Otherwise, try to find a matching device from available devices
            // For API 31+: use communication devices, for API 24-30: use all output devices
            val availableDevices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val commDevices = StreamAudioManager.getAvailableCommunicationDevices(audioManager)
                if (existingInfo != null) {
                    return commDevices.find { it.id == existingInfo }
                }
                commDevices
            } else {
                // For API < 31, use getDevices() to get all output devices
                try {
                    audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
                } catch (e: Exception) {
                    emptyList()
                }
            }

            return when (streamDevice) {
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

        /**
         * Creates a Twilio AudioDevice.BluetoothHeadset instance using reflection.
         */
        internal fun createTwilioBluetoothHeadset(): AudioDevice {
            return createTwilioAudioDevice(AudioDevice.BluetoothHeadset::class.java)
        }

        /**
         * Creates a Twilio AudioDevice.WiredHeadset instance using reflection.
         */
        internal fun createTwilioWiredHeadset(): AudioDevice {
            return createTwilioAudioDevice(AudioDevice.WiredHeadset::class.java)
        }

        /**
         * Creates a Twilio AudioDevice.Earpiece instance using reflection.
         */
        internal fun createTwilioEarpiece(): AudioDevice {
            return createTwilioAudioDevice(AudioDevice.Earpiece::class.java)
        }

        /**
         * Creates a Twilio AudioDevice.Speakerphone instance using reflection.
         */
        internal fun createTwilioSpeakerphone(): AudioDevice {
            return createTwilioAudioDevice(AudioDevice.Speakerphone::class.java)
        }

        /**
         * Generic method to create Twilio AudioDevice instances using reflection.
         * Accesses the private constructor and creates an instance.
         */
        @Suppress("UNCHECKED_CAST")
        private fun <T : AudioDevice> createTwilioAudioDevice(deviceClass: Class<T>): T {
            // Get all declared constructors
            val constructors = deviceClass.declaredConstructors
            if (constructors.isEmpty()) {
                throw IllegalStateException("No constructor found for ${deviceClass.simpleName}")
            }

            // Try each constructor with different parameter combinations
            var lastException: Exception? = null
            for (constructor in constructors) {
                try {
                    constructor.isAccessible = true
                    val paramTypes = constructor.parameterTypes

                    // Create appropriate arguments based on parameter types
                    val args = paramTypes.map { paramType ->
                        when {
                            paramType == String::class.java -> ""
                            paramType.isPrimitive -> when (paramType.name) {
                                "int", "long" -> 0
                                "boolean" -> false
                                "float", "double" -> 0.0
                                else -> null
                            }
                            else -> null
                        }
                    }.toTypedArray()

                    @Suppress("UNCHECKED_CAST")
                    return constructor.newInstance(*args) as T
                } catch (e: Exception) {
                    lastException = e
                    // Try next constructor
                    continue
                }
            }

            throw IllegalStateException(
                "Failed to create Twilio AudioDevice instance for ${deviceClass.simpleName}. " +
                    "None of the constructors could be invoked.",
                lastException,
            )
        }
    }
}
