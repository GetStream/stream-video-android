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

package io.getstream.video.android.ui.call

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.getstream.chat.android.compose.ui.messages.MessagesScreen
import io.getstream.chat.android.compose.ui.theme.ChatTheme
import io.getstream.chat.android.compose.viewmodel.messages.MessageListViewModel
import io.getstream.chat.android.compose.viewmodel.messages.MessagesViewModelFactory
import io.getstream.video.android.core.Call

@Composable
internal fun ChatDialog(
    call: Call,
    state: ModalBottomSheetState,
    content: @Composable () -> Unit,
    updateUnreadCount: (Int) -> Unit,
    onDismissed: () -> Unit,
) {
    val context = LocalContext.current
    val viewModelFactory = MessagesViewModelFactory(
        context = context,
        channelId = "videocall:${call.id}",
    )

    val listViewModel = viewModel(MessageListViewModel::class.java, factory = viewModelFactory)
    val unreadCount: Int = listViewModel.currentMessagesState.unreadCount

    LaunchedEffect(key1 = unreadCount) {
        updateUnreadCount.invoke(unreadCount)
    }

    ChatTheme {
        ModalBottomSheetLayout(
            modifier = Modifier.fillMaxWidth(),
            sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            sheetState = state,
            sheetContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp),
                ) {
                    MessagesScreen(
                        viewModelFactory = viewModelFactory,
                        onBackPressed = { onDismissed.invoke() },
                        onHeaderActionClick = { onDismissed.invoke() },
                    )
                }
            },
            content = content,
        )
    }
}
