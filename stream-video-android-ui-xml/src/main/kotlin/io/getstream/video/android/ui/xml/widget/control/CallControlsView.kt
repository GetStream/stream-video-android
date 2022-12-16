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

package io.getstream.video.android.ui.xml.widget.control

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import io.getstream.video.android.call.state.CallAction
import io.getstream.video.android.call.state.ToggleCamera
import io.getstream.video.android.call.state.ToggleMicrophone
import io.getstream.video.android.call.state.ToggleSpeakerphone
import io.getstream.video.android.ui.xml.R
import io.getstream.video.android.ui.xml.utils.extensions.constrainViewEndToStartOfView
import io.getstream.video.android.ui.xml.utils.extensions.constrainViewStartToEndOfView
import io.getstream.video.android.ui.xml.utils.extensions.constrainViewToParentBySide
import io.getstream.video.android.ui.xml.utils.extensions.getColorCompat
import io.getstream.video.android.ui.xml.utils.extensions.updateConstraints
import io.getstream.video.android.ui.common.R as RCommon

/**
 * Represents the set of controls the user can use to change their audio and video device state, or
 * browse other types of settings, leave the call, or implement something custom.
 */
public class CallControlsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    /**
     * Map of call actions and their corresponding views inside the [CallControlsView].
     */
    private val callControls = mutableMapOf<CallAction, StreamImageButton>()

    /**
     * Handler for call controls click actions.
     */
    public var callControlItemClickListener: (CallAction) -> Unit = { }

    /**
     * Sets the call controls we wish to expose to the users.
     *
     * @param items The list of [CallControlItem]s containing the [CallAction]s we wish to expose to the user.
     */
    public fun setItems(items: List<CallControlItem>) {
        callControls.forEach { removeView(it.value) }
        callControls.clear()
        items.forEach { item ->
            val view = buildControlView(item)
            callControls[item.action] = view
            addView(view)
        }
        defineConstraints()
    }

    /**
     * Adds a new [StreamImageButton] for each [CallControlItem] when [setItems] is called.
     *
     * @param callControlItem The call control item we wish to expose to the user.
     */
    private fun buildControlView(callControlItem: CallControlItem): StreamImageButton {
        val buttonSize = context.resources.getDimension(RCommon.dimen.callControlButtonSize).toInt()
        return StreamImageButton(context).apply {
            id = View.generateViewId()
            tag = callControlItem
            layoutParams = LayoutParams(buttonSize, buttonSize)
            setImageResource(callControlItem.icon)
            setBackgroundResource(R.drawable.bg_call_control_option)
            setColorFilter(context.getColorCompat(callControlItem.iconTint))
            background.setTint(context.getColorCompat(callControlItem.backgroundTint))
            setOnClickListener {
                val data = it.tag as CallControlItem
                when (data.action) {
                    is ToggleCamera -> callControlItemClickListener(ToggleCamera(!data.action.isEnabled))
                    is ToggleMicrophone -> callControlItemClickListener(ToggleMicrophone(!data.action.isEnabled))
                    is ToggleSpeakerphone -> callControlItemClickListener(ToggleSpeakerphone(!data.action.isEnabled))
                    else -> {
                        callControlItemClickListener(data.action)
                    }
                }
            }
        }
    }

    /**
     * Updates the states of views that are currently inside the [CallControlsView].
     *
     * @param items The [CallControlItem] whose state we wish to update.
     */
    public fun updateItems(items: List<CallControlItem>) {
        items.forEach { callControlItem ->
            callControls.keys
                .firstOrNull { it::class == callControlItem.action::class }
                ?.let {
                    val view = callControls[it] ?: return@let
                    view.setImageResource(callControlItem.icon)
                    view.setColorFilter(context.getColorCompat(callControlItem.iconTint))
                    view.background.setTint(context.getColorCompat(callControlItem.backgroundTint))
                    view.tag = callControlItem
                }
        }
    }

    /**
     * Updates constraints of all of the call control views.
     */
    private fun defineConstraints() {
        val controlList = callControls.values.toList()
        updateConstraints {
            controlList.forEachIndexed { index, view ->
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
}
