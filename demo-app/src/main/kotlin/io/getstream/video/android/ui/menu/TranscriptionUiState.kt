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

package io.getstream.video.android.ui.menu

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.ui.graphics.vector.ImageVector

sealed class TranscriptionUiState(
    val text: String,
    val icon: ImageVector, // Assuming it's a drawable resource ID
    val highlight: Boolean,
)

/**
 * Stop Transcription
 * Start Transcription
 * Transcription is disabled
 * Transcription failed
 */

data object TranscriptionAvailableUiState : TranscriptionUiState(
    text = "Transcribe the call",
    icon = Icons.Default.Description,
    highlight = false,
)

data object TranscriptionStoppedUiState : TranscriptionUiState(
    text = "Stop Transcription",
    icon = Icons.Default.Description,
    highlight = true,
)

data object TranscriptionDisabledUiState : TranscriptionUiState(
    text = "Transcription not available",
    icon = Icons.Default.Description,
    highlight = false,
)
