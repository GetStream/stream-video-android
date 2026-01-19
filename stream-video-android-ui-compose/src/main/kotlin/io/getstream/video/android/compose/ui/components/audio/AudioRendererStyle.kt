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

package io.getstream.video.android.compose.ui.components.audio

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Represents audio room render styles.
 *
 * @param isShowingSpeakingBorder Represents whether the participant is speaking or not.
 * @param speakingBorder The shape will be displayed when a participant is speaking.
 * @param isShowingMicrophoneAvailability Represents whether displays the microphone availability indicator or not.
 * @param microphoneLabelPosition The position of the microphone availability indicator.
 * @param isShowingRoleBadge Represents whether the displays the role badge or not.
 */
@Stable
public sealed class AudioRendererStyle(
    public open val isShowingSpeakingBorder: Boolean,
    public open val speakingBorder: BorderStroke,
    public open val isShowingMicrophoneAvailability: Boolean,
    public open val microphoneLabelPosition: Alignment,
    public open val isShowingRoleBadge: Boolean,
)

/**
 * Represents audio room render styles.
 *
 * @param isShowingSpeakingBorder Represents whether the participant is speaking or not.
 * @param speakingBorder The shape will be displayed when a participant is speaking.
 * @param isShowingMicrophoneAvailability Represents whether displays the microphone availability indicator or not.
 * @param microphoneLabelPosition The position of the microphone availability indicator.
 * @param isShowingRoleBadge Represents whether the displays the role badge or not.
 */
public fun AudioRendererStyle.copy(
    isShowingSpeakingBorder: Boolean = this.isShowingSpeakingBorder,
    speakingBorder: BorderStroke = this.speakingBorder,
    isShowingMicrophoneAvailability: Boolean = this.isShowingMicrophoneAvailability,
    microphoneLabelPosition: Alignment = this.microphoneLabelPosition,
    isShowingRoleBadge: Boolean = this.isShowingRoleBadge,
): AudioRendererStyle {
    return RegularAudioRendererStyle(
        isShowingSpeakingBorder = isShowingSpeakingBorder,
        speakingBorder = speakingBorder,
        isShowingMicrophoneAvailability = isShowingMicrophoneAvailability,
        microphoneLabelPosition = microphoneLabelPosition,
        isShowingRoleBadge = isShowingRoleBadge,
    )
}

/**
 * Represents a regular audio room render styles.
 *
 * @param isShowingSpeakingBorder Represents whether the participant is speaking or not.
 * @param speakingBorder The shape will be displayed when a participant is speaking.
 * @param isShowingMicrophoneAvailability Represents whether displays the microphone availability indicator or not.
 * @param microphoneLabelPosition The position of the microphone availability indicator.
 * @param isShowingRoleBadge Represents whether the displays the role badge or not.
 */
@Stable
public data class RegularAudioRendererStyle(
    override val isShowingSpeakingBorder: Boolean = true,
    override val speakingBorder: BorderStroke = BorderStroke(
        2.dp,
        Color(0xFF005FFF),
    ),
    override val isShowingMicrophoneAvailability: Boolean = true,
    override val microphoneLabelPosition: Alignment = Alignment.BottomEnd,
    override val isShowingRoleBadge: Boolean = true,
) : AudioRendererStyle(
    isShowingSpeakingBorder = isShowingSpeakingBorder,
    speakingBorder = speakingBorder,
    isShowingMicrophoneAvailability = isShowingMicrophoneAvailability,
    microphoneLabelPosition = microphoneLabelPosition,
    isShowingRoleBadge = isShowingRoleBadge,
)
