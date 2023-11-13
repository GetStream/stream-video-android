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

package io.getstream.video.android.compose.ui.components.indicator

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.ui.common.R

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
            .size(VideoTheme.dimens.microphoneIndicatorSize)
            .padding(VideoTheme.dimens.microphoneIndicatorPadding),
    ) {
        if (isMicrophoneEnabled) {
            Icon(
                modifier = Modifier.align(Alignment.Center),
                painter = painterResource(id = R.drawable.stream_video_ic_mic_on),
                tint = Color.White,
                contentDescription = "microphone enabled",
            )
        } else {
            Icon(
                modifier = Modifier.align(Alignment.Center),
                painter = painterResource(id = R.drawable.stream_video_ic_mic_off),
                tint = Color.White,
                contentDescription = "microphone disabled",
            )
        }
    }
}

@Preview
@Composable
private fun SoundIndicatorPreview() {
    VideoTheme {
        Row {
            MicrophoneIndicator(isMicrophoneEnabled = true)
            MicrophoneIndicator(isMicrophoneEnabled = false)
        }
    }
}
