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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import io.getstream.video.android.compose.ui.components.call.CallAppBar
import io.getstream.video.android.compose.ui.components.livestream.LivestreamPlayer
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.compose.ui.components.video.VideoScalingType
import io.getstream.video.android.compose.ui.components.video.config.videoRenderConfig
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.internal.service.DefaultCallConfigurations
import kotlin.collections.firstOrNull

@Composable
fun LiveAudience(
    navController: NavController,
    callId: String,
    client: StreamVideo,
    viewModel: LivestreamViewModel,
) {
    val context = LocalContext.current

    // Step 1 - Update call settings via callConfigRegistry
    client.state.callConfigRegistry.register(
        DefaultCallConfigurations.getLivestreamGuestCallServiceConfig(),
    )

    // Step 2 - join a call, which type is `default` and id is `123`.
    val call = client.call("livestream", callId)

    LaunchedEffect(Unit) {
        if (client.state.activeCall.value == null) {
            call.microphone.setEnabled(false, fromUser = true)
            call.camera.setEnabled(false, fromUser = true)
            call.join()
        }
    }

    val participants by call.state.participants.collectAsStateWithLifecycle()
    val participantWithVideo = participants.firstOrNull {
        it.video.value?.enabled == true
    }
    val livestream by call.state.livestream.collectAsStateWithLifecycle()
    val videoTrack = participantWithVideo?.video?.collectAsStateWithLifecycle()

//    Log.d("LivestreamDebug", "participantWithVideo: $participantWithVideo")
//    Log.d("LivestreamDebug", "livestream: ${livestream.value}")
    Log.d("LivestreamDebugFlows", "filteredVideo: ${videoTrack?.value}")

    val videoRendererConfig = videoRenderConfig {
        videoScalingType = VideoScalingType.SCALE_ASPECT_FIT
        fallbackContent = { FallbackContent() }
    }

    var parentSize: IntSize by remember { mutableStateOf(IntSize(0, 0)) }

    Box(
        modifier = Modifier.onSizeChanged { parentSize = it },
    ) {
        Column {
//            ImprovedFallbackPlayer(
//                call,
//                navController,
//            )
            VideoRenderer(
                modifier = Modifier.weight(1f),
                call = call,
                video = videoTrack?.value,
                videoRendererConfig = videoRendererConfig,
            )
            VideoRenderer(
                modifier = Modifier.weight(1f),
                call = call,
                video = videoTrack?.value,
                videoRendererConfig = videoRendererConfig,
            )
        }

//        participantWithVideo?.let {
//            FloatingParticipantVideo(
//                modifier = Modifier.align(Alignment.TopEnd),
//                call = call,
//                participant = it,
//                parentBounds = parentSize,
//            )
//        }

        CallAppBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(end = 16.dp, top = 16.dp),
            call = call,
            centerContent = { },
            onCallAction = {
                viewModel.setCurrentCallId(null)

                call.leave()
                navController.popBackStack()
            },
            onBackPressed = {
                navController.popBackStack()
            },
        )
    }
}

@Composable
fun ImprovedFallbackPlayer(call: Call, navController: NavController) {
    LivestreamPlayer(
        call = call,
        rendererContent = {
            Column {
                CallAppBar(
                    modifier = Modifier.padding(end = 16.dp, top = 16.dp, bottom = 16.dp),
                    call = call,
                    centerContent = { },
                    onCallAction = {
                        call.leave()
                        navController.popBackStack()
                    },
                    onBackPressed = {
                        navController.popBackStack()
                    },
                )

                val livestream by call.state.livestream.collectAsState()
                val track by remember { derivedStateOf { livestream?.track } }
                val participants by call.state.remoteParticipants.collectAsState()
                var firstLoad by remember { mutableStateOf(true) }
                val connection by call.state.connection.collectAsState()
                val participantVideoEnabled = participants.firstOrNull()?.videoEnabled?.collectAsState()?.value
                val isLoading = connection == RealtimeConnection.PreJoin || connection == RealtimeConnection.InProgress
                val trackEnabled by remember { derivedStateOf { track?.video?.enabled() } }
                val participantVideo = participants.firstOrNull()?.video?.collectAsState()
                val participantVideoTrack = participants.firstOrNull()?.videoTrack?.collectAsState()

                val fallbackContent: @Composable (Call) -> Unit = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.LightGray),
                    )
                }

                val progressIndicator: @Composable BoxScope.() -> Unit = {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White,
                    )
                }

                Column {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, bottom = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text("connection: $connection", color = Color.White)
                        Text(
                            "track: " + if (track == null) "null" else "not null",
                            color = Color.White,
                        )
                        Text(
                            "participantVideoEnabled: $participantVideoEnabled",
                            color = Color.White,
                        )
                        Text("firstLoad: $firstLoad", color = Color.White)
                        // Text("video: ${video?.type}")
                        // Text("video.enabled: ${video?.enabled}")
                        // Text("trackEnabled: $trackEnabled")
                        // Text("participantVideo: ${participantVideo?.value}")
                        // Text("participantVideoTrack: ${participantVideoTrack?.value}")
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (firstLoad) {
                            when {
                                isLoading -> progressIndicator()
                                participantVideoEnabled == false -> firstLoad = false
                                track == null -> progressIndicator()
                                else -> firstLoad = false
                            }
                        } else {
                            if (participantVideoEnabled != true) {
                                fallbackContent(call)
                            } else {
                                Renderer(call, livestream, fallbackContent)
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun Renderer(
    call: Call,
    video: ParticipantState.Media?,
    fallbackContent: @Composable (Call) -> Unit,
) {
    val videoRendererConfig = remember {
        videoRenderConfig {
            this.fallbackContent = fallbackContent
        }
    }
    VideoRenderer(
        call = call,
        video = video,
        videoRendererConfig = videoRendererConfig,
    )
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
