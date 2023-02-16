/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.Px
import io.getstream.video.android.xml.R
import io.getstream.video.android.xml.utils.extensions.getColorCompat
import io.getstream.video.android.xml.utils.extensions.getDimension
import io.getstream.video.android.xml.utils.extensions.use
import io.getstream.video.android.xml.widget.transformer.TransformStyle
import io.getstream.video.android.ui.common.R as RCommon

/**
 * Style for [CallControlsStyle].
 * Use this class together with [TransformStyle.callControlsStyleTransformer] to change [CallControlsStyle] styles
 * programmatically.
 *
 * @param callControlButtonSize The size of the call control buttons.
 * @param callControlsBackgroundColor The color of the call controls background.
 */
public data class CallControlsStyle(
    @Px public val callControlButtonSize: Int,
    @ColorInt public val callControlsBackgroundColor: Int
) {

    internal companion object {
        operator fun invoke(context: Context, attrs: AttributeSet?): CallControlsStyle {
            context.obtainStyledAttributes(
                attrs,
                R.styleable.CallControlsView,
                R.attr.streamCallControlsViewStyle,
                R.style.Stream_CallControls
            ).use {

                val callControlButtonSize = it.getDimensionPixelSize(
                    R.styleable.CallControlsView_streamCallControlsButtonSize,
                    context.getDimension(RCommon.dimen.callControlButtonSize)
                )

                val callControlsBackgroundColor = it.getColor(
                    R.styleable.CallControlsView_streamCallControlsBackgroundColor,
                    context.getColorCompat(R.color.stream_white)
                )

                return CallControlsStyle(
                    callControlButtonSize = callControlButtonSize,
                    callControlsBackgroundColor = callControlsBackgroundColor
                ).let(TransformStyle.callControlsStyleTransformer::transform)
            }
        }
    }
}
