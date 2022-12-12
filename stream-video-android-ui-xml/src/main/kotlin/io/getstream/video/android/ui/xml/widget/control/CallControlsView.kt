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
) : ConstraintLayout(context, attrs, defStyleAttr), View.OnClickListener {

    private val logger = StreamLog.getLogger("Call:ControlsView")
    private val controlList = arrayListOf<StreamImageButton>()

    private var listener: OnControlItemClickListener? = null

    public fun setOnControlItemClickListener(listener: OnControlItemClickListener) {
        this.listener = listener
    }

    public fun setItems(items: List<CallControlItem>) {
        logger.d { "[setItems] items: $items" }
        controlList.forEach { removeView(it) }
        controlList.clear()
        val buttonSize = context.resources.getDimension(RCommon.dimen.callControlButtonSize).toInt()
        items.forEach { item ->
            val view = StreamImageButton(context).apply {
                id = View.generateViewId()
                tag = item
                setOnClickListener(this@CallControlsView)
                layoutParams = LayoutParams(buttonSize, buttonSize)
                scaleType = ImageView.ScaleType.CENTER
                setBackgroundResource(R.drawable.bg_call_control_option)
                setImageResource(item.icon)
            }

            controlList.add(view)
            addView(view)
        }
        defineConstraints(controlList)
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

    override fun onClick(view: View) {
        val item = view.tag as? CallControlItem ?: return
        listener?.onControlItemClick(item)
    }

    public fun interface OnControlItemClickListener {

        public fun onControlItemClick(item: CallControlItem)
    }
}
