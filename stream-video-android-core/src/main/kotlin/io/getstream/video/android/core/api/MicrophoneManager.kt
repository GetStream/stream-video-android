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
import io.getstream.video.android.core.DeviceStatus
import io.getstream.video.android.core.audio.StreamAudioDevice
import kotlinx.coroutines.flow.StateFlow
import stream.video.sfu.models.AudioBitrateProfile

/**
 * Manages the local microphone for a video call.
 *
 * Provides controls for enabling/disabling the microphone, selecting audio devices,
 * and observing microphone state.
 */
@Stable
public interface MicrophoneManager {

    /** The status of the microphone (enabled, disabled, or not selected). */
    public val status: StateFlow<DeviceStatus>

    /** Whether the microphone is currently enabled. */
    public val isEnabled: StateFlow<Boolean>

    /** The currently selected audio device. */
    public val selectedDevice: StateFlow<StreamAudioDevice?>

    /** List of available audio devices. */
    public val devices: StateFlow<List<StreamAudioDevice>>

    /** The current audio bitrate profile. */
    public val audioBitrateProfile: StateFlow<AudioBitrateProfile>

    /** Enables the microphone. */
    public fun enable(fromUser: Boolean = true)

    /** Disables the microphone. */
    public fun disable(fromUser: Boolean = true)

    /** Enables or disables the microphone. */
    public fun setEnabled(enabled: Boolean, fromUser: Boolean = true)

    /** Selects a specific audio device. */
    public fun select(device: StreamAudioDevice?)

    /** Returns a StateFlow of available audio devices. */
    public fun listDevices(): StateFlow<List<StreamAudioDevice>>

    /**
     * Sets the audio bitrate profile. Can only be set before joining the call.
     *
     * @param profile The audio bitrate profile to use.
     * @return Result indicating success or failure.
     */
    public suspend fun setAudioBitrateProfile(profile: AudioBitrateProfile): Result<Unit>

    /** Pauses the microphone, remembering the prior state for resume. */
    public fun pause(fromUser: Boolean = true)

    /** Resumes the microphone to the state before pause was called. */
    public fun resume(fromUser: Boolean = true)
}
