/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-video-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.video.android.xml.widget.participant

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children
import androidx.core.view.setPadding
import androidx.transition.TransitionManager
import io.getstream.log.StreamLog
import io.getstream.video.android.core.model.CallParticipantState
import io.getstream.video.android.core.model.ScreenSharingSession
import io.getstream.video.android.xml.R
import io.getstream.video.android.xml.databinding.ViewCallParticipantsBinding
import io.getstream.video.android.xml.utils.extensions.dpToPx
import io.getstream.video.android.xml.utils.extensions.dpToPxPrecise
import io.getstream.video.android.xml.utils.extensions.setConstraints
import io.getstream.video.android.xml.utils.extensions.streamThemeInflater
import io.getstream.video.android.xml.widget.control.CallControlsView
import io.getstream.video.android.xml.widget.screenshare.ScreenSharingView
import java.util.UUID

/**
 * Renders the call participants depending on the number of the participants and the call state.
 */
public class CallParticipantsView : ConstraintLayout {

    private val log = StreamLog.getLogger(this::class.java.simpleName)

    private lateinit var style: CallParticipantsStyle

    private val binding = ViewCallParticipantsBinding.inflate(streamThemeInflater, this)

    /**
     * The list of all [CallParticipantView]s for participants whose videos are being rendered.
     */
    private val childList = arrayListOf<CallParticipantView>()

    /**
     * Handler to initialise the renderer.
     */
    private lateinit var rendererInitializer: RendererInitializer

    private var screenSharingSession: ScreenSharingSession? = null
    private val isScreenSharingActive: Boolean
        get() = screenSharingSession != null
    private var screenSharingView: ScreenSharingView? = null

    private var participants: List<CallParticipantState> = emptyList()

    private var localParticipant: FloatingParticipantView? = null

    public constructor(context: Context) : this(context, null)
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        style = CallParticipantsStyle(context, attrs)
    }

    /**
     * Sets the [RendererInitializer] handler.
     *
     * @param rendererInitializer Handler for initializing the renderer.
     */
    public fun setRendererInitializer(rendererInitializer: RendererInitializer) {
        this.rendererInitializer = rendererInitializer
        childList.forEach {
            it.setRendererInitializer(rendererInitializer)
        }
        screenSharingView?.setRendererInitializer(rendererInitializer)
        localParticipant?.setRendererInitializer(rendererInitializer)
    }

    /**
     * Updates the participants which are to be rendered on the screen. Up to 4 remote participants view will be shown
     * at any time. In case a new participant comes in or an old one leaves will add/remove [CallParticipantView] for
     * that participant and will automatically arrange the views to fit inside the viewport. The local participant will
     * be overlaid over the remote participants in a floating  view.
     *
     * @param participants The list of the participants in the current call.
     */
    public fun updateContent(participants: List<CallParticipantState>, screenSharingSession: ScreenSharingSession?) {
        this.participants = participants
        this.screenSharingSession = screenSharingSession

        if (isScreenSharingActive) {
            enterScreenSharing()
            updateGridParticipants(participants)
            updateFloatingParticipant(null)
        } else {
            exitScreenSharing()
            if (participants.size == 1 || participants.size == 4) {
                updateGridParticipants(participants)
                updateFloatingParticipant(null)
            } else {
                updateGridParticipants(participants.filter { !it.isLocal })
                updateFloatingParticipant(participants.firstOrNull { it.isLocal })
            }
        }
    }

    private fun enterScreenSharing() {
        if (localParticipant != null) {
            removeView(localParticipant)
            localParticipant = null
        }

        if (screenSharingView == null) {
            screenSharingView = ScreenSharingView(context).apply {
                id = UUID.randomUUID().hashCode()
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            screenSharingView?.setRendererInitializer(rendererInitializer)
            binding.screenShareHolder.addView(screenSharingView)
        }
        screenSharingSession?.let { screenSharingView?.setScreenSharingSession(it) }
    }

    private fun exitScreenSharing() {
        screenSharingView?.let {
            binding.screenShareHolder.removeView(it)
            screenSharingView = null
        }
    }

    /**
     * Creates and updates the local participant floating view.
     *
     * @param participant The local participant to be shown in a [FloatingParticipantView].
     */
    private fun updateFloatingParticipant(participant: CallParticipantState?) {
        if (participant != null) {
            if (localParticipant == null) {
                localParticipant = FloatingParticipantView(context)
                if (::rendererInitializer.isInitialized) localParticipant?.setRendererInitializer(rendererInitializer)
                localParticipant?.let { localParticipant ->
                    localParticipant.id = UUID.randomUUID().hashCode()
                    localParticipant.layoutParams =
                        LayoutParams(style.localParticipantWidth.toInt(), style.localParticipantHeight.toInt())
                    localParticipant.radius = style.localParticipantRadius
                    localParticipant.translationX = calculateFloatingParticipantMaxXOffset()
                    localParticipant.translationY = style.localParticipantPadding
                    setLocalParticipantDragInteraction(localParticipant)
                    addView(localParticipant)
                }
            }
            localParticipant?.setParticipant(participant)
        } else if (localParticipant != null) {
            removeView(localParticipant)
            localParticipant = null
        }
        localParticipant?.bringToFront()
    }

    /**
     * Sets the touch listener to the [FloatingParticipantView] showing the local user to enable dragging the view.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setLocalParticipantDragInteraction(localParticipant: FloatingParticipantView) {
        val maxDx = calculateFloatingParticipantMaxXOffset()
        val maxDy = calculateFloatingParticipantMaxYOffset()

        var dx = 0f
        var dy = 0f
        localParticipant.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dx = view.x - event.rawX
                    dy = view.y - event.rawY
                    return@setOnTouchListener true
                }

                MotionEvent.ACTION_MOVE -> {
                    val newX = event.rawX + dx
                    val newY = event.rawY + dy

                    view.animate().x(newX.coerceIn(style.localParticipantPadding, maxDx))
                        .y(newY.coerceIn(style.localParticipantPadding, maxDy)).setDuration(0).start()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    /**
     * Calculates the max X offset that can be applied to the overlaid [FloatingParticipantView] so that it can only be
     * dragged inside this view accounting for the padding.
     *
     * @return The max X offset that can be applied to the overlaid [FloatingParticipantView].
     */
    private fun calculateFloatingParticipantMaxXOffset(): Float {
        return measuredWidth - style.localParticipantWidth - style.localParticipantPadding
    }

    /**
     * Calculates the max Y offset that can be applied to the overlaid [FloatingParticipantView] so that it can only be
     * dragged inside this view accounting for the padding.
     *
     * @return The max Y offset that can be applied to the overlaid [FloatingParticipantView].
     */
    private fun calculateFloatingParticipantMaxYOffset(): Float {
        val controlsHeight = getCallControlsHeight()
        return measuredHeight - style.localParticipantHeight - style.localParticipantPadding - controlsHeight
    }

    /**
     * Gets the [CallControlsView] height from the parent screen if there is any.
     *
     * @return The height of the [CallControlsView].
     */
    private fun getCallControlsHeight(): Int {
        return (parent as? ViewGroup)?.children?.firstOrNull { it is CallControlsView }?.measuredHeight ?: 0
    }

    /**
     * Updates the remote participants. 4 remote participants will be shown at most in a grid. If a new participant
     * joins the call or an old one leaves, a [CallParticipantView] will be added or removed.
     */
    private fun updateGridParticipants(participants: List<CallParticipantState>) {
        val newParticipants = participants.filter { callParticipant ->
            !childList.any { it.tag == callParticipant.id }
        }
        val removedParticipants = childList.filter { participantView ->
            !participants.any { it.id == participantView.tag }
        }

        removedParticipants.forEach { participantView ->
            childList.remove(participantView)
            binding.participantsHolder.removeView(participantView)
        }

        participants.forEach { participant ->
            val participantView = if (newParticipants.contains(participant)) {
                buildParticipantView(participant.id).also {
                    if (::rendererInitializer.isInitialized) it.setRendererInitializer(rendererInitializer)
                    childList.add(it)
                    binding.participantsHolder.addView(it)
                }
            } else {
                childList.first { it.tag == participant.id }
            }
            participantView.setParticipant(participant)
        }
        childList.sortBy { participants.map { it.id }.indexOf(it.tag) }

        updateConstraints()
    }

    /**
     * Updates the current primary speaker and shows a border around the primary speaker.
     *
     * @param participant The call participant marked as a primary speaker.
     */
    public fun updatePrimarySpeaker(participant: CallParticipantState?) {
        childList.forEach {
            it.setActive(it.tag == participant?.id)
        }
    }

    /**
     * Updates the constraints of the shown [CallParticipantView]s so they all fit in the viewport.
     */
    private fun updateConstraints() {
        TransitionManager.beginDelayedTransition(this)

        val participantsHolderParams = (binding.participantsHolder.layoutParams as LayoutParams)
        participantsHolderParams.height = if (isScreenSharingActive) 125.dpToPx() else LayoutParams.MATCH_PARENT
        participantsHolderParams.bottomMargin = if (isScreenSharingActive) calculateOffsetWithControls() else 0
        binding.participantsHolder.layoutParams = participantsHolderParams
        binding.participantsHolder.setPadding(if (isScreenSharingActive) 8.dpToPx() else 0)

        binding.participantsHolder.setConstraints {
            if (isScreenSharingActive) {
                childList.forEachIndexed { index, view ->
                    if (index == 0) {
                        toParentStart(view, childList.getOrNull(1))
                    }
                    if (index in 1 until childList.lastIndex) {
                        toViewEnd(view, childList[index - 1])
                        toViewStart(view, childList[index + 1])
                    }
                    if (index == childList.lastIndex) {
                        toParentEnd(view, childList.getOrNull(index - 1))
                    }
                    setDimensionRatio(view.id, "1")
                    if (index == 0) setHorizontalBias(view.id, 0f)

                    val viewLayoutParams = view.layoutParams as LayoutParams
                    if (index != 0) setMargin(view.id, ConstraintSet.START, 4.dpToPx())
                    if (index != childList.lastIndex) setMargin(view.id, ConstraintSet.END, 4.dpToPx())
                    view.layoutParams = viewLayoutParams
                    view.setLabelBottomOffset(0)
                }
            } else {
                childList.forEach { view ->
                    val viewLayoutParams = view.layoutParams as LayoutParams
                    viewLayoutParams.marginStart = 0
                    viewLayoutParams.marginEnd = 0
                    view.layoutParams = viewLayoutParams
                    if (isBottomChild(view)) view.setLabelBottomOffset(getCallControlsHeight())
                }

                when (childList.size) {
                    1 -> {
                        toParent(childList[0])
                    }
                    2 -> {
                        toTop(childList[0])
                        toBottom(childList[1])
                    }
                    3 -> {
                        toTopStart(childList[0])
                        toTopEnd(childList[1])
                        toBottom(childList[2])
                    }
                    4 -> {
                        toTopStart(childList[0])
                        toTopEnd(childList[1])
                        toBottomStart(childList[2])
                        toBottomEnd(childList[3])
                    }
                }
            }
        }
    }

    private fun isBottomChild(view: CallParticipantView): Boolean {
        return when {
            childList.size == 1 -> true
            childList.size == 2 && childList.indexOf(view) == 1 -> true
            childList.size > 2 && childList.indexOf(view) > 1 -> true
            else -> false
        }
    }

    private fun calculateOffsetWithControls(): Int {
        return getCallControlsHeight()
    }

    /**
     * Used to instantiate a new [CallParticipantView] when participants join the call.
     */
    private fun buildParticipantView(userId: String): CallParticipantView {
        return CallParticipantView(
            context = context,
            attrs = null,
            defStyleAttr = R.attr.streamCallParticipantsCallParticipantStyle,
            defStyleRes = style.callParticipantStyle
        ).apply {
            this.id = View.generateViewId()
            this.tag = userId
            this.layoutParams = LayoutParams(
                LayoutParams.MATCH_CONSTRAINT, LayoutParams.MATCH_CONSTRAINT
            )
        }
    }

    /**
     * Ease of use functions that help us constraint views to certain parts of the screen when new participants enter
     * or old one leave the call.
     */
    private fun ConstraintSet.toParent(target: View) {
        connect(target.id, ConstraintSet.START, LayoutParams.PARENT_ID, ConstraintSet.START)
        connect(target.id, ConstraintSet.TOP, LayoutParams.PARENT_ID, ConstraintSet.TOP)
        connect(target.id, ConstraintSet.END, LayoutParams.PARENT_ID, ConstraintSet.END)
        connect(target.id, ConstraintSet.BOTTOM, LayoutParams.PARENT_ID, ConstraintSet.BOTTOM)
    }

    private fun ConstraintSet.toTop(target: View) {
        connect(target.id, ConstraintSet.START, LayoutParams.PARENT_ID, ConstraintSet.START)
        connect(target.id, ConstraintSet.TOP, LayoutParams.PARENT_ID, ConstraintSet.TOP)
        connect(target.id, ConstraintSet.END, LayoutParams.PARENT_ID, ConstraintSet.END)
        connect(target.id, ConstraintSet.BOTTOM, binding.horizontalGuideline.id, ConstraintSet.TOP)
    }

    private fun ConstraintSet.toBottom(target: View) {
        connect(target.id, ConstraintSet.START, LayoutParams.PARENT_ID, ConstraintSet.START)
        connect(target.id, ConstraintSet.TOP, binding.horizontalGuideline.id, ConstraintSet.BOTTOM)
        connect(target.id, ConstraintSet.END, LayoutParams.PARENT_ID, ConstraintSet.END)
        connect(target.id, ConstraintSet.BOTTOM, LayoutParams.PARENT_ID, ConstraintSet.BOTTOM)
    }

    private fun ConstraintSet.toTopStart(target: View) {
        connect(target.id, ConstraintSet.START, LayoutParams.PARENT_ID, ConstraintSet.START)
        connect(target.id, ConstraintSet.TOP, LayoutParams.PARENT_ID, ConstraintSet.TOP)
        connect(target.id, ConstraintSet.END, binding.verticalGuideline.id, ConstraintSet.START)
        connect(target.id, ConstraintSet.BOTTOM, binding.horizontalGuideline.id, ConstraintSet.TOP)
    }

    private fun ConstraintSet.toTopEnd(target: View) {
        connect(target.id, ConstraintSet.START, binding.verticalGuideline.id, ConstraintSet.END)
        connect(target.id, ConstraintSet.TOP, LayoutParams.PARENT_ID, ConstraintSet.TOP)
        connect(target.id, ConstraintSet.END, LayoutParams.PARENT_ID, ConstraintSet.END)
        connect(target.id, ConstraintSet.BOTTOM, binding.horizontalGuideline.id, ConstraintSet.TOP)
    }

    private fun ConstraintSet.toBottomStart(target: View) {
        connect(target.id, ConstraintSet.START, LayoutParams.PARENT_ID, ConstraintSet.START)
        connect(target.id, ConstraintSet.TOP, binding.horizontalGuideline.id, ConstraintSet.BOTTOM)
        connect(target.id, ConstraintSet.END, binding.verticalGuideline.id, ConstraintSet.START)
        connect(target.id, ConstraintSet.BOTTOM, LayoutParams.PARENT_ID, ConstraintSet.BOTTOM)
    }

    private fun ConstraintSet.toBottomEnd(target: View) {
        connect(target.id, ConstraintSet.START, binding.verticalGuideline.id, ConstraintSet.END)
        connect(target.id, ConstraintSet.TOP, binding.horizontalGuideline.id, ConstraintSet.BOTTOM)
        connect(target.id, ConstraintSet.END, LayoutParams.PARENT_ID, ConstraintSet.END)
        connect(target.id, ConstraintSet.BOTTOM, LayoutParams.PARENT_ID, ConstraintSet.BOTTOM)
    }

    private fun ConstraintSet.toParentStart(target: View, endView: View?) {
        connect(target.id, ConstraintSet.START, LayoutParams.PARENT_ID, ConstraintSet.START)
        connect(target.id, ConstraintSet.TOP, LayoutParams.PARENT_ID, ConstraintSet.TOP)
        connect(
            target.id,
            ConstraintSet.END,
            endView?.id ?: LayoutParams.PARENT_ID,
            if (endView != null) ConstraintSet.START else ConstraintSet.END
        )
        connect(target.id, ConstraintSet.BOTTOM, LayoutParams.PARENT_ID, ConstraintSet.BOTTOM)
    }

    private fun ConstraintSet.toParentEnd(target: View, startView: View?) {
        connect(
            target.id,
            ConstraintSet.START,
            startView?.id ?: LayoutParams.PARENT_ID,
            if (startView != null) ConstraintSet.END else ConstraintSet.START
        )
        connect(target.id, ConstraintSet.TOP, LayoutParams.PARENT_ID, ConstraintSet.TOP)
        connect(target.id, ConstraintSet.END, LayoutParams.PARENT_ID, ConstraintSet.END)
        connect(target.id, ConstraintSet.BOTTOM, LayoutParams.PARENT_ID, ConstraintSet.BOTTOM)
    }

    private fun ConstraintSet.toViewEnd(target: View, endView: View) {
        connect(target.id, ConstraintSet.TOP, LayoutParams.PARENT_ID, ConstraintSet.TOP)
        connect(target.id, ConstraintSet.START, endView.id, ConstraintSet.END)
        connect(target.id, ConstraintSet.BOTTOM, LayoutParams.PARENT_ID, ConstraintSet.BOTTOM)
    }

    private fun ConstraintSet.toViewStart(target: View, startView: View) {
        connect(target.id, ConstraintSet.TOP, LayoutParams.PARENT_ID, ConstraintSet.TOP)
        connect(target.id, ConstraintSet.END, startView.id, ConstraintSet.START)
        connect(target.id, ConstraintSet.BOTTOM, LayoutParams.PARENT_ID, ConstraintSet.BOTTOM)
    }
}
