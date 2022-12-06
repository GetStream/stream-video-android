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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import io.getstream.log.StreamLog
import io.getstream.video.android.ui.xml.R
import io.getstream.video.android.ui.xml.utils.extensions.constrainViewEndToStartOfView
import io.getstream.video.android.ui.xml.utils.extensions.constrainViewStartToEndOfView
import io.getstream.video.android.ui.xml.utils.extensions.constrainViewToParentBySide
import io.getstream.video.android.ui.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.ui.xml.utils.extensions.updateConstraints
import io.getstream.video.android.ui.common.R as RCommon

public class CallControlsView : ConstraintLayout, View.OnClickListener {

    public constructor(context: Context) : this(context, null)
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context.createStreamThemeWrapper(),
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        init(context, attrs)
    }

    private val logger = StreamLog.getLogger("Call:ControlsView")
    private val controlList = arrayListOf<ImageButton>()

    private var listener: OnControlItemClickListener? = null

    private fun init(context: Context, attrs: AttributeSet?) {
        // TODO
    }

    public fun setOnControlItemClickListener(listener: OnControlItemClickListener) {
        this.listener = listener
    }

    public fun setItems(items: List<CallControlItem>) {
        logger.d { "[setItems] items: $items" }
        controlList.forEach { removeView(it) }
        controlList.clear()
        val buttonSize = context.resources.getDimension(RCommon.dimen.callControlButtonSize).toInt()
        items.forEach { item ->
            val view = ImageButton(context).apply {
                id = View.generateViewId()
                tag = item
                setOnClickListener(this@CallControlsView)
                layoutParams = LayoutParams(buttonSize, buttonSize)
                setBackgroundResource(R.drawable.bg_call_option)
                setImageResource(item.icon)
            }

            // val binding = ViewCallMediaButtonBinding.inflate(inflater, this, true).apply {
            //     root.id = View.generateViewId()
            //     root.tag = item
            //     root.setOnClickListener(this@CallControlsView)
            //     root.layoutParams = LayoutParams(buttonSize, buttonSize)
            //     iconView.setImageResource(item.icon)
            // }
            controlList.add(view)
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
