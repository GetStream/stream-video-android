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

package io.getstream.video.android.ui.call

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.mock.previewParticipantsList
import io.getstream.video.android.util.config.AppConfig

@Composable
public fun ParticipantsDialog(call: Call, onDismiss: () -> Unit) {
    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = Color.Black,
                ),
        ) {
            ParticipantsList(call = call)
            IconButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .testTag("Stream_ParticipantsListCloseButton"),
                onClick = {
                    onDismiss()
                },
            ) {
                Icon(
                    tint = Color.White,
                    imageVector = Icons.Default.Close,
                    contentDescription = Icons.Default.Close.name,
                )
            }
            Spacer(modifier = Modifier.size(VideoTheme.dimens.spacingM))
        }
    }
}

@Composable
fun ParticipantsList(call: Call) {
    val participants by call.state.participants.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    }
    ParticipantsListContent(call, clipboardManager, participants)
}

@Composable
fun ParticipantsListContent(
    call: Call,
    clipboardManager: ClipboardManager? = null,
    participants: List<ParticipantState>,
) {
    val context = LocalContext.current
    LazyColumn {
        item {
            Text(
                text = "Participants (${participants.size})",
                style = VideoTheme.typography.labelM,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.size(16.dp))
            val env = AppConfig.currentEnvironment.collectAsStateWithLifecycle()
            ShareCallWithOthers(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                call,
                clipboardManager,
                env,
                context,
            )
            Spacer(modifier = Modifier.size(16.dp))
        }

        items(count = participants.size, key = { index -> participants[index].sessionId }) {
            val participant = participants[it]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VideoTheme.dimens.spacingM),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val userName by participant.userNameOrId.collectAsStateWithLifecycle()
                    val userImage by participant.image.collectAsStateWithLifecycle()
                    UserAvatar(
                        modifier = Modifier
                            .size(VideoTheme.dimens.genericXxl)
                            .testTag("Stream_ParticipantsListUserAvatar"),
                        userImage = userImage,
                        userName = userName,
                        isShowingOnlineIndicator = false,
                    )
                    Spacer(modifier = Modifier.size(VideoTheme.dimens.spacingM))
                    Text(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .testTag("Stream_ParticipantsListUserName"),
                        text = userName,
                        style = VideoTheme.typography.bodyM,
                        color = VideoTheme.colors.basePrimary,
                        fontSize = 16.sp,
                        maxLines = 1,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val audioEnabled by participant.audioEnabled.collectAsStateWithLifecycle()
                    val iconAudio = if (audioEnabled) {
                        Icons.Default.Mic
                    } else {
                        Icons.Default.MicOff
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        modifier = Modifier
                            .testTag("Stream_ParticipantsListUserMicrophone_Enabled_$audioEnabled"),
                        tint = VideoTheme.colors.basePrimary,
                        imageVector = iconAudio,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    val videoEnabled by participant.videoEnabled.collectAsStateWithLifecycle()
                    val iconVideo = if (videoEnabled) {
                        Icons.Default.Videocam
                    } else {
                        Icons.Default.VideocamOff
                    }
                    Icon(
                        modifier = Modifier
                            .testTag("Stream_ParticipantsListUserCamera_Enabled_$videoEnabled"),
                        tint = VideoTheme.colors.basePrimary,
                        imageVector = iconVideo,
                        contentDescription = null,
                    )
                }
            }
            Spacer(modifier = Modifier.size(VideoTheme.dimens.spacingM))
        }
    }
}

@Preview
@Composable
private fun ParticipantsDialogPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        ParticipantsListContent(call = previewCall, participants = previewParticipantsList)
    }
}
