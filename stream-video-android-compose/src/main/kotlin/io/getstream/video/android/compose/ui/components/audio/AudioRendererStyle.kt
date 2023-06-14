/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.compose.ui.components.audio

import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment

/**
 * Represents audio room render styles.
 *
 * @param isSpeaking Represents whether the participant is speaking or not.
 * @param isShowingMicrophoneAvailability Represents whether displays the microphone availability indicator or not.
 * @param microphoneAvailabilityLabel The position of the microphone availability indicator.
 * @param isShowingRoleBadge Represents whether the displays the role badge or not.
 */
@Stable
public sealed class AudioRendererStyle(
    public open val isSpeaking: Boolean,
    public open val isShowingMicrophoneAvailability: Boolean,
    public open val microphoneAvailabilityLabel: Alignment,
    public open val isShowingRoleBadge: Boolean
)

/**
 * Represents audio room render styles.
 *
 * @param isSpeaking Represents whether the participant is speaking or not.
 * @param isShowingMicrophoneAvailability Represents whether displays the microphone availability indicator or not.
 * @param microphoneAvailabilityLabel The position of the microphone availability indicator.
 * @param isShowingRoleBadge Represents whether the displays the role badge or not.
 */
public fun AudioRendererStyle.copy(
    isSpeaking: Boolean = this.isSpeaking,
    isShowingMicrophoneAvailability: Boolean = this.isShowingMicrophoneAvailability,
    microphoneAvailabilityLabel: Alignment = this.microphoneAvailabilityLabel,
    isShowingRoleBadge: Boolean = this.isShowingRoleBadge
): AudioRendererStyle {
    return RegularAudioRendererStyle(
        isSpeaking = isSpeaking,
        isShowingMicrophoneAvailability = isShowingMicrophoneAvailability,
        microphoneAvailabilityLabel = microphoneAvailabilityLabel,
        isShowingRoleBadge = isShowingRoleBadge
    )
}

/**
 * Represents a regular audio room render styles.
 *
 * @param isSpeaking Represents whether the participant is speaking or not.
 * @param isShowingMicrophoneAvailability Represents whether displays the microphone availability indicator or not.
 * @param microphoneAvailabilityLabel The position of the microphone availability indicator.
 * @param isShowingRoleBadge Represents whether the displays the role badge or not.
 */
@Stable
public data class RegularAudioRendererStyle(
    override val isSpeaking: Boolean = true,
    override val isShowingMicrophoneAvailability: Boolean = true,
    override val microphoneAvailabilityLabel: Alignment = Alignment.BottomEnd,
    override val isShowingRoleBadge: Boolean = true
) : AudioRendererStyle(
    isSpeaking = isSpeaking,
    isShowingMicrophoneAvailability = isShowingMicrophoneAvailability,
    microphoneAvailabilityLabel = microphoneAvailabilityLabel,
    isShowingRoleBadge = isShowingRoleBadge
)
