/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.tutorial.audio

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.audio.AudioRoomContent
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CreateCallOptions
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.model.User
import kotlinx.coroutines.launch
import org.openapitools.client.models.MemberRequest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        val userToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiQW5ha2luX1NvbG8iLCJpc3MiOiJodHRwczovL3Byb250by5nZXRzdHJlYW0uaW8iLCJzdWIiOiJ1c2VyL0FuYWtpbl9Tb2xvIiwiaWF0IjoxNzE0MTE1MzkzLCJleHAiOjE3MTQ3MjAxOTh9.ro7JxpfzuGgcEtQ4QnjULPC2Z8qW-swVJAxHlVdpne8"
        val userId = "Liviu-NonMember-User"
//        val userId = "Liviu-Listener-12"
        val userToken = StreamVideo.devToken(userId)
//        val userToken = runBlocking {
//            StreamService.instance.getAuthData(
//                environment = "demo",
//                userId = userId,
//            )
//        }.token
        val callId = "liviu-android-room-2"

        // step1 - create a user.
        val user = User(
            id = userId, // any string
            name = userId,
//            role = "admin"
        )

        // step2 - initialize StreamVideo. For a production app we recommend adding the client to your Application class or di module.
        val client = StreamVideoBuilder(
            context = applicationContext,
            apiKey = "wq5vyn9n6v5h", // liviu@getstream Dashboard - Audio Room Tutorial app - Backstage disabled
            geo = GEO.GlobalEdgeNetwork,
            user = user,
            token = userToken,
        ).build()

        // step3 - join a call, which type is `audio_room` and id is `123`.
        val call = client.call("audio_room", callId)
        lifecycleScope.launch {
            val result = call.join(
                create = true,
                createOptions = CreateCallOptions(
                    members = listOf(
                        MemberRequest(userId = userId, role = "user", custom = emptyMap()),
                        MemberRequest(userId = "Liviu-Host-10", role = "host", custom = emptyMap()),
                        MemberRequest(userId = "Liviu-Host-11", role = "user", custom = emptyMap()),
                    ),
                    custom = mapOf(
                        "title" to "Compose Trends",
                        "description" to "Talk about how easy compose makes it to reuse and combine UI",
                    ),
                ),
            )
//            val result = call.join()

            result.onError {
                Toast.makeText(applicationContext, it.message, Toast.LENGTH_LONG).show()
            }
        }

        Log.d("RtcDebug", "[CallActivity] User: ${call.user.id}, role: ${call.user.role}")
        lifecycleScope.launch {
            call.state.connection.collect {
                Log.d("RtcDebug", "[CallActivity] RtcConnection: $it")
            }
        }

        setContent {
//            LaunchMicrophonePermissions(call = call)

            VideoTheme {
                val connection by call.state.connection.collectAsState()
                val activeSpeakers by call.state.activeSpeakers.collectAsState()
                val audioLevel = activeSpeakers.firstOrNull()?.audioLevel?.collectAsState()

                val color1 = Color.White.copy(alpha = 0.2f + (audioLevel?.value ?: 0f) * 0.8f)
                val color2 = Color.White.copy(alpha = 0.2f + (audioLevel?.value ?: 0f) * 0.8f)

                val scope = rememberCoroutineScope()
                val custom by call.state.custom.collectAsState()
                val backstage by call.state.backstage.collectAsState()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .background(Brush.linearGradient(listOf(color1, color2)))
                        .fillMaxSize()
                        .fillMaxHeight()
                        .padding(16.dp),
                ) {
                    if (connection != RealtimeConnection.Connected) {
                        Text("loading", fontSize = 30.sp)
                    } else {
//                        AudioRoom(call = call)
                        AudioRoomContent(
                            modifier = Modifier.height(500.dp),
                            call = call,
                            title = custom["title"] as String,
                        )

                        Button(
                            onClick = {
                                scope.launch {
                                    if (backstage) call.goLive() else call.stopLive()
                                }
                            },
                        ) {
                            Text(text = if (backstage) "Go Live" else "End")
                        }
                    }
                }
            }
        }
    }

    @Composable
    public fun AudioRoom(
        call: Call,
    ) {
        val custom by call.state.custom.collectAsState()
        val title = custom["title"] as? String
        val description = custom["description"] as? String
        val participants by call.state.participants.collectAsState()
        val activeSpeakers by call.state.activeSpeakers.collectAsState()
        val activeSpeaker = activeSpeakers.firstOrNull()
        val sortedParticipants by call.state.sortedParticipants.collectAsState(
            initial = emptyList(),
        )

        val backstage by call.state.backstage.collectAsState()
        val isMicrophoneEnabled by call.microphone.isEnabled.collectAsState()

        Description(title, description, participants)

        activeSpeaker?.let {
            Text("${it.userNameOrId.value} is speaking")
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(0.dp, 32.dp, 0.dp, 0.dp),
        ) {
            Participants(
                modifier = Modifier.weight(4f),
                sortedParticipants = sortedParticipants,
            )
            Controls(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                call = call,
                isMicrophoneEnabled = isMicrophoneEnabled,
                backstage = backstage,
                enableMicrophone = { call.microphone.setEnabled(it) },
            )
        }
    }

    @Composable
    public fun Description(
        title: String?,
        description: String?,
        participants: List<ParticipantState>,
    ) {
        Text("$title", fontSize = 30.sp)
        Text("$description", fontSize = 20.sp, modifier = Modifier.padding(16.dp))
        Text("${participants.size} participants", fontSize = 20.sp)
    }

    @Composable
    public fun Participants(
        modifier: Modifier = Modifier,
        sortedParticipants: List<ParticipantState>,
    ) {
        LazyVerticalGrid(
            modifier = modifier,
            columns = GridCells.Adaptive(minSize = 128.dp),
        ) {
            items(sortedParticipants.size) { index ->
                UserAvatar(
                    userName = sortedParticipants[index].userNameOrId.value,
                )
            }
        }
    }

    @Composable
    public fun Controls(
        modifier: Modifier = Modifier,
        call: Call,
        backstage: Boolean = false,
        isMicrophoneEnabled: Boolean = false,
        enableMicrophone: (Boolean) -> Unit = {},
    ) {
        val scope = rememberCoroutineScope()
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ToggleMicrophoneAction(
                modifier = Modifier.size(52.dp),
                isMicrophoneEnabled = isMicrophoneEnabled,
                onCallAction = { enableMicrophone(it.isEnabled) },
            )

            Button(
                onClick = {
                    scope.launch {
                        if (backstage) call.goLive() else call.stopLive()
                    }
                },
            ) {
                Text(text = if (backstage) "Go Live" else "End")
            }
        }
    }
}
