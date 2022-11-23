package io.getstream.video.chat_with_video_sample.ui.channels

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.getstream.chat.android.client.api.models.querysort.QuerySortByField
import io.getstream.chat.android.client.models.Channel
import io.getstream.chat.android.compose.ui.channels.header.ChannelListHeader
import io.getstream.chat.android.compose.ui.channels.list.ChannelList
import io.getstream.chat.android.compose.ui.theme.ChatTheme
import io.getstream.chat.android.compose.viewmodel.channels.ChannelListViewModel
import io.getstream.chat.android.compose.viewmodel.channels.ChannelViewModelFactory
import io.getstream.video.chat_with_video_sample.application.ChatWithVideoApp
import io.getstream.video.chat_with_video_sample.application.chatWithVideoApp
import io.getstream.video.chat_with_video_sample.ui.messages.MessagesActivity

class ChannelsActivity : ComponentActivity() {

    private val factory by lazy {
        ChannelViewModelFactory(
            chatClient = chatWithVideoApp.chatClient,
            querySort = QuerySortByField.descByName("last_updated"),
            filters = null
        )
    }

    private val channelListViewModel by viewModels<ChannelListViewModel> { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = ChatTheme.colors.appBackground)
                ) {
                    val user by channelListViewModel.user.collectAsState()

                    ChannelListHeader(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Chat with Video Sample",
                        currentUser = user,
                        trailingContent = {
                            IconButton(
                                onClick = { logOut() },
                                content = {
                                    Icon(
                                        imageVector = Icons.Default.Logout,
                                        contentDescription = null,
                                        tint = ChatTheme.colors.errorAccent
                                    )
                                }
                            )
                        }
                    )

                    ChannelList(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = channelListViewModel,
                        onChannelLongClick = {},
                        onChannelClick = { channel ->
                            openMessages(channel)
                        }
                    )
                }
            }
        }
    }

    private fun openMessages(channel: Channel) {
        startActivity(MessagesActivity.getIntent(this, channel.cid))
    }

    private fun logOut() {
        chatWithVideoApp.logOut()
        finish()
    }

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, ChannelsActivity::class.java)
        }
    }
}