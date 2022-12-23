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
import androidx.transition.TransitionManager
import io.getstream.video.android.model.CallParticipantState
import io.getstream.video.android.ui.xml.R
import io.getstream.video.android.ui.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.ui.xml.utils.extensions.updateConstraints

/**
 * Renders the call participants depending on the number of the participants and the call state.
 */
public class CallParticipantsView : ConstraintLayout {

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

    /**
     * The list of all [CallParticipantView]s for participants whose videos are being rendered.
     */
    private val childList = arrayListOf<CallParticipantView>()

    /**
     * Handler to initialise the renderer.
     */
    private lateinit var rendererInitializer: RendererInitializer

    public constructor(context: Context) : this(context, null)
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context,
        attrs,
        defStyleAttr,
        0)

    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context.createStreamThemeWrapper(),
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        style = CallParticipantsStyle(context, attrs)

        addView(verticalGuideline)
        addView(horizontalGuideline)
    }

    /**
     * Sets the [RendererInitializer] handler.
     *
     * @param rendererInitializer Handler for initializing the renderer.
     */
    public fun setRendererInitializer(rendererInitializer: RendererInitializer) {
        this.rendererInitializer = rendererInitializer
        childList.forEach {
            it.setRendererInitializer(rendererInitializer)
        }
    }

    /**
     * Updates the participants which are to be rendered on the screen. Up to 4 participants view will be shown at any
     * time. In case a new participant comes in or an old one leaves will add/remove [CallParticipantView] for that
     * participant and will automatically arrange the views to fit inside the viewport.
     *
     * @param participants The list of the participants in the current call.
     */
    public fun updateParticipants(participants: List<CallParticipantState>) {
        when {
            participants.size > childList.size -> {
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

            participants.size < childList.size -> {
                val surplusViews = childList.filter { !participants.map { it.id }.contains(it.tag) }.toSet()
                childList.removeAll(surplusViews)
                updateConstraints()
                surplusViews.forEach {
                    removeView(it)
                }
            }

            else -> {
                participants.forEach { participant ->
                    childList.firstOrNull { it.tag == participant.id }?.setParticipant(participant)
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
            it.setActive(it.tag == participant?.id)
        }
    }

    /**
     * Updates the constraints of the shown [CallParticipantView]s so they all fit in the viewport.
     */
    private fun updateConstraints() {
        TransitionManager.beginDelayedTransition(this)
        updateConstraints {
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

    private companion object {
        private const val HALF_OF_VIEW = 0.5f
    }
}
