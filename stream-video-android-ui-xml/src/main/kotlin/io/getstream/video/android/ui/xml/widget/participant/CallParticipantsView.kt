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

package io.getstream.video.android.ui.xml.widget.participant

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Guideline
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import io.getstream.log.StreamLog
import io.getstream.video.android.model.CallParticipantState
import io.getstream.video.android.ui.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.ui.xml.utils.extensions.updateConstraints

public class CallParticipantsView : ConstraintLayout {

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

    private val logger = StreamLog.getLogger("Call:ParticipantsView")

    private val verticalGuideline by lazy {
        context.buildGuideline(
            orientation = LayoutParams.VERTICAL,
            guidePercent = HALF_OF_VIEW
        )
    }

    private val horizontalGuideline by lazy {
        context.buildGuideline(
            orientation = LayoutParams.HORIZONTAL,
            guidePercent = HALF_OF_VIEW
        )
    }

    private val childList = arrayListOf(
        context.buildParticipantView(),
        context.buildParticipantView(),
        context.buildParticipantView(),
        context.buildParticipantView()
    )

    private fun init(context: Context, attrs: AttributeSet?) {
        addView(verticalGuideline)
        addView(horizontalGuideline)

        childList.forEach { addView(it) }
    }

    public fun setRendererInitializer(rendererInitializer: RendererInitializer) {
        logger.i { "[setRendererInitializer] rendererInitializer: $rendererInitializer" }
        childList.forEach {
            it.setRendererInitializer(rendererInitializer)
        }
    }

    public fun setParticipants(participants: List<CallParticipantState>) {
        logger.i { "[setParticipants] participants: $participants" }
        show(participants.size)
        participants.take(4).forEachIndexed { index, state ->
            childList.getOrNull(index)?.also { participantView ->
                participantView.isVisible = true
                participantView.setData(state.profileImageURL.orEmpty(), state.name.ifEmpty { state.id })
                state.track?.also {
                    participantView.setTrack(it)
                }
            }
        }
    }

    private fun show(count: Int) {
        logger.i { "[show] count: $count" }
        TransitionManager.beginDelayedTransition(this)
        childList.forEachIndexed { index, view ->
            val isVisible = index < count
            logger.v { "[show] index: $index, isVisible: $isVisible" }
            view.isVisible = isVisible
        }
        updateConstraints {
            when (count) {
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

    private companion object {
        private const val HALF_OF_VIEW = 0.5f
        private const val MAX_CHILD_COUNT = 4
    }
}

private fun Context.buildParticipantView(): CallParticipantView {
    return CallParticipantView(this).apply {
        this.id = View.generateViewId()
        this.layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        )
        this.isVisible = false
    }
}

private fun Context.buildGuideline(orientation: Int, guidePercent: Float) = Guideline(this).apply {
    this.id = View.generateViewId()
    this.layoutParams = ConstraintLayout.LayoutParams(
        ConstraintLayout.LayoutParams.WRAP_CONTENT,
        ConstraintLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        this.orientation = orientation
        this.guidePercent = guidePercent
    }
}
