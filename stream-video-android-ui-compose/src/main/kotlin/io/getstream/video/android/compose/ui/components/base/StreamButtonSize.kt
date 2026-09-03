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

package io.getstream.video.android.compose.ui.components.base

import androidx.compose.ui.unit.Dp
import io.getstream.video.android.compose.theme.design.StreamTokens

/**
 * The size of a [StreamButton].
 *
 * The size sets the visual height of the button. The touch target is always at least 48dp.
 */
public enum class StreamButtonSize(
    internal val minimumSize: Dp,
    internal val iconSize: Dp,
    internal val labelPadding: Dp,
) {
    /** 32dp tall, 16dp icons. */
    Small(
        minimumSize = StreamTokens.buttonVisualHeightSm,
        iconSize = StreamTokens.iconSizeSm,
        labelPadding = StreamTokens.buttonPaddingXWithLabelSm,
    ),

    /** 40dp tall, 20dp icons. The size of the call control buttons. */
    Medium(
        minimumSize = StreamTokens.buttonVisualHeightMd,
        iconSize = StreamTokens.iconSizeMd,
        labelPadding = StreamTokens.buttonPaddingXWithLabelMd,
    ),

    /** 48dp tall, 20dp icons. The size of full-width actions such as "Start Call". */
    Large(
        minimumSize = StreamTokens.buttonVisualHeightLg,
        iconSize = StreamTokens.iconSizeMd,
        labelPadding = StreamTokens.buttonPaddingXWithLabelLg,
    ),
}
