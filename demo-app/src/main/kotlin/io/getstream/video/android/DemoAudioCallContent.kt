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

package io.getstream.video.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.styling.StreamFixedSizeButtonStyle
import io.getstream.video.android.compose.ui.components.call.activecall.AudioOnlyCallContent
import io.getstream.video.android.compose.ui.components.call.controls.actions.LeaveCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleSpeakerphoneAction
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.state.CallAction

@Composable
fun DemoAudioCallContent(
    call: Call,
    onCallAction: (CallAction) -> Unit,
    onBackPressed: () -> Unit,
) {
    val micEnabled by call.microphone.isEnabled.collectAsStateWithLifecycle()
    val isSpeakerEnabled by call.speaker.isEnabled.collectAsStateWithLifecycle()

    AudioOnlyCallContent(
        call = call,
        isMicrophoneEnabled = micEnabled,
        onCallAction = onCallAction,
        onBackPressed = onBackPressed,
        controlsContent = {
            AudioOnlyCallControls(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = VideoTheme.dimens.genericXxl),
                isMicrophoneEnabled = micEnabled,
                isSpeakerEnabled = isSpeakerEnabled,
                onCallAction = onCallAction,
            )
        },
    )
}

@Composable
fun AudioOnlyCallControls(
    modifier: Modifier,
    isMicrophoneEnabled: Boolean,
    isSpeakerEnabled: Boolean,
    onCallAction: (CallAction) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ToggleMicrophoneAction(
            modifier = Modifier.testTag(
                "Stream_MicrophoneToggle_Enabled_$isMicrophoneEnabled",
            ),
            isMicrophoneEnabled = isMicrophoneEnabled,
            onCallAction = onCallAction,
            offStyle = VideoTheme.styles.buttonStyles.secondaryIconButtonStyle().fillCircle(
                1.5f,
            ),
            onStyle = VideoTheme.styles.buttonStyles.tertiaryIconButtonStyle().fillCircle(
                1.5f,
            ),
        )

        ToggleSpeakerphoneAction(
            modifier = Modifier.testTag(
                "Stream_SpeakerToggle_Enabled_$isSpeakerEnabled",
            ),
            isSpeakerphoneEnabled = isSpeakerEnabled,
            offStyle = VideoTheme.styles.buttonStyles.secondaryIconButtonStyle().fillCircle(
                1.5f,
            ),
            onStyle = VideoTheme.styles.buttonStyles.tertiaryIconButtonStyle().fillCircle(
                1.5f,
            ),
            onCallAction = onCallAction,
        )

        LeaveCallAction(
            modifier = Modifier.testTag("Stream_HangUpButton"),
            onCallAction = onCallAction,
            style = VideoTheme.styles.buttonStyles.primaryIconButtonStyle().fillCircle(
                1.5f,
            ),
        )
    }
}

internal fun StreamFixedSizeButtonStyle.fillCircle(fraction: Float): StreamFixedSizeButtonStyle {
    return this.copyFixed(
        width * fraction,
        height * fraction,
        shape = CircleShape,
    )
}
