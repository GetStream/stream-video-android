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

package io.getstream.video.android.tutorial.livestream

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.getstream.log.Priority
import io.getstream.video.android.compose.permission.LaunchCallPermissions
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.model.User
import kotlinx.coroutines.launch

@Composable
fun LiveHost() {
    val context = LocalContext.current

    val userToken =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiRGFydGhfS3JheXQiLCJpc3MiOiJwcm9udG8iLCJzdWIiOiJ1c2VyL0RhcnRoX0tyYXl0IiwiaWF0IjoxNjk2OTgzMjk1LCJleHAiOjE2OTc1ODgxMDB9.g5K76Vv5D-uCoBfAfDpI3pyQIpoFMx8J9Eus0VkHk-M"
    val userId = "Darth_Krayt"
    val callId = "dE8AsD5Qxqrt"

    // step1 - create a user.
    val user = User(
        id = userId, // any string
        name = "Tutorial", // name and image are used in the UI
        role = "guest",
    )

    // step2 - initialize StreamVideo. For a production app we recommend adding the client to your Application class or di module.
    val client = StreamVideoBuilder(
        context = context,
        apiKey = "hd8szvscpxvd", // demo API key
        geo = GEO.GlobalEdgeNetwork,
        user = user,
        token = userToken,
        ensureSingleInstance = false,
        loggingLevel = LoggingLevel(priority = Priority.VERBOSE),
    ).build()

    // step3 - join a call, which type is `default` and id is `123`.
    val call = client.call("livestream", callId)

    LaunchCallPermissions(call = call) {
        val result = call.join(create = true)
        result.onError {
            Toast.makeText(context, "uh oh $it", Toast.LENGTH_SHORT).show()
        }
    }
    LiveHostContent(call)
}

@Composable
private fun LiveHostContent(call: Call) {
    LaunchCallPermissions(call = call)

    val connection by call.state.connection.collectAsState()
    val totalParticipants by call.state.totalParticipants.collectAsState()
    val backstage by call.state.backstage.collectAsState()
    val localParticipant by call.state.localParticipant.collectAsState()
    val video = localParticipant?.video?.collectAsState()?.value
    val duration by call.state.duration.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(VideoTheme.colors.baseSheetPrimary)
            .padding(6.dp),
        contentColor = VideoTheme.colors.baseSheetPrimary,
        backgroundColor = VideoTheme.colors.baseSheetPrimary,
        topBar = {
            if (connection == RealtimeConnection.Connected) {
                if (!backstage) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .background(
                                    color = VideoTheme.colors.brandPrimary,
                                    shape = RoundedCornerShape(6.dp),
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            text = "Live $totalParticipants",
                            color = Color.White,
                        )

                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = "Live for $duration",
                            color = VideoTheme.colors.basePrimary,
                        )
                    }
                } else {
                    Text(
                        text = "Backstage",
                        color = VideoTheme.colors.basePrimary,
                    )
                }
            } else if (connection is RealtimeConnection.Failed) {
                Text(
                    text = "Connection failed",
                    color = VideoTheme.colors.basePrimary,
                )
            }
        },
        bottomBar = {
            Button(
                colors = ButtonDefaults.buttonColors(
                    contentColor = VideoTheme.colors.brandPrimary,
                    backgroundColor = VideoTheme.colors.brandPrimary,
                ),
                onClick = {
                    scope.launch {
                        if (backstage) call.goLive() else call.stopLive()
                    }
                },
            ) {
                Text(
                    text = if (backstage) "Start Broadcast" else "Stop Broadcast",
                    color = Color.White,
                )
            }
        },
    ) {
        VideoRenderer(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .clip(RoundedCornerShape(6.dp)),
            call = call,
            video = video,
            videoFallbackContent = {
                Text(text = "Video rendering failed")
            },
        )
    }
}
