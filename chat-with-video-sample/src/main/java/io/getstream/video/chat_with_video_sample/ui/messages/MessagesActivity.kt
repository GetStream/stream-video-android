package io.getstream.video.chat_with_video_sample.ui.messages

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.getstream.chat.android.compose.ui.messages.composer.MessageComposer
import io.getstream.chat.android.compose.ui.messages.header.MessageListHeader
import io.getstream.chat.android.compose.ui.messages.list.MessageList
import io.getstream.chat.android.compose.ui.theme.ChatTheme
import io.getstream.chat.android.compose.viewmodel.messages.MessageComposerViewModel
import io.getstream.chat.android.compose.viewmodel.messages.MessageListViewModel
import io.getstream.chat.android.compose.viewmodel.messages.MessagesViewModelFactory
import io.getstream.video.chat_with_video_sample.application.chatWithVideoApp
import kotlinx.coroutines.launch
import java.util.*

class MessagesActivity : ComponentActivity() {

    private val factory by lazy {
        MessagesViewModelFactory(
            context = this,
            channelId = intent.getStringExtra(KEY_CHANNEL_ID)!!
        )
    }

    private val messageListViewModel by viewModels<MessageListViewModel> { factory }

    private val composerViewModel by viewModels<MessageComposerViewModel> { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ChatTheme {
                val channelState = messageListViewModel.channel
                val currentUser by messageListViewModel.user.collectAsState()

                Scaffold(
                    topBar = {
                        MessageListHeader(
                            channel = channelState,
                            currentUser = currentUser,
                            trailingContent = { CallButton() },
                            onBackPressed = { finish() }
                        )
                    },
                    bottomBar = { MessageComposer(viewModel = composerViewModel) },
                    content = { paddingValues ->
                        MessageList(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ChatTheme.colors.appBackground)
                                .padding(paddingValues),
                            viewModel = messageListViewModel
                        )
                    }
                )
            }
        }
    }

    @Composable
    private fun CallButton() {
        Icon(
            modifier = Modifier
                .padding(end = 8.dp)
                .clickable { startCall() },
            imageVector = Icons.Default.Call,
            contentDescription = null,
            tint = ChatTheme.colors.textHighEmphasis
        )
    }

    private fun startCall() {
        val users = messageListViewModel.channel.members.map {
            it.user.id
        }

        val videoClient = chatWithVideoApp.streamVideo

        lifecycleScope.launch {
            videoClient.createAndJoinCall(
                id = UUID.randomUUID().toString(),
                type = "default",
                ringing = true,
                participantIds = users
            )
        }
    }

    companion object {
        private const val KEY_CHANNEL_ID = "channel_id"
        fun getIntent(context: Context, channelId: String): Intent {
            return Intent(context, MessagesActivity::class.java).apply {
                putExtra(KEY_CHANNEL_ID, channelId)
            }
        }
    }
}