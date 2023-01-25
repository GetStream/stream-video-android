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

package io.getstream.video.video_with_chat_starter.ui.login

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.ImageAvatar
import io.getstream.video.android.compose.utils.rememberStreamImagePainter
import io.getstream.video.android.model.User
import io.getstream.video.chat_with_video_starter.application.chatWithVideoApp
import io.getstream.video.chat_with_video_starter.ui.channels.ChannelsActivity

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dataSet = chatWithVideoApp.usersLoginProvider.provideUsers()

        setContent {
            VideoTheme {
                UserList(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VideoTheme.colors.appBackground),
                    userItems = dataSet,
                    onClick = { user ->
                        logIn(user)
                    }
                )
            }
        }
    }

    private fun logIn(user: User) {
        logInToChat(user)
        logInToVideo(user)
        startActivity(ChannelsActivity.getIntent(this))
    }

    private fun logInToChat(user: User) {
        // TODO
    }

    private fun logInToVideo(user: User) {
        // TODO
    }

    @Composable
    fun UserList(
        userItems: List<User>,
        modifier: Modifier = Modifier,
        onClick: (User) -> Unit
    ) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            userItems.forEachIndexed { index, credentials ->
                if (index > 0) {
                    Divider(startIndent = 16.dp, thickness = 0.5.dp, color = Color.LightGray)
                }
                UserItem(credentials = credentials, onClick = onClick)
            }
        }
    }

    @Composable
    fun UserItem(
        credentials: User,
        onClick: (User) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { onClick(credentials) }),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .align(alignment = Alignment.CenterVertically)
                        .clip(CircleShape)
                        .background(VideoTheme.colors.appBackground)
                ) {
                    ImageAvatar(
                        modifier = Modifier
                            .size(40.dp)
                            .align(alignment = Alignment.Center),
                        painter = rememberStreamImagePainter(data = credentials.imageUrl)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .weight(1f),
                    text = credentials.name,
                    fontSize = 16.sp,
                    color = VideoTheme.colors.textHighEmphasis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
