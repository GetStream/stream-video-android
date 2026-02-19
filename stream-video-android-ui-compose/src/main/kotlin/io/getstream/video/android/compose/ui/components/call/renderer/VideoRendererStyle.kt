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

package io.getstream.video.android.compose.ui.components.call.renderer

import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment

/**
 * Represents video call render styles.
 *
 * @param isFocused Represents whether the participant is focused or not.
 * @param isScreenSharing Represents whether the video renderer is about screen sharing.
 * @param isShowingReactions Represents whether display reaction comes from the call state.
 * @param isShowingParticipantLabel Represents whether display the participant label that contains the name and microphone status of a participant.
 * @param isShowingConnectionQualityIndicator Represents whether displays the connection quality indicator or not.
 * @param labelPosition The position of the participant label that contains the name and microphone status of a participant.
 * @param reactionDuration The duration of the reaction animation.
 * @param reactionPosition The position of the reaction.
 */
@Stable
public sealed class VideoRendererStyle(
    public open val isFocused: Boolean,
    public open val isScreenSharing: Boolean,
    public open val isShowingReactions: Boolean,
    public open val isShowingParticipantLabel: Boolean,
    public open val isShowingConnectionQualityIndicator: Boolean,
    public open val labelPosition: Alignment,
    public open val reactionDuration: Int,
    public open val reactionPosition: Alignment,
)

/**
 * Represents video call render styles.
 *
 * @param isFocused Represents whether the participant is focused or not.
 * @param isScreenSharing Represents whether the video renderer is about screen sharing.
 * @param isShowingReactions Represents whether display reaction comes from the call state.
 * @param isShowingParticipantLabel Represents whether display the participant label that contains the name and microphone status of a participant.
 * @param isShowingConnectionQualityIndicator Represents whether displays the connection quality indicator or not.
 * @param labelPosition The position of the participant label that contains the name and microphone status of a participant.
 * @param reactionDuration The duration of the reaction animation.
 * @param reactionPosition The position of the reaction.
 */
public fun VideoRendererStyle.copy(
    isFocused: Boolean = this.isFocused,
    isScreenSharing: Boolean = this.isScreenSharing,
    isShowingReactions: Boolean = this.isShowingReactions,
    isShowingParticipantLabel: Boolean = this.isShowingParticipantLabel,
    isShowingConnectionQualityIndicator: Boolean = this.isShowingConnectionQualityIndicator,
    labelPosition: Alignment = this.labelPosition,
    reactionDuration: Int = this.reactionDuration,
    reactionPosition: Alignment = this.reactionPosition,
): VideoRendererStyle {
    return if (this is RegularVideoRendererStyle) {
        RegularVideoRendererStyle(
            isFocused = isFocused,
            isScreenSharing = isScreenSharing,
            isShowingReactions = isShowingReactions,
            isShowingParticipantLabel = isShowingParticipantLabel,
            isShowingConnectionQualityIndicator = isShowingConnectionQualityIndicator,
            labelPosition = labelPosition,
            reactionDuration = reactionDuration,
            reactionPosition = reactionPosition,
        )
    } else {
        ScreenSharingVideoRendererStyle(
            isFocused = isFocused,
            isScreenSharing = isScreenSharing,
            isShowingReactions = isShowingReactions,
            isShowingParticipantLabel = isShowingParticipantLabel,
            isShowingConnectionQualityIndicator = isShowingConnectionQualityIndicator,
            labelPosition = labelPosition,
            reactionDuration = reactionDuration,
            reactionPosition = reactionPosition,
        )
    }
}

/**
 * A regular video renderer style, which displays the reactions, participant label, and connection quality indicator.
 *
 * @param isFocused Represents whether the participant is focused or not.
 * @param isScreenSharing Represents whether the video renderer is about screen sharing.
 * @param isShowingReactions Represents whether display reaction comes from the call state.
 * @param isShowingParticipantLabel Represents whether display the participant label that contains the name and microphone status of a participant.
 * @param isShowingConnectionQualityIndicator Represents whether displays the connection quality indicator or not.
 * @param labelPosition The position of the participant label that contains the name and microphone status of a participant.
 * @param reactionDuration The duration of the reaction animation.
 * @param reactionPosition The position of the reaction.
 */
@Stable
public data class RegularVideoRendererStyle(
    override val isFocused: Boolean = false,
    override val isScreenSharing: Boolean = false,
    override val isShowingReactions: Boolean = true,
    override val isShowingParticipantLabel: Boolean = true,
    override val isShowingConnectionQualityIndicator: Boolean = true,
    override val labelPosition: Alignment = Alignment.BottomStart,
    override val reactionDuration: Int = 650,
    override val reactionPosition: Alignment = Alignment.TopEnd,

) : VideoRendererStyle(
    isFocused,
    isScreenSharing,
    isShowingReactions,
    isShowingParticipantLabel,
    isShowingConnectionQualityIndicator,
    labelPosition,
    reactionDuration,
    reactionPosition,
)

/**
 * A screen sharing video renderer style, which displays the reactions, and participant label.
 *
 * @param isFocused Represents whether the participant is focused or not.
 * @param isScreenSharing Represents whether the video renderer is about screen sharing.
 * @param isShowingReactions Represents whether display reaction comes from the call state.
 * @param isShowingParticipantLabel Represents whether display the participant label that contains the name and microphone status of a participant.
 * @param isShowingConnectionQualityIndicator Represents whether displays the connection quality indicator or not.
 * @param labelPosition The position of the participant label that contains the name and microphone status of a participant.
 * @param reactionDuration The duration of the reaction animation.
 * @param reactionPosition The position of the reaction.
 */
@Stable
public data class ScreenSharingVideoRendererStyle(
    override val isFocused: Boolean = false,
    override val isScreenSharing: Boolean = true,
    override val isShowingReactions: Boolean = true,
    override val isShowingParticipantLabel: Boolean = true,
    override val isShowingConnectionQualityIndicator: Boolean = false,
    override val labelPosition: Alignment = Alignment.BottomStart,
    override val reactionDuration: Int = 1000,
    override val reactionPosition: Alignment = Alignment.TopEnd,

) : VideoRendererStyle(
    isFocused,
    isScreenSharing,
    isShowingReactions,
    isShowingParticipantLabel,
    isShowingConnectionQualityIndicator,
    labelPosition,
    reactionDuration,
    reactionPosition,
)

/**
 * A spotlight video renderer style, which displays the reactions, and participant label.
 *
 * @param isFocused Represents whether the participant is focused or not.
 * @param isScreenSharing Represents whether the video renderer is about screen sharing.
 * @param isShowingReactions Represents whether display reaction comes from the call state.
 * @param isShowingParticipantLabel Represents whether display the participant label that contains the name and microphone status of a participant.
 * @param isShowingConnectionQualityIndicator Represents whether displays the connection quality indicator or not.
 * @param labelPosition The position of the participant label that contains the name and microphone status of a participant.
 * @param reactionDuration The duration of the reaction animation.
 * @param reactionPosition The position of the reaction.
 */
@Stable
public data class SpotlightVideoRendererStyle(
    override val isFocused: Boolean = false,
    override val isScreenSharing: Boolean = false,
    override val isShowingReactions: Boolean = true,
    override val isShowingParticipantLabel: Boolean = true,
    override val isShowingConnectionQualityIndicator: Boolean = true,
    override val labelPosition: Alignment = Alignment.BottomStart,
    override val reactionDuration: Int = 1000,
    override val reactionPosition: Alignment = Alignment.TopEnd,

) : VideoRendererStyle(
    isFocused,
    isScreenSharing,
    isShowingReactions,
    isShowingParticipantLabel,
    isShowingConnectionQualityIndicator,
    labelPosition,
    reactionDuration,
    reactionPosition,
)
