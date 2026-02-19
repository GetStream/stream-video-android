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

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import io.getstream.video.android.ui.common.R

public data class StreamColors(
    val brandPrimary: Color,
    val brandPrimaryLt: Color,
    val brandPrimaryDk: Color,
    val brandSecondary: Color,
    val brandSecondaryTransparent: Color,
    val brandCyan: Color,
    val brandGreen: Color,
    val brandYellow: Color,
    val brandRed: Color,
    val brandRedLt: Color,
    val brandRedDk: Color,
    val brandMaroon: Color,
    val brandViolet: Color,
    val basePrimary: Color,
    val baseSecondary: Color,
    val baseTertiary: Color,
    val baseQuaternary: Color,
    val baseQuinary: Color,
    val baseSenary: Color,
    val baseSheetPrimary: Color,
    val baseSheetSecondary: Color,
    val baseSheetTertiary: Color,
    val baseSheetQuarternary: Color,
    val buttonPrimaryDefault: Color,
    val buttonPrimaryPressed: Color,
    val buttonPrimaryDisabled: Color,
    val buttonBrandDefault: Color,
    val buttonBrandPressed: Color,
    val buttonBrandDisabled: Color,
    val buttonAlertDefault: Color,
    val buttonAlertPressed: Color,
    val buttonAlertDisabled: Color,
    val iconDefault: Color,
    val iconPressed: Color,
    val iconActive: Color,
    val iconAlert: Color,
    val iconDisabled: Color,
    val alertSuccess: Color,
    val alertCaution: Color,
    val alertWarning: Color,
) {
    public companion object {
        @Composable
        public fun defaultColors(): StreamColors = StreamColors(
            brandPrimary = colorResource(id = R.color.stream_video_brand_primary),
            brandPrimaryLt = colorResource(id = R.color.stream_video_brand_primary_lt),
            brandPrimaryDk = colorResource(id = R.color.stream_video_brand_primary_dk),
            brandSecondary = colorResource(id = R.color.stream_video_brand_secondary),
            brandSecondaryTransparent = colorResource(
                id = R.color.stream_video_brand_secondary_transparent,
            ),
            brandCyan = colorResource(id = R.color.stream_video_brand_cyan),
            brandGreen = colorResource(id = R.color.stream_video_brand_green),
            brandYellow = colorResource(id = R.color.stream_video_brand_yellow),
            brandRed = colorResource(id = R.color.stream_video_brand_red),
            brandRedLt = colorResource(id = R.color.stream_video_brand_red_lt),
            brandRedDk = colorResource(id = R.color.stream_video_brand_red_dk),
            brandMaroon = colorResource(id = R.color.stream_video_brand_maroon),
            brandViolet = colorResource(id = R.color.stream_video_brand_violet),
            basePrimary = colorResource(id = R.color.stream_video_base_primary),
            baseSecondary = colorResource(id = R.color.stream_video_base_secondary),
            baseTertiary = colorResource(id = R.color.stream_video_base_tertiary),
            baseQuaternary = colorResource(id = R.color.stream_video_base_quaternary),
            baseQuinary = colorResource(id = R.color.stream_video_base_quinary),
            baseSenary = colorResource(id = R.color.stream_video_base_senary),
            baseSheetPrimary = colorResource(id = R.color.stream_video_base_sheet_primary),
            baseSheetSecondary = colorResource(id = R.color.stream_video_base_sheet_secondary),
            baseSheetTertiary = colorResource(id = R.color.stream_video_base_sheet_tertiary),
            baseSheetQuarternary = colorResource(id = R.color.stream_video_base_sheet_quaternary),
            buttonPrimaryDefault = colorResource(id = R.color.stream_video_button_primary_default),
            buttonPrimaryPressed = colorResource(id = R.color.stream_video_button_primary_pressed),
            buttonPrimaryDisabled = colorResource(
                id = R.color.stream_video_button_primary_disabled,
            ),
            buttonBrandDefault = colorResource(id = R.color.stream_video_button_brand_default),
            buttonBrandPressed = colorResource(id = R.color.stream_video_button_brand_pressed),
            buttonBrandDisabled = colorResource(id = R.color.stream_video_button_brand_disabled),
            buttonAlertDefault = colorResource(id = R.color.stream_video_button_alert_default),
            buttonAlertPressed = colorResource(id = R.color.stream_video_button_alert_pressed),
            buttonAlertDisabled = colorResource(id = R.color.stream_video_button_alert_disabled),
            iconDefault = colorResource(id = R.color.stream_video_icon_default),
            iconPressed = colorResource(id = R.color.stream_video_icon_pressed),
            iconActive = colorResource(id = R.color.stream_video_icon_active),
            iconAlert = colorResource(id = R.color.stream_video_icon_alert),
            iconDisabled = colorResource(id = R.color.stream_video_icon_disabled),
            alertSuccess = colorResource(id = R.color.stream_video_alert_success),
            alertCaution = colorResource(id = R.color.stream_video_alert_caution),
            alertWarning = colorResource(id = R.color.stream_video_alert_warning),
        )
    }
}
