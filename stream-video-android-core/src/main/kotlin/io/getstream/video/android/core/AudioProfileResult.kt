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
 * What [MicrophoneManager.applyAudioProfile] managed to change.
 *
 * Every stage is reported separately because they fail independently and for unrelated reasons:
 * a device may have no platform noise suppressor, nothing may be publishing audio yet. A stage
 * that reports false is not an error — it is a stage that is still processing audio the way the
 * previous profile wanted, and the reason is logged.
 *
 * @property profile The profile that was asked for.
 * @property audioMaxBitrateBps The maximum audio bitrate requested for [profile].
 */
public data class AudioProfileResult(
    val profile: AudioBitrateProfile,
    val audioMaxBitrateBps: Int,
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
     * [audioMaxBitrateBps] reached the live publisher.
     *
     * False when nothing is publishing audio yet.
     */
    val audioMaxBitrateApplied: Boolean,
) {
    /** Every stage reached. */
    val complete: Boolean
        get() = noiseCancellationApplied && platformNoiseSuppressorApplied &&
            softwareAudioProcessingApplied && audioMaxBitrateApplied
}
