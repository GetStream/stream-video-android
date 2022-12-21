package io.getstream.video.android.ui.xml.widget.control

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import io.getstream.video.android.common.util.getFloatResource
import io.getstream.video.android.ui.common.R as RCommon

/**
 * Class used to show call actions. Call [setEnabled] to make the button opaque or not depending on state.
 */
internal class StreamImageButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageButton(context, attrs, defStyleAttr) {

    init {
        scaleType = ScaleType.CENTER
    }

    internal var disabledAlpha: Float = context.getFloatResource(RCommon.dimen.buttonToggleOffAlpha)
    internal var enabledAlpha: Float = context.getFloatResource(RCommon.dimen.buttonToggleOnAlpha)

    override fun setEnabled(enabled: Boolean) {
        val alpha = if (enabled) enabledAlpha else disabledAlpha
        background.alpha = (alpha * 255).toInt()
    }

    override fun setBackground(background: Drawable?) {
        super.setBackground(background?.constantState?.newDrawable())
    }
}