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

package io.getstream.video.android.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.getstream.result.Result
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.model.User

class MainActivity : ComponentActivity() {

    private val vm by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // step 1 - create a user
        val user = User(
            id = "jaewoong",
            name = "Jaewoong",
            role = "admin"
        )

        // step 2 - join a call
        vm.joinCall(context = this, user = user)

        // step 3 - build a call screen
        setContent {
            val call by vm.call.collectAsState()
            val result by vm.result.collectAsState()

            MainScreen(call, result) {
                finish()
            }
        }
    }
}

@Composable
private fun MainScreen(
    call: Call?,
    result: Result<RtcSession>?,
    onLeaveCall: () -> Unit
) {
    VideoTheme {
        if (call != null) {
            CallScreen(
                call = call,
                onLeaveCall = {
                    call.leave()
                    onLeaveCall.invoke()
                }
            )
        } else if (result is Result.Failure) {
            Text(text = result.value.message)
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = VideoTheme.colors.primaryAccent
                )
            }
        }
    }
}
