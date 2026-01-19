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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.state.ui.internal.CallParticipantInfoMode
import io.getstream.video.android.compose.state.ui.internal.ParticipantListMode
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.participants.internal.CallParticipantsList
import io.getstream.video.android.compose.ui.components.participants.internal.InviteUserList
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.InviteUsersToCall
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall

/**
 * Represents a menu that shows information on the current call participants, while allowing the user
 * to trigger the Invite Users flow as well as trigger actions on any participants in a call.
 *
 * @param call The call that contains all the participants state and tracks.
 * @param modifier Modifier for styling.
 * @param onDismiss Handler when the user dismisses the UI through various actions.
 */
@Composable
public fun CallParticipantsInfoMenu(
    call: Call,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    onCallAction: (CallAction) -> Unit = {},
) {
    val me by call.state.me.collectAsStateWithLifecycle()
    val participants by call.state.participants.collectAsStateWithLifecycle()
    val audioEnabled = me?.audioEnabled?.collectAsStateWithLifecycle()

    var infoStateMode by remember { mutableStateOf<CallParticipantInfoMode>(ParticipantListMode) }
    var selectedUsers: List<ParticipantState> = remember { mutableStateListOf() }

    val onBackPressed: () -> Unit = {
        when (infoStateMode) {
            is ParticipantListMode -> onDismiss()
            else -> onDismiss()
        }
    }

    BackHandler { onBackPressed.invoke() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = VideoTheme.colors.baseSheetPrimary),
    ) {
        Column(modifier) {
            val listModifier = Modifier
                .weight(2f)
                .fillMaxWidth()
                .background(VideoTheme.colors.baseSheetPrimary)

            if (infoStateMode is ParticipantListMode) {
                CallParticipantsList(
                    modifier = listModifier,
                    participants = participants,
                    isLocalAudioEnabled = audioEnabled?.value ?: false,
                    onUserOptionsSelected = { },
                    onInviteUser = { onCallAction.invoke(InviteUsersToCall(selectedUsers)) },
                    onMute = { enabled -> onCallAction.invoke(ToggleMicrophone(enabled)) },
                    onBackPressed = onBackPressed,
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
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun CallParticipantsInfoMenuPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallParticipantsInfoMenu(
            call = previewCall,
        )
    }
}
