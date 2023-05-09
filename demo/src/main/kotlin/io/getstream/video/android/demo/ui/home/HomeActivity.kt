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

package io.getstream.video.android.demo.ui.home

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.getstream.log.taggedLogger
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.Avatar
import io.getstream.video.android.core.user.UserPreferencesManager
import io.getstream.video.android.core.utils.initials
import io.getstream.video.android.demo.demoVideoApp
import io.getstream.video.android.demo.model.HomeScreenOption
import io.getstream.video.android.demo.ui.call.CallActivity
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private val logger by taggedLogger("Call:HomeView")

    private val streamVideo by lazy {
        logger.d { "[initStreamVideo] no args" }
        demoVideoApp.streamVideo
    }

    private val selectedOption: MutableState<HomeScreenOption> =
        mutableStateOf(HomeScreenOption.CREATE_CALL)

    private val callIdState: MutableState<String> = mutableStateOf("NnXAIvBKE4Hy")

    private val loadingState: MutableState<Boolean> = mutableStateOf(false)

    init {
        logger.i { "<init> this: $this" }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        logger.d { "[onCreate] savedInstanceState: $savedInstanceState" }
        super.onCreate(savedInstanceState)
        setContent {
            VideoTheme {
                HomeScreen()
            }
        }
    }

    @Composable
    private fun HomeScreen() {
        Box(modifier = Modifier.fillMaxSize()) {
            UserIcon()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                CallIdInput()

                val isDataValid = callIdState.value.isNotBlank()

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .align(CenterHorizontally),
                    enabled = isDataValid,
                    onClick = {
                        createMeeting(
                            callId = callIdState.value,
                            participants = listOf(streamVideo.userId)
                        )
                    }
                ) {
                    Text(text = "Create and Join call")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .align(CenterHorizontally),
                    enabled = isDataValid,
                    onClick = { startCallActivity() }
                ) {
                    Text(text = "Join call")
                }

                val isLoading by loadingState

                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))

                    CircularProgressIndicator(modifier = Modifier.align(CenterHorizontally))
                }
            }
        }
    }

    private fun createMeeting(callId: String, participants: List<String>) {
        lifecycleScope.launch {
            logger.d { "[createMeeting] callId: $callId, participants: $participants" }

            loadingState.value = true
            val call = streamVideo.call("default", callId)

            val result = call.create(memberIds = participants)
            result.onSuccess { data ->
                logger.d { "[createMeeting] successful: $data" }
                loadingState.value = false

                startCallActivity()
            }.onError {
                logger.e { "[createMeeting] failed: $it" }
                Toast.makeText(this@HomeActivity, it.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCallActivity() {
        val intent = CallActivity.getIntent(this@HomeActivity, "default:${callIdState.value}")
        startActivity(intent)
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
            label = {
                Text(text = "Enter the call ID")
            }
        )
    }

    @Composable
    fun CallOptions() {
        val selectedOption by remember { selectedOption }

        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
        ) {
            Button(
                modifier = Modifier.padding(8.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (selectedOption == HomeScreenOption.CREATE_CALL) MaterialTheme.colors.primary else Color.LightGray,
                    contentColor = Color.White
                ),
                onClick = { this@HomeActivity.selectedOption.value = HomeScreenOption.CREATE_CALL },
                content = {
                    Text(text = "Create Call")
                }
            )
            val context = LocalContext.current

            Button(
                modifier = Modifier.padding(8.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (selectedOption == HomeScreenOption.JOIN_CALL) MaterialTheme.colors.primary else Color.LightGray,
                    contentColor = Color.White
                ),
                onClick = {
                    TODO()
                },
                content = {
                    Text(text = "Join Call")
                }
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
            imageUrl = user.image.orEmpty(),
            initials = if (user.image.isEmpty()) {
                user.name.initials()
            } else {
                null
            },
        )
    }
}
