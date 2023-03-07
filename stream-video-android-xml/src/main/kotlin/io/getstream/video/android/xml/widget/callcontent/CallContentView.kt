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

package io.getstream.video.android.xml.widget.callcontent

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children
import androidx.core.view.setPadding
import io.getstream.video.android.core.model.CallParticipantState
import io.getstream.video.android.core.model.ScreenSharingSession
import io.getstream.video.android.xml.R
import io.getstream.video.android.xml.font.setTextStyle
import io.getstream.video.android.xml.utils.extensions.constrainViewBottomToTopOfView
import io.getstream.video.android.xml.utils.extensions.constrainViewToParent
import io.getstream.video.android.xml.utils.extensions.constrainViewToParentBySide
import io.getstream.video.android.xml.utils.extensions.constrainViewTopToBottomOfView
import io.getstream.video.android.xml.utils.extensions.getFirstViewInstance
import io.getstream.video.android.xml.utils.extensions.setConstraints
import io.getstream.video.android.xml.utils.extensions.updateConstraints
import io.getstream.video.android.xml.widget.control.CallControlItem
import io.getstream.video.android.xml.widget.control.CallControlsView
import io.getstream.video.android.xml.widget.participant.CallParticipantView
import io.getstream.video.android.xml.widget.participant.FloatingParticipantView
import io.getstream.video.android.xml.widget.participant.RendererInitializer
import io.getstream.video.android.xml.widget.participant.internal.CallParticipantsGridView
import io.getstream.video.android.xml.widget.participant.internal.CallParticipantsListView
import io.getstream.video.android.xml.widget.renderer.VideoRenderer
import io.getstream.video.android.xml.widget.screenshare.ScreenShareView
import io.getstream.video.android.xml.widget.view.JobHolder
import kotlinx.coroutines.Job
import java.util.UUID
import io.getstream.video.android.ui.common.R as RCommon

/**
 * Renders the call participants depending on the number of the participants and the call state.
 */
public class CallContentView : ConstraintLayout, JobHolder {

    private lateinit var style: CallContentStyle

    override val runningJobs: MutableList<Job> = mutableListOf()

    /**
     * Handler to initialise the renderer.
     */
    private lateinit var rendererInitializer: RendererInitializer

    private var isScreenSharingActive: Boolean = false

    public constructor(context: Context) : this(context, null)
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        style = CallContentStyle(context, attrs)

        showPreConnectedHolder()
        addCallControlsView()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            getFirstViewInstance<FloatingParticipantView>()?.apply {
                radius = style.localParticipantRadius
                translationX = calculateFloatingParticipantMaxXOffset()
            }
        }
    }

    private fun showPreConnectedHolder() {
        if (getFirstViewInstance<ImageView> { it.tag == PRECONNECTION_IMAGE_TAG } != null) return
        val preConnectionImage = ImageView(context).apply {
            id = ViewGroup.generateViewId()
            setImageDrawable(style.preConnectionImage)
            scaleType = ImageView.ScaleType.CENTER
        }
        addView(preConnectionImage)
        setConstraints {
            constrainViewToParent(preConnectionImage)
        }
    }

    private fun removePreConnectionHolder() {
        val preConnectionImage = getFirstViewInstance<ImageView> { it.tag == PRECONNECTION_IMAGE_TAG } ?: return
        removeView(preConnectionImage)
    }

    /**
     * Sets the [RendererInitializer] handler.
     *
     * @param rendererInitializer Handler for initializing the renderer.
     */
    public fun setRendererInitializer(rendererInitializer: RendererInitializer) {
        this.rendererInitializer = rendererInitializer
        children.filterIsInstance<VideoRenderer>().forEach { it.setRendererInitializer(rendererInitializer) }
    }

    /**
     * Adds a [CallControlsView] view to the view hierarchy.
     */
    private fun addCallControlsView() {
        if (getFirstViewInstance<CallControlsView>() != null) return

        val callControlsView = CallControlsView(context).apply {
            id = ViewGroup.generateViewId()
            this@CallContentView.addView(this)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, style.callControlsHeight)
        }

        updateConstraints {
            constrainViewToParentBySide(callControlsView, ConstraintSet.BOTTOM)
        }
    }

    /**
     * Updates participants and screen share previews on screen.
     *
     * In case there is no screen share up to 4 participants will be rendered in a grid. The current user will be in the
     * grid if he is the only participant in the call or if there are at least 4 participant, otherwise he is shown in a
     * draggable floating view.
     *
     * When there ia an active screen share session all users are shown in a scrollable list under the preview which
     * is the main focus.
     *
     * @param participants The list of the participants in the current call.
     * @param screenSharingSession The currently active screen sharing session if there or null if there is none.
     */
    public fun updateContent(participants: List<CallParticipantState>, screenSharingSession: ScreenSharingSession?) {
        isScreenSharingActive = screenSharingSession != null
        if (participants.isNotEmpty() || screenSharingSession != null) {
            removePreConnectionHolder()
        } else {
            showPreConnectedHolder()
        }

        if (isScreenSharingActive) {
            setScreenSharingContent()
            screenSharingSession?.let {
                val sharingParticipant = it.participant
                getFirstViewInstance<ScreenShareView>()?.setScreenSharingSession(it)
                getFirstViewInstance<TextView>()?.text = context.getString(
                    RCommon.string.stream_screen_sharing_title,
                    sharingParticipant.name.ifEmpty { sharingParticipant.id }
                )
            }
            getFirstViewInstance<CallParticipantsListView>()?.updateParticipants(participants)
            updateFloatingParticipant(null)
        } else {
            setNormalContent()

            val floatingParticipant =
                if (participants.size == 1 || participants.size == 4) null else participants.firstOrNull { it.isLocal }
            val gridParticipants =
                if (participants.size == 1 || participants.size == 4) participants else participants.filter { !it.isLocal }

            getFirstViewInstance<CallParticipantsGridView>()?.updateParticipants(gridParticipants)
            updateFloatingParticipant(floatingParticipant)
        }
        getFirstViewInstance<CallControlsView>()?.bringToFront()
    }

    /**
     * Populates the view with the screen share content. Will remove all views that are used when there is no screen
     * share content and add [ScreenShareView] and [CallParticipantsListView].
     */
    internal fun setScreenSharingContent() {
        if (getFirstViewInstance<ScreenShareView>() != null) return

        children.forEach {
            if (it !is CallControlsView) {
                removeView(it)
            }
        }

        val presenterText = TextView(context).apply {
            id = ViewGroup.generateViewId()
            maxLines = 1
            setTextStyle(style.presenterTextStyle)
            setPadding(style.presenterTextPadding)
            this@CallContentView.addView(this)
        }

        val screenShareView = ScreenShareView(context).apply {
            id = ViewGroup.generateViewId()
            if (::rendererInitializer.isInitialized) setRendererInitializer(rendererInitializer)
            this@CallContentView.addView(this)
        }

        val listView = CallParticipantsListView(context).apply {
            id = ViewGroup.generateViewId()
            if (::rendererInitializer.isInitialized) setRendererInitializer(rendererInitializer)
            buildParticipantView = { this@CallContentView.buildParticipantView(true) }
            this@CallContentView.addView(this)
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                style.participantListHeight
            )
            setPadding(style.participantListPadding)
            setItemMargin(style.participantListItemMargin)
            clipToPadding = false
            clipChildren = false
        }

        updateConstraints {
            constrainViewToParentBySide(presenterText, ConstraintSet.START)
            constrainViewToParentBySide(presenterText, ConstraintSet.END)
            constrainViewToParentBySide(presenterText, ConstraintSet.TOP)

            constrainViewTopToBottomOfView(screenShareView, presenterText, style.presenterTextMargin)
            constrainViewToParentBySide(screenShareView, ConstraintSet.START)
            constrainViewToParentBySide(screenShareView, ConstraintSet.END)
            constrainViewBottomToTopOfView(screenShareView, listView, style.screenShareMargin)

            constrainViewToParentBySide(listView, ConstraintSet.BOTTOM, getCallControlsHeight())
            constrainViewToParentBySide(listView, ConstraintSet.START)
            constrainViewToParentBySide(listView, ConstraintSet.END)
        }
    }

    /**
     * Populates the view with the regular screen content. Will remove all views that are used when a screen share
     * session is active and will add a [CallParticipantsGridView].
     */
    internal fun setNormalContent() {
        if (getFirstViewInstance<CallParticipantsGridView>() != null) return

        children.forEach {
            if (it !is CallControlsView) {
                removeView(it)
            }
        }

        CallParticipantsGridView(context).apply {
            id = ViewGroup.generateViewId()
            if (::rendererInitializer.isInitialized) setRendererInitializer(rendererInitializer)
            buildParticipantView = { this@CallContentView.buildParticipantView(false) }
            getBottomLabelOffset = { this@CallContentView.getCallControlsHeight() }
            this@CallContentView.addView(this)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
    }

    /**
     * Creates and updates the local participant floating view. If null is passed will remove the view.
     *
     * @param participant The local participant to be shown in a [FloatingParticipantView].
     */
    private fun updateFloatingParticipant(participant: CallParticipantState?) {
        var floatingParticipant = getFirstViewInstance<FloatingParticipantView>()

        if (participant != null) {
            if (floatingParticipant == null) {
                floatingParticipant = buildFloatingView()
                addView(floatingParticipant)
            }
            floatingParticipant.setParticipant(participant)
        } else {
            removeView(floatingParticipant)
        }
        floatingParticipant?.bringToFront()
    }

    /**
     * Builds a [FloatingParticipantView] to be used for the local participant.
     *
     * @return [FloatingParticipantView]
     */
    private fun buildFloatingView(): FloatingParticipantView {
        return FloatingParticipantView(context).apply {
            if (::rendererInitializer.isInitialized) {
                setRendererInitializer(rendererInitializer)
            }
            id = UUID.randomUUID().hashCode()
            layoutParams = LayoutParams(
                style.localParticipantWidth.toInt(),
                style.localParticipantHeight.toInt()
            )
            radius = style.localParticipantRadius
            translationX = calculateFloatingParticipantMaxXOffset()
            translationY = style.localParticipantPadding
            setLocalParticipantDragInteraction(this)
        }
    }

    /**
     * Sets the touch listener to the [FloatingParticipantView] showing the local user to enable dragging the view.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setLocalParticipantDragInteraction(localParticipant: FloatingParticipantView) {
        var dx = 0f
        var dy = 0f
        localParticipant.setOnTouchListener { view, event ->
            val maxDx = calculateFloatingParticipantMaxXOffset()
            val maxDy = calculateFloatingParticipantMaxYOffset()

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dx = view.x - event.rawX
                    dy = view.y - event.rawY
                    return@setOnTouchListener true
                }

                MotionEvent.ACTION_MOVE -> {
                    val newX = event.rawX + dx
                    val newY = event.rawY + dy

                    if (maxDx < style.localParticipantPadding ||
                        maxDy < style.localParticipantPadding
                    ) {
                        return@setOnTouchListener false
                    }

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
        return width - style.localParticipantWidth - style.localParticipantPadding
    }

    /**
     * Calculates the max Y offset that can be applied to the overlaid [FloatingParticipantView] so that it can only be
     * dragged inside this view accounting for the padding.
     *
     * @return The max Y offset that can be applied to the overlaid [FloatingParticipantView].
     */
    private fun calculateFloatingParticipantMaxYOffset(): Float {
        return height - style.localParticipantHeight - style.localParticipantPadding - getCallControlsHeight()
    }

    private fun getCallControlsHeight(): Int {
        return style.callControlsHeight
    }

    /**
     * Updates the current primary speaker and shows a border around the primary speaker.
     *
     * @param participant The call participant marked as a primary speaker.
     */
    public fun updatePrimarySpeaker(participant: CallParticipantState?) {
        children.forEach {
            when (it) {
                is CallParticipantsGridView -> it.updatePrimarySpeaker(participant)
                is CallParticipantsListView -> it.updatePrimarySpeaker(participant)
            }
        }
    }

    /**
     * Sets the Call Controls with which the user can interact.
     *
     * @param items The CallActions wrapped in [CallControlItem]s which we wish to expose to the users.
     */
    public fun setControlItems(items: List<CallControlItem>) {
        getFirstViewInstance<CallControlsView>()?.apply { setItems(items) }
    }

    /**
     * Used to instantiate a new [CallParticipantView] when participants join the call. Will apply different styles
     * whether the view is in the [CallParticipantsGridView] od [CallParticipantsListView].
     *
     * @return [CallParticipantView] to be used to render participants.
     */
    private fun buildParticipantView(isListView: Boolean): CallParticipantView {
        val defStyleAttr = if (isListView) {
            R.attr.streamCallContentListParticipantStyle
        } else {
            R.attr.streamCallContentGridParticipantStyle
        }

        val defStyleRes = if (isListView) style.listCallParticipantStyle else style.gridCallParticipantStyle

        return CallParticipantView(
            context = context,
            attrs = null,
            defStyleAttr = defStyleAttr,
            defStyleRes = defStyleRes
        ).apply {
            this.id = View.generateViewId()
            if (::rendererInitializer.isInitialized) setRendererInitializer(rendererInitializer)
            if (isListView) {
                layoutParams = LinearLayout.LayoutParams(
                    style.participantListItemWidth,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            }
        }
    }

    override fun onDetachedFromWindow() {
        stopAllJobs()
        super.onDetachedFromWindow()
    }

    companion object {
        private const val PRECONNECTION_IMAGE_TAG = "preconeection_image"
    }
}
