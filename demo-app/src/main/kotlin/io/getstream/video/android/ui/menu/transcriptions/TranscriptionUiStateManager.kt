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

package io.getstream.video.android.ui.menu.transcriptions

import io.getstream.android.video.generated.models.CallSettingsResponse
import io.getstream.android.video.generated.models.TranscriptionSettingsResponse
import io.getstream.video.android.ui.menu.TranscriptionAvailableUiState
import io.getstream.video.android.ui.menu.TranscriptionDisabledUiState
import io.getstream.video.android.ui.menu.TranscriptionStoppedUiState
import io.getstream.video.android.ui.menu.TranscriptionUiState

class TranscriptionUiStateManager(
    private val isTranscribing: Boolean,
    private val settings: CallSettingsResponse?,
) {

    fun getUiState(): TranscriptionUiState {
        return if (settings != null) {
            val mode = settings.transcription.mode
            when (mode) {
                TranscriptionSettingsResponse.Mode.Available, TranscriptionSettingsResponse.Mode.AutoOn -> {
                    if (isTranscribing) {
                        TranscriptionStoppedUiState
                    } else {
                        TranscriptionAvailableUiState
                    }
                }
                else -> {
                    TranscriptionDisabledUiState
                }
            }
        } else {
            TranscriptionDisabledUiState
        }
    }
}
