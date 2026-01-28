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

import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Compatibility wrapper for AudioManager APIs.
 * Supports API 24+ (Android 7.0 Nougat and above).
 */
internal object StreamAudioManager {

    /**
     * Registers an audio device callback to monitor device changes.
     * always available since minSdk is 24 (Available from API 23+)
     */
    fun registerAudioDeviceCallback(
        audioManager: AudioManager,
        callback: AudioDeviceCallback,
        handler: android.os.Handler? = null,
    ): Boolean {
        return try {
            audioManager.registerAudioDeviceCallback(callback, handler)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Unregisters an audio device callback.
     * always available since minSdk is 24 (Available from API 23+)
     */
    fun unregisterAudioDeviceCallback(
        audioManager: AudioManager,
        callback: AudioDeviceCallback,
    ): Boolean {
        return try {
            audioManager.unregisterAudioDeviceCallback(callback)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Gets the list of available communication devices.
     * For API 31+: Uses getAvailableCommunicationDevices()
     * For API 24-30: Uses getDevices(AudioManager.GET_DEVICES_OUTPUTS) to get only output devices
     * (using GET_DEVICES_ALL would include both input and output, causing duplicates for Bluetooth devices)
     */
    fun getAvailableCommunicationDevices(audioManager: AudioManager): List<AudioDeviceInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                audioManager.availableCommunicationDevices
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            return try {
                // Use GET_DEVICES_OUTPUTS instead of GET_DEVICES_ALL to avoid duplicates
                // (Bluetooth devices appear as both input and output, causing duplicates)
                audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Gets the currently selected communication device.
     * On API < 31, returns null.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun getCommunicationDevice(audioManager: AudioManager): AudioDeviceInfo? {
        return try {
            audioManager.communicationDevice
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Sets the communication device
     * For API 31+: Uses setCommunicationDevice()
     * For API < 31: Returns false (use setDeviceLegacy in StreamAudioSwitch instead)
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun setCommunicationDevice(
        audioManager: AudioManager,
        device: AudioDeviceInfo?,
    ): Boolean {
        return try {
            if (device != null) {
                audioManager.setCommunicationDevice(device)
            }
            audioManager.clearCommunicationDevice()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clears the communication device selection.
     * On API < 31, uses legacy AudioManager APIs.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun clearCommunicationDevice(audioManager: AudioManager): Boolean {
        return try {
            audioManager.clearCommunicationDevice()
            true
        } catch (e: Exception) {
            false
        }
    }
}
