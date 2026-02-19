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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState

/**
 * Marker interface for all stream styles.
 */
@Stable
public interface StreamStyle

/**
 * Possible interaction states.
 */
public enum class StyleState {
    ENABLED, DISABLED, PRESSED
}

/**
 * Possible sizes for the stile.
 */
public enum class StyleSize {
    XS, S, M, L, XL, XXL
}

/**
 * Stream style container, containing multiple styles
 */
@Stable
public interface StreamStateStyle<T : StreamStyle> {

    /** Default style for the component. */
    public val default: T

    /** Pressed style for the component */
    public val pressed: T

    /** Disabled style for the component */
    public val disabled: T

    /**
     * Get the style  based on [StyleState].
     */
    @Composable
    public fun of(state: StyleState): State<T> = rememberUpdatedState(
        when (state) {
            StyleState.ENABLED -> default
            StyleState.DISABLED -> disabled
            StyleState.PRESSED -> pressed
        },
    )
}
