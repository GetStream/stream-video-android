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

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import io.getstream.video.android.compose.ui.components.call.CallAppBar
import io.getstream.video.android.compose.ui.components.livestream.LivestreamPlayer
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.compose.ui.components.video.VideoScalingType
import io.getstream.video.android.compose.ui.components.video.config.videoRenderConfig
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.internal.service.DefaultCallConfigurations
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.collections.firstOrNull

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
    val call = client.call("livestream", callId)

    LaunchedEffect(Unit) {
        call.microphone.setEnabled(false, fromUser = true)
        call.camera.setEnabled(false, fromUser = true)
        call.join()
    }

    val participants = call.state.participants.collectAsStateWithLifecycle()
    val participantWithVideo = participants.value.firstOrNull {
        it.video.value?.enabled == true
    }
    val livestream = call.state.livestream.collectAsStateWithLifecycle()
    val filteredVideo = participantWithVideo?.video?.collectAsStateWithLifecycle()
    val filteredVideo2 = call.state.participants
        .flatMapLatest { participants ->
            val participant = participants.firstOrNull { it.video.value?.enabled == true }
            participant?.video?.map { it } ?: flowOf(null)
        }.collectAsState(initial = null)

    Log.d("LivestreamDebug", "participantWithVideo: $participantWithVideo")
    Log.d("LivestreamDebug", "livestream: ${livestream.value}")
    Log.d("LivestreamDebug", "filteredVideo: ${filteredVideo?.value}")
    Log.d("LivestreamDebug", "filteredVideo2: ${filteredVideo2.value}")

    val videoRendererConfig = videoRenderConfig {
        videoScalingType = VideoScalingType.SCALE_ASPECT_FIT
        fallbackContent = { FallbackContent() }
    }

    Box {
        Column {
            LivestreamPlayer(
                call = call,
                modifier = Modifier.weight(1f),
            )
            VideoRenderer(
                modifier = Modifier.weight(1f),
                call = call,
                video = filteredVideo?.value,
                videoRendererConfig = videoRendererConfig,
            )
            VideoRenderer(
                modifier = Modifier.weight(1f),
                call = call,
                video = filteredVideo2.value,
                videoRendererConfig = videoRendererConfig,
            )
        }

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

@Composable
fun FallbackContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(40.dp),
            color = Color.White,
        )
    }
}
