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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.ui.common.R

/**
 * Used to indicate the sound state of a given participant. Either shows a mute icon or the sound
 * levels.
 *
 * @param modifier Modifier for styling.
 * @param isSpeaking Represents is user speaking or not.
 * @param isAudioEnabled Represents is audio enabled or not.
 * @param audioLevels Indicates the audio levels that will be drawn. This list must contains thee float values (0 ~ 1f).
 */
@Composable
public fun SoundIndicator(
    modifier: Modifier = Modifier,
    isSpeaking: Boolean,
    isAudioEnabled: Boolean,
    audioLevels: List<Float>,
) {
    if (isSpeaking && isAudioEnabled) {
        AudioVolumeIndicator(
            modifier = modifier.padding(end = VideoTheme.dimens.audioLevelIndicatorBarPadding),
            audioLevels = audioLevels
        )
    } else if (isAudioEnabled) {
        AudioVolumeIndicator(
            modifier = modifier.padding(end = VideoTheme.dimens.audioLevelIndicatorBarPadding),
            audioLevels = listOf(0f, 0f, 0f, 0f, 0f)
        )
    } else {
        Icon(
            modifier = modifier
                .size(VideoTheme.dimens.microphoneIndicatorSize)
                .padding(end = VideoTheme.dimens.microphoneIndicatorPadding),
            painter = painterResource(id = R.drawable.stream_video_ic_mic_off),
            tint = VideoTheme.colors.errorAccent,
            contentDescription = null
        )
    }
}

@Preview
@Composable
private fun SoundIndicatorPreview() {
    VideoTheme {
        Row {
            SoundIndicator(
                isSpeaking = true,
                isAudioEnabled = true,
                audioLevels = listOf(0.7f, 0f, 0.5f, 0f, 0.9f)
            )
            SoundIndicator(
                isSpeaking = false,
                isAudioEnabled = false,
                audioLevels = listOf(0.7f, 0f, 0.5f, 0f, 0.9f)
            )
        }
    }
}
