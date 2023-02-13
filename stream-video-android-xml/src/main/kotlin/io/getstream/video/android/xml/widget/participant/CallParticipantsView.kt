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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Guideline
import androidx.core.view.children
import androidx.transition.TransitionManager
import io.getstream.log.StreamLog
import io.getstream.video.android.core.model.CallParticipantState
import io.getstream.video.android.core.model.ScreenSharingSession
import io.getstream.video.android.xml.R
import io.getstream.video.android.xml.utils.extensions.dpToPxPrecise
import io.getstream.video.android.xml.utils.extensions.updateConstraints
import io.getstream.video.android.xml.widget.control.CallControlsView
import io.getstream.video.android.xml.widget.renderer.VideoRenderer
import io.getstream.video.android.xml.widget.screenshare.ScreenSharingView
import java.util.UUID

/**
 * Renders the call participants depending on the number of the participants and the call state.
 */
public class CallParticipantsView : ConstraintLayout {

    private val log = StreamLog.getLogger(this::class.java.simpleName)

    private lateinit var style: CallParticipantsStyle

    /**
     * Guideline that helps constraining the view on half of the screen vertically.
     */
    private val verticalGuideline by lazy {
        buildGuideline(
            orientation = LayoutParams.VERTICAL,
            guidePercent = HALF_OF_VIEW
        )
    }

    /**
     * Guideline that helps constraining the view on half of the screen horizontally.
     */
    private val horizontalGuideline by lazy {
        buildGuideline(
            orientation = LayoutParams.HORIZONTAL,
            guidePercent = HALF_OF_VIEW
        )
    }

    private val screenSharingTopGuideline by lazy {
        buildGuideline(
            orientation = LayoutParams.HORIZONTAL,
            guidePercent = calculateTopGuidePercent()
        )
    }

    private val screenSharingBottomGuideline by lazy {
        buildGuideline(
            orientation = LayoutParams.HORIZONTAL,
            guidePercent = calculateBottomGuidePercent()
        )
    }

    private fun calculateTopGuidePercent(): Float {
        return (measuredHeight - (getCallControlsHeight() + 32.dpToPxPrecise() + 125.dpToPxPrecise())) / measuredHeight
    }

    private fun calculateBottomGuidePercent(): Float {
        return (measuredHeight - getCallControlsHeight().toFloat()) / measuredHeight
    }

    /**
     * The list of all [CallParticipantView]s for participants whose videos are being rendered.
     */
    private val childList = arrayListOf<View>()

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
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        style = CallParticipantsStyle(context, attrs)

        addView(verticalGuideline)
        addView(horizontalGuideline)
        addView(screenSharingTopGuideline)
        addView(screenSharingBottomGuideline)
    }

    /**
     * Sets the [RendererInitializer] handler.
     *
     * @param rendererInitializer Handler for initializing the renderer.
     */
    public fun setRendererInitializer(rendererInitializer: RendererInitializer) {
        this.rendererInitializer = rendererInitializer
        childList.forEach {
            if (it is VideoRenderer) it.setRendererInitializer(rendererInitializer)
        }
    }

    /**
     * Updates the participants which are to be rendered on the screen. Up to 4 remote participants view will be shown
     * at any time. In case a new participant comes in or an old one leaves will add/remove [CallParticipantView] for
     * that participant and will automatically arrange the views to fit inside the viewport. The local participant will
     * be overlaid over the remote participants in a floating  view.
     *
     * @param participants The list of the participants in the current call.
     */
    public fun updateParticipants(participants: List<CallParticipantState>) {
        this.participants = participants

        if (isScreenSharingActive) {
            log.d { "update all participants state" }
            updateParticipantsState(participants)
        } else {
            log.d { "update remote/local participants state" }
            updateParticipantsState(participants.filter { !it.isLocal })
            updateLocalParticipant(participants.firstOrNull { it.isLocal })
        }
    }

    public fun updateScreenSharingSession(screenSharingSession: ScreenSharingSession?) {
        this.screenSharingSession = screenSharingSession
        log.d { "is screen sharing active: $isScreenSharingActive" }
        if (isScreenSharingActive) enterScreenSharing() else exitScreenSharing()
    }

    private fun enterScreenSharing() {
        log.d { "enter screen sharing mode" }
        if (localParticipant != null) {
            removeView(localParticipant)
            localParticipant = null
        }

        if (screenSharingView == null) {
            screenSharingView = ScreenSharingView(context).apply {
                layoutParams = LayoutParams(
                    LayoutParams.MATCH_CONSTRAINT,
                    LayoutParams.MATCH_CONSTRAINT
                )
            }
            screenSharingView?.setRendererInitializer(rendererInitializer)
            addView(screenSharingView)
        }
        screenSharingSession?.let { screenSharingView?.setScreenSharingSession(it) }
        updateParticipantsState(participants)
        updateConstraints()
    }

    private fun exitScreenSharing() {
        log.d { "exit screen sharing mode" }
        removeView(screenSharingView)
        screenSharingView = null
        updateConstraints()
    }

    /**
     * Creates and updates the local participant floating view.
     *
     * @param participant The local participant to be shown in a [FloatingParticipantView].
     */
    private fun updateLocalParticipant(participant: CallParticipantState?) {
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
            log.d { "update local participant" }
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

                    view.animate()
                        .x(newX.coerceIn(style.localParticipantPadding, maxDx))
                        .y(newY.coerceIn(style.localParticipantPadding, maxDy))
                        .setDuration(0)
                        .start()
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
    private fun updateParticipantsState(participants: List<CallParticipantState>) {
        log.d { "update remote state" }
        val participantCount = childList.filterIsInstance<CallParticipantView>().size
        when {
            participants.size > participantCount -> {
                val missingParticipants = participants.filter { !childList.map { it.tag }.contains(it.id) }
                missingParticipants.forEach { participant ->
                    val view = buildParticipantView(participant.id).also { view ->
                        if (::rendererInitializer.isInitialized) view.setRendererInitializer(rendererInitializer)
                        view.setParticipant(participant)
                    }
                    childList.add(view)
                    addView(view)
                }
                updateConstraints()
            }

            participants.size < participantCount -> {
                val surplusViews = childList.filter { !participants.map { it.id }.contains(it.tag) }.toSet()
                childList.removeAll(surplusViews)
                updateConstraints()
                surplusViews.forEach {
                    removeView(it)
                }
            }

            else -> {
                participants.forEach { participant ->
                    (childList.firstOrNull { it.tag == participant.id && it is CallParticipantView } as? CallParticipantView)?.setParticipant(
                        participant
                    )
                }
            }
        }
    }

    /**
     * Updates the current primary speaker and shows a border around the primary speaker.
     *
     * @param participant The call participant marked as a primary speaker.
     */
    public fun updatePrimarySpeaker(participant: CallParticipantState?) {
        childList.forEach {
            if (it is CallParticipantView) it.setActive(it.tag == participant?.id)
        }
    }

    /**
     * Updates the constraints of the shown [CallParticipantView]s so they all fit in the viewport.
     */
    private fun updateConstraints() {
        TransitionManager.beginDelayedTransition(this)
        updateConstraints {
            screenSharingView?.let { constrainScreenSharingView(it) }

            if (isScreenSharingActive) {
                childList.forEachIndexed { index, view ->
                    if (index == 0) {
                        toParentStart(view, childList.getOrNull(1))
                        if (index != childList.lastIndex) toViewEnd(view, childList[1])
                    }
                    if (index in 1 until childList.lastIndex) {
                        toViewEnd(view, childList[index - 1])
                        toViewStart(view, childList[index + 1])
                    }
                    if (index == childList.lastIndex) {
                        if (index != 0) toViewEnd(view, childList[index - 1])
                        toParentEnd(view, childList.getOrNull(index - 1))
                    }
                }
            } else {
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
                LayoutParams.MATCH_CONSTRAINT,
                LayoutParams.MATCH_CONSTRAINT
            )
        }
    }

    /**
     * Used to instantiate a new [Guideline] which help us to divide the screen to sections.
     */
    private fun buildGuideline(orientation: Int, guidePercent: Float) = Guideline(context).apply {
        this.id = View.generateViewId()
        this.layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            this.orientation = orientation
            this.guidePercent = guidePercent
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
        connect(target.id, ConstraintSet.BOTTOM, horizontalGuideline.id, ConstraintSet.BOTTOM)
    }

    private fun ConstraintSet.toBottom(target: View) {
        connect(target.id, ConstraintSet.START, LayoutParams.PARENT_ID, ConstraintSet.START)
        connect(target.id, ConstraintSet.TOP, horizontalGuideline.id, ConstraintSet.TOP)
        connect(target.id, ConstraintSet.END, LayoutParams.PARENT_ID, ConstraintSet.END)
        connect(target.id, ConstraintSet.BOTTOM, LayoutParams.PARENT_ID, ConstraintSet.BOTTOM)
    }

    private fun ConstraintSet.toTopStart(target: View) {
        connect(target.id, ConstraintSet.START, LayoutParams.PARENT_ID, ConstraintSet.START)
        connect(target.id, ConstraintSet.TOP, LayoutParams.PARENT_ID, ConstraintSet.TOP)
        connect(target.id, ConstraintSet.END, verticalGuideline.id, ConstraintSet.END)
        connect(target.id, ConstraintSet.BOTTOM, horizontalGuideline.id, ConstraintSet.BOTTOM)
    }

    private fun ConstraintSet.toTopEnd(target: View) {
        connect(target.id, ConstraintSet.START, verticalGuideline.id, ConstraintSet.START)
        connect(target.id, ConstraintSet.TOP, LayoutParams.PARENT_ID, ConstraintSet.TOP)
        connect(target.id, ConstraintSet.END, LayoutParams.PARENT_ID, ConstraintSet.END)
        connect(target.id, ConstraintSet.BOTTOM, horizontalGuideline.id, ConstraintSet.BOTTOM)
    }

    private fun ConstraintSet.toBottomStart(target: View) {
        connect(target.id, ConstraintSet.START, LayoutParams.PARENT_ID, ConstraintSet.START)
        connect(target.id, ConstraintSet.TOP, horizontalGuideline.id, ConstraintSet.TOP)
        connect(target.id, ConstraintSet.END, verticalGuideline.id, ConstraintSet.END)
        connect(target.id, ConstraintSet.BOTTOM, LayoutParams.PARENT_ID, ConstraintSet.BOTTOM)
    }

    private fun ConstraintSet.toBottomEnd(target: View) {
        connect(target.id, ConstraintSet.START, verticalGuideline.id, ConstraintSet.START)
        connect(target.id, ConstraintSet.TOP, horizontalGuideline.id, ConstraintSet.TOP)
        connect(target.id, ConstraintSet.END, LayoutParams.PARENT_ID, ConstraintSet.END)
        connect(target.id, ConstraintSet.BOTTOM, LayoutParams.PARENT_ID, ConstraintSet.BOTTOM)
    }

    private fun ConstraintSet.toParentStart(target: View, endView: View?) {
        connect(target.id, ConstraintSet.START, LayoutParams.PARENT_ID, ConstraintSet.START)
        connect(target.id, ConstraintSet.TOP, screenSharingTopGuideline.id, ConstraintSet.TOP)
        endView?.let { connect(target.id, ConstraintSet.END, it.id, ConstraintSet.END) }
        connect(target.id, ConstraintSet.BOTTOM, screenSharingBottomGuideline.id, ConstraintSet.BOTTOM)
    }

    private fun ConstraintSet.toParentEnd(target: View, startView: View?) {
        startView?.let { connect(target.id, ConstraintSet.START, it.id, ConstraintSet.START) }
        connect(target.id, ConstraintSet.TOP, screenSharingTopGuideline.id, ConstraintSet.TOP)
        connect(target.id, ConstraintSet.END, LayoutParams.PARENT_ID, ConstraintSet.END)
        connect(target.id, ConstraintSet.BOTTOM, screenSharingBottomGuideline.id, ConstraintSet.BOTTOM)
    }

    private fun ConstraintSet.toViewEnd(target: View, endView: View) {
        connect(target.id, ConstraintSet.TOP, screenSharingTopGuideline.id, ConstraintSet.TOP)
        connect(target.id, ConstraintSet.START, endView.id, ConstraintSet.END)
        connect(target.id, ConstraintSet.BOTTOM, screenSharingBottomGuideline.id, ConstraintSet.BOTTOM)
    }

    private fun ConstraintSet.toViewStart(target: View, startView: View) {
        connect(target.id, ConstraintSet.TOP, screenSharingTopGuideline.id, ConstraintSet.TOP)
        connect(target.id, ConstraintSet.END, startView.id, ConstraintSet.START)
        connect(target.id, ConstraintSet.BOTTOM, screenSharingBottomGuideline.id, ConstraintSet.BOTTOM)
    }

    private fun ConstraintSet.constrainScreenSharingView(target: View) {
        connect(target.id, ConstraintSet.START, LayoutParams.PARENT_ID, ConstraintSet.START)
        connect(target.id, ConstraintSet.TOP, LayoutParams.PARENT_ID, ConstraintSet.TOP)
        connect(target.id, ConstraintSet.END, LayoutParams.PARENT_ID, ConstraintSet.END)
        connect(target.id, ConstraintSet.BOTTOM, screenSharingTopGuideline.id, ConstraintSet.BOTTOM)
    }

    private companion object {
        private const val HALF_OF_VIEW = 0.5f
    }
}
