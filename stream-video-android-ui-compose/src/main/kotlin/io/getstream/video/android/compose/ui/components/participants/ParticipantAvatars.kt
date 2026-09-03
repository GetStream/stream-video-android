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

package io.getstream.video.android.compose.ui.components.participants

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.UserAvatarParams
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.theme.design.StreamTokens
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.model.userNameOrId
import io.getstream.video.android.core.utils.toCallUser

/**
 * Component that renders user avatars for call participants.
 *
 * @param participants The list of participants to render avatars for.
 *
 * @see [UserAvatar]
 */
@Deprecated(
    message = "This version of ParticipantAvatars is deprecated. Use the newer overload.",
    replaceWith = ReplaceWith("ParticipantAvatars"),
)
@Composable
public fun ParticipantAvatars(
    participants: List<MemberState>,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        if (participants.isNotEmpty()) {
            if (participants.size == 1) {
                val participant = participants.first()

                VideoTheme.componentFactory.UserAvatar(
                    UserAvatarParams(
                        userImage = participant.user.image,
                        userName = participant.user.userNameOrId,
                        modifier = Modifier.size(100.dp),
                    ),
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        items(participants.take(2)) { participant ->
                            VideoTheme.componentFactory.UserAvatar(
                                UserAvatarParams(
                                    userImage = participant.user.image,
                                    userName = participant.user.userNameOrId,
                                    modifier = Modifier.size(StreamTokens.size24),
                                ),
                            )
                        }
                    }

                    if (participants.size >= 3) {
                        VideoTheme.componentFactory.UserAvatar(
                            UserAvatarParams(
                                userImage = participants[2].user.image,
                                userName = participants[2].user.userNameOrId,
                                modifier = Modifier.size(StreamTokens.size16),
                            ),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Component that renders user avatars for a call.
 *
 * @param members The list of call members to render avatars for. If `null`, [participants] will be used instead. Takes precedence over `participants` if both are not `null`.
 * @param participants The list of call participants to render avatars for. If `null`, [members] will be used instead.
 *
 * @see [UserAvatar]
 */
@OptIn(ExperimentalLayoutApi::class)
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
                VideoTheme.componentFactory.UserAvatar(
                    UserAvatarParams(
                        userImage = user.imageUrl,
                        userName = user.name ?: user.id,
                        modifier = Modifier
                            .size(100.dp * 2)
                            .testTag("Stream_ParticipantAvatar"),
                    ),
                )
            } else if (callUsers.size == 2) {
                val firstThree = callUsers.take(2)
                Row {
                    VideoTheme.componentFactory.UserAvatar(
                        UserAvatarParams(
                            userImage = firstThree[0].imageUrl,
                            userName = firstThree[0].userNameOrId,
                            modifier = Modifier
                                .size(100.dp)
                                .testTag("Stream_ParticipantAvatar"),
                        ),
                    )
                    Spacer(modifier = Modifier.width(StreamTokens.spacingXl))
                    VideoTheme.componentFactory.UserAvatar(
                        UserAvatarParams(
                            userImage = firstThree[1].imageUrl,
                            userName = firstThree[1].userNameOrId,
                            modifier = Modifier
                                .size(100.dp)
                                .testTag("Stream_ParticipantAvatar"),
                        ),
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val firstThree = callUsers.take(if (callUsers.size >= 3) 3 else 2)
                    VideoTheme.componentFactory.UserAvatar(
                        UserAvatarParams(
                            userImage = firstThree[0].imageUrl,
                            userName = firstThree[0].userNameOrId,
                            modifier = Modifier
                                .size(160.dp)
                                .padding(16.dp)
                                .testTag("Stream_ParticipantAvatar"),
                        ),
                    )
                    Row {
                        VideoTheme.componentFactory.UserAvatar(
                            UserAvatarParams(
                                userImage = firstThree[1].imageUrl,
                                userName = firstThree[1].userNameOrId,
                                modifier = Modifier
                                    .size(100.dp)
                                    .testTag("Stream_ParticipantAvatar"),
                            ),
                        )
                        Spacer(modifier = Modifier.width(StreamTokens.spacingXl))
                        VideoTheme.componentFactory.UserAvatar(
                            UserAvatarParams(
                                userImage = firstThree[2].imageUrl,
                                userName = firstThree[2].userNameOrId,
                                modifier = Modifier
                                    .size(100.dp)
                                    .testTag("Stream_ParticipantAvatar"),
                            ),
                        )
                    }
                }
            }
        }
    }
}
