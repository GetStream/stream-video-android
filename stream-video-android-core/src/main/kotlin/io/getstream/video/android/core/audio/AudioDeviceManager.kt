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
 * Uses StreamAudioDevice for the custom audio switch implementation.
 */
internal interface AudioDeviceManager {
    /**
 * Lists the currently available audio devices.
 *
 * @return A list of available StreamAudioDevice instances representing detected audio input/output devices.
 */
    fun enumerateDevices(): List<StreamAudioDevice>

    /**
 * Sets the active audio routing device.
 *
 * @param device The device to make active for audio routing.
 * @return `true` if the device was successfully selected, `false` otherwise.
 */
    fun selectDevice(device: StreamAudioDevice): Boolean

    /**
     * Clears the current device selection.
     */
    fun clearDevice()

    /**
 * Returns the currently selected audio device.
 *
 * @return The selected StreamAudioDevice, or `null` if no device is selected.
 */
    fun getSelectedDevice(): StreamAudioDevice?

    /**
 * Initializes the device manager and prepares it for use.
 */
    fun start()

    /**
 * Releases resources and tears down the device manager.
 *
 * Stops device monitoring, unregisters listeners or observers, and performs any necessary cleanup so the manager is no longer active. The manager must be started again before it can be used after this call.
 */
    fun stop()
}