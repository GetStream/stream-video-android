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

package io.getstream.video.android.compose.ui.components.call.activecall.internal

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamDialogPositiveNegative
import io.getstream.video.android.mock.previewUsers
import io.getstream.video.android.model.User
import io.getstream.video.android.ui.common.R

/**
 * Simple dialog asking for invite confirmation from the user.
 *
 * @param users Users to invite.
 * @param onDismiss Handler when the user wants to dismiss and cancel the operation.
 * @param onInviteUsers Handler when the user wants to confirm invites.
 */
// TODO AAP: Move into demo-app
@Composable
internal fun InviteUsersDialog(
    users: List<User>,
    onDismiss: () -> Unit,
    onInviteUsers: (List<User>) -> Unit,
) {
    StreamDialogPositiveNegative(
        icon = Icons.Default.GroupAdd,
        title = stringResource(R.string.stream_video_invite_users_title),
        positiveButton = Triple(
            stringResource(R.string.stream_video_invite_users_accept),
            VideoTheme.styles.buttonStyles.secondaryButtonStyle(),
        ) {
            onInviteUsers(users)
        },
        negativeButton = Triple(
            stringResource(R.string.stream_video_invite_users_cancel),
            VideoTheme.styles.buttonStyles.tertiaryButtonStyle(),
        ) {
            onDismiss()
        },
        contentText = stringResource(R.string.stream_video_invite_users_message, users.size),
        style = VideoTheme.styles.dialogStyles.defaultDialogStyle(),
    )
}

@Preview
@Composable
private fun InviteUsersDialogPreview() {
    VideoTheme {
        InviteUsersDialog(users = previewUsers, onDismiss = { }) {
        }
    }
}
