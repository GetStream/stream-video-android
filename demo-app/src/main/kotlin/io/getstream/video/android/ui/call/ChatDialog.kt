/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.getstream.chat.android.compose.ui.messages.MessagesScreen
import io.getstream.chat.android.compose.ui.theme.ChatTheme
import io.getstream.chat.android.compose.viewmodel.messages.MessageListViewModel
import io.getstream.chat.android.compose.viewmodel.messages.MessagesViewModelFactory
import io.getstream.chat.android.ui.common.state.messages.list.MessageItemState
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.Call
import io.getstream.video.android.ui.common.R
import java.time.Instant
import java.util.Date

@Composable
internal fun ChatDialog(
    call: Call,
    state: ModalBottomSheetState,
    content: @Composable () -> Unit,
    updateUnreadCount: (Int) -> Unit,
    onNewMessages: (List<MessageItemState>) -> Unit,
    onDismissed: () -> Unit,
) {
    val context = LocalContext.current
    val viewModelFactory = remember {
        MessagesViewModelFactory(
            context = context,
            channelId = "videocall:${call.id}",
        )
    }
    val listViewModel = viewModel(MessageListViewModel::class.java, factory = viewModelFactory)
    val unreadCount = listViewModel.currentMessagesState.unreadCount

    LaunchedEffect(key1 = unreadCount) {
        updateUnreadCount.invoke(unreadCount)
    }

    val messageItems = listViewModel.currentMessagesState.messageItems
    LaunchedEffect(key1 = messageItems) {
        onNewMessages.invoke(
            messageItems.filterIsInstance<MessageItemState>().take(3)
                .filter {
                    it.message.createdAt?.after(
                        Date.from(
                            Instant.now().minusSeconds(10),
                        ),
                    ) == true
                }
                .map { messageItemState ->
                    if (messageItemState.message.text.isNotEmpty()) {
                        messageItemState
                    } else {
                        messageItemState.copy(
                            message = messageItemState.message.copy(
                                text = "[User shared an attachment]",
                            ),
                        )
                    }
                }
                .reversed(),
        )
    }

    ChatTheme(isInDarkMode = true) {
        ModalBottomSheetLayout(
            modifier = Modifier.fillMaxWidth(),
            sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            sheetState = state,
            sheetBackgroundColor = VideoTheme.colors.baseSheetPrimary,
            sheetContent = {
                if (state.isVisible) {
                    Column(
                        modifier = Modifier
                            .background(ChatTheme.colors.appBackground)
                            .fillMaxWidth()
                            .height(500.dp),
                    ) {
                        Icon(
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(16.dp)
                                .clickable { onDismissed.invoke() }
                                .testTag("Stream_CloseChatButton"),
                            tint = ChatTheme.colors.textHighEmphasis,
                            painter = painterResource(id = R.drawable.stream_video_ic_close),
                            contentDescription = null,
                        )

                        MessagesScreen(
                            showHeader = false,
                            viewModelFactory = viewModelFactory,
                            onBackPressed = { onDismissed.invoke() },
                            onHeaderTitleClick = { onDismissed.invoke() },
                        )
                    }
                }
            },
            content = content,
        )
    }
}
