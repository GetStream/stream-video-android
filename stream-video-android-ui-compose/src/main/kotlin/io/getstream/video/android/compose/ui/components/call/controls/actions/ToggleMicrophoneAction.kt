/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.compose.ui.components.call.controls.actions

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.getstream.video.android.compose.R
import io.getstream.video.android.compose.ui.components.base.StreamButtonSize
import io.getstream.video.android.compose.ui.components.base.StreamButtonStyle
import io.getstream.video.android.compose.ui.components.base.StreamButtonStyleDefaults
import io.getstream.video.android.core.call.state.ToggleMicrophone

/**
 * Mutes and unmutes the microphone.
 *
 * @param modifier The modifier applied to the button.
 * @param isMicrophoneEnabled Whether the action is in its active state.
 * @param enabled Whether the action accepts clicks.
 * @param onStyle The colors of the active state. See [StreamButtonStyleDefaults].
 * @param offStyle The colors of the inactive state. See [StreamButtonStyleDefaults].
 * @param size The visual size of the button.
 * @param onCallAction Called with [ToggleMicrophone] when the action is clicked.
 */
@Composable
public fun ToggleMicrophoneAction(
    modifier: Modifier = Modifier,
    isMicrophoneEnabled: Boolean,
    enabled: Boolean = true,
    onStyle: StreamButtonStyle = StreamButtonStyleDefaults.secondarySolid,
    offStyle: StreamButtonStyle = StreamButtonStyleDefaults.destructiveSolid,
    size: StreamButtonSize = StreamButtonSize.Medium,
    onCallAction: (ToggleMicrophone) -> Unit,
): Unit = ToggleAction(
    modifier = modifier,
    isActionActive = isMicrophoneEnabled,
    iconOnOff = Pair(
        painterResource(R.drawable.stream_design_ic_voice_fill),
        painterResource(R.drawable.stream_design_ic_voice_off_fill),
    ),
    contentDescription = stringResource(
        io.getstream.video.android.ui.common.R.string.stream_video_call_controls_toggle_microphone,
    ),
    enabled = enabled,
    onStyle = onStyle,
    offStyle = offStyle,
    size = size,
    onAction = { onCallAction(ToggleMicrophone(isMicrophoneEnabled.not())) },
)
