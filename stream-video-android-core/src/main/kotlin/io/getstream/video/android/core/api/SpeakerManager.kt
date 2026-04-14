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

/**
 * Manages the speaker output for a video call.
 *
 * Provides controls for enabling/disabling the speaker, toggling speakerphone,
 * adjusting volume, and observing speaker state.
 *
 * This interface is not intended for external implementation. The SDK provides
 * the only supported implementation. New members may be added in minor releases.
 */
@Stable
public interface SpeakerManager {

    /** The status of the speaker (enabled, disabled, or not selected). */
    public val status: StateFlow<DeviceStatus>

    /** Whether the speaker is currently enabled. */
    public val isEnabled: StateFlow<Boolean>

    /** The currently selected audio output device. */
    public val selectedDevice: StateFlow<StreamAudioDevice?>

    /** List of available audio output devices. */
    public val devices: StateFlow<List<StreamAudioDevice>>

    /** The current volume level (0-100), or null if not set. */
    public val volume: StateFlow<Int?>

    /** Whether the speakerphone mode is enabled. */
    public val speakerPhoneEnabled: StateFlow<Boolean>

    /** The current audio usage value. */
    public val audioUsage: StateFlow<Int>

    /** Enables the speaker. */
    public fun enable(fromUser: Boolean = true)

    /** Disables the speaker. */
    public fun disable(fromUser: Boolean = true)

    /** Enables or disables the speaker. */
    public fun setEnabled(enabled: Boolean, fromUser: Boolean = true)

    /**
     * Enables or disables speakerphone mode.
     *
     * @param enable Whether to enable the speakerphone.
     * @param defaultFallback The device to fall back to when disabling the speakerphone.
     */
    public fun setSpeakerPhone(enable: Boolean, defaultFallback: StreamAudioDevice? = null)

    /**
     * Sets the volume as a percentage.
     *
     * @param volumePercentage Volume level from 0 to 100.
     */
    public fun setVolume(volumePercentage: Int)

    /**
     * Sets the audio usage value (e.g., USAGE_MEDIA or USAGE_VOICE_COMMUNICATION).
     *
     * @param audioUsage The audio usage value to set.
     * @return true if the update was successful, false otherwise.
     */
    public fun setAudioUsage(audioUsage: Int): Boolean

    /** Pauses audio output, remembering the prior volume for resume. */
    public fun pause()

    /** Resumes audio output to the volume before pause was called. */
    public fun resume()
}
