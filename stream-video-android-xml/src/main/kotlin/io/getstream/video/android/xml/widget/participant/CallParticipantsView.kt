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
import androidx.core.view.children
import io.getstream.log.StreamLog
import io.getstream.video.android.core.model.CallParticipantState
import io.getstream.video.android.core.model.ScreenSharingSession
import io.getstream.video.android.xml.R
import io.getstream.video.android.xml.utils.extensions.constrainViewBottomToTopOfView
import io.getstream.video.android.xml.utils.extensions.constrainViewToParentBySide
import io.getstream.video.android.xml.utils.extensions.dpToPx
import io.getstream.video.android.xml.utils.extensions.setConstraints
import io.getstream.video.android.xml.widget.control.CallControlsView
import io.getstream.video.android.xml.widget.participant.internal.CallParticipantsGridView
import io.getstream.video.android.xml.widget.participant.internal.CallParticipantsListView
import io.getstream.video.android.xml.widget.renderer.VideoRenderer
import io.getstream.video.android.xml.widget.screenshare.ScreenSharingView
import java.util.UUID

/**
 * Renders the call participants depending on the number of the participants and the call state.
 */
public class CallParticipantsView : ConstraintLayout {

    private lateinit var style: CallParticipantsStyle

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
        style = CallParticipantsStyle(context, attrs)
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
     * Updates the participants which are to be rendered on the screen. Up to 4 remote participants view will be shown
     * at any time. In case a new participant comes in or an old one leaves will add/remove [CallParticipantView] for
     * that participant and will automatically arrange the views to fit inside the viewport. The local participant will
     * be overlaid over the remote participants in a floating  view.
     *
     * @param participants The list of the participants in the current call.
     * @param screenSharingSession The currently active screen sharing session if there or null if there is none.
     */
    public fun updateContent(participants: List<CallParticipantState>, screenSharingSession: ScreenSharingSession?) {
        isScreenSharingActive = screenSharingSession != null

        if (isScreenSharingActive) {
            enterScreenSharing()
            screenSharingSession?.let {
                getViewByInstance<ScreenSharingView>()?.setScreenSharingSession(screenSharingSession)
            }
            getViewByInstance<CallParticipantsListView>()?.updateParticipants(participants)
            updateFloatingParticipant(null)
        } else {
            exitScreenSharing()

            val floatingParticipant =
                if (participants.size == 1 || participants.size == 4) null else participants.firstOrNull { it.isLocal }
            val gridParticipants =
                if (participants.size == 1 || participants.size == 4) participants else participants.filter { !it.isLocal }

            updateFloatingParticipant(floatingParticipant)
            getViewByInstance<CallParticipantsGridView>()?.updateParticipants(gridParticipants)
        }
    }

    private fun enterScreenSharing() {
        if (children.firstOrNull { it is ScreenSharingView } != null) return

        removeAllViews()

        val screenShareView = ScreenSharingView(context).apply {
            id = ViewGroup.generateViewId()
            if (::rendererInitializer.isInitialized) setRendererInitializer(rendererInitializer)
            this@CallParticipantsView.addView(this)
        }

        val listView = CallParticipantsListView(context).apply {
            id = ViewGroup.generateViewId()
            if (::rendererInitializer.isInitialized) setRendererInitializer(rendererInitializer)
            buildParticipantView = { this@CallParticipantsView.buildParticipantView() }
            this@CallParticipantsView.addView(this)
        }


        setConstraints {
            constrainViewToParentBySide(screenShareView, ConstraintSet.TOP)
            constrainViewToParentBySide(screenShareView, ConstraintSet.START)
            constrainViewToParentBySide(screenShareView, ConstraintSet.END)
            constrainViewBottomToTopOfView(screenShareView, listView)

            constrainViewToParentBySide(listView, ConstraintSet.BOTTOM, getCallControlsHeight())
            constrainViewToParentBySide(listView, ConstraintSet.START)
            constrainViewToParentBySide(listView, ConstraintSet.END)
        }

        val listViewParams = listView.layoutParams as LayoutParams
        listViewParams.height = 125.dpToPx()
        listView.layoutParams = listViewParams
    }

    private fun exitScreenSharing() {
        if (children.firstOrNull { it is CallParticipantsGridView } != null) return

        StreamLog.d("pleaseShow") { "exit screen sharing and remove all views" }

        removeAllViews()

        val gridView = CallParticipantsGridView(context).apply {
            id = ViewGroup.generateViewId()
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            if (::rendererInitializer.isInitialized) setRendererInitializer(rendererInitializer)
            buildParticipantView = { this@CallParticipantsView.buildParticipantView() }
            callControlsHeight = { this@CallParticipantsView.getCallControlsHeight() }
        }
        addView(gridView)
    }

    /**
     * Creates and updates the local participant floating view.
     *
     * @param participant The local participant to be shown in a [FloatingParticipantView].
     */
    private fun updateFloatingParticipant(participant: CallParticipantState?) {
        var localParticipant = children.find { it is FloatingParticipantView } as? FloatingParticipantView

        if (participant != null) {
            if (localParticipant == null) {
                localParticipant = buildFloatingView()
                addView(localParticipant)
            }
            localParticipant.setParticipant(participant)
        } else if (localParticipant != null) {
            removeView(localParticipant)
        }
        localParticipant?.bringToFront()
    }

    private fun buildFloatingView(): FloatingParticipantView {
        return FloatingParticipantView(context).apply {
            if (::rendererInitializer.isInitialized) setRendererInitializer(rendererInitializer)
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
     * Calculates the margin necessary to push the participant list above the call controls.
     *
     * @return The offset of the participants list.
     */
    private fun getParticipantListOffset(): Int {
        return getCallControlsHeight()
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
     * Used to instantiate a new [CallParticipantView] when participants join the call.
     */
    private fun buildParticipantView(): CallParticipantView {
        return CallParticipantView(
            context = context,
            attrs = null,
            defStyleAttr = R.attr.streamCallParticipantsCallParticipantStyle,
            defStyleRes = style.callParticipantStyle
        ).apply {
            this.id = View.generateViewId()
            if (::rendererInitializer.isInitialized) setRendererInitializer(rendererInitializer)
        }
    }

    private inline fun <reified T : View> getViewByInstance(): T? {
        return children.firstOrNull { it is T } as? T
    }
}
