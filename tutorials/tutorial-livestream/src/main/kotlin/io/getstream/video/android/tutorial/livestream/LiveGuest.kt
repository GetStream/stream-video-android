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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.getstream.video.android.compose.ui.components.call.CallAppBar
import io.getstream.video.android.compose.ui.components.livestream.LivestreamPlayer
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.internal.service.DefaultCallConfigurations

@Composable
fun LiveAudience(
    navController: NavController,
    callId: String,
    client: StreamVideo,
) {
    val context = LocalContext.current

    // Step 1 - Update call settings via callConfigRegistry
    client.state.callConfigRegistry.register(
        DefaultCallConfigurations.getLivestreamGuestCallServiceConfig(),
    )

    // Step 2 - join a call, which type is `default` and id is `123`.
    val call = remember(callId) { client.call("livestream", callId) }

    LaunchedEffect(call) {
        call.microphone.setEnabled(false, fromUser = true)
        call.camera.setEnabled(false, fromUser = true)
        call.join()
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
