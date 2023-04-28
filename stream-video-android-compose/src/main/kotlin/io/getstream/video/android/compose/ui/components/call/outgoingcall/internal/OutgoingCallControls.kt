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

package io.getstream.video.android.compose.ui.components.call.outgoingcall.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.controls.actions.CancelCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.compose.ui.components.extensions.toggleAlpha
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CallDeviceState

@Composable
internal fun OutgoingCallControls(
    callDeviceState: CallDeviceState,
    modifier: Modifier = Modifier,
    onCallAction: (CallAction) -> Unit,
) {
    val isMicEnabled = callDeviceState.isMicrophoneEnabled
    val isCameraEnabled = callDeviceState.isCameraEnabled

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ToggleMicrophoneAction(
                modifier = Modifier
                    .toggleAlpha(isMicEnabled)
                    .background(
                        color = VideoTheme.colors.appBackground,
                        shape = VideoTheme.shapes.callButton
                    )
                    .size(VideoTheme.dimens.mediumButtonSize),
                isMicrophoneEnabled = isMicEnabled,
                onCallAction = onCallAction,
            )

            ToggleCameraAction(
                modifier = Modifier
                    .toggleAlpha(isCameraEnabled)
                    .background(
                        color = VideoTheme.colors.appBackground,
                        shape = VideoTheme.shapes.callButton
                    )
                    .size(VideoTheme.dimens.mediumButtonSize),
                isCameraEnabled = isCameraEnabled,
                onCallAction = onCallAction
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        CancelCallAction(
            modifier = Modifier.size(VideoTheme.dimens.largeButtonSize),
            onCallAction = onCallAction
        )
    }
}

@Preview
@Composable
private fun OutgoingCallOptionsPreview() {
    VideoTheme {
        Column {
            OutgoingCallControls(
                callDeviceState = CallDeviceState(
                    isMicrophoneEnabled = true,
                    isSpeakerphoneEnabled = true,
                    isCameraEnabled = true
                ),
                onCallAction = { }
            )

            OutgoingCallControls(
                callDeviceState = CallDeviceState(),
                onCallAction = { }
            )
        }
    }
}
