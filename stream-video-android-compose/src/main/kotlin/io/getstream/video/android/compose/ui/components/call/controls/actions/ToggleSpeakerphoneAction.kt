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

package io.getstream.video.android.compose.ui.components.call.controls.actions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.ToggleSpeakerphone
import io.getstream.video.android.ui.common.R

/**
 * A call action button represents toggling a speakerphone.
 *
 * @param modifier Optional Modifier for this action button.
 * @param isSpeakerphoneEnabled Represent is speaker enabled.
 * @param enabled Whether or not this action button will handle input events.
 * @param onCallAction A [CallAction] event that will be fired.
 */
@Composable
public fun ToggleSpeakerphoneAction(
    modifier: Modifier = Modifier,
    isSpeakerphoneEnabled: Boolean,
    enabled: Boolean = true,
    onCallAction: (ToggleSpeakerphone) -> Unit
) {
    val cameraIcon = painterResource(
        id = if (isSpeakerphoneEnabled) {
            R.drawable.stream_video_ic_speaker_on
        } else {
            R.drawable.stream_video_ic_speaker_off
        }
    )

    CallControlActionBackground(
        modifier = modifier,
        isEnabled = isSpeakerphoneEnabled
    ) {
        Icon(
            modifier = Modifier
                .padding(13.dp)
                .clickable(enabled = enabled) {
                    onCallAction(
                        ToggleSpeakerphone(isSpeakerphoneEnabled.not())
                    )
                },
            tint = if (isSpeakerphoneEnabled) {
                VideoTheme.colors.callActionIconEnabled
            } else {
                VideoTheme.colors.callActionIconDisabled
            },
            painter = cameraIcon,
            contentDescription = stringResource(R.string.stream_video_call_controls_toggle_speakerphone)
        )
    }
}
