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

package io.getstream.video.android.app.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.TopStart
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import io.getstream.logging.StreamLog
import io.getstream.video.android.app.model.HomeScreenOption
import io.getstream.video.android.app.ui.call.CallActivity
import io.getstream.video.android.app.ui.components.UserList
import io.getstream.video.android.app.ui.login.LoginActivity
import io.getstream.video.android.app.utils.getUsers
import io.getstream.video.android.app.videoApp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.Avatar
import io.getstream.video.android.events.VideoEvent
import io.getstream.video.android.model.IceServer
import io.getstream.video.android.model.UserCredentials
import io.getstream.video.android.socket.SocketListener
import io.getstream.video.android.utils.onError
import io.getstream.video.android.utils.onSuccessSuspend
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private val logger = StreamLog.getLogger("Call:HomeView")

    private val controller by lazy {
        logger.d { "[initController] no args" }
        videoApp.streamCalls
    }

    private val selectedOption: MutableState<HomeScreenOption> =
        mutableStateOf(HomeScreenOption.CREATE_CALL)

    private val participantsOptions: MutableState<List<UserCredentials>> by lazy {
        mutableStateOf(
            getUsers().filter {
                it.id != controller.getUser().id
            }
        )
    }

    private val callIdState: MutableState<String> = mutableStateOf("call:123")

    private val loadingState: MutableState<Boolean> = mutableStateOf(false)

    private val socketListener = object : SocketListener {
        override fun onEvent(event: VideoEvent) {
            /*if (event is CallCreatedEvent) {
                startActivity(
                    IncomingCallActivity.getLaunchIntent(
                        this@HomeActivity,
                        event
                    )
                )
            }*/
        }
    }

    init {
        logger.i { "<init> this: $this" }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        logger.d { "[onCreate] savedInstanceState: $savedInstanceState" }
        super.onCreate(savedInstanceState)
        controller.addSocketListener(socketListener)
        setContent {
            VideoTheme {
                HomeScreen()
            }
        }
    }

    override fun onDestroy() {
        controller.removeSocketListener(socketListener)
        super.onDestroy()
    }

    @Composable
    private fun HomeScreen() {
        Box(modifier = Modifier.fillMaxSize()) {
            UserIcon()
            Column(modifier = Modifier.fillMaxSize()) {
                CallOptions()

                val selectedOption by remember { selectedOption }

                if (selectedOption == HomeScreenOption.CREATE_CALL) {
                    CreateCallContent()
                } else {
                    JoinCallContent()
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    onClick = ::logOut,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = VideoTheme.colors.errorAccent,
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "Log Out")
                }

                val isLoading by loadingState

                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))

                    CircularProgressIndicator(modifier = Modifier.align(CenterHorizontally))
                }
            }
        }
    }

    private fun logOut() {
        videoApp.userPreferences.clear()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    @Composable
    fun ColumnScope.CreateCallContent() {
        CallIdInput()

        ParticipantsOptions()

        val isDataValid = callIdState.value.isNotBlank()

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .align(CenterHorizontally),
            enabled = isDataValid,
            onClick = {
                joinCall(
                    callId = callIdState.value,
                    participants = participantsOptions.value.filter { it.isSelected }.map { it.id }
                )
            },
            content = {
                Text(text = "Create call")
            }
        )
    }

    @Composable
    fun ColumnScope.JoinCallContent() {
        CallIdInput()

        val isDataValid = callIdState.value.isNotBlank()

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .align(CenterHorizontally),
            enabled = isDataValid,
            onClick = {
                joinCall(
                    callId = callIdState.value,
                    participants = participantsOptions.value.filter { it.isSelected }.map { it.id }
                )
            },
            content = {
                Text(text = "Join call")
            }
        )
    }

    private fun joinCall(callId: String, participants: List<String> = emptyList()) {
        lifecycleScope.launch {
            loadingState.value = true

            val createCallResult = controller.createAndJoinCall(
                "default", // TODO - hardcoded for now
                id = callId,
                participantIds = participants.toList(),
                ringing = participants.isNotEmpty()
            )

            createCallResult.onSuccessSuspend { response ->
                loadingState.value = false
                navigateToCall(
                    response.call.cid,
                    response.callUrl,
                    response.userToken,
                    response.iceServers
                )
            }
            createCallResult.onError {
                Log.d("Couldn't select server", it.message ?: "")
                Toast.makeText(this@HomeActivity, it.message, Toast.LENGTH_SHORT).show()
                loadingState.value = false
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
            label = {
                Text(text = "Enter the call ID")
            }
        )
    }

    @Composable
    fun CallOptions() {
        val selectedOption by remember { selectedOption }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
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

            Button(
                modifier = Modifier.padding(8.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (selectedOption == HomeScreenOption.JOIN_CALL) MaterialTheme.colors.primary else Color.LightGray,
                    contentColor = Color.White
                ),
                onClick = { this@HomeActivity.selectedOption.value = HomeScreenOption.JOIN_CALL },
                content = {
                    Text(text = "Join Call")
                }
            )
        }
    }

    @Composable
    fun ColumnScope.ParticipantsOptions() {
        Text(
            modifier = Modifier.align(CenterHorizontally),
            text = "Select other participants",
            fontSize = 18.sp
        )

        val users by remember { participantsOptions }

        UserList(userItems = users, onClick = { user ->
            toggleSelectState(user, users)
        })
    }

    @Composable
    fun BoxScope.UserIcon() {
        val user = videoApp.credentialsProvider.getUserCredentials()
        Box(
            modifier = Modifier
                .padding(top = 8.dp, start = 8.dp)
                .align(TopStart)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.secondary)
            ) {
                Avatar(
                    imageUrl = user.imageUrl.orEmpty(),
                    initials = user.name,
                    modifier = Modifier.size(40.dp)
                        .align(Center)
                )
            }
        }
    }

    private fun navigateToCall(
        callId: String,
        signalUrl: String,
        userToken: String,
        iceServers: List<IceServer>
    ) {
        val intent = CallActivity.getIntent(this, callId, signalUrl, userToken, iceServers)
        startActivity(intent)
    }

    private fun toggleSelectState(user: UserCredentials, users: List<UserCredentials>) {
        val currentUsers = users.toMutableList()
        val selectedUser = currentUsers.firstOrNull { it.id == user.id }

        if (selectedUser != null) {
            val index = currentUsers.indexOf(selectedUser)
            val updated = selectedUser.copy(isSelected = !selectedUser.isSelected)

            currentUsers.removeAt(index)
            currentUsers.add(index, updated)

            participantsOptions.value = currentUsers
        }
    }

    companion object {
        fun getIntent(context: Context): Intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    }
}
