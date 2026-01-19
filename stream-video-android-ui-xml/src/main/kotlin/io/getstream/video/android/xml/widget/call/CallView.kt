/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.xml.widget.call

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.xml.R
import io.getstream.video.android.xml.font.setTextStyle
import io.getstream.video.android.xml.utils.extensions.clearConstraints
import io.getstream.video.android.xml.utils.extensions.constrainViewBottomToTopOfView
import io.getstream.video.android.xml.utils.extensions.constrainViewEndToStartOfView
import io.getstream.video.android.xml.utils.extensions.constrainViewToParent
import io.getstream.video.android.xml.utils.extensions.constrainViewToParentBySide
import io.getstream.video.android.xml.utils.extensions.constrainViewTopToBottomOfView
import io.getstream.video.android.xml.utils.extensions.getFirstViewInstance
import io.getstream.video.android.xml.utils.extensions.isLandscape
import io.getstream.video.android.xml.utils.extensions.setConstraints
import io.getstream.video.android.xml.utils.extensions.updateConstraints
import io.getstream.video.android.xml.widget.control.CallControlsView
import io.getstream.video.android.xml.widget.participant.CallParticipantView
import io.getstream.video.android.xml.widget.participant.FloatingParticipantView
import io.getstream.video.android.xml.widget.participant.internal.CallParticipantsGridView
import io.getstream.video.android.xml.widget.participant.internal.CallParticipantsListView
import io.getstream.video.android.xml.widget.screenshare.ScreenShareView
import io.getstream.video.android.xml.widget.view.CallConstraintLayout
import java.util.UUID

/**
 * Renders the call participants depending on the number of the participants and the call state.
 */
public class CallView : CallConstraintLayout {

    private lateinit var style: CallViewStyle

    public constructor(context: Context) : this(context, null)
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        style = CallViewStyle(context, attrs)

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

    /**
     * Shows a placeholder before any participant view has been added.
     */
    private fun showPreConnectedHolder() {
        if (getFirstViewInstance<ImageView>() != null) return
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

    /**
     * Adds a [CallControlsView] view to the view hierarchy.
     */
    private fun addCallControlsView() {
        if (getFirstViewInstance<CallControlsView>() != null) return

        CallControlsView(context).apply {
            id = ViewGroup.generateViewId()
            this@CallView.addView(this)
        }

        updateCallControlsConstraints()
    }

    /**
     * Updates the constraints of the [CallControlsView].
     */
    private fun updateCallControlsConstraints() {
        val callControlsView = getFirstViewInstance<CallControlsView>() ?: return

        val appBarWidth: Int
        val appBarHeight: Int
        val constraintSide: Int

        if (isLandscape) {
            appBarWidth = style.callControlsWidthLandscape
            appBarHeight = LayoutParams.MATCH_PARENT
            constraintSide = ConstraintSet.END
        } else {
            appBarWidth = LayoutParams.MATCH_PARENT
            appBarHeight = style.callControlsHeight
            constraintSide = ConstraintSet.BOTTOM
        }

        updateConstraints {
            clearConstraints(callControlsView)
            constrainViewToParentBySide(callControlsView, constraintSide)
        }
        callControlsView.updateLayoutParams {
            width = appBarWidth
            height = appBarHeight
        }
    }

    /**
     * Populates the view with the screen share content. Will remove all views that are used when there is no screen
     * share content and add [ScreenShareView] and [CallParticipantsListView].
     *
     * @param onViewInitialized Notifies when a new [ScreenShareView] or [CallParticipantsListView] has been initialized
     * so that they can be bound to the view model.
     */
    public fun setScreenSharingContent(onViewInitialized: (View) -> Unit) {
        if (getFirstViewInstance<ScreenShareView>() != null) return

        children.forEach {
            if (it !is CallControlsView) removeView(it)
        }

        val presenterText = TextView(context).apply {
            id = ViewGroup.generateViewId()
            maxLines = 1
            setTextStyle(style.presenterTextStyle)
            setPadding(style.presenterTextPadding)
            this@CallView.addView(this)
        }

        val screenShareView = ScreenShareView(context).apply {
            id = ViewGroup.generateViewId()
            this@CallView.addView(this)
            onViewInitialized(this)
        }

        val listView = CallParticipantsListView(context).apply {
            id = ViewGroup.generateViewId()
            buildParticipantView = { this@CallView.buildParticipantView(true) }
            this@CallView.addView(this)
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                style.participantListHeight,
            )
            setPadding(style.participantListPadding)
            setItemMargin(style.participantListItemMargin)
            clipToPadding = false
            clipChildren = false
            onViewInitialized(this)
        }

        val callControlsView = getFirstViewInstance<CallControlsView>() ?: return

        updateConstraints {
            constrainViewToParentBySide(presenterText, ConstraintSet.START)
            constrainViewToParentBySide(presenterText, ConstraintSet.END)
            constrainViewToParentBySide(presenterText, ConstraintSet.TOP)

            constrainViewTopToBottomOfView(
                screenShareView,
                presenterText,
                style.presenterTextMargin,
            )
            constrainViewToParentBySide(screenShareView, ConstraintSet.START)
            constrainViewToParentBySide(screenShareView, ConstraintSet.END)
            constrainViewBottomToTopOfView(screenShareView, listView, style.screenShareMargin)

            constrainViewBottomToTopOfView(listView, callControlsView)
            constrainViewToParentBySide(listView, ConstraintSet.START)
            constrainViewToParentBySide(listView, ConstraintSet.END)
        }
    }

    public fun updatePresenterText(text: String) {
        getFirstViewInstance<TextView>()?.text = text
    }

    /**
     * Populates the view with the regular screen content. Will remove all views that are used when a screen share
     * session is active and will add a [CallParticipantsGridView].
     *
     * @param onViewInitialized Notifies when a new [CallParticipantsGridView] has been initialized so that it cen be
     * bound to the view model.
     */
    internal fun setRegularContent(onViewInitialized: (CallParticipantsGridView) -> Unit) {
        if (getFirstViewInstance<CallParticipantsGridView>() != null) return

        children.forEach {
            if (it !is CallControlsView) removeView(it)
        }
        CallParticipantsGridView(context).apply {
            id = ViewGroup.generateViewId()
            isLandscapeListLayout = style.shouldShowGridUsersAsListLandscape
            buildParticipantView = { this@CallView.buildParticipantView(false) }
            this@CallView.addView(this)
            onViewInitialized(this)
        }
        updateRegularContentConstraints()
    }

    private fun updateRegularContentConstraints() {
        val participantsView = getFirstViewInstance<CallParticipantsGridView>() ?: return
        val callControlsView = getFirstViewInstance<CallControlsView>() ?: return

        val participantsWidth: Int
        val participantsHeight: Int

        if (isLandscape) {
            updateConstraints {
                clearConstraints(participantsView)
                constrainViewToParentBySide(participantsView, ConstraintSet.START)
                constrainViewEndToStartOfView(participantsView, callControlsView)
            }

            participantsWidth = LayoutParams.MATCH_CONSTRAINT
            participantsHeight = LayoutParams.MATCH_PARENT
        } else {
            updateConstraints {
                clearConstraints(participantsView)
                constrainViewToParentBySide(participantsView, ConstraintSet.TOP)
                constrainViewBottomToTopOfView(participantsView, callControlsView)
            }
            participantsWidth = LayoutParams.MATCH_PARENT
            participantsHeight = LayoutParams.MATCH_CONSTRAINT
        }

        participantsView.updateLayoutParams {
            width = participantsWidth
            height = participantsHeight
        }
    }

    /**
     * Creates and updates the local participant floating view. If null is passed will remove the view.
     *
     * @param participant The local participant to be shown in a [FloatingParticipantView].
     * @param onViewInitialized Notifies when a new [FloatingParticipantView] has been initialized so that it can be
     * bound to the view model.
     */
    public fun setFloatingParticipant(
        participant: ParticipantState?,
        onViewInitialized: (FloatingParticipantView) -> Unit = {},
    ) {
        if (participant == null) {
            removeView(getFirstViewInstance<FloatingParticipantView>())
            return
        }

        var floatingParticipant = getFirstViewInstance<FloatingParticipantView>()
        if (floatingParticipant == null) {
            floatingParticipant = FloatingParticipantView(context).apply {
                id = UUID.randomUUID().hashCode()
                layoutParams = LayoutParams(
                    style.localParticipantWidth.toInt(),
                    style.localParticipantHeight.toInt(),
                )
                radius = style.localParticipantRadius
                translationX = calculateFloatingParticipantMaxXOffset()
                translationY = style.localParticipantPadding
                setLocalParticipantDragInteraction(this)
                this@CallView.addView(this)
                onViewInitialized(this)
            }
        }

        floatingParticipant.setParticipant(participant)
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
                        .y(
                            newY.coerceIn(style.localParticipantPadding, maxDy),
                        ).setDuration(0).start()
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
        return if (isLandscape) {
            width - style.localParticipantWidth - style.localParticipantPadding - getCallControlsSize()
        } else {
            width - style.localParticipantWidth - style.localParticipantPadding
        }
    }

    /**
     * Calculates the max Y offset that can be applied to the overlaid [FloatingParticipantView] so that it can only be
     * dragged inside this view accounting for the padding.
     *
     * @return The max Y offset that can be applied to the overlaid [FloatingParticipantView].
     */
    private fun calculateFloatingParticipantMaxYOffset(): Float {
        return if (isLandscape) {
            height - style.localParticipantHeight - style.localParticipantPadding
        } else {
            height - style.localParticipantHeight - style.localParticipantPadding - getCallControlsSize()
        }
    }

    /**
     * Returns the [CallControlsView] width if landscape and height if in portrait mode.
     *
     * @return The size of the [CallControlsView] depending on orientation.
     */
    public fun getCallControlsSize(): Int {
        return if (isLandscape) style.callControlsWidthLandscape else style.callControlsHeight
    }

    override fun onOrientationChanged(isLandscape: Boolean) {
        updateCallControlsConstraints()
        updateRegularContentConstraints()
    }

    /**
     * Used to instantiate a new [CallParticipantView] when participants join the call. Will apply different styles
     * whether the view is in the [CallParticipantsGridView] od [CallParticipantsListView].
     *
     * @param isListView True if the view is created for [CallParticipantsListView], false otherwise.
     *
     * @return [CallParticipantView] to be used to render participants.
     */
    private fun buildParticipantView(isListView: Boolean): CallParticipantView {
        val defStyleAttr = if (isListView) {
            R.attr.streamVideoCallViewListParticipantStyle
        } else {
            R.attr.streamVideoCallViewGridParticipantStyle
        }

        val defStyleRes = if (isListView) style.listCallParticipantStyle else style.gridCallParticipantStyle

        return CallParticipantView(
            context = context,
            attrs = null,
            defStyleAttr = defStyleAttr,
            defStyleRes = defStyleRes,
        ).apply {
            this.id = View.generateViewId()
            if (isListView) {
                layoutParams = LinearLayout.LayoutParams(
                    style.participantListItemWidth,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                )
            }
        }
    }

    /**
     * Overridden function from the [ViewGroup] class that notifies when a view has been added to the group. We use it
     * to bring the content we need on top, in our case the [FloatingParticipantView] and [CallControlsView].
     */
    override fun onViewAdded(view: View?) {
        super.onViewAdded(view)
        getFirstViewInstance<FloatingParticipantView>()?.bringToFront()
        getFirstViewInstance<CallControlsView>()?.bringToFront()
    }
}
