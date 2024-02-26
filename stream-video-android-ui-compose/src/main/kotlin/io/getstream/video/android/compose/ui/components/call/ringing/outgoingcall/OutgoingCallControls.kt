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

package io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.base.VideoTheme
import io.getstream.video.android.compose.ui.components.call.controls.actions.CancelCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.core.call.state.CallAction

/**
 * A list of call control action buttons that allows people to accept or cancel a call.
 *
 * @param modifier Modifier for styling.
 * @param isCameraEnabled Represents is camera enabled or not.
 * @param isMicrophoneEnabled Represents is microphone enabled or not.
 * @param onCallAction Handler used when the user interacts with Call UI.
 */
@Composable
public fun OutgoingCallControls(
    isCameraEnabled: Boolean,
    isMicrophoneEnabled: Boolean,
    modifier: Modifier = Modifier,
    onCallAction: (CallAction) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ToggleMicrophoneAction(
                isMicrophoneEnabled = isMicrophoneEnabled,
                onCallAction = onCallAction,
            )

            ToggleCameraAction(
                offStyle = VideoTheme.styles.buttonStyles.secondaryIconButtonStyle(),
                onStyle = VideoTheme.styles.buttonStyles.tetriaryIconButtonStyle(),
                isCameraEnabled = isCameraEnabled,
                onCallAction = onCallAction,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        CancelCallAction(
            onCallAction = onCallAction,
        )
    }
}

@Preview
@Composable
private fun OutgoingCallOptionsPreview() {
    VideoTheme {
        Column {
            OutgoingCallControls(
                isMicrophoneEnabled = true,
                isCameraEnabled = true,
                onCallAction = { },
            )

            OutgoingCallControls(
                isMicrophoneEnabled = false,
                isCameraEnabled = false,
                onCallAction = { },
            )
        }
    }
}
