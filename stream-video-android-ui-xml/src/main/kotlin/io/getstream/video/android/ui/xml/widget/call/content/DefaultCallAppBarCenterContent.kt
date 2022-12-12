package io.getstream.video.android.ui.xml.widget.call.content

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import io.getstream.video.android.ui.common.R
import io.getstream.video.android.ui.xml.utils.extensions.getDimension

public class DefaultCallAppBarCenterContent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
): AppCompatTextView(
    context, attrs, defStyleAttr
) {

    init {
        text = context.getString(R.string.default_app_bar_title)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (layoutParams as FrameLayout.LayoutParams).apply {
            marginStart = context.getDimension(R.dimen.callAppBarCenterContentSpacingStart)
            marginEnd = context.getDimension(R.dimen.callAppBarCenterContentSpacingEnd)
        }
    }

}