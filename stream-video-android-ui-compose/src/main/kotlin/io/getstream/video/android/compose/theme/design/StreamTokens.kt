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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Layout, radius, typography and component primitives from the design system foundations.
 * Semantic classes build on these; components use them directly.
 */
@Suppress("MagicNumber")
public object StreamTokens {

    // Spacing
    public val spacingNone: Dp = 0.dp
    public val spacing3xs: Dp = 2.dp
    public val spacing2xs: Dp = 4.dp
    public val spacingXs: Dp = 8.dp
    public val spacingSm: Dp = 12.dp
    public val spacingMd: Dp = 16.dp
    public val spacingLg: Dp = 20.dp
    public val spacingXl: Dp = 24.dp
    public val spacing2xl: Dp = 32.dp
    public val spacing3xl: Dp = 40.dp

    // Radius
    public val radiusNone: CornerSize = CornerSize(0.dp)
    public val radius2xs: CornerSize = CornerSize(2.dp)
    public val radiusXs: CornerSize = CornerSize(4.dp)
    public val radiusSm: CornerSize = CornerSize(6.dp)
    public val radiusMd: CornerSize = CornerSize(8.dp)
    public val radiusLg: CornerSize = CornerSize(12.dp)
    public val radiusXl: CornerSize = CornerSize(16.dp)
    public val radius2xl: CornerSize = CornerSize(20.dp)
    public val radius3xl: CornerSize = CornerSize(24.dp)
    public val radius4xl: CornerSize = CornerSize(32.dp)
    public val radiusMax: CornerSize = CornerSize(percent = 50)

    // Sizes
    public val size2: Dp = 2.dp
    public val size4: Dp = 4.dp
    public val size6: Dp = 6.dp
    public val size8: Dp = 8.dp
    public val size10: Dp = 10.dp
    public val size12: Dp = 12.dp
    public val size13: Dp = 13.dp
    public val size14: Dp = 14.dp
    public val size15: Dp = 15.dp
    public val size16: Dp = 16.dp
    public val size17: Dp = 17.dp
    public val size18: Dp = 18.dp
    public val size20: Dp = 20.dp
    public val size22: Dp = 22.dp
    public val size24: Dp = 24.dp
    public val size28: Dp = 28.dp
    public val size32: Dp = 32.dp
    public val size40: Dp = 40.dp
    public val size48: Dp = 48.dp
    public val size56: Dp = 56.dp
    public val size64: Dp = 64.dp
    public val size80: Dp = 80.dp
    public val size128: Dp = 128.dp
    public val size144: Dp = 144.dp
    public val size208: Dp = 208.dp
    public val size240: Dp = 240.dp
    public val size320: Dp = 320.dp
    public val size480: Dp = 480.dp
    public val size560: Dp = 560.dp
    public val size640: Dp = 640.dp
    public val size760: Dp = 760.dp

    // Stroke
    public val strokeW80: Dp = 0.8.dp
    public val strokeW100: Dp = 1.dp
    public val strokeW120: Dp = 1.2.dp
    public val strokeW150: Dp = 1.5.dp
    public val strokeW200: Dp = 2.dp
    public val strokeW300: Dp = 3.dp
    public val strokeW400: Dp = 4.dp

    // Typography
    public val fontWeightRegular: FontWeight = FontWeight.W400
    public val fontWeightMedium: FontWeight = FontWeight.W500
    public val fontWeightSemiBold: FontWeight = FontWeight.W600
    public val fontWeightBold: FontWeight = FontWeight.W700
    public val fontSizeMicro: TextUnit = 8.sp
    public val fontSize2xs: TextUnit = 10.sp
    public val fontSizeXs: TextUnit = 12.sp
    public val fontSizeSm: TextUnit = 14.sp
    public val fontSizeMd: TextUnit = 16.sp
    public val fontSizeLg: TextUnit = 18.sp
    public val fontSizeXl: TextUnit = 20.sp
    public val fontSize2xl: TextUnit = 24.sp
    public val lineHeightTight: TextUnit = 16.sp
    public val lineHeightNormal: TextUnit = 20.sp
    public val lineHeightRelaxed: TextUnit = 24.sp

    // Components
    public val deviceRadius: CornerSize = CornerSize(32.dp)
    public val deviceSafeAreaBottom: Dp = 40.dp
    public val deviceSafeAreaTop: Dp = 52.dp
    public val buttonRadiusLg: CornerSize = CornerSize(percent = 50)
    public val buttonRadiusMd: CornerSize = CornerSize(percent = 50)
    public val buttonRadiusSm: CornerSize = CornerSize(percent = 50)
    public val buttonRadiusFull: CornerSize = CornerSize(percent = 50)
    public val buttonVisualHeightSm: Dp = 32.dp
    public val buttonVisualHeightMd: Dp = 40.dp
    public val buttonVisualHeightLg: Dp = 48.dp
    public val buttonVisualHeightXs: Dp = 24.dp
    public val buttonHitTargetMinHeight: Dp = 48.dp
    public val buttonHitTargetMinWidth: Dp = 48.dp
    public val buttonPaddingYLg: Dp = 14.dp
    public val buttonPaddingYMd: Dp = 10.dp
    public val buttonPaddingYSm: Dp = 6.dp
    public val buttonPaddingYXs: Dp = 4.dp
    public val buttonPaddingXIconOnlyLg: Dp = 14.dp
    public val buttonPaddingXIconOnlyMd: Dp = 10.dp
    public val buttonPaddingXIconOnlySm: Dp = 6.dp
    public val buttonPaddingXIconOnlyXs: Dp = 4.dp
    public val buttonPaddingXWithLabelLg: Dp = 16.dp
    public val buttonPaddingXWithLabelMd: Dp = 16.dp
    public val buttonPaddingXWithLabelSm: Dp = 16.dp
    public val buttonPaddingXWithLabelXs: Dp = 12.dp
    public val iconSizeXs: Dp = 12.dp
    public val iconSizeSm: Dp = 16.dp
    public val iconSizeMd: Dp = 20.dp
    public val iconSizeLg: Dp = 32.dp
    public val iconStrokeSubtle: Dp = 1.2.dp
    public val iconStrokeDefault: Dp = 1.5.dp
    public val iconStrokeEmphasis: Dp = 2.dp
    public val emojiSm: TextUnit = 16.sp
    public val emojiMd: TextUnit = 24.sp
    public val emojiLg: TextUnit = 32.sp
    public val emojiXl: TextUnit = 48.sp
    public val emoji2xl: TextUnit = 64.sp
    public val inputRadiusTextInput: CornerSize = CornerSize(16.dp)
    public val inputRadiusSelectInput: CornerSize = CornerSize(16.dp)
    public val inputRadiusSearchInput: CornerSize = CornerSize(percent = 50)
    public val inputRadiusOptionCard: CornerSize = CornerSize(16.dp)
}
