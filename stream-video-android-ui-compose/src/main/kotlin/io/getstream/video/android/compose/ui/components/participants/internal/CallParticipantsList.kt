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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.UserAvatarParams
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.theme.design.StreamTokens
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.ui.common.R

/**
 * Represents the list of active call participants.
 *
 * @param participants The list of participants.
 * @param onUserOptionsSelected Handler when the options for a given participant are selected.
 * @param modifier Modifier for styling.
 */
@Composable
internal fun CallParticipantsList(
    modifier: Modifier = Modifier,
    participants: List<ParticipantState>,
    isLocalAudioEnabled: Boolean,
    onUserOptionsSelected: (ParticipantState) -> Unit,
    onInviteUser: () -> Unit,
    onMute: (Boolean) -> Unit,
    onBackPressed: () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        backgroundColor = VideoTheme.colors.backgroundCoreApp,
        topBar = {
            CallParticipantListAppBar(
                numberOfParticipants = participants.size,
                onBackPressed = onBackPressed,
            )
        },
        bottomBar = {
            CallParticipantsInfoActions(
                modifier = Modifier.fillMaxWidth().padding(horizontal = StreamTokens.spacingMd),
                isLocalAudioEnabled = isLocalAudioEnabled,
                onInviteUser = onInviteUser,
                onMute = onMute,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(VideoTheme.colors.backgroundCoreApp),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(participants, key = { it.sessionId }) {
                CallParticipantInfoItem(it, onUserOptionsSelected)
            }
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
    onUserOptionsSelected: ((ParticipantState) -> Unit)? = null,
) {
    Row(
        modifier = Modifier.wrapContentWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Spacer(modifier = Modifier.width(8.dp))

        val userName by participant.userNameOrId.collectAsStateWithLifecycle()
        val userImage by participant.image.collectAsStateWithLifecycle()
        VideoTheme.componentFactory.UserAvatar(
            UserAvatarParams(
                userImage = userImage,
                userName = userName,
                modifier = Modifier.size(StreamTokens.size24),
                isShowingOnlineIndicator = true,
            ),
        )

        Text(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
            text = userName,
            style = VideoTheme.typography.bodyDefault,
            color = VideoTheme.colors.textPrimary,
            fontSize = 16.sp,
            maxLines = 1,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            val audioEnabled by participant.audioEnabled.collectAsStateWithLifecycle()
            if (!audioEnabled) {
                Icon(
                    painter = painterResource(
                        id = io.getstream.video.android.compose.R.drawable.stream_design_ic_voice_off_fill,
                    ),
                    tint = VideoTheme.colors.accentError,
                    contentDescription = null,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            val videoEnabled by participant.videoEnabled.collectAsStateWithLifecycle()
            if (!videoEnabled) {
                Icon(
                    painter = painterResource(
                        id = io.getstream.video.android.compose.R.drawable.stream_design_ic_video_off_fill,
                    ),
                    tint = VideoTheme.colors.accentError,
                    contentDescription = null,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            onUserOptionsSelected?.let {
                Icon(
                    modifier = Modifier.clickable { onUserOptionsSelected(participant) },
                    painter = painterResource(
                        id = io.getstream.video.android.compose.R.drawable.stream_design_ic_more_vertical_fill,
                    ),
                    tint = VideoTheme.colors.textPrimary,
                    contentDescription = null,
                )

                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}
