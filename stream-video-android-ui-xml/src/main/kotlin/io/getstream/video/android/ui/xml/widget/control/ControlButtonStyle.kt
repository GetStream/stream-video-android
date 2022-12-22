package io.getstream.video.android.ui.xml.widget.control

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import io.getstream.video.android.common.util.getFloatResource
import io.getstream.video.android.ui.xml.R
import io.getstream.video.android.ui.xml.utils.extensions.getColorCompat
import io.getstream.video.android.ui.xml.utils.extensions.use
import io.getstream.video.android.ui.xml.widget.transformer.TransformStyle
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
    public val backgroundDisabledAlpha: Float
) {

    internal companion object {
        operator fun invoke(context: Context, attrs: AttributeSet?): ControlButtonStyle {
            context.obtainStyledAttributes(
                attrs,
                R.styleable.ControlButton,
                0,
                0
            ).use {

                val icon = it.getDrawable(R.styleable.ControlButton_android_src)

                val iconTint = it.getColor(
                    R.styleable.ControlButton_android_tint,
                    context.getColorCompat(R.color.stream_black)
                )

                val background = it.getDrawable(R.styleable.ControlButton_android_background)

                val backgroundTint = it.getColor(
                    R.styleable.ControlButton_android_backgroundTint,
                    context.getColorCompat(RCommon.color.stream_app_background)
                )

                val enabled = it.getBoolean(
                    R.styleable.ControlButton_android_enabled,
                    true
                )

                val backgroundEnabledAlpha = it.getFloat(
                    R.styleable.ControlButton_streamControlButtonBackgroundEnabledAlpha,
                    context.getFloatResource(RCommon.dimen.buttonToggleOnAlpha)
                )

                val backgroundDisabledAlpha = it.getFloat(
                    R.styleable.ControlButton_streamControlButtonBackgroundDisabledAlpha,
                    context.getFloatResource(RCommon.dimen.buttonToggleOffAlpha)
                )

                return ControlButtonStyle(
                    icon = icon,
                    iconTint = iconTint,
                    background = background,
                    backgroundTint = backgroundTint,
                    enabled = enabled,
                    backgroundEnabledAlpha = backgroundEnabledAlpha,
                    backgroundDisabledAlpha = backgroundDisabledAlpha
                ).let(TransformStyle.controlButtonStyleTransformer::transform)
            }
        }
    }

}