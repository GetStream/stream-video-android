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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.getstream.video.android.call.state.CallAction
import io.getstream.video.android.call.state.CallMediaState
import io.getstream.video.android.call.state.LeaveCall
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.CallAppBar
import io.getstream.video.android.compose.ui.components.call.CallControls
import io.getstream.video.android.compose.ui.components.participants.CallParticipant
import io.getstream.video.android.compose.ui.components.participants.CallParticipants
import io.getstream.video.android.model.state.StreamCallState
import io.getstream.video.android.utils.formatAsTitle
import io.getstream.video.android.viewmodel.CallViewModel

/**
 * Represents the UI in an Active call that shows participants and their video, as well as some
 * extra UI features to control the call settings, browse participants and more.
 *
 * @param callViewModel The ViewModel required to fetch the Call state and render the UI.
 * @param modifier Modifier for styling.
 * @param onBackPressed Handler when the user taps on the back button.
 * @param onCallAction Handler when the user triggers a Call Control Action.
 * @param onCallInfoSelected Handler when the user taps on the participant menu.
 */
@Composable
public fun ActiveCallContent(
    callViewModel: CallViewModel,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = { callViewModel.onCallAction(LeaveCall) },
    onCallAction: (CallAction) -> Unit = callViewModel::onCallAction,
    onCallInfoSelected: () -> Unit = callViewModel::showCallInfo
) {
    val room by callViewModel.callState.collectAsState(initial = null)
    val isShowingParticipantsInfo by callViewModel.isShowingCallInfo.collectAsState(
        false
    )

    val isShowingAudioDevicePicker by callViewModel.isShowingAudioDevicePicker.collectAsState(
        false
    )

    val callMediaState by callViewModel.callMediaState.collectAsState(initial = CallMediaState())

    val isInPiPMode by callViewModel.isInPictureInPicture.collectAsState()

    BackHandler {
        if (isShowingParticipantsInfo || isShowingAudioDevicePicker) {
            callViewModel.dismissOptions()
        } else {
            onBackPressed()
        }
    }

    Box(
        modifier = modifier, contentAlignment = Alignment.Center
    ) {
        val roomState = room

        Column(modifier = Modifier.fillMaxSize()) {

            if (!isInPiPMode) {
                ActiveCallAppBar(
                    callViewModel = callViewModel,
                    onBackPressed = onBackPressed,
                    onCallInfoSelected = onCallInfoSelected
                )
            }

            if (roomState == null) {
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
                if (!isInPiPMode) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CallParticipants(
                            modifier = Modifier.fillMaxSize(), call = roomState
                        )

                        // TODO - colors
                        CallControls(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(VideoTheme.dimens.callControlsSheetHeight),
                            callMediaState = callMediaState,
                            onCallAction = onCallAction
                        )
                    }
                } else {
                    val primarySpeaker by roomState.primarySpeaker.collectAsState(initial = null)
                    val currentPrimary = primarySpeaker

                    if (currentPrimary != null) {
                        CallParticipant(call = roomState, participant = currentPrimary)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ActiveCallAppBar(
    callViewModel: CallViewModel,
    onBackPressed: () -> Unit,
    onCallInfoSelected: () -> Unit
) {
    val callState by callViewModel.streamCallState.collectAsState(initial = StreamCallState.Idle)

    val callId = when (val state = callState) {
        is StreamCallState.Active -> state.callGuid.id
        else -> ""
    }
    val status = callState.formatAsTitle(LocalContext.current)

    val title = when (callId.isBlank()) {
        true -> status
        else -> "$status: $callId"
    }

    CallAppBar(
        title = title,
        onBackPressed = onBackPressed,
        onCallInfoSelected = onCallInfoSelected
    )
}
