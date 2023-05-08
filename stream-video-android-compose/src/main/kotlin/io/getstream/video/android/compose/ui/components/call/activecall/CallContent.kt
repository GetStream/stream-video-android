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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import io.getstream.video.android.compose.ui.components.call.CallAppBar
import io.getstream.video.android.compose.ui.components.call.controls.CallControls
import io.getstream.video.android.compose.ui.components.call.renderer.CallSingleVideoRenderer
import io.getstream.video.android.compose.ui.components.call.renderer.CallVideoRenderer
import io.getstream.video.android.compose.ui.components.call.renderer.internal.ScreenShareAspectRatio
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CallDeviceState
import io.getstream.video.android.core.call.state.LeaveCall
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
    onBackPressed: () -> Unit = { callViewModel.onCallAction(LeaveCall) },
    onCallAction: (CallAction) -> Unit = callViewModel::onCallAction,
    callAppBar: @Composable () -> Unit = {
        CallAppBar(
            call = callViewModel.call,
            leadingContent = null,
            onCallAction = onCallAction
        )
    },
    callControlsContent: @Composable (call: Call) -> Unit = {
        CallControls(
            callViewModel = callViewModel,
            onCallAction = onCallAction
        )
    },
    pictureInPictureContent: @Composable (Call) -> Unit = { DefaultPictureInPictureContent(it) }
) {
    val callDeviceState by callViewModel.callDeviceState.collectAsState(initial = CallDeviceState())

    val isInPiPMode by callViewModel.isInPictureInPicture.collectAsState()
    val isShowingCallInfo by callViewModel.isShowingCallInfoMenu.collectAsState(false)

    val backAction = {
        if (isShowingCallInfo) {
            callViewModel.dismissCallInfoMenu()
        } else {
            onBackPressed()
        }
    }

    BackHandler { backAction() }

    CallContent(
        modifier = modifier,
        call = callViewModel.call,
        callDeviceState = callDeviceState,
        isInPictureInPicture = isInPiPMode,
        onCallAction = onCallAction,
        callAppBar = callAppBar,
        callControlsContent = callControlsContent,
        pictureInPictureContent = pictureInPictureContent
    )
}

/**
 * Represents the UI in an Active call that shows participants and their video, as well as some
 * extra UI features to control the call settings, browse participants and more.
 *
 * @param call The state of the call with its participants.
 * @param callDeviceState Media state of the call, for audio and video.
 * @param modifier Modifier for styling.
 * @param isInPictureInPicture If the user has engaged in Picture-In-Picture mode.
 * @param onBackPressed Handler when the user taps on the back button.
 * @param onCallAction Handler when the user triggers a Call Control Action.
 * @param callControlsContent Content shown that allows users to trigger different actions.
 * @param pictureInPictureContent Content shown when the user enters Picture in Picture mode, if
 * it's been enabled in the app.
 */
@Composable
public fun CallContent(
    call: Call,
    modifier: Modifier = Modifier,
    callDeviceState: CallDeviceState,
    isShowingOverlayCallAppBar: Boolean = true,
    isInPictureInPicture: Boolean = false,
    onCallAction: (CallAction) -> Unit = {},
    callAppBar: @Composable () -> Unit = {
        CallAppBar(
            call = call,
            leadingContent = null,
            onCallAction = onCallAction
        )
    },
    callControlsContent: @Composable (call: Call) -> Unit = {
        CallControls(
            callDeviceState = callDeviceState,
            onCallAction = onCallAction
        )
    },
    pictureInPictureContent: @Composable (Call) -> Unit = { DefaultPictureInPictureContent(it) }
) {
    val orientation = LocalConfiguration.current.orientation

    if (!isInPictureInPicture) {
        Scaffold(
            modifier = modifier,
            contentColor = VideoTheme.colors.appBackground,
            bottomBar = {
                if (orientation != ORIENTATION_LANDSCAPE) {
                    callControlsContent.invoke(call)
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

                Row(
                    modifier = modifier
                        .background(color = VideoTheme.colors.appBackground)
                        .padding(paddings)
                ) {
                    CallVideoRenderer(
                        call = call,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                    )

                    if (orientation == ORIENTATION_LANDSCAPE) {
                        callControlsContent.invoke(call)
                    }
                }

                if (isShowingOverlayCallAppBar) {
                    callAppBar.invoke()
                }
            }
        )
    } else {
        pictureInPictureContent(call)
    }
}

/**
 * Renders the default PiP content, using the call state that's provided.
 *
 * @param call The state of the call, with its participants.
 */
@Composable
internal fun DefaultPictureInPictureContent(call: Call) {
    val screenSharingSession by call.state.screenSharingSession.collectAsState(initial = null)
    val session = screenSharingSession
    if (session != null) {
        VideoRenderer(
            modifier = Modifier.aspectRatio(ScreenShareAspectRatio, false),
            call = call,
            mediaTrack = session.track,
            trackType = TrackType.TRACK_TYPE_SCREEN_SHARE,
            sessionId = session.participant.sessionId
        )
    } else {
        val activeSpeakers by call.state.activeSpeakers.collectAsState(initial = emptyList())
        val me by call.state.me.collectAsState()

        if (activeSpeakers.isNotEmpty()) {
            CallSingleVideoRenderer(
                call = call,
                participant = activeSpeakers.first(),
                labelPosition = Alignment.BottomStart
            )
        } else if (me != null) {
            CallSingleVideoRenderer(
                call = call,
                participant = me!!,
                labelPosition = Alignment.BottomStart
            )
        }
    }
}
