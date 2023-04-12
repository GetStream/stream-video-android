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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.getstream.video.android.compose.state.ui.participants.ParticipantInfoAction

/**
 * Represents a menu that shows information on the current call participants, while allowing the user
 * to trigger the Invite Users flow as well as trigger actions on any participants in a call.
 *
 * @param participantsState The list of active participants.
 * @param users The list of users that can be invited to the call.
 * @param modifier Modifier for styling.
 * @param onDismiss Handler when the user dismisses the UI through various actions.
 * @param onInfoMenuAction Handler when one of the menu actions is triggered.
 */
@Composable
public fun CallParticipantsInfoMenu(
    participantsState: List<Any>,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    onInfoMenuAction: (ParticipantInfoAction) -> Unit = {},
) {
//    var infoStateMode by remember { mutableStateOf<CallParticipantInfoMode>(ParticipantList) }
//    val isCurrentUserMuted = !(participantsState.firstOrNull { it.isLocal }?.hasAudio ?: false)
//    var selectedUsers by remember {
//        mutableStateOf(
//            users.map { InviteUserItemState(it) }
//        )
//    }
//    var selectedUser by remember { mutableStateOf<ParticipantState?>(null) }
//
//    BackHandler {
//        when {
//            infoStateMode is ParticipantList -> onDismiss()
//            selectedUser != null -> selectedUser = null
//            else -> {
//                infoStateMode = ParticipantList
//                val currentUsers = selectedUsers.updateAll { it.copy(isSelected = false) }
//
//                selectedUsers = currentUsers
//            }
//        }
//    }
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(color = VideoTheme.colors.infoMenuOverlayColor)
//    ) {
//        Column(modifier) {
//            CallParticipantsInfoAppBar(
//                numberOfParticipants = participantsState.size,
//                infoStateMode = infoStateMode,
//                onBackPressed = {
//                    when (infoStateMode) {
//                        is ParticipantList -> onDismiss()
//                        else -> infoStateMode = ParticipantList
//                    }
//                },
//                selectedParticipants = selectedUsers,
//                onInviteParticipants = onInfoMenuAction
//            )
//
//            val listModifier = Modifier
//                .weight(2f)
//                .fillMaxWidth()
//                .background(VideoTheme.colors.appBackground)
//
//            if (infoStateMode is ParticipantList) {
//                CallParticipantsList(
//                    modifier = listModifier,
//                    participantsState = participantsState,
//                    onUserOptionsSelected = { selectedUser = it }
//                )
//            } else {
//                InviteUserList(
//                    modifier = listModifier,
//                    users = selectedUsers,
//                    onUserSelected = { item ->
//                        val currentList = selectedUsers
//
//                        val updated = currentList.updateValue(
//                            predicate = { it.user.id == item.user.id },
//                            transformer = { it.copy(isSelected = !it.isSelected) }
//                        )
//
//                        selectedUsers = updated
//                    }
//                )
//            }
//
//            if (infoStateMode !is ParticipantInvites) {
//                CallParticipantsInfoOptions(
//                    isCurrentUserMuted = isCurrentUserMuted,
//                    modifier = Modifier
//                        .padding(12.dp)
//                        .fillMaxWidth()
//                        .background(color = VideoTheme.colors.appBackground)
//                        .height(VideoTheme.dimens.callParticipantInfoMenuOptionsHeight),
//                    onOptionSelected = { option ->
//                        when (option) {
//                            Invite -> infoStateMode = ParticipantInvites
//                            is ToggleMute -> onInfoMenuAction(ChangeMuteState(option.isMuted))
//                        }
//                    }
//                )
//            }
//
//            val currentlyUser = selectedUser
//            if (currentlyUser != null) {
//                SelectedCallParticipantOptions()
//            }
//        }
//    }
}
