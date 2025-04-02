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

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import io.getstream.android.video.generated.models.CallLiveStartedEvent
import io.getstream.log.Priority
import io.getstream.video.android.compose.ui.components.call.CallAppBar
import io.getstream.video.android.compose.ui.components.livestream.LivestreamPlayer
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.User
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object GuestClient {
    val userId = "Darth_Client"
    val user = User(
        id = userId, // any string
        name = "Tutorial", // name and image are used in the UI
        role = "admin",
    )
    val userToken = StreamVideo.devToken(userId)
    var client: StreamVideo? = null
    fun client(context: Context): StreamVideo {
        if (client == null) {
            client = StreamVideoBuilder(
                context = context,
                apiKey = "k436tyde94hj", // demo API key
                geo = GEO.GlobalEdgeNetwork,
                user = user,
                token = userToken,
                ensureSingleInstance = false,
                loggingLevel = LoggingLevel(priority = Priority.VERBOSE),
            ).build()
        }

        return client!!
    }
}

@Composable
fun LiveAudience(
    navController: NavController,
    callId: String,
) {
    // step2 - initialize StreamVideo. For a production app we recommend adding the client to your Application class or di module.
    val client = GuestClient.client(LocalContext.current)
    // Step 2 - join a call, which type is `default` and id is `123`.
    val cid = StreamCallId.fromCallCid(callId)
    val call = client.call(cid.type, cid.id)

    LaunchedEffect(call) {
        call.get()
        delay(3000)
        call.join()

        GlobalScope.launch {
            call.subscribe {
                if (it is CallLiveStartedEvent) {
                    GlobalScope.launch {
                        call.join()
                    }
                }
            }
        }
    }

    Column {
        val live = call.state.live.collectAsStateWithLifecycle()
        val startedAt = call.state.startedAt.collectAsStateWithLifecycle()
        val startsAt = call.state.startsAt.collectAsStateWithLifecycle()
        val backstage = call.state.backstage.collectAsStateWithLifecycle()
        val session = call.state.session.collectAsStateWithLifecycle()
        Log.d(
            "LiveAudience",
            "Session: ${session.value}",
        )
        Log.d(
            "LiveAudience",
            "Backstage: ${backstage.value}, Live: ${live.value}, Started at: ${startedAt.value}, Starts at: ${startsAt.value}",
        )
        Text(
            text = "Backstage: ${backstage.value}, Live: ${live.value}, Started at: ${startedAt.value}, Starts at: ${startsAt.value}",
        )
        CallAppBar(
            modifier = Modifier
                .padding(end = 16.dp, top = 16.dp),
            call = call,
            centerContent = { },
            onCallAction = {
                call.leave()
                navController.popBackStack()
            },
        )
        LivestreamPlayer(call = call)
    }
}
