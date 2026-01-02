/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import io.getstream.video.android.compose.permission.LaunchCallPermissions
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.call.state.FlipCamera
import io.getstream.video.android.core.call.state.LeaveCall
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.model.User

/**
 * This tutorial demonstrates how to implement a video call screen by using higher-level APIs, such as [CallContent].
 * You can customize [CallContent] and build your own call screen to your taste.
 *
 * You will be able to build your call screen following the steps below.
 */
class MainActivity2 : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // replace the secrets with the following instruction:
        // https://getstream.io/video/docs/android/playground/demo-credentials/
        val apiKey = "REPLACE_WITH_API_KEY"
        val userToken = "REPLACE_WITH_TOKEN"
        val userId = "REPLACE_WITH_orange-flower-9"
        val callId = "REPLACE_WITH_CALL_ID"

        // step1 - create a user.
        val user = User(
            id = userId, // any string
            name = "Tutorial", // name and image are used in the UI
            image = "https://bit.ly/2TIt8NR",
            role = "admin",
        )

        // step2 - initialize StreamVideo. For a production app we recommend adding the client to your Application class or di module.
        val client = StreamVideoBuilder(
            context = applicationContext,
            apiKey = apiKey, // demo API key
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
                // step5 - render videos
                CallContent(
                    modifier = Modifier.fillMaxSize(),
                    call = call,
                    onCallAction = { callAction ->
                        when (callAction) {
                            is FlipCamera -> call.camera.flip()
                            is ToggleCamera -> call.camera.setEnabled(callAction.isEnabled)
                            is ToggleMicrophone -> call.microphone.setEnabled(callAction.isEnabled)
                            is LeaveCall -> finish()
                            else -> Unit
                        }
                    },
                )
            }
        }
    }
}
