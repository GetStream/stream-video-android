/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import io.getstream.video.android.compose.utils.floatResource
import io.getstream.video.android.compose.utils.textSizeResource
import io.getstream.video.android.ui.common.R

/**
 * Contains all the dimens we provide for our components.
 *
 * @param callAvatarSize The size of channel avatar.
 * @param headerElevation The elevation of the headers, such as the ones appearing on the Channel or Message screens.
 */
@Immutable
public data class StreamDimens(
    public val callAvatarSize: Dp,
    public val singleAvatarSize: Dp,
    public val headerElevation: Dp,
    public val largeButtonSize: Dp,
    public val mediumButtonSize: Dp,
    public val smallButtonSize: Dp,
    public val topAppbarHeight: Dp,
    public val landscapeTopAppBarHeight: Dp,
    public val avatarAppbarPadding: Dp,
    public val singleAvatarAppbarPadding: Dp,
    public val participantsTextPadding: Dp,
    public val directCallUserNameTextSize: TextUnit,
    public val groupCallUserNameTextSize: TextUnit,
    public val onCallStatusTextSize: TextUnit,
    public val topAppbarTextSize: TextUnit,
    public val onCallStatusTextAlpha: Float,
    public val buttonToggleOnAlpha: Float,
    public val buttonToggleOffAlpha: Float,
    public val incomingCallOptionsBottomPadding: Dp,
    public val outgoingCallOptionsBottomPadding: Dp,
    public val callParticipantsAvatarsMargin: Dp,
    public val callStatusParticipantsMargin: Dp,
    public val callAppBarPadding: Dp,
    public val callAppBarLeadingContentSpacingStart: Dp,
    public val callAppBarLeadingContentSpacingEnd: Dp,
    public val callAppBarCenterContentSpacingStart: Dp,
    public val callAppBarCenterContentSpacingEnd: Dp,
    public val callAppBarTrailingContentSpacingStart: Dp,
    public val callAppBarTrailingContentSpacingEnd: Dp,
    public val callAppBarRecordingIndicatorSize: Dp,
    public val controlActionsBottomPadding: Dp,
    public val controlActionsHeight: Dp,
    public val controlActionsButtonSize: Dp,
    public val controlActionsElevation: Dp,
    public val landscapeControlActionsWidth: Dp,
    public val landscapeControlActionsButtonSize: Dp,
    public val participantFocusedBorderWidth: Dp,
    public val participantScreenSharingFocusedBorderWidth: Dp,
    public val participantLabelHeight: Dp,
    public val participantLabelPadding: Dp,
    public val participantLabelTextMaxWidth: Dp,
    public val participantSoundIndicatorPadding: Dp,
    public val participantLabelTextPaddingStart: Dp,
    public val participantInfoMenuAppBarHeight: Dp,
    public val participantInfoMenuOptionsHeight: Dp,
    public val participantsInfoMenuOptionsButtonHeight: Dp,
    public val participantsInfoAvatarSize: Dp,
    public val participantsGridPadding: Dp,
    public val floatingVideoPadding: Dp,
    public val floatingVideoHeight: Dp,
    public val floatingVideoWidth: Dp,
    public val connectionIndicatorBarMaxHeight: Dp,
    public val connectionIndicatorBarWidth: Dp,
    public val connectionIndicatorBarSeparatorWidth: Dp,
    public val audioLevelIndicatorBarMaxHeight: Dp,
    public val audioLevelIndicatorBarWidth: Dp,
    public val audioLevelIndicatorBarSeparatorWidth: Dp,
    public val audioLevelIndicatorBarPadding: Dp,
    public val microphoneIndicatorSize: Dp,
    public val microphoneIndicatorPadding: Dp,
    public val screenShareParticipantItemSize: Dp,
    public val screenShareParticipantsRowHeight: Dp,
    public val screenShareParticipantsRowPadding: Dp,
    public val screenShareParticipantsListItemMargin: Dp,
    public val screenShareParticipantsScreenShareListMargin: Dp,
    public val screenShareParticipantsRadius: Dp,
    public val screenSharePresenterPadding: Dp,
    public val screenSharePresenterTooltipMargin: Dp,
    public val screenSharePresenterTooltipPadding: Dp,
    public val screenSharePresenterTooltipIconPadding: Dp,
    public val screenSharePresenterTooltipHeight: Dp,
    public val lobbyVideoHeight: Dp,
    public val lobbyControlActionsPadding: Dp,
    public val lobbyControlActionsItemSpaceBy: Dp,
    public val reactionSize: Dp,
    public val audioContentTopPadding: Dp,
    public val audioAvatarSize: Dp,
    public val audioAvatarPadding: Dp,
    public val audioRoomMicSize: Dp,
    public val audioRoomMicPadding: Dp,
    public val audioRoomAvatarPortraitPadding: Dp,
    public val audioRoomAvatarLandscapePadding: Dp,
) {
    public companion object {
        /**
         * Builds the default dimensions for our theme.
         *
         * @return A [StreamDimens] instance holding our default dimensions.
         */
        @Composable
        public fun defaultDimens(): StreamDimens = StreamDimens(
            callAvatarSize = dimensionResource(id = R.dimen.stream_video_callAvatarSize),
            singleAvatarSize = dimensionResource(id = R.dimen.stream_video_singleAvatarSize),
            headerElevation = dimensionResource(id = R.dimen.stream_video_headerElevation),
            largeButtonSize = dimensionResource(id = R.dimen.stream_video_largeButtonSize),
            mediumButtonSize = dimensionResource(id = R.dimen.stream_video_mediumButtonSize),
            smallButtonSize = dimensionResource(id = R.dimen.stream_video_smallButtonSize),
            topAppbarHeight = dimensionResource(id = R.dimen.stream_video_topAppbarHeight),
            landscapeTopAppBarHeight = dimensionResource(
                id = R.dimen.stream_video_landscapeTopAppBarHeight,
            ),
            avatarAppbarPadding = dimensionResource(id = R.dimen.stream_video_avatarAppbarPadding),
            singleAvatarAppbarPadding = dimensionResource(
                id = R.dimen.stream_video_singleAvatarAppbarPadding,
            ),
            participantsTextPadding = dimensionResource(
                id = R.dimen.stream_video_participantsTextPadding,
            ),
            topAppbarTextSize = textSizeResource(id = R.dimen.stream_video_topAppbarTextSize),
            directCallUserNameTextSize = textSizeResource(
                id = R.dimen.stream_video_directCallUserNameTextSize,
            ),
            groupCallUserNameTextSize = textSizeResource(
                id = R.dimen.stream_video_groupCallUserNameTextSize,
            ),
            onCallStatusTextSize = textSizeResource(id = R.dimen.stream_video_onCallStatusTextSize),
            onCallStatusTextAlpha = floatResource(R.dimen.stream_video_onCallStatusTextAlpha),
            buttonToggleOnAlpha = floatResource(R.dimen.stream_video_buttonToggleOnAlpha),
            buttonToggleOffAlpha = floatResource(R.dimen.stream_video_buttonToggleOffAlpha),
            incomingCallOptionsBottomPadding = dimensionResource(
                id = R.dimen.stream_video_incomingCallOptionsBottomPadding,
            ),
            outgoingCallOptionsBottomPadding = dimensionResource(
                id = R.dimen.stream_video_outgoingCallOptionsBottomPadding,
            ),
            callParticipantsAvatarsMargin = dimensionResource(
                id = R.dimen.stream_video_callParticipantsAvatarsMargin,
            ),
            callStatusParticipantsMargin = dimensionResource(
                id = R.dimen.stream_video_callStatusParticipantsMargin,
            ),
            callAppBarPadding = dimensionResource(id = R.dimen.stream_video_callAppBarPadding),
            callAppBarLeadingContentSpacingStart = dimensionResource(
                id = R.dimen.stream_video_callAppBarLeadingContentSpacingStart,
            ),
            callAppBarLeadingContentSpacingEnd = dimensionResource(
                id = R.dimen.stream_video_callAppBarLeadingContentSpacingEnd,
            ),
            callAppBarCenterContentSpacingStart = dimensionResource(
                id = R.dimen.stream_video_callAppBarCenterContentSpacingStart,
            ),
            callAppBarCenterContentSpacingEnd = dimensionResource(
                id = R.dimen.stream_video_callAppBarCenterContentSpacingEnd,
            ),
            callAppBarTrailingContentSpacingStart = dimensionResource(
                id = R.dimen.stream_video_callAppBarTrailingContentSpacingStart,
            ),
            callAppBarTrailingContentSpacingEnd = dimensionResource(
                id = R.dimen.stream_video_callAppBarTrailingContentSpacingEnd,
            ),
            callAppBarRecordingIndicatorSize = dimensionResource(
                id = R.dimen.stream_video_callAppBarRecordingIndicatorSize,
            ),
            controlActionsButtonSize = dimensionResource(
                id = R.dimen.stream_video_controlActionsButtonSize,
            ),
            participantFocusedBorderWidth = dimensionResource(
                id = R.dimen.stream_video_activeSpeakerBoarderWidth,
            ),
            participantScreenSharingFocusedBorderWidth = dimensionResource(
                id = R.dimen.stream_video_activeSpeakerScreenSharingBoarderWidth,
            ),
            participantLabelHeight = dimensionResource(
                id = R.dimen.stream_video_callParticipantLabelHeight,
            ),
            participantLabelPadding = dimensionResource(
                id = R.dimen.stream_video_callParticipantLabelPadding,
            ),
            participantLabelTextMaxWidth = dimensionResource(
                id = R.dimen.stream_video_callParticipantLabelTextMaxWidth,
            ),
            participantSoundIndicatorPadding = dimensionResource(
                id = R.dimen.stream_video_callParticipantLabelTextPadding,
            ),
            participantLabelTextPaddingStart = dimensionResource(
                id = R.dimen.stream_video_participantsGridPadding,
            ),
            participantsGridPadding = dimensionResource(
                id = R.dimen.stream_video_audioRoomAvatarLandscapePadding,
            ),
            landscapeControlActionsButtonSize = dimensionResource(
                id = R.dimen.stream_video_landscapeControlActionsButtonSize,
            ),
            controlActionsHeight = dimensionResource(
                id = R.dimen.stream_video_controlActionsHeight,
            ),
            controlActionsElevation = dimensionResource(
                id = R.dimen.stream_video_controlActionsElevation,
            ),
            landscapeControlActionsWidth = dimensionResource(
                id = R.dimen.stream_video_landscapeControlActionsWidth,
            ),
            participantInfoMenuAppBarHeight = dimensionResource(
                id = R.dimen.stream_video_participantInfoMenuAppBarHeight,
            ),
            participantInfoMenuOptionsHeight = dimensionResource(
                id = R.dimen.stream_video_participantInfoMenuOptionsHeight,
            ),
            participantsInfoMenuOptionsButtonHeight = dimensionResource(
                id = R.dimen.stream_video_participantsInfoMenuOptionsButtonHeight,
            ),
            participantsInfoAvatarSize = dimensionResource(
                id = R.dimen.stream_video_participantsInfoAvatarSize,
            ),
            floatingVideoPadding = dimensionResource(
                id = R.dimen.stream_video_floatingVideoPadding,
            ),
            floatingVideoHeight = dimensionResource(id = R.dimen.stream_video_floatingVideoHeight),
            floatingVideoWidth = dimensionResource(id = R.dimen.stream_video_floatingVideoWidth),
            connectionIndicatorBarMaxHeight = dimensionResource(
                id = R.dimen.stream_video_connectionIndicatorBarMaxHeight,
            ),
            connectionIndicatorBarWidth = dimensionResource(
                id = R.dimen.stream_video_connectionIndicatorBarWidth,
            ),
            connectionIndicatorBarSeparatorWidth = dimensionResource(
                id = R.dimen.stream_video_connectionIndicatorBarSeparatorWidth,
            ),
            audioLevelIndicatorBarMaxHeight = dimensionResource(
                id = R.dimen.stream_video_audioLevelIndicatorBarMaxHeight,
            ),
            audioLevelIndicatorBarWidth = dimensionResource(
                id = R.dimen.stream_video_audioLevelIndicatorBarWidth,
            ),
            audioLevelIndicatorBarSeparatorWidth = dimensionResource(
                id = R.dimen.stream_video_audioLevelIndicatorBarSeparatorWidth,
            ),
            microphoneIndicatorSize = dimensionResource(
                id = R.dimen.stream_video_microphoneIndicatorSize,
            ),
            microphoneIndicatorPadding = dimensionResource(
                id = R.dimen.stream_video_microphoneIndicatorPadding,
            ),
            audioLevelIndicatorBarPadding = dimensionResource(
                id = R.dimen.stream_video_audioLevelIndicatorBarPadding,
            ),
            screenShareParticipantItemSize = dimensionResource(
                id = R.dimen.stream_video_screenShareParticipantItemSize,
            ),
            screenShareParticipantsRowHeight = dimensionResource(
                id = R.dimen.stream_video_screenShareParticipantsListHeight,
            ),
            screenShareParticipantsRowPadding = dimensionResource(
                id = R.dimen.stream_video_screenShareParticipantsListPadding,
            ),
            screenShareParticipantsListItemMargin = dimensionResource(
                id = R.dimen.stream_video_screenShareParticipantsListItemMargin,
            ),
            screenShareParticipantsScreenShareListMargin = dimensionResource(
                id = R.dimen.stream_video_screenShareParticipantsMargin,
            ),
            screenShareParticipantsRadius = dimensionResource(
                id = R.dimen.stream_video_screenShareParticipantsRadius,
            ),
            screenSharePresenterTooltipMargin = dimensionResource(
                id = R.dimen.stream_video_screenSharePresenterTooltipMargin,
            ),
            screenSharePresenterTooltipPadding = dimensionResource(
                id = R.dimen.stream_video_screenSharePresenterTooltipPadding,
            ),
            screenSharePresenterPadding = dimensionResource(
                id = R.dimen.stream_video_screenSharePresenterPadding,
            ),
            controlActionsBottomPadding = dimensionResource(
                id = R.dimen.stream_video_controlActionsBottomPadding,
            ),
            screenSharePresenterTooltipIconPadding = dimensionResource(
                id = R.dimen.stream_video_screenShareTooltipIconPadding,
            ),
            screenSharePresenterTooltipHeight = dimensionResource(
                id = R.dimen.stream_video_screenSharePresenterTooltipHeight,
            ),
            lobbyVideoHeight = dimensionResource(id = R.dimen.stream_video_lobbyVideoHeight),
            lobbyControlActionsPadding = dimensionResource(
                id = R.dimen.stream_video_lobbyControlActionsPadding,
            ),
            lobbyControlActionsItemSpaceBy = dimensionResource(
                id = R.dimen.stream_video_lobbyControlActionsItemSpaceBy,
            ),
            reactionSize = dimensionResource(id = R.dimen.stream_video_reactionSize),
            audioContentTopPadding = dimensionResource(
                id = R.dimen.stream_video_audioContentTopPadding,
            ),
            audioAvatarSize = dimensionResource(id = R.dimen.stream_video_audioAvatarSize),
            audioAvatarPadding = dimensionResource(id = R.dimen.stream_video_audioAvatarPadding),
            audioRoomMicSize = dimensionResource(id = R.dimen.stream_video_audioMicSize),
            audioRoomMicPadding = dimensionResource(id = R.dimen.stream_video_audioMicPadding),
            audioRoomAvatarPortraitPadding = dimensionResource(
                id = R.dimen.stream_video_audioRoomAvatarPortraitPadding,
            ),
            audioRoomAvatarLandscapePadding = dimensionResource(
                id = R.dimen.stream_video_audioRoomAvatarLandscapePadding,
            ),
        )
    }
}
