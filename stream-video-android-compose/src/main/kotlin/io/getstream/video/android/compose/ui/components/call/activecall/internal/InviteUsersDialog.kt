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

package io.getstream.video.android.compose.ui.components.call.activecall.internal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.model.User
import io.getstream.video.android.ui.common.R

/**
 * Simple dialog asking for invite confirmation from the user.
 *
 * @param users Users to invite.
 * @param onDismiss Handler when the user wants to dismiss and cancel the operation.
 * @param onInviteUsers Handler when the user wants to confirm invites.
 */
@Composable
internal fun InviteUsersDialog(
    users: List<io.getstream.video.android.model.User>,
    onDismiss: () -> Unit,
    onInviteUsers: (List<io.getstream.video.android.model.User>) -> Unit
) {

    Dialog(onDismissRequest = onDismiss, content = {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = VideoTheme.shapes.dialog,
            color = VideoTheme.colors.appBackground
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 32.dp),
                    text = stringResource(R.string.stream_video_invite_users_title),
                    style = VideoTheme.typography.bodyBold,
                    color = VideoTheme.colors.textHighEmphasis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.stream_video_invite_users_message, users.size),
                    style = VideoTheme.typography.body,
                    color = VideoTheme.colors.textHighEmphasis
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.align(End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = VideoTheme.colors.primaryAccent)
                    ) {
                        Text(text = stringResource(R.string.stream_video_invite_users_cancel))
                    }

                    TextButton(
                        onClick = { onInviteUsers(users) },
                        colors = ButtonDefaults.textButtonColors(contentColor = VideoTheme.colors.primaryAccent)
                    ) {
                        Text(text = stringResource(R.string.stream_video_invite_users_accept))
                    }
                }
            }
        }
    })
}
