package io.getstream.video.android.xml.widget.participant.internal

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import io.getstream.video.android.core.model.CallParticipantState
import io.getstream.video.android.xml.R
import io.getstream.video.android.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.xml.utils.extensions.dpToPx
import io.getstream.video.android.xml.utils.extensions.dpToPxPrecise
import io.getstream.video.android.xml.utils.extensions.getDrawableCompat
import io.getstream.video.android.xml.widget.participant.CallParticipantView
import io.getstream.video.android.xml.widget.participant.RendererInitializer
import io.getstream.video.android.xml.widget.renderer.VideoRenderer

internal class CallParticipantsListView : HorizontalScrollView, VideoRenderer {

    internal var buildParticipantView: () -> CallParticipantView = { CallParticipantView(context) }

    private val childList: MutableList<CallParticipantView> = mutableListOf()

    private val participantsList: LinearLayout by lazy {
        LinearLayout(context).apply {
            this@CallParticipantsListView.addView(this)
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
            showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
            dividerDrawable = ShapeDrawable(RectShape()).apply {
                intrinsicWidth = 10.dpToPx()
                paint.color = Color.TRANSPARENT
            }
        }
    }

    /**
     * Sets the [RendererInitializer] handler.
     *
     * @param rendererInitializer Handler for initializing the renderer.
     */
    override fun setRendererInitializer(rendererInitializer: RendererInitializer) {
        childList.forEach { it.setRendererInitializer(rendererInitializer) }
    }

    public constructor(context: Context) : this(context, null)
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
        this(context, attrs, defStyleAttr, 0)

    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context.createStreamThemeWrapper(),
        attrs,
        defStyleAttr,
        defStyleRes
    )

    /**
     * Updates the remote participants. 4 remote participants will be shown at most in a grid. If a new participant
     * joins the call or an old one leaves, a [CallParticipantView] will be added or removed.
     */
    internal fun updateParticipants(participants: List<CallParticipantState>) {
        when {
            childList.size > participants.size -> {
                val diff = childList.size - participants.size
                for (index in 0 until diff) {
                    val view = childList.last()
                    participantsList.removeView(view)
                    childList.remove(view)
                }
            }

            childList.size < participants.size -> {
                val diff = participants.size - childList.size
                for (index in 0 until diff) {
                    val view = buildParticipantView().apply {
                        layoutParams = LinearLayout.LayoutParams(125.dpToPx(), LinearLayout.LayoutParams.MATCH_PARENT)
                    }
                    childList.add(view)
                    participantsList.addView(view)
                }
            }
        }

        childList.forEachIndexed { index, view ->
            val participant = participants[index]
            view.setParticipant(participant)
            view.tag = participant.id
        }
    }

    /**
     * Updates the current primary speaker and shows a border around the primary speaker.
     *
     * @param participant The call participant marked as a primary speaker.
     */
    internal fun updatePrimarySpeaker(participant: CallParticipantState?) {
        childList.forEach {
            it.setActive(it.tag == participant?.id)
        }
    }
}