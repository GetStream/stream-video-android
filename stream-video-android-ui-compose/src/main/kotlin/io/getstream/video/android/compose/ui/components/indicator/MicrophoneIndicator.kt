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

package io.getstream.video.android.compose.ui.components.indicator

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.getstream.video.android.compose.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.theme.design.StreamTokens

/**
 * Used to indicate the microphone state of a given participant.
 *
 * @param modifier Modifier for styling.
 * @param isMicrophoneEnabled Represents is audio enabled or not.
 */
@Composable
public fun MicrophoneIndicator(
    modifier: Modifier = Modifier,
    isMicrophoneEnabled: Boolean,
) {
    Box(
        modifier = modifier
            .size(StreamTokens.size16),
    ) {
        if (isMicrophoneEnabled) {
            Icon(
                modifier = Modifier.align(Alignment.Center),
                painter = painterResource(R.drawable.stream_design_ic_voice_fill),
                tint = VideoTheme.colors.textOnAccent,
                contentDescription = stringResource(
                    io.getstream.video.android.ui.common.R.string.stream_video_call_participants_info_options_mute,
                ),
            )
        } else {
            Icon(
                modifier = Modifier.align(Alignment.Center),
                painter = painterResource(R.drawable.stream_design_ic_voice_off_fill),
                tint = VideoTheme.colors.textOnAccent,
                contentDescription = stringResource(
                    io.getstream.video.android.ui.common.R.string.stream_video_call_participants_info_options_unmute,
                ),
            )
        }
    }
}
