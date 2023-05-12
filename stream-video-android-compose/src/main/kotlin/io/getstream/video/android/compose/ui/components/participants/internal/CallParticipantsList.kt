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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.state.ui.internal.CallParticipantsInfoOption
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.mock.mockParticipantList
import io.getstream.video.android.ui.common.R

/**
 * Represents the list of active call participants.
 *
 * @param participantsState The list of participants.
 * @param onUserOptionsSelected Handler when the options for a given participant are selected.
 * @param modifier Modifier for styling.
 */
@Composable
internal fun CallParticipantsList(
    participantsState: List<ParticipantState>,
    onUserOptionsSelected: (ParticipantState) -> Unit,
    onOptionSelected: (CallParticipantsInfoOption) -> Unit,
    isCurrentUserMuted: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.background(VideoTheme.colors.appBackground),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(participantsState, key = { it.sessionId }) {
            CallParticipantInfoItem(it, onUserOptionsSelected)
        }

        item {
            CallParticipantsInfoActions(
                modifier = Modifier.fillMaxWidth(),
                isCurrentUserMuted = isCurrentUserMuted,
                onOptionSelected = onOptionSelected
            )
        }
    }
}

/**
 * Represents a single active [ParticipantState] item.
 *
 * @param participant The participant data.
 * @param onUserOptionsSelected Handler when the options for a given participant are selected.
 */
@Composable
private fun CallParticipantInfoItem(
    participant: ParticipantState,
    onUserOptionsSelected: (ParticipantState) -> Unit
) {
    Row(
        modifier = Modifier.wrapContentWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {

        Spacer(modifier = Modifier.width(8.dp))

        val user by participant.user.collectAsStateWithLifecycle()
        UserAvatar(
            modifier = Modifier.size(VideoTheme.dimens.callParticipantsInfoAvatarSize),
            user = user,
            isShowingOnlineIndicator = true
        )

        val userName by participant.userNameOrId.collectAsStateWithLifecycle()
        Text(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
            text = userName,
            style = VideoTheme.typography.bodyBold,
            color = VideoTheme.colors.textHighEmphasis,
            fontSize = 16.sp,
            maxLines = 1
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            val audioEnabled by participant.audioEnabled.collectAsStateWithLifecycle()
            if (!audioEnabled) {
                Icon(
                    painter = painterResource(id = R.drawable.stream_video_ic_mic_off),
                    tint = VideoTheme.colors.errorAccent,
                    contentDescription = null
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            val videoEnabled by participant.videoEnabled.collectAsStateWithLifecycle()
            if (!videoEnabled) {
                Icon(
                    painter = painterResource(id = R.drawable.stream_video_ic_videocam_off),
                    tint = VideoTheme.colors.errorAccent,
                    contentDescription = null
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                modifier = Modifier.clickable { onUserOptionsSelected(participant) },
                painter = painterResource(id = R.drawable.stream_video_ic_options),
                tint = VideoTheme.colors.textHighEmphasis,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Preview
@Composable
private fun CallParticipantsListPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallParticipantsList(
            participantsState = mockParticipantList,
            onUserOptionsSelected = {},
            isCurrentUserMuted = false,
            onOptionSelected = {}
        )
    }
}
