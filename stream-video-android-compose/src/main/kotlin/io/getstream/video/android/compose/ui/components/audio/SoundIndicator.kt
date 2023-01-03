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

package io.getstream.video.android.compose.ui.components.audio

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.ui.common.R

/**
 * Used to indicate the sound state of a given participant. Either shows a mute icon or the sound
 * levels.
 *
 * @param hasSound If the participant has sound active.
 * @param audioLevel The level of audio of the participant.
 */
@Composable
public fun SoundIndicator(
    hasSound: Boolean,
    audioLevel: Float
) {
    if (hasSound) {
        SoundLevels(audioLevel)
    } else {
        Icon(
            modifier = Modifier
                .size(20.dp)
                .padding(end = 4.dp),
            painter = painterResource(id = R.drawable.ic_mic_off),
            tint = VideoTheme.colors.errorAccent,
            contentDescription = null
        )
    }
}
