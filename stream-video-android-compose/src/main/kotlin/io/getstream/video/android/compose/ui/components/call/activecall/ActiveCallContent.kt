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
import androidx.compose.ui.unit.dp
import io.getstream.video.android.call.state.CallAction
import io.getstream.video.android.call.state.CallMediaState
import io.getstream.video.android.call.state.LeaveCall
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.CallControls
import io.getstream.video.android.compose.ui.components.call.activecall.internal.ActiveCallAppBar
import io.getstream.video.android.compose.ui.components.call.activecall.internal.AudioDeviceMenu
import io.getstream.video.android.compose.ui.components.participants.CallParticipants
import io.getstream.video.android.compose.ui.components.participants.CallParticipantsInfoMenu
import io.getstream.video.android.viewmodel.CallViewModel

/**
 * Represents the UI in an Active call that shows participants and their video, as well as some
 * extra UI features to control the call settings, browse participants and more.
 *
 * @param callViewModel The ViewModel required to fetch the Call state and render the UI.
 * @param modifier Modifier for styling.
 * @param onCallAction Handler when the user triggers a Call Control Action.
 * @param onParticipantsMenuClick Handler when the user taps on the participant menu.
 */
@Composable
public fun ActiveCallContent(
    callViewModel: CallViewModel,
    modifier: Modifier = Modifier,
    onCallAction: (CallAction) -> Unit = callViewModel::onCallAction,
    onParticipantsMenuClick: () -> Unit = callViewModel::showParticipants
) {
    val room by callViewModel.callState.collectAsState(initial = null)
    val isShowingParticipantsInfo by callViewModel.isShowingParticipantsInfo.collectAsState(
        false
    )

    val isShowingAudioDevicePicker by callViewModel.isShowingAudioDevicePicker.collectAsState(
        false
    )

    val participantsState by callViewModel.participantList.collectAsState(initial = emptyList())

    val callMediaState by callViewModel.callMediaState.collectAsState(initial = CallMediaState())

    BackHandler {
        if (isShowingParticipantsInfo || isShowingAudioDevicePicker) {
            callViewModel.dismissOptions()
        } else {
            onCallAction(LeaveCall)
        }
    }

    Box(
        modifier = modifier, contentAlignment = Alignment.Center
    ) {
        val roomState = room

        Column(modifier = Modifier.fillMaxSize()) {

            ActiveCallAppBar(callViewModel, onParticipantsMenuClick)

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
            }
        }

        if (isShowingParticipantsInfo) {
            CallParticipantsInfoMenu(callViewModel, participantsState)
        }

        if (isShowingAudioDevicePicker) {
            AudioDeviceMenu(callViewModel)
        }
    }
}
