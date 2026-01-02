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

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme

/**
 * Used to indicate the sound state of a given participant. Either shows a mute icon or the sound
 * levels.
 *
 * @param modifier Modifier for styling.
 * @param isSpeaking Represents is user speaking or not.
 * @param isAudioEnabled Represents is audio enabled or not.
 * @param audioLevel Indicates the audio level that will be drawn.
 */
@Composable
public fun SoundIndicator(
    modifier: Modifier = Modifier,
    isSpeaking: Boolean,
    isAudioEnabled: Boolean,
    audioLevel: Float,
) {
    GenericIndicator(modifier = modifier) {
        if (isAudioEnabled && isSpeaking) {
            AudioVolumeIndicator(
                modifier = Modifier.align(Alignment.Center),
                audioLevels = audioLevel,
            )
        } else {
            MicrophoneIndicator(isMicrophoneEnabled = isAudioEnabled)
        }
    }
}

@Preview
@Composable
private fun SoundIndicatorPreview() {
    VideoTheme {
        Column {
            SoundIndicator(
                isSpeaking = true,
                isAudioEnabled = true,
                audioLevel = 0f,
            )
            SoundIndicator(
                isSpeaking = false,
                isAudioEnabled = false,
                audioLevel = 0.5f,
            )
            SoundIndicator(
                isSpeaking = false,
                isAudioEnabled = true,
                audioLevel = 0.5f,
            )
        }
    }
}
