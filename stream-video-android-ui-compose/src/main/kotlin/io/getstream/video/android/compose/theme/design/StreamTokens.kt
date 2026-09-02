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

package io.getstream.video.android.compose.theme.design

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Layout, radius, typography and component primitives from the design system foundations.
 * Semantic classes build on these; components use them directly.
 */
@Suppress("MagicNumber")
internal object StreamTokens {

    // Spacing
    val spacingNone = 0.dp
    val spacingXxxs = 2.dp
    val spacingXxs = 4.dp
    val spacingXs = 8.dp
    val spacingSm = 12.dp
    val spacingMd = 16.dp
    val spacingLg = 20.dp
    val spacingXl = 24.dp
    val spacing2xl = 32.dp
    val spacing3xl = 40.dp

    // Radius
    val radiusNone = CornerSize(0.dp)
    val radiusXxs = CornerSize(2.dp)
    val radiusXs = CornerSize(4.dp)
    val radiusSm = CornerSize(6.dp)
    val radiusMd = CornerSize(8.dp)
    val radiusLg = CornerSize(12.dp)
    val radiusXl = CornerSize(16.dp)
    val radius2xl = CornerSize(20.dp)
    val radius3xl = CornerSize(24.dp)
    val radius4xl = CornerSize(32.dp)
    val radiusMax = CornerSize(percent = 50)

    // Sizes
    val size2 = 2.dp
    val size4 = 4.dp
    val size6 = 6.dp
    val size8 = 8.dp
    val size10 = 10.dp
    val size12 = 12.dp
    val size13 = 13.dp
    val size14 = 14.dp
    val size15 = 15.dp
    val size16 = 16.dp
    val size17 = 17.dp
    val size18 = 18.dp
    val size20 = 20.dp
    val size22 = 22.dp
    val size24 = 24.dp
    val size28 = 28.dp
    val size32 = 32.dp
    val size40 = 40.dp
    val size48 = 48.dp
    val size56 = 56.dp
    val size64 = 64.dp
    val size80 = 80.dp
    val size128 = 128.dp
    val size144 = 144.dp
    val size208 = 208.dp
    val size240 = 240.dp
    val size320 = 320.dp
    val size480 = 480.dp
    val size560 = 560.dp
    val size640 = 640.dp
    val size760 = 760.dp

    // Stroke
    val strokeW80 = 0.8.dp
    val strokeW100 = 1.dp
    val strokeW120 = 1.2.dp
    val strokeW150 = 1.5.dp
    val strokeW200 = 2.dp
    val strokeW300 = 3.dp
    val strokeW400 = 4.dp

    // Typography
    val fontWeightRegular = FontWeight.W400
    val fontWeightMedium = FontWeight.W500
    val fontWeightSemiBold = FontWeight.W600
    val fontWeightBold = FontWeight.W700
    val fontSizeMicro = 8.sp
    val fontSizeXxs = 10.sp
    val fontSizeXs = 12.sp
    val fontSizeSm = 14.sp
    val fontSizeMd = 16.sp
    val fontSizeLg = 18.sp
    val fontSizeXl = 20.sp
    val fontSize2xl = 24.sp
    val lineHeightTight = 16.sp
    val lineHeightNormal = 20.sp
    val lineHeightRelaxed = 24.sp

    // Components
    val deviceRadius = CornerSize(32.dp)
    val deviceSafeAreaBottom = 40.dp
    val deviceSafeAreaTop = 52.dp
    val buttonRadiusLg = CornerSize(percent = 50)
    val buttonRadiusMd = CornerSize(percent = 50)
    val buttonRadiusSm = CornerSize(percent = 50)
    val buttonRadiusFull = CornerSize(percent = 50)
    val buttonVisualHeightSm = 32.dp
    val buttonVisualHeightMd = 40.dp
    val buttonVisualHeightLg = 48.dp
    val buttonVisualHeightXs = 24.dp
    val buttonHitTargetMinHeight = 48.dp
    val buttonHitTargetMinWidth = 48.dp
    val buttonPaddingYLg = 14.dp
    val buttonPaddingYMd = 10.dp
    val buttonPaddingYSm = 6.dp
    val buttonPaddingYXs = 4.dp
    val buttonPaddingXIconOnlyLg = 14.dp
    val buttonPaddingXIconOnlyMd = 10.dp
    val buttonPaddingXIconOnlySm = 6.dp
    val buttonPaddingXIconOnlyXs = 4.dp
    val buttonPaddingXWithLabelLg = 16.dp
    val buttonPaddingXWithLabelMd = 16.dp
    val buttonPaddingXWithLabelSm = 16.dp
    val buttonPaddingXWithLabelXs = 12.dp
    val iconSizeXs = 12.dp
    val iconSizeSm = 16.dp
    val iconSizeMd = 20.dp
    val iconSizeLg = 32.dp
    val iconStrokeSubtle = 1.2.dp
    val iconStrokeDefault = 1.5.dp
    val iconStrokeEmphasis = 2.dp
    val emojiSm = 16.sp
    val emojiMd = 24.sp
    val emojiLg = 32.sp
    val emojiXl = 48.sp
    val emoji2xl = 64.sp
    val inputRadiusTextInput = CornerSize(16.dp)
    val inputRadiusSelectInput = CornerSize(16.dp)
    val inputRadiusSearchInput = CornerSize(percent = 50)
    val inputRadiusOptionCard = CornerSize(16.dp)
}
