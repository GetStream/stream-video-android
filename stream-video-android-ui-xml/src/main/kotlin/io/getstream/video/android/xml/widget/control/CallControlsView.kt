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

package io.getstream.video.android.xml.widget.control

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintSet
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.xml.R
import io.getstream.video.android.xml.utils.extensions.constrainViewBottomToTopOfView
import io.getstream.video.android.xml.utils.extensions.constrainViewEndToStartOfView
import io.getstream.video.android.xml.utils.extensions.constrainViewStartToEndOfView
import io.getstream.video.android.xml.utils.extensions.constrainViewToParentBySide
import io.getstream.video.android.xml.utils.extensions.constrainViewTopToBottomOfView
import io.getstream.video.android.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.xml.utils.extensions.getColorCompat
import io.getstream.video.android.xml.utils.extensions.isLandscape
import io.getstream.video.android.xml.utils.extensions.updateConstraints
import io.getstream.video.android.xml.utils.extensions.updateLayoutParams
import io.getstream.video.android.xml.widget.view.CallConstraintLayout

/**
 * Represents the set of controls the user can use to change their audio and video device state, or
 * browse other types of settings, leave the call, or implement something custom.
 */
public class CallControlsView : CallConstraintLayout {

    /**
     * Style of the view.
     */
    private lateinit var style: CallControlsStyle

    /**
     * Map of call actions and their corresponding views inside the [CallControlsView].
     */
    private val callControls = mutableMapOf<CallAction, ControlButtonView>()

    /**
     * Handler for call controls click actions.
     */
    public var onCallAction: (CallAction) -> Unit = { }

    public constructor(context: Context) : this(context, null, 0)
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context.createStreamThemeWrapper(),
        attrs,
        defStyleAttr,
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        style = CallControlsStyle(context, attrs)

        setBackground()
    }

    /**
     * Sets the call controls we wish to expose to the users.
     *
     * @param items The list of [CallControlItem]s containing the [CallAction]s we wish to expose to the user.
     */
    public fun setItems(items: List<CallControlItem>) {
        // Clear views we do not need any more in case some are removed
        callControls.keys.subtract(items.map { it.action }.toSet()).forEach {
            removeView(callControls[it])
            callControls.remove(it)
        }

        // Used to make sorting the controls a little bit easier
        val views = mutableListOf<ControlButtonView>()
        items.forEach { callControlItem ->
            val callControlView = callControls[callControlItem.action] ?: buildControlView(callControlItem)
            callControlView.setImageResource(callControlItem.icon)
            callControlView.setColorFilter(context.getColorCompat(callControlItem.iconTint))
            callControlView.background.setTint(
                context.getColorCompat(callControlItem.backgroundTint),
            )
            callControlView.setOnClickListener {
                if (callControlItem.enabled) {
                    onCallAction(callControlItem.action)
                }
            }
            views.add(callControlView)
        }

        callControls.clear()
        items.forEachIndexed { index, callControlItem ->
            callControls[callControlItem.action] = views[index]
        }

        defineConstraints()
    }

    /**
     * Adds a new [ControlButtonView] for each [CallControlItem] when [setItems] is called.
     *
     * @param callControlItem The call control item we wish to expose to the user.
     *
     * @return The newly created [ControlButtonView]
     */
    private fun buildControlView(callControlItem: CallControlItem): ControlButtonView {
        val callControlButton = ControlButtonView(context).apply {
            id = View.generateViewId()
            tag = callControlItem
            setBackgroundResource(R.drawable.stream_video_bg_call_control_option)
        }
        addView(callControlButton)
        return callControlButton
    }

    override fun onOrientationChanged(isLandscape: Boolean) {
        defineConstraints()
        setBackground()
    }

    /**
     * Updates the background of the [CallControlsView].
     */
    private fun setBackground() {
        background = if (isLandscape) {
            style.callControlsBackgroundLandscape
        } else {
            style.callControlsBackground
        }
        background.setTint(style.callControlsBackgroundColor)
    }

    /**
     * Updates constraints of all of the call control views.
     */
    private fun defineConstraints() {
        val controlList = callControls.values.toList()

        updateConstraints(true) {
            controlList.forEachIndexed { index, view ->
                if (isLandscape) {
                    constrainViewToParentBySide(view, ConstraintSet.START)
                    constrainViewToParentBySide(view, ConstraintSet.END)
                    if (index == 0) {
                        constrainViewToParentBySide(view, ConstraintSet.TOP)
                    }
                    if (index == controlList.lastIndex) {
                        constrainViewToParentBySide(view, ConstraintSet.BOTTOM)
                    }
                    if (index > 0) {
                        val prevBinding = controlList[index - 1]
                        constrainViewTopToBottomOfView(view, prevBinding)
                    }
                    if (index < controlList.lastIndex) {
                        val nextBinding = controlList[index + 1]
                        constrainViewBottomToTopOfView(view, nextBinding)
                    }
                } else {
                    constrainViewToParentBySide(view, ConstraintSet.TOP)
                    constrainViewToParentBySide(view, ConstraintSet.BOTTOM)
                    if (index == 0) {
                        constrainViewToParentBySide(view, ConstraintSet.START)
                    }
                    if (index == controlList.lastIndex) {
                        constrainViewToParentBySide(view, ConstraintSet.END)
                    }
                    if (index > 0) {
                        val prevBinding = controlList[index - 1]
                        constrainViewStartToEndOfView(view, prevBinding)
                    }
                    if (index < controlList.lastIndex) {
                        val nextBinding = controlList[index + 1]
                        constrainViewEndToStartOfView(view, nextBinding)
                    }
                }
            }
        }

        val size = if (isLandscape) {
            style.callControlButtonSizeLandscape
        } else {
            style.callControlButtonSize
        }
        controlList.forEach {
            it.updateLayoutParams {
                width = size
                height = size
            }
        }
    }
}
