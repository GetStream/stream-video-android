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

package io.getstream.video.video_with_chat_final.ui.call

import android.content.Context
import android.content.res.Configuration
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.getstream.chat.android.client.extensions.cidToTypeAndId
import io.getstream.chat.android.compose.ui.messages.composer.MessageComposer
import io.getstream.chat.android.compose.ui.messages.header.MessageListHeader
import io.getstream.chat.android.compose.ui.messages.list.MessageList
import io.getstream.chat.android.compose.viewmodel.messages.MessageComposerViewModel
import io.getstream.chat.android.compose.viewmodel.messages.MessageListViewModel
import io.getstream.chat.android.compose.viewmodel.messages.MessagesViewModelFactory
import io.getstream.video.android.StreamVideo
import io.getstream.video.android.call.state.CallAction
import io.getstream.video.android.call.state.CallMediaState
import io.getstream.video.android.call.state.CustomAction
import io.getstream.video.android.compose.state.ui.call.CallControlAction
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.AbstractComposeCallActivity
import io.getstream.video.android.compose.ui.components.call.CallContent
import io.getstream.video.android.compose.ui.components.call.controls.CallControls
import io.getstream.video.android.compose.ui.components.call.controls.buildDefaultCallControlActions
import io.getstream.video.android.model.state.StreamCallState
import io.getstream.video.android.viewmodel.CallViewModelFactory
import io.getstream.video.video_with_chat_final.R
import io.getstream.video.video_with_chat_final.application.videoWithChatApp
import kotlinx.coroutines.flow.emptyFlow

class CallActivity : AbstractComposeCallActivity() {

    /**
     * Provides the StreamVideo instance through the videoApp.
     */
    override fun getStreamVideo(context: Context): StreamVideo =
        context.videoWithChatApp.streamVideo

    private val isShowingChatState = mutableStateOf(false)

    /**
     * Provides a custom factory for the ViewModel, that provides fake users for invites.
     */
    override fun getCallViewModelFactory(): CallViewModelFactory {
        return CallViewModelFactory(
            streamVideo = getStreamVideo(this),
            permissionManager = getPermissionManager(),
            usersProvider = videoWithChatApp.usersLoginProvider
        )
    }

    override fun buildContent(): @Composable () -> Unit {
        return {
            VideoTheme {
                val isShowingChat by remember { isShowingChatState }

                if (isShowingChat) {
                    ChatContent()
                } else {
                    CallContent(
                        modifier = Modifier.background(color = VideoTheme.colors.appBackground),
                        viewModel = callViewModel,
                        onCallAction = ::handleCallAction,
                        onBackPressed = ::handleBackPressed,
                        pictureInPictureContent = { PictureInPictureContent(call = it) },
                        callControlsContent = { CustomCallControlsContent() }
                    )
                }
            }
        }
    }

    private val factory by lazy {
        val callState = callViewModel.streamCallState.value as StreamCallState.Connected

        MessagesViewModelFactory(
            context = this,
            channelId = callState.callGuid.cid
        )
    }

    private val messageListViewModel by viewModels<MessageListViewModel> { factory }

    private val composerViewModel by viewModels<MessageComposerViewModel> { factory }


    @Composable
    private fun ChatContent() {
        Scaffold(modifier = Modifier.fillMaxSize(),
            topBar = { ChatTopBar() },
            bottomBar = { ChatComposer() },
            content = {
                MessageList(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it),
                    viewModel = messageListViewModel
                )
            }
        )
    }

    @Composable
    private fun ChatTopBar() {
        val channel = messageListViewModel.channel
        val currentUser by messageListViewModel.user.collectAsState()

        MessageListHeader(
            modifier = Modifier.fillMaxWidth(),
            channel = channel,
            currentUser = currentUser,
            onBackPressed = { isShowingChatState.value = false }
        )
    }

    @Composable
    private fun ChatComposer() {
        MessageComposer(
            modifier = Modifier.fillMaxWidth(),
            viewModel = composerViewModel,
            integrations = {}
        )
    }

    @Composable
    private fun CustomCallControlsContent() {
        val call by callViewModel.callState.collectAsState(initial = null)
        val callMediaState by callViewModel.callMediaState.collectAsState(initial = CallMediaState())

        val screenShareSessionsState = call?.screenSharingSessions ?: emptyFlow()
        val state by screenShareSessionsState.collectAsState(initial = emptyList())

        val streamCallState by callViewModel.streamCallState.collectAsState()

        val isScreenSharing = state.isNotEmpty()

        val orientation = LocalConfiguration.current.orientation

        val modifier = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Modifier
                .fillMaxHeight()
                .width(VideoTheme.dimens.landscapeCallControlsSheetWidth)
        } else {
            Modifier
                .fillMaxWidth()
                .height(VideoTheme.dimens.callControlsSheetHeight)
        }

        val currentCallState = (streamCallState as? StreamCallState.Connected)
        val defaultActions = buildDefaultCallControlActions(callMediaState = callMediaState)

        val actions = if (currentCallState is StreamCallState.Connected) {
            val customAction = CustomAction(
                data = mapOf(
                    KEY_CHANNEL_CID to currentCallState.callGuid.cid
                )
            )

            val customCallControlAction = CallControlAction(
                actionBackgroundTint = Color.White,
                icon = painterResource(id = R.drawable.ic_chat_bubble),
                iconTint = Color.DarkGray,
                callAction = customAction,
                description = stringResource(R.string.open_chat),
                decorator = {
                    val unreadMessages = messageListViewModel.currentMessagesState.unreadCount

                    if (unreadMessages > 1) {
                        Text(
                            modifier = Modifier
                                .background(
                                    VideoTheme.colors.errorAccent,
                                    shape = CircleShape
                                )
                                .padding(3.dp)
                                .align(Alignment.TopEnd),
                            text = unreadMessages.toString()
                        )
                    }
                }
            )

            listOf(customCallControlAction) + defaultActions
        } else {
            defaultActions
        }

        CallControls(
            modifier = modifier,
            callMediaState = callMediaState,
            isScreenSharing = isScreenSharing,
            onCallAction = ::handleCallAction,
            actions = actions
        )
    }

    override fun handleCallAction(action: CallAction) {
        if (action is CustomAction) {
            val channelData =
                callViewModel.streamCallState.value as? StreamCallState.Connected ?: return
            val data = action.data[KEY_CHANNEL_CID] as? String

            if (data.isNullOrBlank()) {
                return
            }

            val (id, type) = data.cidToTypeAndId()

            val channelRequest = videoWithChatApp.chatClient.createChannel(
                channelType = type,
                channelId = id,
                memberIds = channelData.users.keys.toList(),
                extraData = emptyMap()
            )

            channelRequest.enqueue {
                if (it.isSuccess) {
                    isShowingChatState.value = true
                }
            }
        } else {
            super.handleCallAction(action)
        }
    }

    private companion object {
        private const val KEY_CHANNEL_CID = "channelCid"
    }
}
