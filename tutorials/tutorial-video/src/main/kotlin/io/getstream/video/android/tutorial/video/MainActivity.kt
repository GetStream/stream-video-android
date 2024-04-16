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

package io.getstream.video.android.tutorial.video

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.compose.permission.LaunchCallPermissions
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.renderer.FloatingParticipantVideo
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantVideo
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.model.User

/**
 * This tutorial demonstrates how to implement a video call screen by using low-level APIs, such as
 * [ParticipantVideo] and [FloatingParticipantVideo]. You can build your own call screen with theses
 * components.
 *
 * You will be able to build your call screen following the steps below.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // replace the secrets with the following instruction:
        // https://getstream.io/video/docs/android/playground/demo-credentials/
        val userId = "REPLACE_WITH_USER_ID"
        val userToken = "REPLACE_WITH_TOKEN"
        val callId = "REPLACE_WITH_CALL_ID"

        // step1 - create a user.
        val user = User(
            id = userId, // any string
            name = "Tutorial", // name and image are used in the UI
            role = "admin",
        )

        // step2 - initialize StreamVideo. For a production app we recommend adding the client to your Application class or di module.
        val client = StreamVideoBuilder(
            context = applicationContext,
            apiKey = "REPLACE_WITH_API_KEY", // demo API key
            geo = GEO.GlobalEdgeNetwork,
            user = user,
            token = userToken,
        ).build()

        setContent {
            // step3 - request permissions and join a call, which type is `default` and id is `123`.
            val call = client.call(type = "default", id = callId)
            LaunchCallPermissions(call = call) {
                call.join(create = true)
            }

            // step4 - apply VideTheme
            VideoTheme {
                // step5 - define required properties.
                val remoteParticipants by call.state.remoteParticipants.collectAsState()
                val remoteParticipant = remoteParticipants.firstOrNull()
                val me by call.state.localParticipant.collectAsState()
                val connection by call.state.connection.collectAsState()
                var parentSize: IntSize by remember { mutableStateOf(IntSize(0, 0)) }

                LaunchedEffect(key1 = connection) {
                    Log.e("Test", "connection: $connection")
                }

                // step5 - render a local and remote videos.
                BoxWithConstraints(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VideoTheme.colors.baseSheetPrimary)
                        .onSizeChanged { parentSize = it },
                ) {
                    if (remoteParticipant != null) {
                        val remoteVideo by remoteParticipant.video.collectAsState()

                        Column(modifier = Modifier.fillMaxSize()) {
                            VideoRenderer(
                                modifier = Modifier.weight(1f),
                                call = call,
                                video = remoteVideo,
                            )
                        }
                    } else {
                        if (connection != RealtimeConnection.Connected) {
                            Text(
                                text = "loading...",
                                fontSize = 30.sp,
                                color = VideoTheme.colors.basePrimary,
                            )
                        } else {
                            Text(
                                modifier = Modifier.padding(30.dp),
                                text = "Join call ${call.id} in your browser",
                                fontSize = 30.sp,
                                color = VideoTheme.colors.basePrimary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    // floating video UI for the local video participant
                    if (me != null) {
                        FloatingParticipantVideo(
                            modifier = Modifier.align(Alignment.TopEnd),
                            call = call,
                            participant = me!!,
                            parentBounds = parentSize,
                        )
                    }
                }
            }
        }
    }
}
