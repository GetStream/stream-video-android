package io.getstream.video.android.ui.xml.widget.call.content

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import io.getstream.video.android.ui.common.R
import io.getstream.video.android.ui.xml.utils.extensions.getDimension

/**
 * Represents the default implementation of the AppBar leading content. By default shows a back button.
 */
public class DefaultCallAppBarLeadingContent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(
    context, attrs, defStyleAttr
) {

    /**
     * Handler that notifies when the back button has been clicked.
     */
    public var backListener: () -> Unit = {}

    init {
        setRipple()
        setImageResource(R.drawable.ic_arrow_back)
        setOnClickListener { backListener() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (layoutParams as FrameLayout.LayoutParams).apply {
            marginStart = context.getDimension(R.dimen.callAppBarLeadingContentSpacingStart)
            marginEnd = context.getDimension(R.dimen.callAppBarLeadingContentSpacingEnd)
        }
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams?) {
        super.setLayoutParams(params)
    }

    /**
     * Adds a ripple effect on clicks.
     */
    private fun setRipple() {
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
        setBackgroundResource(outValue.resourceId)
    }
}