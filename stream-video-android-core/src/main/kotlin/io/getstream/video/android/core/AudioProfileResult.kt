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

package io.getstream.video.android.core

import stream.video.sfu.models.AudioBitrateProfile

/**
 * What [MicrophoneManager.setAudioBitrateProfile] managed to change.
 *
 * Every stage is reported separately because they fail independently and for unrelated reasons:
 * a device may have no platform noise suppressor, nothing may be publishing audio yet. A stage
 * that reports false is not an error — it is a stage that is still processing audio the way the
 * previous profile wanted, and the reason is logged.
 *
 * Setting a profile **before joining** reports every stage applied and no bitrate: nothing is
 * capturing or publishing yet, so there is no stage to move and no ceiling to put on a publisher —
 * the pipeline is built from the profile when the call joins, and the SFU picks the bitrate.
 *
 * @property profile The profile that was asked for.
 * @property audioMaxBitrateBps The maximum bitrate now on the live audio sender, or null when the
 * bitrate the SFU negotiated stands — before joining, or when nothing is publishing audio.
 */
public data class AudioProfileResult(
    val profile: AudioBitrateProfile,
    val audioMaxBitrateBps: Int?,
    /**
     * The noise-cancellation processor now matches [profile].
     *
     * True when no processor is configured for this call: nothing is processing the audio, so
     * there is nothing left for this stage to get wrong. False only when a processor is attached
     * and refused — which is what happens when the call is not allowed noise cancellation.
     */
    val noiseCancellationApplied: Boolean,
    /**
     * The platform (hardware) noise suppressor now matches [profile].
     *
     * False when audio is not being captured yet, or the device has no platform noise suppressor.
     * The request is remembered and re-applied when capture restarts either way.
     */
    val platformNoiseSuppressorApplied: Boolean,
    /**
     * WebRTC's software audio processing now matches [profile].
     *
     * False when the audio pipeline could not be rebuilt; the next source built picks the value up.
     */
    val softwareAudioProcessingApplied: Boolean,
    /**
     * The bitrate the profile calls for is in force.
     *
     * False when nothing is publishing audio yet, so no ceiling could be put on a sender.
     */
    val audioMaxBitrateApplied: Boolean,
) {
    /** Every stage reached. */
    val complete: Boolean
        get() = noiseCancellationApplied && platformNoiseSuppressorApplied &&
            softwareAudioProcessingApplied && audioMaxBitrateApplied
}
