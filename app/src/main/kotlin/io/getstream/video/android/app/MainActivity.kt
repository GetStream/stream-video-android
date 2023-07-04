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

package io.getstream.video.android.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import io.getstream.log.Priority
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.dogfooding.ui.call.CallScreen
import io.getstream.video.android.model.User
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // step 1 - create a user
        val user = User(
            id = "jaewoong",
            name = "Jaewoong",
            role = "admin"
        )

        // step 2 - initialize StreamVideo
        val streamVideo = videoApp.initializeStreamVideo(
            user = user,
            apiKey = VideoApp.API_KEY,
            loggingLevel = LoggingLevel(priority = Priority.DEBUG)
        )

        // step 3 - join a call
        val call = streamVideo.call(type = "default", id = "1egoN4tKm4w2")
        lifecycleScope.launch { call.join(create = true) }

        // step 4 - build a call screen
        setContent {
            CallScreen(
                call = call,
                onLeaveCall = {
                    call.leave()
                    finish()
                }
            )
        }
    }
}
