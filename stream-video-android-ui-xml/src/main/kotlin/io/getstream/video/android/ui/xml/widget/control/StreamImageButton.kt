package io.getstream.video.android.ui.xml.widget.control

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import io.getstream.video.android.common.util.getFloatResource
import io.getstream.video.android.ui.common.R

internal class StreamImageButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageButton(context, attrs, defStyleAttr) {

    override fun setEnabled(enabled: Boolean) {
        background.alpha = (context.getFloatResource(if (enabled) {
            R.dimen.buttonToggleOnAlpha
        } else {
            R.dimen.buttonToggleOffAlpha
        }) * 255).toInt()
    }

}