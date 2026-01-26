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

package io.getstream.video.android.compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.getstream.video.android.ui.common.R
import kotlin.math.abs

@Composable
@ReadOnlyComposable
internal fun initialsColors(text: String): Pair<Color, Color> {
    val gradientBaseColors =
        LocalContext.current.resources.getIntArray(R.array.stream_video_avatar_gradient_colors)

    val baseColorIndex = abs(text.hashCode()) % gradientBaseColors.size
    val baseColor = Color(gradientBaseColors[baseColorIndex])
    return Pair(baseColor, baseColor.copy(alpha = 0.16f))
}
