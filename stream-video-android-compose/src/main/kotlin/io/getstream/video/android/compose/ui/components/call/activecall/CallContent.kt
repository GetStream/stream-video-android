/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.compose.ui.components.call.activecall

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import io.getstream.video.android.call.state.CallAction
import io.getstream.video.android.call.state.CallMediaState
import io.getstream.video.android.call.state.LeaveCall
import io.getstream.video.android.compose.ui.components.call.activecall.internal.ActiveCallAppBar
import io.getstream.video.android.compose.ui.components.call.controls.internal.DefaultCallControlsContent
import io.getstream.video.android.compose.ui.components.participants.CallParticipant
import io.getstream.video.android.compose.ui.components.participants.CallParticipants
import io.getstream.video.android.compose.ui.components.participants.internal.ScreenShareAspectRatio
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.state.StreamCallState
import io.getstream.video.android.viewmodel.CallViewModel
import stream.video.sfu.models.TrackType

/**
 * Represents the UI in an Active call that shows participants and their video, as well as some
 * extra UI features to control the call settings, browse participants and more.
 *
 * @param callViewModel The ViewModel required to fetch the Call state and render the UI.
 * @param modifier Modifier for styling.
 * @param onBackPressed Handler when the user taps on the back button.
 * @param onCallAction Handler when the user triggers a Call Control Action.
 * @param callControlsContent Content shown that allows users to trigger different actions.
 * @param pictureInPictureContent Content shown when the user enters Picture in Picture mode, if
 * it's been enabled in the app.
 */
@Composable
public fun CallContent(
    callViewModel: CallViewModel,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = { callViewModel.onCallAction(LeaveCall) },
    onCallAction: (CallAction) -> Unit = callViewModel::onCallAction,
    callControlsContent: @Composable () -> Unit = {
        DefaultCallControlsContent(
            callViewModel,
            onCallAction
        )
    },
    pictureInPictureContent: @Composable (Call) -> Unit = { DefaultPictureInPictureContent(it) }
) {
    val call by callViewModel.callState.collectAsState(initial = null)
    val isShowingParticipantsInfo by callViewModel.isShowingCallInfo.collectAsState(false)

    val callMediaState by callViewModel.callMediaState.collectAsState(initial = CallMediaState())

    val isInPiPMode by callViewModel.isInPictureInPicture.collectAsState()
    val isFullscreen by callViewModel.isFullscreen.collectAsState()
    val orientation = LocalConfiguration.current.orientation
    val callState by callViewModel.streamCallState.collectAsState(StreamCallState.Idle)

    val screenSharingSessions by callViewModel.screenSharingSessions.collectAsState(initial = emptyList())
    val screenSharing = screenSharingSessions.firstOrNull()

    val backAction = {
        if (isShowingParticipantsInfo) {
            callViewModel.dismissOptions()
        } else {
            onBackPressed()
        }
    }

    BackHandler { backAction() }

    val currentCall = call

    if (!isInPiPMode) {
        Scaffold(
            modifier = modifier,
            topBar = {
                if (!isFullscreen && orientation != ORIENTATION_LANDSCAPE) {
                    ActiveCallAppBar(
                        callViewModel = callViewModel,
                        onBackPressed = backAction,
                        onCallAction = onCallAction
                    )
                }
            },
            bottomBar = {
                if (!isFullscreen && orientation != ORIENTATION_LANDSCAPE) {
                    callControlsContent()
                }
            },
            content = {
                if (currentCall == null) {
                    Box(
                        modifier = Modifier
                            .height(250.dp)
                            .fillMaxWidth()
                    ) {
                        Image(
                            modifier = Modifier.align(Alignment.Center),
                            imageVector = Icons.Default.Call,
                            contentDescription = null
                        )
                    }
                } else {
                    CallParticipants(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = it.calculateTopPadding(),
                                start = it.calculateStartPadding(layoutDirection = LocalLayoutDirection.current),
                                end = it.calculateEndPadding(layoutDirection = LocalLayoutDirection.current),
                            ),
                        call = currentCall,
                        paddingValues = it,
                        isFullscreen = isFullscreen,
                        callMediaState = callMediaState,
                        callState = callState,
                        onCallAction = onCallAction,
                        onBackPressed = onBackPressed,
                        callControlsContent = callControlsContent,
                        screenSharing = screenSharing
                    )
                }
            }
        )
    } else {
        if (currentCall != null) {
            pictureInPictureContent(currentCall)
        }
    }
}

/**
 * Renders the default PiP content, using the call state that's provided.
 *
 * @param call The state of the call, with its participants.
 */
@Composable
internal fun DefaultPictureInPictureContent(call: Call) {
    val screenSharingSessions by call.screenSharingSessions.collectAsState(initial = emptyList())

    val currentSessions = screenSharingSessions

    if (currentSessions.isNotEmpty()) {
        val session = currentSessions.first()

        VideoRenderer(
            modifier = Modifier.aspectRatio(ScreenShareAspectRatio, false),
            call = call,
            videoTrack = session.track,
            trackType = TrackType.TRACK_TYPE_SCREEN_SHARE,
            sessionId = session.participant.sessionId
        )
    } else {
        val primarySpeaker by call.primarySpeaker.collectAsState(initial = null)
        val currentPrimary = primarySpeaker

        if (currentPrimary != null) {
            CallParticipant(
                call = call,
                participant = currentPrimary,
                labelPosition = Alignment.BottomStart
            )
        }
    }
}
