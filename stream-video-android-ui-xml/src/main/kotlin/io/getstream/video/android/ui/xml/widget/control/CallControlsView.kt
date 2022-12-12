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
import io.getstream.log.StreamLog
import io.getstream.video.android.call.state.CallAction
import io.getstream.video.android.call.state.ToggleCamera
import io.getstream.video.android.call.state.ToggleMicrophone
import io.getstream.video.android.call.state.ToggleSpeakerphone
import io.getstream.video.android.ui.xml.R
import io.getstream.video.android.ui.xml.utils.extensions.constrainViewEndToStartOfView
import io.getstream.video.android.ui.xml.utils.extensions.constrainViewStartToEndOfView
import io.getstream.video.android.ui.xml.utils.extensions.constrainViewToParentBySide
import io.getstream.video.android.ui.xml.utils.extensions.updateConstraints
import io.getstream.video.android.ui.common.R as RCommon

public class CallControlsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val controlList = mutableMapOf<CallAction, StreamImageButton>()

    public var callControlItemClickListener: (CallAction) -> Unit = { }

    public fun setItems(items: List<CallControlItem>) {
        controlList.forEach { removeView(it.value) }
        controlList.clear()
        items.forEach { item ->
            val view = addControlView(item)
            controlList[item.action] = view
            addView(view)
        }
        defineConstraints(controlList.values.toList())
    }

    private fun addControlView(callControlItem: CallControlItem): StreamImageButton {
        val buttonSize = context.resources.getDimension(RCommon.dimen.callControlButtonSize).toInt()
        return StreamImageButton(context).apply {
            id = View.generateViewId()
            tag = callControlItem
            layoutParams = LayoutParams(buttonSize, buttonSize)
            scaleType = ImageView.ScaleType.CENTER
            setBackgroundResource(R.drawable.bg_call_control_option)
            setImageResource(callControlItem.icon)
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

    public fun updateItems(items: List<CallControlItem>) {
        items.forEach { callControlItem ->
            controlList.keys
                .firstOrNull { it::class == callControlItem.action::class }
                ?.let {
                    val view = controlList[it] ?: return@let
                    view.setImageResource(callControlItem.icon)
                    view.tag = callControlItem
                }
        }
    }

    private fun defineConstraints(controlList: List<ImageButton>) {
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
