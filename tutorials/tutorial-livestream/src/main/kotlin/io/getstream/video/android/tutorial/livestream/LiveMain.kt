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
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamTextField
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.compose.ui.components.video.VideoScalingType
import io.getstream.video.android.compose.ui.components.video.config.videoRenderConfig
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.model.QueriedCalls
import io.getstream.webrtc.android.ui.VideoTextureViewRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LiveMain(
    navController: NavHostController,
    viewModel: LivestreamViewModel,
) {
    val currentCallId by viewModel.currentCallId.collectAsStateWithLifecycle()
    val client = StreamVideo.instance()

    // Use one of these
    val isInViewerScreenFromFlow by viewModel.isInViewerScreen.collectAsStateWithLifecycle()
    val isInViewerScreenFromState by viewModel.isInViewerScreenState

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VideoTheme.colors.baseSheetPrimary),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            var callId by remember { mutableStateOf(TextFieldValue("room188")) }
            val scope = rememberCoroutineScope()
            val context = LocalContext.current

            StreamTextField(
                modifier = Modifier.width(300.dp),
                value = callId,
                placeholder = "Call Id (required)",
                onValueChange = {
                    callId = it
                },
            )

            Spacer(modifier = Modifier.height(44.dp))

            Button(
                modifier = Modifier
                    .width(300.dp)
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    contentColor = VideoTheme.colors.brandPrimary,
                    backgroundColor = VideoTheme.colors.brandPrimary,
                ),
                onClick = {
                    navController.navigate(LiveScreens.Host.destination(callId.text))
                },
            ) {
                Text(text = "host", color = VideoTheme.colors.basePrimary)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                modifier = Modifier
                    .width(300.dp)
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    contentColor = VideoTheme.colors.brandPrimary,
                    backgroundColor = VideoTheme.colors.brandPrimary,
                ),
                onClick = {
//                    viewModel.setInViewerScreen(true)
//                    viewModel.isInViewerScreenBool = true
//                    viewModel.isInViewerScreenState.value = true
                    navController.navigate(LiveScreens.Guest.destination(callId.text))
                },
            ) {
                Text(text = "guest", color = VideoTheme.colors.basePrimary)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                modifier = Modifier
                    .width(300.dp)
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    contentColor = VideoTheme.colors.brandPrimary,
                    backgroundColor = VideoTheme.colors.brandPrimary,
                ),
                onClick = {
                    scope.launch {
                        val cid = withContext(Dispatchers.IO) {
                            getOngoingLives(StreamVideo.instance())
                        }
                        callId = callId.copy(text = cid)

                        val toastMessage = if (cid.isEmpty()) {
                            "No ongoing livestreams"
                        } else {
                            "Found $cid"
                        }
                        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
                    }
                },
            ) {
                Text(text = "query ongoing live", color = VideoTheme.colors.basePrimary)
            }
        }

        if (currentCallId != null && !isInViewerScreenFromState) {
            MiniPlayer(
                navController = navController,
                callId = currentCallId!!,
                client = client,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(200.dp),
            )
        }
    }
}

@Composable
fun MiniPlayer(
    navController: NavHostController,
    callId: String,
    client: StreamVideo,
    modifier: Modifier = Modifier,
) {
    val call = client.call("livestream", callId)
    val participants by call.state.participants.collectAsStateWithLifecycle()
    val participantWithVideo = participants.firstOrNull {
        it.video.value?.enabled == true
    }
    val videoTrack = participantWithVideo?.video?.collectAsStateWithLifecycle()

    var rendererView by remember { mutableStateOf<VideoTextureViewRenderer?>(null) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                navController.navigate(LiveScreens.Guest.destination(callId))
            },
    ) {
        VideoRenderer(
            call = call,
            video = videoTrack?.value,
            videoRendererConfig = videoRenderConfig {
                videoScalingType = VideoScalingType.SCALE_ASPECT_FIT
                fallbackContent = { FallbackContent() }
            },
            onRendered = {
                rendererView = it
                Log.d("LivestreamDebugMiniPlayer", "MiniPlayer rendererView created: $it")
            },
        )
    }
}

private suspend fun getOngoingLives(client: StreamVideo): String {
    val filters = mutableMapOf(
        "type" to "livestream",
        "ongoing" to true,
//        "ended_at" to mutableMapOf("\$gt" to "2025-03-20T07:34:57.022975Z"),
    )
    var returnedId = ""

    client.queryCalls(filters).let { result ->
        result
            .onSuccess { calls: QueriedCalls ->
                Log.d("LivestreamDebug", "Query success: $calls")

//                returnedCid = calls.calls.fold("") { acc, callData -> acc + callData.call.id }
                returnedId = calls.calls.firstOrNull()?.call?.id ?: ""
            }
            .onError { error -> Log.d("LivestreamDebug", "Query error: $error") }
    }

    return returnedId
}
