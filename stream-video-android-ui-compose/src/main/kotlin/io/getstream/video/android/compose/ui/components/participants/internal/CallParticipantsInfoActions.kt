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

package io.getstream.video.android.compose.ui.components.participants.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamButton
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.ui.common.R

/**
 * Renders the options at the bottom of the
 * [io.getstream.video.android.compose.ui.components.participants.CallParticipantsInfoMenu].
 *
 * Used to trigger invites or muting/unmuting the current user.
 *
 * @param isLocalAudioEnabled If the current user has audio or not.
 * @param modifier Modifier for styling.
 */
@Composable
internal fun CallParticipantsInfoActions(
    modifier: Modifier = Modifier,
    isLocalAudioEnabled: Boolean,
    onInviteUser: () -> Unit,
    onMute: (Boolean) -> Unit,
) {
    Row(
        modifier = modifier.padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        StreamButton(
            modifier = Modifier
                .weight(1f),
            onClick = { onInviteUser.invoke() },
            text = stringResource(id = R.string.stream_video_call_participants_info_options_invite),
            style = VideoTheme.styles.buttonStyles.secondaryButtonStyle(),
        )
        Spacer(modifier = Modifier.size(VideoTheme.dimens.spacingM))
        ToggleMicrophoneAction(
            isMicrophoneEnabled = isLocalAudioEnabled,
            onCallAction = { onMute(!isLocalAudioEnabled) },
        )
    }
}

@Preview
@Composable
private fun CallParticipantsInfoOptionsPreview() {
    VideoTheme {
        CallParticipantsInfoActions(
            isLocalAudioEnabled = false,
            onInviteUser = {},
            onMute = {},
        )
    }
}
