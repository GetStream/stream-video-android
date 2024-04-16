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
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import io.getstream.video.android.compose.permission.LaunchCallPermissions
import io.getstream.video.android.compose.ui.components.livestream.LivestreamPlayer
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.model.User

@Composable
fun LiveAudience() {
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
    val context = LocalContext.current
    val client = StreamVideoBuilder(
        context = context,
        apiKey = "hd8szvscpxvd", // demo API key
        geo = GEO.GlobalEdgeNetwork,
        user = user,
        token = userToken,
        ensureSingleInstance = false,
    ).build()

    // step3 - join a call, which type is `default` and id is `123`.
    val call = client.call("livestream", callId)
    LaunchCallPermissions(call = call) {
        val result = call.join()
        result.onError {
            Toast.makeText(context, "uh oh $it", Toast.LENGTH_SHORT).show()
        }
    }

    LivestreamPlayer(call = call)
}
