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

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.getstream.video.android.compose.theme.design.StreamTokens

/**
 * Used to indicate the sound state of a given participant. A muted participant gets a plain mute
 * icon; an unmuted one gets the indicator box with the sound levels, at rest while not speaking.
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
    if (!isAudioEnabled) {
        MicrophoneIndicator(modifier = modifier, isMicrophoneEnabled = false)
        return
    }
    GenericIndicator(modifier = modifier, size = StreamTokens.size24) {
        AudioVolumeIndicator(
            modifier = Modifier
                .align(Alignment.Center)
                .height(StreamTokens.size10),
            audioLevels = if (isSpeaking) audioLevel else 0f,
        )
    }
}
