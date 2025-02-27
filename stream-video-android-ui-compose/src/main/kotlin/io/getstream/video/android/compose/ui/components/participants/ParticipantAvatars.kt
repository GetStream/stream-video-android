/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.compose.ui.components.participants

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.model.userNameOrId
import io.getstream.video.android.core.utils.toCallUser
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewMemberListState
import io.getstream.video.android.model.User.Companion.isLocalUser

/**
 * Component that renders user avatars for call participants.
 *
 * @param members The list of participants to render avatars for.
 *
 * @see [UserAvatar]
 */
@Composable
public fun ParticipantAvatars(
    members: List<MemberState>? = null,
    participants: List<ParticipantState>? = null,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val callUsers by remember(members, participants) {
            derivedStateOf {
                members?.map { it.toCallUser() }
                    ?: participants?.map { it.toCallUser() }
                    ?: emptyList()
            }
        }

        if (callUsers.isNotEmpty()) {
            if (callUsers.size == 1) {
                val user = callUsers.first()

                UserAvatar(
                    modifier = Modifier.size(VideoTheme.dimens.genericMax),
                    userName = user.name ?: user.id,
                    userImage = user.imageUrl,
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        items(callUsers.take(3)) { user ->
                            UserAvatar(
                                modifier = Modifier.size(VideoTheme.dimens.genericXl),
                                userName = user.userNameOrId,
                                userImage = user.imageUrl,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ParticipantAvatarsPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        ParticipantAvatars(
            members = previewMemberListState,
        )
    }
}
