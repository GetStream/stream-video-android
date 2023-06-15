/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.compose.ui.components.audio

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.core.Call
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.mock.mockCall

/**
 * Represents the set of controls the user can use to change their audio and video device state, or
 * browse other types of settings, leave the call, or implement something custom.
 *
 * @param call The call that contains all the participants state and tracks.
 * @param modifier The modifier to be applied to the call controls.
 * @param onLeaveRoom A lambda that will be invoked when the leave quietly button was clicked.
 */
@Composable
public fun AudioControlActions(
    call: Call,
    modifier: Modifier = Modifier,
    onLeaveRoom: (() -> Unit)? = null
) {
    val isMicrophoneEnabled by call.microphone.isEnabled.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? ComponentActivity

    Box(modifier = modifier) {
        Button(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .align(Alignment.CenterStart),
            onClick = {
                onLeaveRoom?.invoke() ?: let {
                    call.leave()
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = VideoTheme.colors.primaryAccent,
                contentColor = VideoTheme.colors.primaryAccent,
            )
        ) {
            Image(
                painterResource(id = io.getstream.video.android.ui.common.R.drawable.stream_video_ic_leave),
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = stringResource(id = io.getstream.video.android.ui.common.R.string.stream_video_audio_leave),
                color = VideoTheme.colors.audioLeaveButton,
                fontSize = 16.sp
            )
        }

        ToggleMicrophoneAction(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(45.dp),
            isMicrophoneEnabled = isMicrophoneEnabled,
            enabledColor = VideoTheme.colors.callActionIconEnabledBackground,
            disabledColor = VideoTheme.colors.callActionIconEnabledBackground,
            disabledIconTint = VideoTheme.colors.errorAccent,
            shape = RoundedCornerShape(8.dp),
            onCallAction = { callAction -> call.microphone.setEnabled(callAction.isEnabled) }
        )
    }
}

@Preview
@Composable
private fun AudioControlActionsPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        AudioControlActions(
            modifier = Modifier.fillMaxSize(),
            call = mockCall
        )
    }
}
