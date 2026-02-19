/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Represents a USB audio input device (microphone) that may not be detected by AudioSwitch.
 *
 * USB microphones that are input-only (no speaker output) are not recognized by Twilio's
 * AudioSwitch library because it only detects devices that trigger ACTION_HEADSET_PLUG,
 * which requires both input and output capabilities.
 *
 * This class wraps Android's [AudioDeviceInfo] to provide access to USB microphones
 * like the Rode Wireless Go II and similar professional audio equipment.
 *
 * @property deviceInfo The underlying Android AudioDeviceInfo
 * @property name The product name of the device (e.g., "Rode Wireless Go II")
 * @property id The unique device ID for routing
 * @property type The AudioDeviceInfo type (e.g., TYPE_USB_DEVICE)
 */
@RequiresApi(Build.VERSION_CODES.M)
data class UsbAudioInputDevice(
    val deviceInfo: AudioDeviceInfo,
) {
    /** The product name of the USB device */
    val name: String
        get() = deviceInfo.productName?.toString() ?: "USB Microphone"

    /** Unique device ID */
    val id: Int
        get() = deviceInfo.id

    /** The device type from AudioDeviceInfo */
    val type: Int
        get() = deviceInfo.type

    /** Human-readable type name */
    val typeName: String
        get() = when (type) {
            AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Device"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
            AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB Accessory"
            else -> "USB Audio"
        }

    companion object {
        /** USB device types that represent input-capable devices */
        val USB_INPUT_TYPES = listOf(
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_ACCESSORY,
        )

        /**
         * Check if an AudioDeviceInfo represents a USB input device
         */
        fun AudioDeviceInfo.isUsbInputDevice(): Boolean {
            return type in USB_INPUT_TYPES && isSource
        }
    }
}
