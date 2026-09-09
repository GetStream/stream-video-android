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

package io.getstream.video.android.compose.theme

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.ripple.RippleAlpha
import io.getstream.video.android.compose.theme.design.StreamDesign

/**
 * The ripple derived from the theme colors, so pressed states follow the palette.
 */
@OptIn(ExperimentalMaterialApi::class)
internal fun streamRippleConfiguration(
    colors: StreamDesign.Colors,
    lightTheme: Boolean,
): RippleConfiguration = RippleConfiguration(
    color = if (lightTheme) colors.chrome.s900 else colors.chrome.s1000,
    rippleAlpha = if (lightTheme) LightRippleAlpha else DarkRippleAlpha,
)

private val LightRippleAlpha = RippleAlpha(
    pressedAlpha = 0.15f,
    focusedAlpha = 0.15f,
    draggedAlpha = 0.10f,
    hoveredAlpha = 0.10f,
)

private val DarkRippleAlpha = RippleAlpha(
    pressedAlpha = 0.20f,
    focusedAlpha = 0.20f,
    draggedAlpha = 0.15f,
    hoveredAlpha = 0.15f,
)
