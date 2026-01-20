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

package io.getstream.video.android.xml.widget.control

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import io.getstream.video.android.ui.common.util.getFloatResource
import io.getstream.video.android.xml.R
import io.getstream.video.android.xml.utils.extensions.getColorCompat
import io.getstream.video.android.xml.utils.extensions.use
import io.getstream.video.android.xml.widget.transformer.TransformStyle
import io.getstream.video.android.ui.common.R as RCommon

/**
 * Style for [ControlButtonStyle].
 * Use this class together with [TransformStyle.controlButtonStyleTransformer] to change [ControlButtonStyle] styles
 * programmatically.
 *
 * @param icon The icon for the button.
 * @param iconTint The color of the icon.
 * @param background The button background.
 * @param backgroundTint The background color.
 * @param enabled Whether the view is enabled or not. Will set opacity on background depending on the state.
 * @param backgroundEnabledAlpha Background opacity when the option is enabled.
 * @param backgroundDisabledAlpha Background opacity when the option is disabled.
 */
public data class ControlButtonStyle(
    public val icon: Drawable?,
    @ColorInt public val iconTint: Int,
    public val background: Drawable?,
    @ColorInt val backgroundTint: Int,
    public val enabled: Boolean,
    public val backgroundEnabledAlpha: Float,
    public val backgroundDisabledAlpha: Float,
) {

    internal companion object {
        operator fun invoke(context: Context, attrs: AttributeSet?): ControlButtonStyle {
            context.obtainStyledAttributes(
                attrs,
                R.styleable.ControlButton,
                0,
                0,
            ).use {
                val icon = it.getDrawable(R.styleable.ControlButton_android_src)

                val iconTint = it.getColor(
                    R.styleable.ControlButton_android_tint,
                    context.getColorCompat(R.color.stream_video_black),
                )

                val background = it.getDrawable(R.styleable.ControlButton_android_background)

                val backgroundTint = it.getColor(
                    R.styleable.ControlButton_android_backgroundTint,
                    context.getColorCompat(RCommon.color.stream_video_app_background),
                )

                val enabled = it.getBoolean(
                    R.styleable.ControlButton_android_enabled,
                    true,
                )

                val backgroundEnabledAlpha = it.getFloat(
                    R.styleable.ControlButton_streamVideoControlButtonBackgroundEnabledAlpha,
                    context.getFloatResource(RCommon.dimen.stream_video_buttonToggleOnAlpha),
                )

                val backgroundDisabledAlpha = it.getFloat(
                    R.styleable.ControlButton_streamVideoControlButtonBackgroundDisabledAlpha,
                    context.getFloatResource(RCommon.dimen.stream_video_buttonToggleOffAlpha),
                )

                return ControlButtonStyle(
                    icon = icon,
                    iconTint = iconTint,
                    background = background,
                    backgroundTint = backgroundTint,
                    enabled = enabled,
                    backgroundEnabledAlpha = backgroundEnabledAlpha,
                    backgroundDisabledAlpha = backgroundDisabledAlpha,
                ).let(TransformStyle.controlButtonStyleTransformer::transform)
            }
        }
    }
}
