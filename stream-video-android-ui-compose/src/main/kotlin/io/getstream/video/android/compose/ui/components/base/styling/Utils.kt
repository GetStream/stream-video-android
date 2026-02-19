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

package io.getstream.video.android.compose.ui.components.base.styling

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

@Composable
internal fun styleState(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
): StyleState {
    val pressed by interactionSource.collectIsPressedAsState()
    val state = if (enabled) {
        StyleState.ENABLED
    } else if (pressed) {
        StyleState.PRESSED
    } else {
        StyleState.DISABLED
    }
    return state
}
