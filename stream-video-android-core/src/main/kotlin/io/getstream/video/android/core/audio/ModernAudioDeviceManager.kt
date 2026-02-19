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

    override fun clearDevice() {
        StreamAudioManager.clearCommunicationDevice(audioManager)
        selectedDevice = null
    }

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

    override fun start() {
        // No special setup needed for modern API
    }

    override fun stop() {
        clearDevice()
    }

    public companion object {
        private const val TAG = "ModernAudioDeviceManager"
    }
}
