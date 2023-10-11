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

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.model.User

@Composable
fun LiveAudience() {
    val context = LocalContext.current

    var call: Call? by remember { mutableStateOf(null) }

    LaunchedEffect(key1 = Unit) {
        val userToken =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiQmVuX1NreXdhbGtlciIsImlzcyI6InByb250byIsInN1YiI6InVzZXIvQmVuX1NreXdhbGtlciIsImlhdCI6MTY5Njk4NDE3MywiZXhwIjoxNjk3NTg4OTc4fQ.Cdq_sw1ZA_PiGNXmOIZdxZjmlBKK8DuW8Oy_YjKloZw"
        val userId = "Ben_Skywalker"
        val callId = "dE8AsD5Qxqrt"

        // step1 - create a user.
        val user = User(
            id = userId, // any string
            name = "Tutorial", // name and image are used in the UI
            role = "admin",
        )

        // step2 - initialize StreamVideo. For a production app we recommend adding the client to your Application class or di module.
        val client = StreamVideoBuilder(
            context = context,
            apiKey = "hd8szvscpxvd", // demo API key
            geo = GEO.GlobalEdgeNetwork,
            user = user,
            token = userToken,
            ensureSingleInstance = false,
        ).build()

        // step3 - join a call, which type is `default` and id is `123`.
        call = client.call("livestream", callId)

        // join the cal
        val result = call?.join()
        result?.onError {
            Toast.makeText(context, "uh oh $it", Toast.LENGTH_SHORT).show()
        }
    }

    if (call != null) {
        LiveGuestContent(call!!)
    }
}

@Composable
private fun LiveGuestContent(call: Call) {
    val participants by call.state.participants.collectAsState()
    val totalParticipants by call.state.totalParticipants.collectAsState()
    val backstage by call.state.backstage.collectAsState()
    val duration by call.state.duration.collectAsState()
    val livestream by call.state.livestream.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VideoTheme.colors.appBackground)
            .padding(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
        ) {
            if (backstage) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "Waiting for live host",
                    color = VideoTheme.colors.textHighEmphasis,
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier
                                .background(
                                    color = VideoTheme.colors.primaryAccent,
                                    shape = RoundedCornerShape(6.dp),
                                )
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            text = "Live $totalParticipants",
                            color = Color.White,
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "Live for $duration",
                            color = VideoTheme.colors.textHighEmphasis,
                        )
                    }

                    VideoRenderer(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        call = call,
                        video = livestream,
                    )
                }
            }
        }
    }
}
