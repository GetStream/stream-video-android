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

package io.getstream.video.chat_with_video_starter.ui.messages.attachment

import android.content.Context
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.getstream.chat.android.client.models.Attachment
import io.getstream.chat.android.compose.state.messages.attachments.AttachmentState
import io.getstream.chat.android.compose.ui.attachments.AttachmentFactory
import io.getstream.chat.android.compose.ui.theme.ChatTheme
import io.getstream.chat.android.ui.common.helper.DateFormatter
import io.getstream.video.chat_with_video_starter.application.chatWithVideoApp
import kotlinx.coroutines.launch

fun CallAttachmentFactory(): AttachmentFactory = AttachmentFactory(
    previewContent = null,
    content = { modifier, state -> CallAttachment(modifier, state) },
    canHandle = ::canHandleCallAttachments
)

private fun canHandleCallAttachments(attachments: List<Attachment>): Boolean {
    val result = attachments.any {
        it.extraData["callCid"] != null &&
            it.extraData["members"] != null &&
            it.extraData["callName"] != null
    }

    Log.d("customAttachments", result.toString())

    return result
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
private fun CallAttachment(
    modifier: Modifier,
    attachmentState: AttachmentState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val message = attachmentState.message

    val callAttachment = message.attachments.first() // there has to be one
    val createdAt = message.createdAt
    val date = DateFormatter.from(LocalContext.current).formatDate(createdAt)
    val name = (callAttachment.extraData["callName"] as? String) ?: callAttachment.name
    val cid = callAttachment.extraData["callCid"] as String

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onLongClick = {
                attachmentState.onLongItemClick(message)
            }) { },
        shape = RoundedCornerShape(16.dp),
        color = ChatTheme.colors.infoAccent,
        onClick = {
            scope.launch {
                joinCall(
                    cid,
                    context
                )
            }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = name ?: "Unknown",
                    style = ChatTheme.typography.bodyBold,
                    color = ChatTheme.colors.textHighEmphasis
                )

                Text(
                    text = date,
                    style = ChatTheme.typography.body,
                    color = ChatTheme.colors.textHighEmphasis
                )
            }

            Button(
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
                onClick = {
                    scope.launch {
                        joinCall(
                            cid,
                            context
                        )
                    }
                },
                content = {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Videocam, contentDescription = null)

                        Text(
                            text = "Join",
                            style = ChatTheme.typography.body,
                            color = Color.Black
                        )
                    }
                }
            )
        }
    }
}

private suspend fun joinCall(callId: String, context: Context) {
    val (type, id) = callId.split(":").take(2)
    context.chatWithVideoApp.streamVideo.joinCall(type, id)
}
