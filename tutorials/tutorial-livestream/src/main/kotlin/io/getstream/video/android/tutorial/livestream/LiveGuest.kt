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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.getstream.log.Priority
import io.getstream.video.android.compose.permission.LaunchCallPermissions
import io.getstream.video.android.compose.ui.components.call.CallAppBar
import io.getstream.video.android.compose.ui.components.livestream.LivestreamPlayer
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.notifications.internal.service.livestreamGuestCallServiceConfig
import io.getstream.video.android.model.User

@Composable
fun LiveAudience(
    navController: NavController,
    callId: String,
) {
    val context = LocalContext.current
    val userId = "Ben_Skywalker"
    val userToken = StreamVideo.devToken(userId)

    // step1 - create a user.
    val user = User(
        id = userId, // any string
        name = "Tutorial", // name and image are used in the UI
        role = "user",
    )

    // step2 - initialize StreamVideo. For a production app we recommend adding the client to your Application class or di module.
    val client = StreamVideoBuilder(
        context = context,
        apiKey = "k436tyde94hj", // demo API key
        geo = GEO.GlobalEdgeNetwork,
        user = user,
        token = userToken,
        callServiceConfig = livestreamGuestCallServiceConfig(),
        ensureSingleInstance = false,
        loggingLevel = LoggingLevel(priority = Priority.VERBOSE),
    ).build()

    // step3 - join a call, which type is `default` and id is `123`.
    val call = client.call("livestream", callId)
    LaunchCallPermissions(call = call) {
        val result = call.join()
        result.onError {
            Toast.makeText(context, "uh oh $it", Toast.LENGTH_SHORT).show()
        }
    }

    Box {
        LivestreamPlayer(call = call)
        CallAppBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(end = 16.dp, top = 16.dp),
            call = call,
            centerContent = { },
            onCallAction = {
                call.leave()
                navController.popBackStack()
            },
        )
    }
}
