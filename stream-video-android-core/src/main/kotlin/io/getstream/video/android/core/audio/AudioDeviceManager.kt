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

/**
 * Interface for managing audio device operations.
 * Different implementations handle API level differences.
 * Uses NativeStreamAudioDevice for the custom audio switch implementation.
 */
internal interface AudioDeviceManager {
    /**
     * Enumerates available audio devices.
     */
    fun enumerateDevices(): List<CustomAudioDevice>

    /**
     * Selects an audio device for routing.
     * @param device The device to select
     * @return true if selection was successful, false otherwise
     */
    fun selectDevice(device: CustomAudioDevice): Boolean

    /**
     * Clears the current device selection.
     */
    fun clearDevice()

    /**
     * Gets the currently selected device.
     */
    fun getSelectedDevice(): CustomAudioDevice?

    /**
     * Starts the device manager (registers listeners, etc.)
     */
    fun start()

    /**
     * Stops the device manager (unregisters listeners, etc.)
     */
    fun stop()
}
