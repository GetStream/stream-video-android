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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.activecall.internal.ActiveCallAppBar
import io.getstream.video.android.compose.ui.components.call.controls.internal.DefaultCallControlsContent
import io.getstream.video.android.compose.ui.components.participants.CallParticipant
import io.getstream.video.android.compose.ui.components.participants.CallParticipants
import io.getstream.video.android.compose.ui.components.participants.internal.ScreenShareAspectRatio
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CallMediaState
import io.getstream.video.android.core.call.state.LeaveCall
import io.getstream.video.android.core.model.ScreenSharingSession
import io.getstream.video.android.core.model.state.StreamCallState
import io.getstream.video.android.core.viewmodel.CallViewModel
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
    //onBackPressed: () -> Unit = { callViewModel.onCallAction(LeaveCall) },
    //onCallAction: (CallAction) -> Unit = callViewModel::onCallAction,
    callControlsContent: @Composable () -> Unit = {
//        DefaultCallControlsContent(
//            callViewModel,
//            //onCallAction
//        )
    },
    pictureInPictureContent: @Composable (Call) -> Unit = { DefaultPictureInPictureContent(it) }
) {
    //val call by callViewModel.callState.collectAsState(initial = null)
    val isShowingCallInfo by callViewModel.isShowingCallInfo.collectAsState(false)

    //val callMediaState by callViewModel.callMediaState.collectAsState(initial = CallMediaState())

    val isInPiPMode by callViewModel.isInPictureInPicture.collectAsState()
    val isFullscreen by callViewModel.isFullscreen.collectAsState()
    //val callState by callViewModel.streamCallState.collectAsState(StreamCallState.Idle)

    val screenSharingSessions = callViewModel.call.state.screenSharingSession.collectAsState()
    val screenSharing = screenSharingSessions.value

    val backAction = {
        if (isShowingCallInfo) {
           // callViewModel.dismissOptions()
        } else {
           // onBackPressed()
        }
    }

    BackHandler { backAction() }

//    CallContent(
//        modifier = modifier,
//        call = call,
//        callState = callState,
//        callMediaState = callMediaState,
//        isShowingCallInfo = isShowingCallInfo,
//        isInPictureInPicture = isInPiPMode,
//        isFullscreen = isFullscreen,
//        screenSharing = screenSharing,
//        onBackPressed = onBackPressed,
//        onCallAction = onCallAction,
//        callControlsContent = callControlsContent,
//        pictureInPictureContent = pictureInPictureContent
//    )
}

/**
 * Represents the UI in an Active call that shows participants and their video, as well as some
 * extra UI features to control the call settings, browse participants and more.
 *
 * @param call The state of the call with its participants.
 * @param callState StreamCallEngine state representing in which state of the call the user is.
 * @param callMediaState Media state of the call, for audio and video.
 * @param modifier Modifier for styling.
 * @param isShowingCallInfo If the call info menu is being shown.
 * @param isInPictureInPicture If the user has engaged in Picture-In-Picture mode.
 * @param isFullscreen If the user is in fullscreen.
 * @param screenSharing The active screen sharing session, if it exists.
 * @param onBackPressed Handler when the user taps on the back button.
 * @param onCallAction Handler when the user triggers a Call Control Action.
 * @param callControlsContent Content shown that allows users to trigger different actions.
 * @param pictureInPictureContent Content shown when the user enters Picture in Picture mode, if
 * it's been enabled in the app.
 */
@Composable
public fun CallContent(
    call: Call?,
    callState: StreamCallState,
    callMediaState: CallMediaState,
    modifier: Modifier = Modifier,
    isShowingCallInfo: Boolean = false,
    isInPictureInPicture: Boolean = false,
    isFullscreen: Boolean = false,
    screenSharing: ScreenSharingSession? = null,
    onBackPressed: () -> Unit = { },
    onCallAction: (CallAction) -> Unit = { },
    callControlsContent: @Composable () -> Unit = {
        DefaultCallControlsContent(
            call,
            callMediaState,
            onCallAction
        )
    },
    pictureInPictureContent: @Composable (Call) -> Unit = { DefaultPictureInPictureContent(it) }
) {
    val orientation = LocalConfiguration.current.orientation

    if (!isInPictureInPicture) {
        Scaffold(
            modifier = modifier,
            contentColor = VideoTheme.colors.appBackground,
            topBar = {
                if (!isFullscreen && orientation != ORIENTATION_LANDSCAPE) {
                    ActiveCallAppBar(
                        callState = callState,
                        isShowingCallInfo = isShowingCallInfo,
                        onBackPressed = onBackPressed,
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
                val paddings = PaddingValues(
                    top = it.calculateTopPadding(),
                    start = it.calculateStartPadding(layoutDirection = LocalLayoutDirection.current),
                    end = it.calculateEndPadding(layoutDirection = LocalLayoutDirection.current),
                    bottom = (it.calculateBottomPadding() - VideoTheme.dimens.callControllerBottomPadding)
                        .coerceAtLeast(0.dp)
                )

                if (call == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddings)
                            .background(VideoTheme.colors.appBackground)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = VideoTheme.colors.primaryAccent
                        )
                    }
                } else {
                    CallParticipants(
                        modifier = Modifier.fillMaxSize(),
                        call = call,
                        paddingValues = paddings,
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
        if (call != null) {
            pictureInPictureContent(call)
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
//    val screenSharingSessions by call.state.screenSharingSession.collectAsState(initial = emptyList())
//
//    val currentSessions = screenSharingSessions

//    if (currentSessions.isNotEmpty()) {
//        val session = currentSessions.first()
//
//        VideoRenderer(
//            modifier = Modifier.aspectRatio(ScreenShareAspectRatio, false),
//            call = call,
//            videoTrackWrapper = session.track,
//            trackType = TrackType.TRACK_TYPE_SCREEN_SHARE,
//            sessionId = session.participant.sessionId
//        )
//    } else {
//        val primarySpeaker by call.primarySpeaker.collectAsState(initial = null)
//        val currentPrimary = primarySpeaker
//
//        if (currentPrimary != null) {
//            CallParticipant(
//                call = call,
//                participant = currentPrimary,
//                labelPosition = Alignment.BottomStart
//            )
//        }
//    }
}
