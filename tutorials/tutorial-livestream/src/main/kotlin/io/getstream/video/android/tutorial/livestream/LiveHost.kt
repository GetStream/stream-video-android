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

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.getstream.log.Priority
import io.getstream.video.android.compose.permission.LaunchCallPermissions
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamButton
import io.getstream.video.android.compose.ui.components.call.controls.actions.FlipCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.LeaveCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleCameraAction
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfigRegistry
import io.getstream.video.android.core.notifications.internal.service.DefaultCallConfigurations
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.User
import io.getstream.video.android.tutorial.livestream.ui.CidInput
import io.getstream.video.android.tutorial.livestream.ui.rememberCidInputState
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime

object HostClient {
    val userId = "Darth_Krayt"
    val user = User(
        id = userId, // any string
        name = "Tutorial", // name and image are used in the UI
        role = "admin",
    )
    val userToken = StreamVideo.devToken(userId)
    var client: StreamVideo? = null

    fun client(context: Context): StreamVideo {
        if (client == null) {
            client = StreamVideoBuilder(
                context = context,
                apiKey = "k436tyde94hj", // demo API key
                geo = GEO.GlobalEdgeNetwork,
                user = user,
                token = userToken,
                ensureSingleInstance = false,
                loggingLevel = LoggingLevel(priority = Priority.VERBOSE),
            ).build()
        }

        return client!!
    }
}

@Composable
fun LiveHost(
    navController: NavController,
    callId: String,
) {
    val callServiceConfigRegistry = CallServiceConfigRegistry()
    callServiceConfigRegistry.register(DefaultCallConfigurations.getLivestreamCallServiceConfig())

    // step2 - initialize StreamVideo. For a production app we recommend adding the client to your Application class or di module.
    val client = HostClient.client(LocalContext.current)
    val cidState = rememberCidInputState(callId)
    val call: MutableState<Call?> = remember { mutableStateOf<Call?>(null) }

    Column {
        Spacer(Modifier.padding(16.dp))
        CidInput(cidState)
        Spacer(Modifier.padding(16.dp))
        StreamButton(
            text = "Schedule",
            onClick = {
                val cid = StreamCallId.fromCallCid(cidState.value.cid)
                call.value = client.call(cid.type, cid.id)
                GlobalScope.launch {
                    call.value?.create(
                        startsAt = OffsetDateTime.now().plusMinutes(1),
                    )
                }
            },
        )
        // Step 2 - join a call, which type is `default` and id is `123`.
        if (call.value != null) {
            val started = remember { mutableStateOf(false) }
            if (started.value.not()) {
                StreamButton(
                    text = "Join and start",
                    onClick = {
                        started.value = true
                        GlobalScope.launch {
                            call.value?.join()
                        }
                    },
                )
            }

            val nonNullCall = call.value!!
            if (started.value) {
                LiveHostContent(navController, nonNullCall)
            }
        }
    }
}

@Composable
private fun LiveHostContent(
    navController: NavController,
    call: Call,
) {
    LaunchCallPermissions(call = call)

    val connection by call.state.connection.collectAsState()
    val totalParticipants by call.state.totalParticipants.collectAsState()
    val backstage by call.state.backstage.collectAsState()
    val localParticipant by call.state.localParticipant.collectAsState()
    val video = localParticipant?.video?.collectAsState()?.value
    val duration by call.state.duration.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(VideoTheme.colors.baseSheetPrimary)
            .padding(6.dp),
        contentColor = VideoTheme.colors.baseSheetPrimary,
        backgroundColor = VideoTheme.colors.baseSheetPrimary,
        topBar = {
            if (connection == RealtimeConnection.Connected) {
                if (!backstage) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .background(
                                    color = VideoTheme.colors.brandPrimary,
                                    shape = RoundedCornerShape(6.dp),
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            text = "Live $totalParticipants",
                            color = Color.White,
                        )

                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = "Live for $duration",
                            color = VideoTheme.colors.basePrimary,
                        )
                    }
                } else {
                    Text(
                        text = "Backstage",
                        color = VideoTheme.colors.basePrimary,
                    )
                }
            } else if (connection is RealtimeConnection.Failed) {
                Text(
                    text = "Connection failed",
                    color = VideoTheme.colors.basePrimary,
                )
            }
        },
        bottomBar = {
            val isCameraEnabled by call.camera.isEnabled.collectAsState()

            Row {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        contentColor = VideoTheme.colors.brandPrimary,
                        backgroundColor = VideoTheme.colors.brandPrimary,
                    ),
                    onClick = {
                        scope.launch {
                            if (backstage) call.goLive() else call.stopLive()
                        }
                    },
                ) {
                    Text(
                        text = if (backstage) "Start Broadcast" else "Stop Broadcast",
                        color = Color.White,
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                ToggleCameraAction(isCameraEnabled = isCameraEnabled) {
                    call.camera.setEnabled(it.isEnabled)
                }
                Spacer(modifier = Modifier.width(16.dp))
                FlipCameraAction {
                    call.camera.flip()
                }
                Spacer(modifier = Modifier.width(16.dp))
                LeaveCallAction {
                    call.leave()
                    navController.popBackStack()
                }
            }
        },
    ) {
        VideoRenderer(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .clip(RoundedCornerShape(6.dp)),
            call = call,
            video = video,
            videoFallbackContent = {
                Text(text = "Video rendering failed")
            },
        )
    }
}
