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

package io.getstream.video.android.xml.widget.participant.internal

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.TransitionManager
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.xml.utils.extensions.constrainViewBottomToTopOfView
import io.getstream.video.android.xml.utils.extensions.constrainViewEndToStartOfView
import io.getstream.video.android.xml.utils.extensions.constrainViewStartToEndOfView
import io.getstream.video.android.xml.utils.extensions.constrainViewToParent
import io.getstream.video.android.xml.utils.extensions.constrainViewToParentBySide
import io.getstream.video.android.xml.utils.extensions.constrainViewTopToBottomOfView
import io.getstream.video.android.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.xml.utils.extensions.horizontalChainInParent
import io.getstream.video.android.xml.utils.extensions.isLandscape
import io.getstream.video.android.xml.utils.extensions.setConstraints
import io.getstream.video.android.xml.widget.participant.CallParticipantView
import io.getstream.video.android.xml.widget.participant.RendererInitializer
import io.getstream.video.android.xml.widget.renderer.VideoRenderer
import io.getstream.video.android.xml.widget.view.CallConstraintLayout

internal class CallParticipantsGridView : CallConstraintLayout, VideoRenderer {

    private val childList: MutableList<CallParticipantView> = mutableListOf()

    private var rendererInitializer: RendererInitializer? = null

    /**
     * Whether to populate the users in a list or a grid while in landscape mode.
     */
    internal var isLandscapeListLayout: Boolean = true

    /**
     * Sets the [RendererInitializer] handler.
     *
     * @param rendererInitializer Handler for initializing the renderer.
     */
    override fun setRendererInitializer(rendererInitializer: RendererInitializer) {
        this.rendererInitializer = rendererInitializer
        childList.forEach { it.setRendererInitializer(rendererInitializer) }
    }

    internal constructor(context: Context) : this(context, null)
    internal constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    internal constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context.createStreamThemeWrapper(),
        attrs,
        defStyleAttr,
    )

    /**
     * Handler that provides new [CallParticipantView].
     */
    internal lateinit var buildParticipantView: () -> CallParticipantView

    /**
     * Updates the participants grid. If there are more or less views than participants, the views will be added or
     * removed from the view and constraints updated.
     *
     * @param participants The list of participants to show on the screen.
     */
    internal fun updateParticipants(participants: List<ParticipantState>) {
        when {
            childList.size > participants.size -> {
                val diff = childList.size - participants.size
                for (index in 0 until diff) {
                    val view = childList.last()
                    removeView(view)
                    childList.remove(view)
                }
            }

            childList.size < participants.size -> {
                val diff = participants.size - childList.size
                for (index in 0 until diff) {
                    val view = buildParticipantView().apply {
                        rendererInitializer?.let { setRendererInitializer(it) }
                    }
                    childList.add(view)
                    addView(view)
                }
            }
        }

        participants.forEachIndexed { index, participant ->
            val view = childList[index]
            view.setParticipant(participant)
            view.tag = participant.sessionId
        }

        updateConstraints()
    }

    override fun onOrientationChanged(isLandscape: Boolean) {
        updateConstraints()
    }

    /**
     * Updates the constraints of the shown [CallParticipantView]s so they all fit in the viewport.
     */
    private fun updateConstraints() {
        if (childList.size == 0) return

        TransitionManager.beginDelayedTransition(this)
        setConstraints {
            if (isLandscape && isLandscapeListLayout) {
                childList.forEach { callParticipantView ->
                    if (childList.size == 1) {
                        constrainViewToParent(callParticipantView)
                    } else {
                        constrainViewToParentBySide(callParticipantView, ConstraintSet.TOP)
                        constrainViewToParentBySide(callParticipantView, ConstraintSet.BOTTOM)
                        horizontalChainInParent(childList)
                    }
                }
            } else {
                when (childList.size) {
                    1 -> {
                        constrainViewToParent(childList[0])
                    }
                    2 -> {
                        constrainViewToParentBySide(childList[0], ConstraintSet.TOP)
                        constrainViewToParentBySide(childList[0], ConstraintSet.START)
                        constrainViewToParentBySide(childList[0], ConstraintSet.END)
                        constrainViewBottomToTopOfView(childList[0], childList[1])

                        constrainViewToParentBySide(childList[1], ConstraintSet.BOTTOM)
                        constrainViewToParentBySide(childList[1], ConstraintSet.START)
                        constrainViewToParentBySide(childList[1], ConstraintSet.END)
                        constrainViewTopToBottomOfView(childList[1], childList[0])
                    }
                    3 -> {
                        constrainViewToParentBySide(childList[0], ConstraintSet.TOP)
                        constrainViewToParentBySide(childList[0], ConstraintSet.START)
                        constrainViewEndToStartOfView(childList[0], childList[1])
                        constrainViewBottomToTopOfView(childList[0], childList[2])

                        constrainViewToParentBySide(childList[1], ConstraintSet.TOP)
                        constrainViewToParentBySide(childList[1], ConstraintSet.END)
                        constrainViewStartToEndOfView(childList[1], childList[0])
                        constrainViewBottomToTopOfView(childList[1], childList[2])

                        constrainViewToParentBySide(childList[2], ConstraintSet.BOTTOM)
                        constrainViewToParentBySide(childList[2], ConstraintSet.START)
                        constrainViewToParentBySide(childList[2], ConstraintSet.END)
                        constrainViewTopToBottomOfView(childList[2], childList[1])
                    }
                    4 -> {
                        constrainViewToParentBySide(childList[0], ConstraintSet.TOP)
                        constrainViewToParentBySide(childList[0], ConstraintSet.START)
                        constrainViewEndToStartOfView(childList[0], childList[1])
                        constrainViewBottomToTopOfView(childList[0], childList[2])

                        constrainViewToParentBySide(childList[1], ConstraintSet.TOP)
                        constrainViewToParentBySide(childList[1], ConstraintSet.END)
                        constrainViewStartToEndOfView(childList[1], childList[0])
                        constrainViewBottomToTopOfView(childList[1], childList[3])

                        constrainViewToParentBySide(childList[2], ConstraintSet.BOTTOM)
                        constrainViewToParentBySide(childList[2], ConstraintSet.START)
                        constrainViewTopToBottomOfView(childList[2], childList[0])
                        constrainViewEndToStartOfView(childList[2], childList[3])

                        constrainViewToParentBySide(childList[3], ConstraintSet.BOTTOM)
                        constrainViewToParentBySide(childList[3], ConstraintSet.END)
                        constrainViewTopToBottomOfView(childList[3], childList[1])
                        constrainViewStartToEndOfView(childList[3], childList[2])
                    }
                }
            }
        }
    }

    /**
     * Updates the current primary speaker and shows a border around the primary speaker.
     *
     * @param participant The call participant marked as a primary speaker.
     */
    internal fun updatePrimarySpeaker(participant: ParticipantState?) {
        childList.forEach { it.setActive(it.tag == participant?.sessionId) }
    }
}
