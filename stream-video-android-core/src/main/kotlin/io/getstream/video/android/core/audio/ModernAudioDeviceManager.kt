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

import android.media.AudioManager
import androidx.annotation.RequiresApi
import io.getstream.log.taggedLogger

/**
 * Audio device manager for API 31+ (Android S and above).
 * Uses modern communication device APIs.
 */
@RequiresApi(android.os.Build.VERSION_CODES.S)
internal class ModernAudioDeviceManager(
    private val audioManager: AudioManager,
) : AudioDeviceManager {

    private val logger by taggedLogger(TAG)
    private var selectedDevice: StreamAudioDevice? = null

    /**
     * Enumerates available communication audio devices and returns them as StreamAudioDevice instances.
     *
     * Converts Android AudioDeviceInfo entries reported by StreamAudioManager into StreamAudioDevice objects,
     * omitting any devices that cannot be converted.
     *
     * @return A list of converted StreamAudioDevice objects representing available communication devices.
     */
    override fun enumerateDevices(): List<StreamAudioDevice> {
        val androidDevices = StreamAudioManager.getAvailableCommunicationDevices(audioManager)
        logger.d { "[enumerateDevices] Found ${androidDevices.size} available communication devices" }

        // Log details of each available device
        androidDevices.forEachIndexed { index, device ->
            logger.d {
                "[enumerateDevices] Device $index: type=${device.type}, " +
                    "name=${device.productName}, " +
                    "id=${device.id}, " +
                    "address=${device.address}"
            }
        }

        val streamAudioDevices = mutableListOf<StreamAudioDevice>()

        for (androidDevice in androidDevices) {
            val streamAudioDevice = StreamAudioDevice.fromAudioDeviceInfo(androidDevice)
            if (streamAudioDevice != null) {
                logger.d {
                    "[enumerateDevices] Detected device: ${streamAudioDevice::class.simpleName} (${streamAudioDevice.name})"
                }
                streamAudioDevices.add(streamAudioDevice)
            } else {
                logger.w {
                    "[enumerateDevices] Could not convert AudioDeviceInfo to StreamAudioDevice: type=${androidDevice.type}, name=${androidDevice.productName}"
                }
            }
        }

        logger.d { "[enumerateDevices] Total enumerated devices: ${streamAudioDevices.size}" }
        streamAudioDevices.forEachIndexed { index, device ->
            logger.d { "[enumerateDevices] Final device $index: ${device::class.simpleName} (${device.name})" }
        }

        return streamAudioDevices
    }

    /**
     * Selects the given StreamAudioDevice as the system communication audio device.
     *
     * If the platform AudioDeviceInfo for the device can be resolved and setting it succeeds, the manager's selected device is updated.
     *
     * @param device The StreamAudioDevice to select.
     * @return `true` if the device was successfully set as the communication device, `false` otherwise.
     */
    override fun selectDevice(device: StreamAudioDevice): Boolean {
        val androidDevice = device.audioDeviceInfo
            ?: StreamAudioDevice.toAudioDeviceInfo(device, audioManager)
        logger.d { "[selectDevice] :: $device" }
        return if (androidDevice != null) {
            val success = StreamAudioManager.setCommunicationDevice(audioManager, androidDevice)
            if (success) {
                selectedDevice = device
            }
            success
        } else {
            false
        }
    }

    /**
     * Clears the current communication audio device and resets the cached selection.
     *
     * This removes any device previously set as the communication device from the system
     * and sets the manager's selectedDevice to null.
     */
    override fun clearDevice() {
        StreamAudioManager.clearCommunicationDevice(audioManager)
        selectedDevice = null
    }

    /**
     * Retrieve the currently active communication audio device.
     *
     * Attempts to read the active communication device from the system AudioManager and convert it to a StreamAudioDevice; if conversion succeeds, caches and returns that device. If no system device is available or conversion fails, returns the locally cached selection.
     *
     * @return The current StreamAudioDevice if available and convertible, otherwise the cached selected device, or `null` if none is set.
     */
    override fun getSelectedDevice(): StreamAudioDevice? {
        // Try to get from AudioManager first
        val currentDevice = StreamAudioManager.getCommunicationDevice(audioManager)
        if (currentDevice != null) {
            val streamAudioDevice = StreamAudioDevice.fromAudioDeviceInfo(currentDevice)
            if (streamAudioDevice != null) {
                selectedDevice = streamAudioDevice
                return streamAudioDevice
            }
        }
        return selectedDevice
    }

    /**
     * Initialize audio device management resources if required.
     *
     * This implementation is a no-op for Android API 31+ because no startup work is necessary.
     */
    override fun start() {
        // No special setup needed for modern API
    }

    /**
     * Stops the audio device manager and clears any selected communication device.
     *
     * Clears the manager's selected device state so no communication device remains set.
     */
    override fun stop() {
        clearDevice()
    }

    public companion object {
        private const val TAG = "ModernAudioDeviceManager"
    }
}