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

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import io.getstream.video.android.common.util.getFloatResource
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
    public val topAppbarHeightSize: Dp,
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
    public val callAppBarPadding: Dp,
    public val callAppBarLeadingContentSpacingStart: Dp,
    public val callAppBarLeadingContentSpacingEnd: Dp,
    public val callAppBarCenterContentSpacingStart: Dp,
    public val callAppBarCenterContentSpacingEnd: Dp,
    public val callAppBarTrailingContentSpacingStart: Dp,
    public val callAppBarTrailingContentSpacingEnd: Dp,
    public val callControlButtonSize: Dp,
    public val callControlsSheetHeight: Dp,
    public val callParticipantInfoMenuAppBarHeight: Dp,
    public val callParticipantInfoMenuOptionsHeight: Dp,
    public val callParticipantsInfoMenuOptionsButtonHeight: Dp,
    public val callParticipantsInfoAvatarSize: Dp,
) {
    public companion object {
        /**
         * Builds the default dimensions for our theme.
         *
         * @return A [StreamDimens] instance holding our default dimensions.
         */
        @Composable
        public fun defaultDimens(context: Context): StreamDimens {

            return StreamDimens(
                callAvatarSize = dimensionResource(id = R.dimen.callAvatarSize),
                singleAvatarSize = dimensionResource(id = R.dimen.singleAvatarSize),
                headerElevation = dimensionResource(id = R.dimen.headerElevation),
                largeButtonSize = dimensionResource(id = R.dimen.largeButtonSize),
                mediumButtonSize = dimensionResource(id = R.dimen.mediumButtonSize),
                smallButtonSize = dimensionResource(id = R.dimen.smallButtonSize),
                topAppbarHeightSize = dimensionResource(id = R.dimen.topAppbarHeightSize),
                avatarAppbarPadding = dimensionResource(id = R.dimen.avatarAppbarPadding),
                singleAvatarAppbarPadding = dimensionResource(id = R.dimen.singleAvatarAppbarPadding),
                participantsTextPadding = dimensionResource(id = R.dimen.participantsTextPadding),
                topAppbarTextSize = dimensionResource(id = R.dimen.topAppbarTextSize).value.sp,
                directCallUserNameTextSize = dimensionResource(id = R.dimen.directCallUserNameTextSize).value.sp,
                groupCallUserNameTextSize = dimensionResource(id = R.dimen.groupCallUserNameTextSize).value.sp,
                onCallStatusTextSize = dimensionResource(id = R.dimen.onCallStatusTextSize).value.sp,
                onCallStatusTextAlpha = context.getFloatResource(R.dimen.onCallStatusTextAlpha),
                buttonToggleOnAlpha = context.getFloatResource(R.dimen.buttonToggleOnAlpha),
                buttonToggleOffAlpha = context.getFloatResource(R.dimen.buttonToggleOffAlpha),
                incomingCallOptionsBottomPadding = dimensionResource(id = R.dimen.incomingCallOptionsBottomPadding),
                callAppBarPadding = dimensionResource(id = R.dimen.callAppBarPadding),
                callAppBarLeadingContentSpacingStart = dimensionResource(id = R.dimen.callAppBarLeadingContentSpacingStart),
                callAppBarLeadingContentSpacingEnd = dimensionResource(id = R.dimen.callAppBarLeadingContentSpacingEnd),
                callAppBarCenterContentSpacingStart = dimensionResource(id = R.dimen.callAppBarCenterContentSpacingStart),
                callAppBarTrailingContentSpacingStart = dimensionResource(id = R.dimen.callAppBarTrailingContentSpacingStart),
                callAppBarTrailingContentSpacingEnd = dimensionResource(id = R.dimen.callAppBarTrailingContentSpacingEnd),
                callAppBarCenterContentSpacingEnd = dimensionResource(id = R.dimen.callAppBarCenterContentSpacingEnd),
                callControlButtonSize = dimensionResource(id = R.dimen.callControlButtonSize),
                callControlsSheetHeight = dimensionResource(id = R.dimen.callControlsSheetHeight),
                callParticipantInfoMenuAppBarHeight = dimensionResource(id = R.dimen.callParticipantInfoMenuAppBarHeight),
                callParticipantInfoMenuOptionsHeight = dimensionResource(id = R.dimen.callParticipantInfoMenuOptionsHeight),
                callParticipantsInfoMenuOptionsButtonHeight = dimensionResource(id = R.dimen.callParticipantsInfoMenuOptionsButtonHeight),
                callParticipantsInfoAvatarSize = dimensionResource(id = R.dimen.callParticipantsInfoAvatarSize)
            )
        }
    }
}
