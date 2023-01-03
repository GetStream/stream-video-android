package io.getstream.video.android.ui.xml.widget.control

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.Px
import io.getstream.video.android.ui.xml.R
import io.getstream.video.android.ui.xml.utils.extensions.getColorCompat
import io.getstream.video.android.ui.xml.utils.extensions.getDimension
import io.getstream.video.android.ui.xml.utils.extensions.use
import io.getstream.video.android.ui.xml.widget.transformer.TransformStyle
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