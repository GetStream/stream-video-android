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

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewParticipantsList
import io.getstream.video.android.ui.common.R

/**
 * Represents a list of users who can be invited to the call.
 *
 * @param users The available users.
 * @param onUserSelected Handler when a user is selected.
 * @param modifier Modifier for styling.
 */
@Composable
internal fun InviteUserList(
    users: List<ParticipantState>,
    onUserSelected: (ParticipantState) -> Unit,
    onUserUnSelected: (ParticipantState) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(users) { user ->
            InviteUserItem(
                user = user,
                isSelected = users.contains(user),
                onUserSelected = onUserSelected,
                onUserUnSelected = onUserUnSelected,
            )
        }
    }
}

/**
 * Represents a single user item.
 *
 * @param user Item that holds the user data.
 * @param onUserSelected Handler when a user is selected.
 */
@Composable
internal fun InviteUserItem(
    user: ParticipantState,
    isSelected: Boolean,
    onUserSelected: (ParticipantState) -> Unit,
    onUserUnSelected: (ParticipantState) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isSelected) {
                    onUserUnSelected.invoke(user)
                } else {
                    onUserSelected(user)
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Spacer(modifier = Modifier.width(8.dp))

        val userName by user.userNameOrId.collectAsStateWithLifecycle()
        val userImage by user.image.collectAsStateWithLifecycle()

        UserAvatar(
            modifier = Modifier.size(VideoTheme.dimens.componentHeightL),
            userImage = userImage,
            userName = userName,
            isShowingOnlineIndicator = true,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            modifier = Modifier.weight(1f),
            text = userName,
            style = VideoTheme.typography.bodyM,
            color = VideoTheme.colors.basePrimary,
        )

        Spacer(modifier = Modifier.width(8.dp))

        if (isSelected) {
            Image(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = R.drawable.stream_video_ic_selected),
                contentDescription = null,
            )
        }

        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Preview
@Composable
private fun InviteUserListPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        InviteUserList(
            previewParticipantsList,
            onUserSelected = {},
            onUserUnSelected = {},
        )
    }
}
