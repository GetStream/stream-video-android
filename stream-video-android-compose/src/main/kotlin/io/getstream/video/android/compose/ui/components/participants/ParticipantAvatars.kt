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

package io.getstream.video.android.compose.ui.components.participants

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.InitialsAvatar
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.compose.ui.components.mock.mockParticipants
import io.getstream.video.android.model.VideoParticipant

@Composable
public fun ParticipantAvatars(
    participants: List<VideoParticipant>
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        if (participants.isNotEmpty()) {
            if (participants.size == 1) {
                val user = participants.first().user

                UserAvatar(
                    modifier = Modifier.size(VideoTheme.dimens.singleAvatarSize),
                    user = user!!
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    items(participants.take(2)) { participant ->
                        UserAvatar(
                            modifier = Modifier.size(VideoTheme.dimens.callAvatarSize),
                            user = participant.user!!
                        )
                    }

                    item {
                        if (participants.size == 3) {
                            UserAvatar(
                                modifier = Modifier.size(VideoTheme.dimens.callAvatarSize),
                                user = participants[2].user!!
                            )
                        } else if (participants.size > 3) {
                            InitialsAvatar(
                                modifier = Modifier.size(VideoTheme.dimens.callAvatarSize),
                                initials = "+${participants.size - 2}"
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
    VideoTheme {
        ParticipantAvatars(participants = mockParticipants)
    }
}
