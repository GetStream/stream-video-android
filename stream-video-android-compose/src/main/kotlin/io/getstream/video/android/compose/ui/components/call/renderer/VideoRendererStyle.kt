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

package io.getstream.video.android.compose.ui.components.call.renderer

import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment

/**
 * Represents video call render styles.
 *
 * @param isFocused If the participant is focused or not.
 * @param isScreenSharing Represents is screen sharing or not.
 * @param isShowingConnectionQualityIndicator Whether displays the connection quality indicator or not.
 * @param labelPosition The position of the user audio state label.
 */
@Stable
public sealed class VideoRendererStyle(
    public open val isFocused: Boolean,
    public open val isScreenSharing: Boolean,
    public open val isShowingReactions: Boolean,
    public open val isShowingParticipantLabel: Boolean,
    public open val isShowingConnectionQualityIndicator: Boolean,
    public open val labelPosition: Alignment,
)

public fun VideoRendererStyle.copy(
    isFocused: Boolean = this.isFocused,
    isScreenSharing: Boolean = this.isScreenSharing,
    isShowingReactions: Boolean = this.isShowingReactions,
    isShowingParticipantLabel: Boolean = this.isShowingParticipantLabel,
    isShowingConnectionQualityIndicator: Boolean = this.isShowingConnectionQualityIndicator,
    labelPosition: Alignment = this.labelPosition,
): VideoRendererStyle {
    return if (this is RegularVideoRendererStyle) {
        RegularVideoRendererStyle(
            isFocused = isFocused,
            isScreenSharing = isScreenSharing,
            isShowingReactions = isShowingReactions,
            isShowingParticipantLabel = isShowingParticipantLabel,
            isShowingConnectionQualityIndicator = isShowingConnectionQualityIndicator,
            labelPosition = labelPosition
        )
    } else {
        ScreenSharingVideoRendererStyle(
            isFocused = isFocused,
            isScreenSharing = isScreenSharing,
            isShowingReactions = isShowingReactions,
            isShowingParticipantLabel = isShowingParticipantLabel,
            isShowingConnectionQualityIndicator = isShowingConnectionQualityIndicator,
            labelPosition = labelPosition
        )
    }
}

/**
 * Represents a regular video call render styles.
 *
 * @param isFocused If the participant is focused or not.
 * @param isScreenSharing Represents is screen sharing or not.
 * @param isShowingConnectionQualityIndicator Whether displays the connection quality indicator or not.
 * @param labelPosition The position of the user audio state label.
 */
@Stable
public data class RegularVideoRendererStyle(
    override val isFocused: Boolean = false,
    override val isScreenSharing: Boolean = false,
    override val isShowingReactions: Boolean = true,
    override val isShowingParticipantLabel: Boolean = true,
    override val isShowingConnectionQualityIndicator: Boolean = true,
    override val labelPosition: Alignment = Alignment.BottomStart

) : VideoRendererStyle(
    isFocused,
    isScreenSharing,
    isShowingReactions,
    isShowingParticipantLabel,
    isShowingConnectionQualityIndicator,
    labelPosition
)

/**
 * Represents a screen sharing video call render styles.
 *
 * @param isFocused If the participant is focused or not.
 * @param isScreenSharing Represents is screen sharing or not.
 * @param isShowingConnectionQualityIndicator Whether displays the connection quality indicator or not.
 * @param labelPosition The position of the user audio state label.
 */
@Stable
public data class ScreenSharingVideoRendererStyle(
    override val isFocused: Boolean = false,
    override val isScreenSharing: Boolean = true,
    override val isShowingReactions: Boolean = true,
    override val isShowingParticipantLabel: Boolean = true,
    override val isShowingConnectionQualityIndicator: Boolean = false,
    override val labelPosition: Alignment = Alignment.BottomStart

) : VideoRendererStyle(
    isFocused,
    isScreenSharing,
    isShowingReactions,
    isShowingParticipantLabel,
    isShowingConnectionQualityIndicator,
    labelPosition
)
