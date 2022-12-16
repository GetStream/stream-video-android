package io.getstream.video.android.ui.xml.widget.call.content

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import io.getstream.video.android.ui.common.R
import io.getstream.video.android.ui.xml.utils.extensions.getDimension

/**
 * Represents the default implementation of the AppBar trailing content.
 */
public class DefaultCallAppBarTrailingContent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
): AppCompatImageView(
    context, attrs, defStyleAttr
) {

    /**
     * Handler that notifies when the participants
     */
    public var participantsClickListener: () -> Unit = {}

    init {
        setRipple()
        setImageResource(R.drawable.ic_participants)
        setOnClickListener { participantsClickListener() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (layoutParams as FrameLayout.LayoutParams).apply {
            marginStart = context.getDimension(R.dimen.callAppBarTrailingContentSpacingStart)
            marginEnd = context.getDimension(R.dimen.callAppBarTrailingContentSpacingEnd)
        }
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