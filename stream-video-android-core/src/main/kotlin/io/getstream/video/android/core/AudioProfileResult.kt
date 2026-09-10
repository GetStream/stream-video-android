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
     * True when nothing is capturing yet: there is no recording session to attach the effect
     * to, and the request is re-applied when capture starts. A device with no suppressor has
     * nothing suppressing, so the profile is satisfied. False only when a live suppressor refused.
     */
    val platformNoiseSuppressorApplied: Boolean,
    /**
     * The platform (hardware) acoustic echo canceller now matches [profile].
     *
     * Same rule as [platformNoiseSuppressorApplied]: a device with no canceller has nothing
     * cancelling, so the profile is satisfied. False when a live canceller refused.
     */
    val platformAcousticEchoCancelerApplied: Boolean,
    /**
     * WebRTC's software audio processing now matches [profile].
     *
     * True when nothing is publishing audio yet: there is no source to rebuild, and the next
     * one is built from [profile]. False only when a live pipeline rebuild failed.
     */
    val softwareAudioProcessingApplied: Boolean,
    /**
     * The bitrate the profile calls for is in force.
     *
     * True when nothing is publishing audio yet: there is no ceiling to move, and the next
     * audio transceiver is built from [profile]. False only when a live sender refused the
     * new parameters.
     */
    val audioMaxBitrateApplied: Boolean,
    /**
     * The capture audio source now matches [profile]: MIC under MUSIC_HIGH_QUALITY,
     * VOICE_COMMUNICATION otherwise.
     *
     * False when no audio device module exists yet, or the platform refused the source and
     * restored the last one that worked. The audio mode is not this stage — it is applied
     * first so a new AudioRecord opens under the right graph.
     */
    val captureAudioSourceApplied: Boolean,
) {
    /** Every stage reached. */
    val complete: Boolean
        get() = noiseCancellationApplied && platformNoiseSuppressorApplied &&
            platformAcousticEchoCancelerApplied &&
            softwareAudioProcessingApplied && audioMaxBitrateApplied &&
            captureAudioSourceApplied
}
