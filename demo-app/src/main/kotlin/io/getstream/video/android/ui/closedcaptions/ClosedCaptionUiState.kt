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

package io.getstream.video.android.ui.closedcaptions

import org.openapitools.client.models.TranscriptionSettingsResponse

sealed class ClosedCaptionUiState {
    /**
     * Indicates that closed captions are available for the current call but are not actively running/displaying.
     * This state usually occurs when the captioning feature is supported but not yet activated/displayed.
     */
    data object Available : ClosedCaptionUiState()

    /**
     * Indicates that closed captions are actively running and displaying captions during the call.
     */
    data object Running : ClosedCaptionUiState()

    /**
     * Indicates that closed captions are unavailable for the current call.
     * This state is used when the feature is disabled or not supported.
     */
    data object UnAvailable : ClosedCaptionUiState()

    public fun TranscriptionSettingsResponse.ClosedCaptionMode.toClosedCaptionUiState(): ClosedCaptionUiState {
        return when (this) {
            is TranscriptionSettingsResponse.ClosedCaptionMode.Available,
            is TranscriptionSettingsResponse.ClosedCaptionMode.AutoOn,
            ->
                Available
            else ->
                UnAvailable
        }
    }
}
