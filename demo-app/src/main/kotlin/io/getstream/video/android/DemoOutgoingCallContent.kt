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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.controls.actions.CancelCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleSpeakerphoneAction
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.call.state.CallAction

@Composable
fun DemoOutgoingCallContent(
    modifier: Modifier,
    call: Call,
    isVideoType: Boolean,
    isShowingHeader: Boolean,
    headerContent: @Composable (ColumnScope.() -> Unit)?,
    detailsContent: @Composable (
        ColumnScope.(List<MemberState>, Dp) -> Unit
    )?,
    onBackPressed: () -> Unit,
    onCallAction: (CallAction) -> Unit,
) {
    val micEnabled by call.microphone.isEnabled.collectAsStateWithLifecycle()
    val isSpeakerEnabled by call.speaker.isEnabled.collectAsStateWithLifecycle()
    val isCameraEnabled by call.camera.isEnabled.collectAsStateWithLifecycle()

    io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallContent(
        call = call,
        isVideoType = isVideoType,
        modifier = modifier,
        isShowingHeader = isShowingHeader,
        headerContent = headerContent,
        detailsContent = detailsContent,
        controlsContent = {
            OutgoingCallControls(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = VideoTheme.dimens.genericXxl),
                isVideoType,
                isCameraEnabled,
                micEnabled,
                isSpeakerEnabled,
                onCallAction,
            )
        },
        onBackPressed = onBackPressed,
        onCallAction = onCallAction,
    )
}

@Composable
private fun OutgoingCallControls(
    modifier: Modifier = Modifier,
    isVideoCall: Boolean,
    isCameraEnabled: Boolean,
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
            modifier = Modifier
                .testTag("Stream_MicrophoneToggle_Enabled_$isMicrophoneEnabled"),
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

        if (isVideoCall) {
            ToggleCameraAction(
                modifier = Modifier
                    .testTag("Stream_CameraToggle_Enabled_$isCameraEnabled"),
                offStyle = VideoTheme.styles.buttonStyles.secondaryIconButtonStyle().fillCircle(
                    1.5f,
                ),
                onStyle = VideoTheme.styles.buttonStyles.tertiaryIconButtonStyle().fillCircle(
                    1.5f,
                ),
                isCameraEnabled = isCameraEnabled,
                onCallAction = onCallAction,
            )
        }

        CancelCallAction(
            modifier = Modifier.testTag("Stream_DeclineCallButton"),
            onCallAction = onCallAction,
            style = VideoTheme.styles.buttonStyles.primaryIconButtonStyle().fillCircle(
                1.5f,
            ),
        )
    }
}
