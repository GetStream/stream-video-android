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

package io.getstream.video.android.core.api

import androidx.compose.runtime.Stable
import io.getstream.video.android.core.CameraDeviceWrapped
import io.getstream.video.android.core.CameraDirection
import io.getstream.video.android.core.DeviceStatus
import io.getstream.webrtc.CameraEnumerationAndroid
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the local camera for a video call.
 *
 * Provides controls for enabling/disabling the camera, flipping between front and back,
 * selecting specific devices, and observing camera state.
 */
@Stable
public interface CameraManager {

    /** The status of the camera (enabled, disabled, or not selected). */
    public val status: StateFlow<DeviceStatus>

    /** Whether the camera is currently enabled. */
    public val isEnabled: StateFlow<Boolean>

    /** The current camera direction (front or back). */
    public val direction: StateFlow<CameraDirection>

    /** The currently selected camera device. */
    public val selectedDevice: StateFlow<CameraDeviceWrapped?>

    /** The currently selected camera resolution. */
    public val resolution: StateFlow<CameraEnumerationAndroid.CaptureFormat?>

    /** The available resolutions for the selected camera device. */
    public val availableResolutions: StateFlow<List<CameraEnumerationAndroid.CaptureFormat>>

    /** Flips the camera between front and back. */
    public fun flip()

    /** Enables the camera. */
    public fun enable(fromUser: Boolean = true)

    /** Disables the camera. */
    public fun disable(fromUser: Boolean = true)

    /** Enables or disables the camera. */
    public fun setEnabled(enabled: Boolean, fromUser: Boolean = true)

    /** Sets the camera direction (front or back). */
    public fun setDirection(cameraDirection: CameraDirection)

    /** Returns the list of available camera devices. */
    public fun listDevices(): List<CameraDeviceWrapped>

    /** Selects a specific camera device by ID. */
    public fun select(deviceId: String, triggeredByFlip: Boolean = false)

    /** Pauses the camera, remembering the prior state for resume. */
    public fun pause(fromUser: Boolean = true)

    /** Resumes the camera to the state before pause was called. */
    public fun resume(fromUser: Boolean = true)
}
