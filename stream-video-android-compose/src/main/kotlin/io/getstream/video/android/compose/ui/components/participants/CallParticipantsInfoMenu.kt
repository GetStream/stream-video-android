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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.state.ui.internal.CallParticipantInfoMode
import io.getstream.video.android.compose.state.ui.internal.Invite
import io.getstream.video.android.compose.state.ui.internal.ParticipantInvitesMode
import io.getstream.video.android.compose.state.ui.internal.ParticipantListMode
import io.getstream.video.android.compose.state.ui.internal.ToggleMute
import io.getstream.video.android.compose.state.ui.participants.ChangeMuteState
import io.getstream.video.android.compose.state.ui.participants.InviteUsers
import io.getstream.video.android.compose.state.ui.participants.ParticipantInfoAction
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.participants.internal.CallParticipantsInfoAppBar
import io.getstream.video.android.compose.ui.components.participants.internal.CallParticipantsList
import io.getstream.video.android.compose.ui.components.participants.internal.InviteUserList
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.model.User

/**
 * Represents a menu that shows information on the current call participants, while allowing the user
 * to trigger the Invite Users flow as well as trigger actions on any participants in a call.
 *
 * @param participants The list of active participants.
 * @param modifier Modifier for styling.
 * @param onDismiss Handler when the user dismisses the UI through various actions.
 * @param onInfoMenuAction Handler when one of the menu actions is triggered.
 */
@Composable
public fun CallParticipantsInfoMenu(
    participants: List<ParticipantState>,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    onInfoMenuAction: (ParticipantInfoAction) -> Unit = {},
) {
    val me by remember(participants) { derivedStateOf { participants.first { it.isLocal } } }
    val isLocalAudioEnabled by me.audioEnabled.collectAsStateWithLifecycle()

    var infoStateMode by remember { mutableStateOf<CallParticipantInfoMode>(ParticipantListMode) }
    var selectedUsers: List<User> = remember { mutableStateListOf() }

    BackHandler {
        when {
            infoStateMode is ParticipantListMode -> onDismiss()
            else -> onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = VideoTheme.colors.infoMenuOverlayColor)
    ) {
        Column(modifier) {
            CallParticipantsInfoAppBar(
                numberOfParticipants = participants.size,
                infoStateMode = infoStateMode,
                onBackPressed = {
                    when (infoStateMode) {
                        is ParticipantListMode -> onDismiss()
                        else -> infoStateMode = ParticipantListMode
                    }
                },
                selectedParticipants = emptyList(),
                onInviteParticipants = { onInfoMenuAction.invoke(InviteUsers(selectedUsers)) }
            )

            val listModifier = Modifier
                .weight(2f)
                .fillMaxWidth()
                .background(VideoTheme.colors.appBackground)

            if (infoStateMode is ParticipantListMode) {
                CallParticipantsList(
                    modifier = listModifier,
                    participantsState = participants,
                    onUserOptionsSelected = { },
                    onOptionSelected = { option ->
                        when (option) {
                            Invite -> infoStateMode = ParticipantInvitesMode
                            is ToggleMute -> {
                                onInfoMenuAction.invoke(ChangeMuteState(!option.isEnabled))
                            }
                        }
                    },
                    isCurrentUserMuted = isLocalAudioEnabled
                )
            } else {
                InviteUserList(
                    modifier = listModifier,
                    users = emptyList(),
                    onUserSelected = { user ->
                        if (!selectedUsers.contains(user)) {
                            selectedUsers = selectedUsers + user
                        }
                    },
                    onUserUnSelected = { user ->
                        selectedUsers = selectedUsers - user
                    }
                )
            }
        }
    }
}
