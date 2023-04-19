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

package io.getstream.video.android.dogfooding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.getstream.log.taggedLogger
import io.getstream.result.flatMapSuspend
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.Avatar
import io.getstream.video.android.core.ConnectionState
import io.getstream.video.android.core.model.StreamCallId
import io.getstream.video.android.core.model.typeToId
import io.getstream.video.android.core.user.UserPreferencesManager
import io.getstream.video.android.core.utils.initials
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class HomeActivity : AppCompatActivity() {

    private val streamVideo by lazy {
        logger.d { "[initStreamVideo] no args" }
        dogfoodingApp.streamVideo
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent { VideoTheme { HomeScreen() } }
    }

    private val logger by taggedLogger("Call:HomeView")

    private val callCidState: MutableState<StreamCallId> = mutableStateOf(
        "default:${Random.nextInt(1000)}"
    )

    private val connectionState: StateFlow<ConnectionState> by lazy { streamVideo.state.connection }

    @Composable
    private fun HomeScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(VideoTheme.colors.appBackground),
            verticalArrangement = Arrangement.Center
        ) {
            UserDetails()

            JoinCallContent()

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = ::logOut,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = VideoTheme.colors.errorAccent, contentColor = Color.White
                )
            ) {
                Text(
                    text = "Log Out", color = Color.White
                )
            }

            val connectionState by connectionState.collectAsState()

            if (connectionState is ConnectionState.Loading) {
                Spacer(modifier = Modifier.height(16.dp))

                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = VideoTheme.colors.primaryAccent
                )
            }
        }
    }

    private fun logOut() {
        dogfoodingApp.logOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    @Composable
    fun ColumnScope.JoinCallContent() {
        CallIdInput()

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp)
                .align(Alignment.CenterHorizontally)
                .testTag("join_call"),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = VideoTheme.colors.primaryAccent
            ),
            onClick = { joinCall() }
        ) {
            Text(text = "Join call", color = Color.White)
        }
    }

    private fun joinCall() {
        lifecycleScope.launch {
            val (type, id) = callCidState.value.typeToId
            val call = streamVideo.call(type = type, id = id)

            val me = streamVideo.user
            val create = call.create(memberIds = listOf(me.id))
            create.flatMapSuspend {
                logger.d { "[createCall] succeed: $call" }

                val join = call.join()
                join.onSuccess {
                    logger.d { "[joinCall] succeed: $call" }

                    val intent = CallActivity.getIntent(this@HomeActivity, type, id)
                    startActivity(intent)
                }
            }.onError {
                logger.d { "failed: $it" }

                Toast.makeText(this@HomeActivity, it.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Composable
    fun CallIdInput() {
        val inputState by remember { callCidState }

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            value = inputState.replace(":", ""),
            onValueChange = { input ->
                callCidState.value = input
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = VideoTheme.colors.textHighEmphasis,
                focusedBorderColor = VideoTheme.colors.primaryAccent,
                focusedLabelColor = VideoTheme.colors.primaryAccent,
                unfocusedBorderColor = VideoTheme.colors.primaryAccent,
                unfocusedLabelColor = VideoTheme.colors.primaryAccent,
            ),
            label = {
                Text(text = "Enter the call ID")
            }
        )
    }

    @Composable
    private fun UserDetails() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            UserIcon()

            val user by streamVideo.state.user.collectAsState()

            val name = user?.name?.ifEmpty {
                user?.id
            }

            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .align(CenterVertically),
                color = VideoTheme.colors.textHighEmphasis,
                text = name ?: "Missing name",
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }

    @Composable
    fun UserIcon() {
        val user = UserPreferencesManager.initialize(this).getUserCredentials() ?: return

        Avatar(
            modifier = Modifier
                .size(40.dp)
                .padding(top = 8.dp, start = 8.dp),
            imageUrl = user.image,
            initials = if (user.image.isEmpty()) {
                user.name.initials()
            } else {
                null
            },
        )
    }

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        }
    }
}
