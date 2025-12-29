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
     * Register an AudioDeviceCallback to receive audio device change events.
     *
     * @param audioManager The AudioManager used to register the callback.
     * @param callback The AudioDeviceCallback to register.
     * @param handler Optional Handler on which callback methods will be invoked; if null the system selects the caller's thread.
     * @return `true` if registration succeeded, `false` if an exception occurred.
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
     * Unregisters an AudioDeviceCallback from the given AudioManager.
     *
     * @param callback The AudioDeviceCallback to unregister.
     * @return `true` if the callback was unregistered successfully, `false` if an exception occurred.
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
     * Retrieve the list of available communication devices.
     *
     * On API 31+ this reflects the platform communication devices; on earlier API levels it returns available output devices.
     *
     * @param audioManager The AudioManager to query.
     * @return A list of AudioDeviceInfo representing available communication devices; an empty list is returned if querying fails.
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
     * Retrieves the currently selected communication device.
     *
     * @return The selected communication `AudioDeviceInfo`, or `null` if no device is selected or an error occurs. 
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
     * Sets or clears the current communication audio device.
     *
     * @param device The AudioDeviceInfo to select for communication; pass `null` to clear the selection.
     * @return `true` if the device was set or cleared successfully, `false` otherwise.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun setCommunicationDevice(
        audioManager: AudioManager,
        device: AudioDeviceInfo?,
    ): Boolean {
        return try {
            if (device != null) {
                audioManager.setCommunicationDevice(device)
            } else {
                audioManager.clearCommunicationDevice()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clears the currently selected communication device.
     *
     * @return `true` if the communication device was cleared successfully, `false` otherwise.
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