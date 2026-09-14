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

import io.getstream.video.android.ui.menu.base.MenuIcon
import io.getstream.video.android.ui.menu.base.menuIcon
import io.getstream.video.android.compose.R as ComposeR

sealed class TranscriptionUiState(
    val text: String,
    val icon: MenuIcon,
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
    icon = menuIcon(ComposeR.drawable.stream_design_ic_file),
    highlight = false,
)

data object TranscriptionStoppedUiState : TranscriptionUiState(
    text = "Stop Transcription",
    icon = menuIcon(ComposeR.drawable.stream_design_ic_file),
    highlight = true,
)

data object TranscriptionDisabledUiState : TranscriptionUiState(
    text = "Transcription not available",
    icon = menuIcon(ComposeR.drawable.stream_design_ic_file),
    highlight = false,
)
