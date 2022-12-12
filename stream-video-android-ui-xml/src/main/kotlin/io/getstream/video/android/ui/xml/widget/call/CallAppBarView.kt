package io.getstream.video.android.ui.xml.widget.call

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import io.getstream.video.android.ui.xml.databinding.ViewCallAppBarBinding
import io.getstream.video.android.ui.xml.utils.extensions.inflater
import io.getstream.video.android.ui.common.R
import io.getstream.video.android.ui.xml.utils.extensions.dpToPxPrecise
import io.getstream.video.android.ui.xml.utils.extensions.getDimension
import io.getstream.video.android.ui.xml.widget.call.content.DefaultCallAppBarCenterContent
import io.getstream.video.android.ui.xml.widget.call.content.DefaultCallAppBarLeadingContent
import io.getstream.video.android.ui.xml.widget.call.content.DefaultCallAppBarTrailingContent

public class CallAppBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewCallAppBarBinding.inflate(inflater, this)

    public var backListener: () -> Unit = {}

    public var participantsClickListener: () -> Unit = {}

    init {
        init()
    }

    private fun init() {
        setBackgroundColor(ContextCompat.getColor(context, R.color.stream_bars_background))
        setPadding(context.getDimension(R.dimen.callAppBarPadding))
        elevation = 8.dpToPxPrecise()

        setLeadingContent(
            DefaultCallAppBarLeadingContent(context).also {
                it.backListener = { backListener() }
            }
        )

        setCenterContent(
            DefaultCallAppBarCenterContent(context)
        )

        setTrailingContent(
            DefaultCallAppBarTrailingContent(context).also {
                it.participantsClickListener = { participantsClickListener() }
            }
        )
    }

    public fun <V : View> setLeadingContent(
        contentView: V,
        layoutParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        ),
    ) {
        with(binding.leadingContent) {
            removeAllViews()
            addView(contentView, layoutParams)
        }
    }

    public fun <V : View> setCenterContent(
        contentView: V,
        layoutParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        ),
    ) {
        with(binding.centerContent) {
            removeAllViews()
            addView(contentView, layoutParams)
        }
    }

    public fun <V : View> setTrailingContent(
        contentView: V,
        layoutParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        ),
    ) {
        with(binding.trailingContent) {
            removeAllViews()
            addView(contentView, layoutParams)
        }
    }
}