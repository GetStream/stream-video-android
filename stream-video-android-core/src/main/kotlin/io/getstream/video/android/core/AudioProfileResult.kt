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
 * What [MicrophoneManager.setAudioBitrateProfile] managed to change, for logging and for naming
 * the stages in a failure. Internal: callers get the profile back on
 * [MicrophoneManager.audioBitrateProfile] and a failure when it did not take.
 *
 * A stage reports true when it matches [profile] *or* when there is nothing for it to move —
 * no live capture, no sender, no such hardware. False means a live stage refused, and the reason
 * is logged.
 *
 * @property audioMaxBitrateBps The bitrate now on the live audio sender, or null when the
 * SFU-negotiated one stands.
 */
internal data class AudioProfileResult(
    val profile: AudioBitrateProfile,
    val audioMaxBitrateBps: Int?,
    val noiseCancellationApplied: Boolean,
    val platformNoiseSuppressorApplied: Boolean,
    val platformAcousticEchoCancelerApplied: Boolean,
    val softwareAudioProcessingApplied: Boolean,
    val audioMaxBitrateApplied: Boolean,
    val captureAudioSourceApplied: Boolean,
) {
    /** Stages that are still processing audio the previous profile's way. */
    val missedStages: List<String>
        get() = buildList {
            if (!noiseCancellationApplied) add("noise cancellation")
            if (!platformNoiseSuppressorApplied) add("hardware noise suppressor")
            if (!platformAcousticEchoCancelerApplied) add("hardware echo canceller")
            if (!softwareAudioProcessingApplied) add("software audio processing")
            if (!audioMaxBitrateApplied) add("max bitrate")
            if (!captureAudioSourceApplied) add("capture audio source")
        }

    /** Every stage reached. */
    val complete: Boolean get() = missedStages.isEmpty()
}
