package io.getstream.video.android.ui.xml.widget.control

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.cardview.widget.CardView
import io.getstream.video.android.ui.xml.databinding.ViewCallControlButtonBinding
import io.getstream.video.android.ui.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.ui.xml.utils.extensions.dpToPxPrecise
import io.getstream.video.android.ui.xml.utils.extensions.streamThemeInflater

/**
 * Class used to show call actions. Call [setEnabled] to make the button opaque or not depending on state.
 */
internal class CallControlButton : CardView {

    private val binding = ViewCallControlButtonBinding.inflate(streamThemeInflater, this)

    private lateinit var style: ControlButtonStyle

    public constructor(context: Context) : this(context, null, 0)
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context.createStreamThemeWrapper(),
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        style = ControlButtonStyle(context, attrs)
        initCard()
        initImageView()
    }

    private fun initCard() {
        radius = 2000.dpToPxPrecise()
        background = style.background
        background?.setTint(style.backgroundTint)
        isEnabled = style.enabled
    }

    private fun initImageView() {
        with(binding.buttonIcon) {
            setImageDrawable(style.icon)
            setColorFilter(style.iconTint)
        }
    }

    /**
     * Sets the background alpha depending if the control is enabled or disabled.
     *
     * @param enabled Whether the control is enabled or not.
     */
    override fun setEnabled(enabled: Boolean) {
        val alpha = if (enabled) style.backgroundEnabledAlpha else style.backgroundDisabledAlpha
        background?.alpha = (alpha * 255).toInt()
    }

    /**
     * Sets the background for the view. Creates new drawable so if a drawable is reused the states don't mix.
     *
     * @param background The background [Drawable] to be set, null to remove it.
     */
    override fun setBackground(background: Drawable?) {
        super.setBackground(background?.constantState?.newDrawable())
    }

    /**
     * Sets the icon for the control button.
     *
     * @param resId The resource id to be used as the icon.
     */
    public fun setImageResource(@DrawableRes resId: Int) {
        binding.buttonIcon.setImageResource(resId)
    }

    /**
     * Sets the icon tint the control button.
     *
     * @param color The color of the icon.
     */
    public fun setColorFilter(@ColorInt color: Int) {
        binding.buttonIcon.setColorFilter(color)
    }

    /**
     * Sets the icon for the control button.
     *
     * @param drawable The drawable to be used as the icon.
     */
    public fun setImageDrawable(drawable: Drawable?) {
        binding.buttonIcon.setImageDrawable(drawable)
    }
}