package io.getstream.video.video_with_chat_final.ui.home

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.compose.ui.components.avatar.InitialsAvatar
import io.getstream.chat.android.compose.ui.components.avatar.UserAvatar
import io.getstream.chat.android.compose.ui.theme.ChatTheme
import io.getstream.chat.android.state.extensions.globalState
import io.getstream.video.android.call.state.CallMediaState
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.model.JoinedCall
import io.getstream.video.android.utils.Failure
import io.getstream.video.android.utils.Success
import io.getstream.video.video_with_chat_final.R
import io.getstream.video.video_with_chat_final.application.videoWithChatApp
import kotlinx.coroutines.launch
import java.util.UUID

class HomeActivity : AppCompatActivity() {

    private val microphoneState = mutableStateOf(false)
    private val cameraState = mutableStateOf(false)
    private val speakerphoneState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatTheme {
                VideoTheme {
                    HomeContent()
                }
            }
        }
    }

    @Composable
    private fun HomeContent() {
        Scaffold(
            topBar = { HomeTopBar() },
            content = { StartCallContent(modifier = Modifier.padding(it)) }
        )
    }

    @Composable
    private fun StartCallContent(
        modifier: Modifier
    ) {
        Column(modifier = modifier) {
            VideoPreview()

            Text(text = stringResource(R.string.call_preview))

            CallOptions()

            Button(
                colors = ButtonDefaults.buttonColors(backgroundColor = VideoTheme.colors.primaryAccent),
                onClick = { startCall() },
                content = {
                    Text(
                        text = stringResource(R.string.start_call),
                        color = Color.White
                    )
                }
            )
        }
    }

    @Composable
    private fun CallOptions() {
        Row(modifier = Modifier.fillMaxWidth()) {
            // microphone, camera, speaker buttons
        }
    }

    private fun startCall() {
        lifecycleScope.launch {
            val callResult = videoWithChatApp.streamVideo.createAndJoinCall(
                id = UUID.randomUUID().toString(),
                type = "default",
                participantIds = emptyList(),
                ringing = false
            )

            when (callResult) {
                is Success -> Unit // No-op, CallActivity will be started automatically

                is Failure -> Toast.makeText(
                    this@HomeActivity,
                    callResult.error.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    @Composable
    private fun VideoPreview() {

    }

    @Composable
    private fun HomeTopBar() {
        TopAppBar(modifier = Modifier.fillMaxWidth()) {
            val userState by ChatClient.instance().globalState.user.collectAsState()
            val currentUser = userState

            if (currentUser != null) {
                UserAvatar(user = currentUser)
            } else {
                InitialsAvatar(initials = "You")
            }

            Text(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .align(CenterVertically)
                    .padding(horizontal = 8.dp),
                text = stringResource(R.string.start_a_call),
                textAlign = TextAlign.Center
            )

            IconButton(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .weight(1f)
                    .align(CenterVertically),
                onClick = {
                    videoWithChatApp.logOut()
                    finish()
                },
                content = {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = stringResource(id = R.string.log_out)
                    )
                }
            )
        }
    }
}