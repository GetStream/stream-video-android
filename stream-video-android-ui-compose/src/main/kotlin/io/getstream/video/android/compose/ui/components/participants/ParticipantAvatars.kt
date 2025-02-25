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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewMemberListState
import io.getstream.video.android.model.User.Companion.isLocalUser

/**
 * Component that renders user avatars for call participants.
 *
 * @param participants The list of participants to render avatars for.
 *
 * @see [UserAvatar]
 */
@Composable
public fun ParticipantAvatars(
    participants: List<MemberState>,
) {
    if (participants.isNotEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            if (participants.size == 1) {
                val participant = participants.first()

                UserAvatar(
                    modifier = Modifier.size(VideoTheme.dimens.genericMax),
                    userName = participant.user.userNameOrId,
                    userImage = participant.user.image,
                )
            } else if (participants.size == 2) {
                val remoteParticipant = participants.firstOrNull { it.user.isLocalUser() != true }

                UserAvatar(
                    modifier = Modifier.size(VideoTheme.dimens.genericMax),
                    userName = remoteParticipant?.user?.userNameOrId,
                    userImage = remoteParticipant?.user?.image,
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        items(participants.take(3)) { participant ->
                            UserAvatar(
                                modifier = Modifier.size(VideoTheme.dimens.genericXl),
                                userName = participant.user.userNameOrId,
                                userImage = participant.user.image,
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
            participants = previewMemberListState,
        )
    }
}
