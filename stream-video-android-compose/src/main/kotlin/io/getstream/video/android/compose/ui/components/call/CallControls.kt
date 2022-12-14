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

package io.getstream.video.android.compose.ui.components.call

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.getstream.video.android.call.state.CallAction
import io.getstream.video.android.call.state.CallMediaState
import io.getstream.video.android.call.state.FlipCamera
import io.getstream.video.android.call.state.LeaveCall
import io.getstream.video.android.call.state.ToggleCamera
import io.getstream.video.android.call.state.ToggleMicrophone
import io.getstream.video.android.call.state.ToggleSpeakerphone
import io.getstream.video.android.compose.R
import io.getstream.video.android.compose.state.ui.call.CallControlAction
import io.getstream.video.android.compose.theme.VideoTheme

/**
 * Represents the set of controls the user can use to change their audio and video device state, or
 * browse other types of settings, leave the call, or implement something custom.
 *
 * @param callMediaState The state of the media devices for the current user.
 * @param onCallAction Handler when the user triggers an action.
 * @param modifier Modifier for styling.
 */
@Composable
public fun CallControls(
    callMediaState: CallMediaState,
    onCallAction: (CallAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = VideoTheme.shapes.callControls,
        color = VideoTheme.colors.barsBackground,
        elevation = 8.dp
    ) {
        CallControlsActions(
            actions = buildDefaultCallControlActions(callMediaState),
            onCallAction = onCallAction
        )
    }
}

/**
 * Represents the list of Call Control actions the user can trigger while in a call.
 *
 * @param actions The list of actions to render.
 * @param onCallAction Handler when a given action is triggered.
 */
@Composable
public fun CallControlsActions(
    actions: List<CallControlAction>,
    onCallAction: (CallAction) -> Unit
) {
    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items(actions) { action ->
            Card(
                modifier = Modifier.size(VideoTheme.dimens.callControlButtonSize),
                shape = VideoTheme.shapes.callControlsButton,
                backgroundColor = action.actionBackgroundTint
            ) {
                Icon(
                    modifier = Modifier
                        .padding(10.dp)
                        .clickable { onCallAction(action.callAction) },
                    tint = action.iconTint,
                    painter = action.icon,
                    contentDescription = action.description
                )
            }
        }
    }
}

/**
 * Builds the default set of Call Control actions based on the [callMediaState].
 *
 * @param callMediaState Information of whether microphone, speaker and camera are on or off.
 * @return [List] of [CallControlAction]s that the user can trigger.
 */
@Composable
public fun buildDefaultCallControlActions(
    callMediaState: CallMediaState
): List<CallControlAction> {
    val speakerphoneIcon =
        painterResource(
            id = if (callMediaState.isSpeakerphoneEnabled) {
                R.drawable.ic_speaker_on
            } else {
                R.drawable.ic_speaker_off
            }
        )

    val microphoneIcon =
        painterResource(
            id = if (callMediaState.isMicrophoneEnabled) {
                R.drawable.ic_mic_on
            } else {
                R.drawable.ic_mic_off
            }
        )

    val cameraIcon = painterResource(
        id = if (callMediaState.isCameraEnabled) {
            R.drawable.ic_videocam_on
        } else {
            R.drawable.ic_videocam_off
        }
    )

    return listOf(
        CallControlAction(
            actionBackgroundTint = Color.White,
            icon = speakerphoneIcon,
            iconTint = Color.DarkGray,
            callAction = ToggleSpeakerphone(callMediaState.isSpeakerphoneEnabled.not()),
            description = stringResource(R.string.call_controls_toggle_speakerphone)
        ),
        CallControlAction(
            actionBackgroundTint = Color.White,
            icon = cameraIcon,
            iconTint = Color.DarkGray,
            callAction = ToggleCamera(callMediaState.isCameraEnabled.not()),
            description = stringResource(R.string.call_controls_toggle_camera)
        ),
        CallControlAction(
            actionBackgroundTint = Color.White,
            icon = microphoneIcon,
            iconTint = Color.DarkGray,
            callAction = ToggleMicrophone(callMediaState.isMicrophoneEnabled.not()),
            description = stringResource(R.string.call_controls_toggle_microphone)
        ),
        CallControlAction(
            actionBackgroundTint = Color.LightGray,
            icon = painterResource(id = R.drawable.ic_camera_flip),
            iconTint = Color.White,
            callAction = FlipCamera,
            description = stringResource(R.string.call_controls_flip_camera)
        ),
        CallControlAction(
            actionBackgroundTint = VideoTheme.colors.errorAccent,
            icon = painterResource(id = R.drawable.ic_call_end),
            iconTint = Color.White,
            callAction = LeaveCall,
            description = stringResource(R.string.call_controls_leave_call)
        ),
    )
}
