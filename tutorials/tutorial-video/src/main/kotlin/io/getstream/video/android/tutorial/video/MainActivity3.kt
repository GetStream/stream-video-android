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

package io.getstream.video.android.tutorial.video

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.common.AbstractCallActivity
import io.getstream.video.android.common.viewmodel.CallViewModel
import io.getstream.video.android.common.viewmodel.CallViewModelFactory
import io.getstream.video.android.compose.permission.LaunchCallPermissions
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.audio.AudioRoom
import io.getstream.video.android.compose.ui.components.call.CallContainer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.model.User
import kotlinx.coroutines.launch

/**
 * This tutorial demonstrates how to implement a video call screen with supporting PIP mode
 * by using higher-level APIs, such as [AbstractCallActivity], and [CallContainer].
 *
 * You can customize [CallContainer] and build your own call screen to your taste.
 *
 * You will be able to build your call screen following the steps below.
 */
class MainActivity3 : AbstractCallActivity() {

    override fun createCall(): Call {
        // step1 - create a user.
        val user = User(
            id = "tutorial@getstream.io", // any string
            name = "Tutorial", // name and image are used in the UI
            role = "admin"
        )

        // step2 - initialize StreamVideo. For a production app we recommend adding the client to your Application class or di module.
        val client = StreamVideoBuilder(
            context = applicationContext,
            apiKey = "hd8szvscpxvd", // demo API key
            geo = GEO.GlobalEdgeNetwork,
            user = user,
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoidHV0b3JpYWxAZ2V0c3RyZWFtLmlvIiwiaXNzIjoicHJvbnRvIiwic3ViIjoidXNlci90dXRvcmlhbEBnZXRzdHJlYW0uaW8iLCJpYXQiOjE2ODY3MDU0MTUsImV4cCI6MTY4NzMxMDIyMH0.YSCQasQnTsM2GFHct_KqW8DYgi88mBerDrB3uQgT3nU",
        ).build()

        return client.call("default", "123")
    }

    // step3 - create a CallViewModel.
    private val factory by lazy { CallViewModelFactory() }
    private val vm by viewModels<CallViewModel> { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // step4 - join a call, which type is `default` and id is `123`.
        lifecycleScope.launch {
            call.join(create = true)
        }

        setContent {
            // step5 - request permissions.
            LaunchCallPermissions(call = call)

            // step6 - apply VideTheme
            VideoTheme {

                // step7 - render videos
                AudioRoom(
                    modifier = Modifier.fillMaxSize().clickable {
                        call.microphone.disable()
                    },
                    call = call
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        call.leave()
    }

    override fun pipChanged(isInPip: Boolean) {
        super.pipChanged(isInPip)
        vm.onPictureInPictureModeChanged(isInPip)
    }
}
