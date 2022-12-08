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

package io.getstream.video.android.compose.ui.components.participants.internal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.getstream.video.android.ui.common.R
import io.getstream.video.android.compose.state.ui.internal.CallParticipantsInfoOption
import io.getstream.video.android.compose.state.ui.internal.Invite
import io.getstream.video.android.compose.state.ui.internal.ToggleMute
import io.getstream.video.android.compose.theme.VideoTheme

/**
 * Renders the options at the bottom of the
 * [io.getstream.video.android.compose.ui.components.participants.CallParticipantsInfoMenu].
 *
 * Used to trigger invites or muting/unmuting the current user.
 *
 * @param isCurrentUserMuted If the current user has audio or not.
 * @param onOptionSelected Handler when the user triggers an option.
 * @param modifier Modifier for styling.
 */
@Composable
internal fun CallParticipantsInfoOptions(
    isCurrentUserMuted: Boolean,
    onOptionSelected: (CallParticipantsInfoOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            modifier = Modifier
                .height(VideoTheme.dimens.callParticipantsInfoMenuOptionsButtonHeight)
                .weight(1f)
                .padding(start = 16.dp, end = 8.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = VideoTheme.colors.primaryAccent),
            shape = VideoTheme.shapes.participantsInfoMenuButton,
            onClick = { onOptionSelected(Invite) },
            content = {
                Text(
                    text = stringResource(R.string.call_participants_info_options_invite),
                    style = VideoTheme.typography.bodyBold,
                    color = Color.White
                )
            }
        )

        OutlinedButton(
            modifier = Modifier
                .weight(1f)
                .height(VideoTheme.dimens.callParticipantsInfoMenuOptionsButtonHeight)
                .padding(start = 8.dp, end = 16.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = VideoTheme.colors.appBackground),
            border = BorderStroke(1.dp, VideoTheme.colors.textLowEmphasis),
            onClick = { onOptionSelected(ToggleMute(isCurrentUserMuted)) },
            shape = VideoTheme.shapes.participantsInfoMenuButton,
            content = {
                Text(
                    text = stringResource(if (isCurrentUserMuted) R.string.call_participants_info_options_unmute else R.string.call_participants_info_options_mute),
                    style = VideoTheme.typography.bodyBold,
                    color = VideoTheme.colors.textLowEmphasis
                )
            }
        )
    }
}
