/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.ui.menu

import android.media.AudioAttributes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AudioUsageUiState(
    val text: String,
    val icon: ImageVector,
    val highlight: Boolean,
    val audioUsage: Int,
)

data object AudioUsageMediaUiState : AudioUsageUiState(
    text = "Toggle to mono playout",
    icon = Icons.Default.Audiotrack,
    highlight = false,
    audioUsage = AudioAttributes.USAGE_MEDIA,
)

data object AudioUsageVoiceCommunicationUiState : AudioUsageUiState(
    text = "Toggle to stereo playout",
    icon = Icons.Default.Phone,
    highlight = true,
    audioUsage = AudioAttributes.USAGE_VOICE_COMMUNICATION,
)

fun getAudioUsageUiState(currentAudioUsage: Int): AudioUsageUiState {
    return when (currentAudioUsage) {
        AudioAttributes.USAGE_MEDIA -> AudioUsageMediaUiState
        AudioAttributes.USAGE_VOICE_COMMUNICATION -> AudioUsageVoiceCommunicationUiState
        else -> AudioUsageVoiceCommunicationUiState // default
    }
}

