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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.getstream.log.taggedLogger
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.Avatar
import io.getstream.video.android.compose.ui.components.avatar.InitialsAvatar
import io.getstream.video.android.core.user.UserPreferencesManager
import io.getstream.video.android.core.utils.initials
import io.getstream.video.android.core.utils.onError
import io.getstream.video.android.core.utils.onSuccess
import kotlinx.coroutines.launch
import kotlin.random.Random

class HomeActivity : AppCompatActivity() {

    private val streamVideo by lazy {
        logger.d { "[initStreamVideo] no args" }
        dogfoodingApp.streamVideo
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VideoTheme {
                HomeScreen()
            }
        }
    }

    private val logger by taggedLogger("Call:HomeView")

    private val callIdState: MutableState<String> = mutableStateOf("call" + Random.nextInt(1000))

    private val loadingState: MutableState<Boolean> = mutableStateOf(false)

    init {
        logger.i { "<init> this: $this" }
    }

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
                    text = "Log Out",
                    color = Color.White
                )
            }

            val isLoading by loadingState

            if (isLoading) {
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

        val isDataValid = callIdState.value.isNotBlank()

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp)
                .align(Alignment.CenterHorizontally),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = VideoTheme.colors.primaryAccent
            ),
            enabled = isDataValid, onClick = {
                joinCall(callId = callIdState.value)
            }
        ) {
            Text(text = "Join call", color = VideoTheme.colors.textHighEmphasis)
        }
    }

    private fun joinCall(callId: String) {
        lifecycleScope.launch {
            logger.d { "[joinCall] callId: $callId" }
            loadingState.value = true
            val result = streamVideo.joinCall(
                "default", id = callId
            )
            loadingState.value = false
            result.onSuccess { joinedCall -> logger.v { "[joinCall] succeed: $joinedCall" } }
            result.onError {
                logger.e { "[joinCall] failed: $it" }
                Toast.makeText(this@HomeActivity, it.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Composable
    fun CallIdInput() {
        val inputState by remember { callIdState }

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            value = inputState,
            onValueChange = { input ->
                callIdState.value = input
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

            val user = streamVideo.getUser()

            val name = user.name.ifEmpty {
                user.id
            }

            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .align(CenterVertically),
                color = VideoTheme.colors.textHighEmphasis,
                text = name,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }

    @Composable
    fun UserIcon() {
        val user = UserPreferencesManager.initialize(this).getUserCredentials() ?: return

        if (user.imageUrl.isNullOrEmpty()) {
            val initials = if (user.name.isNotEmpty()) {
                user.name.first()
            } else {
                user.id.first()
            }
            InitialsAvatar(
                modifier = Modifier
                    .size(40.dp)
                    .padding(top = 8.dp, start = 8.dp)
                    .clip(CircleShape),
                initials = initials.toString()
            )
        } else {
            Avatar(
                modifier = Modifier
                    .size(40.dp)
                    .padding(top = 8.dp, start = 8.dp)
                    .clip(CircleShape),
                imageUrl = user.imageUrl.orEmpty(),
                initials = user.name.initials(),
            )
        }
    }

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        }
    }
}
