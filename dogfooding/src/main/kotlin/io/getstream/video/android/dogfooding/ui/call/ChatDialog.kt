/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

@file:OptIn(ExperimentalMaterialApi::class)

package io.getstream.video.android.dogfooding.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.getstream.chat.android.compose.ui.messages.composer.MessageComposer
import io.getstream.chat.android.compose.ui.messages.list.MessageList
import io.getstream.chat.android.compose.ui.theme.ChatTheme
import io.getstream.chat.android.compose.ui.util.rememberMessageListState
import io.getstream.chat.android.compose.viewmodel.messages.MessageComposerViewModel
import io.getstream.chat.android.compose.viewmodel.messages.MessageListViewModel
import io.getstream.chat.android.compose.viewmodel.messages.MessagesViewModelFactory
import io.getstream.video.android.core.Call

@Composable
internal fun ChatDialog(
    call: Call,
    state: ModalBottomSheetState,
    content: @Composable () -> Unit,
    onDismissed: () -> Unit
) {
    val context = LocalContext.current
    val factory = MessagesViewModelFactory(
        context = context,
        channelId = "videocall:${call.id}",
    )

    var messageListViewModel by remember { mutableStateOf<MessageListViewModel?>(null) }
    LaunchedEffect(key1 = call) {
        messageListViewModel = factory.create(MessageListViewModel::class.java)
    }

    var composerViewModel by remember { mutableStateOf<MessageComposerViewModel?>(null) }
    LaunchedEffect(key1 = call) {
        composerViewModel = factory.create(MessageComposerViewModel::class.java)
    }

    ChatTheme {
        ModalBottomSheetLayout(
            modifier = Modifier
                .fillMaxWidth(),
            sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            sheetState = state,
            sheetContent = {
                Scaffold(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    backgroundColor = ChatTheme.colors.appBackground,
                    contentColor = ChatTheme.colors.appBackground,
                    topBar = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp)
                        ) {
                            Icon(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(end = 21.dp)
                                    .clickable { onDismissed.invoke() },
                                tint = ChatTheme.colors.textHighEmphasis,
                                painter = painterResource(id = io.getstream.video.android.ui.common.R.drawable.stream_video_ic_close),
                                contentDescription = null
                            )
                        }
                    },
                    bottomBar = {
                        if (composerViewModel != null) {
                            MessageComposer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                viewModel = composerViewModel!!,
                                onCommandsClick = { composerViewModel!!.toggleCommandsVisibility() },
                                onCancelAction = {
                                    messageListViewModel?.dismissAllMessageActions()
                                    composerViewModel!!.dismissMessageActions()
                                }
                            )
                        }
                    }
                ) {
                    if (messageListViewModel != null) {
                        val currentState = messageListViewModel!!.currentMessagesState
                        MessageList(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ChatTheme.colors.appBackground)
                                .padding(it),
                            viewModel = messageListViewModel!!,
                            messagesLazyListState = rememberMessageListState(parentMessageId = currentState.parentMessageId),
                        )
                    }
                }
            },
            content = content
        )
    }
}
