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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme

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
            .size(VideoTheme.dimens.genericM),
    ) {
        if (isMicrophoneEnabled) {
            Icon(
                modifier = Modifier.align(Alignment.Center),
                imageVector = Icons.Default.Mic,
                tint = VideoTheme.colors.basePrimary,
                contentDescription = Icons.Default.Mic.name,
            )
        } else {
            Icon(
                modifier = Modifier.align(Alignment.Center),
                imageVector = Icons.Default.MicOff,
                tint = VideoTheme.colors.basePrimary,
                contentDescription = Icons.Default.MicOff.name,
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
