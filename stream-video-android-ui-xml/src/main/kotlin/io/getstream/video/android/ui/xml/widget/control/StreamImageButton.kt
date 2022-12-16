package io.getstream.video.android.ui.xml.widget.control

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import io.getstream.video.android.common.util.getFloatResource
import io.getstream.video.android.ui.xml.R
import io.getstream.video.android.ui.common.R as RCommon

/**
 * Class used to show call actions.
 */
public class StreamImageButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageButton(context, attrs, defStyleAttr) {

    init {
        scaleType = ScaleType.CENTER
    }

    override fun setEnabled(enabled: Boolean) {
        background.alpha = (context.getFloatResource(if (enabled) {
            RCommon.dimen.buttonToggleOnAlpha
        } else {
            RCommon.dimen.buttonToggleOffAlpha
        }) * 255).toInt()
    }
}