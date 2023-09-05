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

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiQWRtaXJhbF9BY2tiYXIiLCJpc3MiOiJwcm9udG8iLCJzdWIiOiJ1c2VyL0FkbWlyYWxfQWNrYmFyIiwiaWF0IjoxNjkzNzk0NTc4LCJleHAiOjE2OTQzOTkzODN9.7uYF4xB1zUrQ1GIpsoICoU5G0DpXq_5_IDyohz6p3VU"
        val userId = "Admiral_Ackbar"
        val callId = "szua8Iy5iMX2"

        // step1 - create a user.
        val user = User(
            id = userId, // any string
            name = "Tutorial", // name and image are used in the UI
            role = "admin",
        )

        // step2 - initialize StreamVideo. For a production app we recommend adding the client to your Application class or di module.
        val client = StreamVideoBuilder(
            context = context,
            apiKey = "mmhfdzb5evj2", // demo API key
            geo = GEO.GlobalEdgeNetwork,
            user = user,
            token = userToken,
            ensureSingleInstance = false,
        ).build()

        // step3 - join a call, which type is `default` and id is `123`.
        call = client.call("livestream", callId)

        // join the call
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

    LaunchedEffect(key1 = participants) {
        Log.e("Test", "participants: $participants")
    }

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
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .background(
                            color = VideoTheme.colors.primaryAccent,
                            shape = RoundedCornerShape(6.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    text = "Live $totalParticipants",
                    color = Color.White,
                )

                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "Live for $duration",
                    color = VideoTheme.colors.textHighEmphasis,
                )
            }
        }
    }
}
