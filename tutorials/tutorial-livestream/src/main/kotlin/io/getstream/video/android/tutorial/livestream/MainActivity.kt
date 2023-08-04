/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.compose.permission.LaunchCallPermissions
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.model.User
import io.getstream.video.android.tutorial.livestream.ui.LiveButton
import io.getstream.video.android.tutorial.livestream.ui.LiveLabel
import io.getstream.video.android.tutorial.livestream.ui.TimeLabel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiU2hhYWtfVGkiLCJpc3MiOiJwcm9udG8iLCJzdWIiOiJ1c2VyL1NoYWFrX1RpIiwiaWF0IjoxNjkxMDAyODQ1LCJleHAiOjE2OTE2MDc2NTB9.Siv53HaphgCP4NNY9WlxmznrBPE5Yo2WcO1LZzmG9eU"
        val userId = "Shaak_Ti"
        val callId = "BJxqOk7FHbKR"

        // step1 - create a user.
        val user = User(
            id = userId, // any string
            name = "Tutorial", // name and image are used in the UI
            role = "admin",
        )

        // step2 - initialize StreamVideo. For a production app we recommend adding the client to your Application class or di module.
        val client = StreamVideoBuilder(
            context = applicationContext,
            apiKey = "hd8szvscpxvd", // demo API key
            geo = GEO.GlobalEdgeNetwork,
            user = user,
            token = userToken,
        ).build()

        // step3 - join a call, which type is `default` and id is `123`.
        val call = client.call("livestream", callId)
        lifecycleScope.launch {
            // join the call
            val result = call.join(create = true)
            result.onError {
                Toast.makeText(applicationContext, "uh oh $it", Toast.LENGTH_SHORT).show()
            }
        }

        setContent {
            // request the Android runtime permissions for the camera and microphone
            LaunchCallPermissions(call = call)

            // step4 - apply VideoTheme
            VideoTheme {
                val connection by call.state.connection.collectAsState()
                val totalParticipants by call.state.totalParticipants.collectAsState()
                val backstage by call.state.backstage.collectAsState()
                val localParticipant by call.state.localParticipant.collectAsState()
                val video = localParticipant?.video?.collectAsState()?.value
                val duration by call.state.duration.collectAsState()

                androidx.compose.material.Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VideoTheme.colors.appBackground)
                        .padding(6.dp),
                    contentColor = VideoTheme.colors.appBackground,
                    backgroundColor = VideoTheme.colors.appBackground,
                    topBar = {
                        if (connection == RealtimeConnection.Connected) {
                            if (!backstage) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(6.dp)
                                ) {
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .background(
                                                color = VideoTheme.colors.primaryAccent,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                        text = "Live $totalParticipants",
                                        color = VideoTheme.colors.textHighEmphasis
                                    )

                                    Text(
                                        modifier = Modifier.align(Alignment.Center),
                                        text = "Live for $duration",
                                        color = VideoTheme.colors.textHighEmphasis
                                    )
                                }
                            }
                        }
                    },
                    bottomBar = {
                        androidx.compose.material.Button(
                            colors = ButtonDefaults.buttonColors(
                                contentColor = VideoTheme.colors.primaryAccent,
                                backgroundColor = VideoTheme.colors.primaryAccent
                            ),
                            onClick = {
                                lifecycleScope.launch {
                                    if (backstage) call.goLive() else call.stopLive()
                                }
                            }
                        ) {
                            Text(
                                text = if (backstage) "Go Live" else "Stop Broadcast",
                                color = Color.White
                            )
                        }
                    }
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
                        }
                    )
                }
            }
        }
    }
}
