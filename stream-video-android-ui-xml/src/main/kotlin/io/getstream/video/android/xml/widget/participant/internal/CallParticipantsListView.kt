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
import android.graphics.Color
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.util.AttributeSet
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.annotation.Px
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.xml.widget.participant.CallParticipantView
import io.getstream.video.android.xml.widget.participant.RendererInitializer
import io.getstream.video.android.xml.widget.renderer.VideoRenderer
import io.getstream.video.android.xml.widget.view.JobHolder
import kotlinx.coroutines.Job

internal class CallParticipantsListView : HorizontalScrollView, VideoRenderer, JobHolder {

    internal var buildParticipantView: () -> CallParticipantView = { CallParticipantView(context) }

    private val childList: MutableList<CallParticipantView> = mutableListOf()

    override val runningJobs: MutableList<Job> = mutableListOf()

    private var rendererInitializer: RendererInitializer? = null

    private val participantsList: LinearLayout by lazy {
        LinearLayout(context).apply {
            this@CallParticipantsListView.addView(this)
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
            showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
        }
    }

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
    internal constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
        this(context, attrs, defStyleAttr, 0)

    internal constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context.createStreamThemeWrapper(),
        attrs,
        defStyleAttr,
        defStyleRes,
    )

    /**
     * Set the margin between participant views in the list.
     *
     * @param size The size of the divider.
     */
    internal fun setItemMargin(@Px size: Int) {
        participantsList.dividerDrawable = ShapeDrawable(RectShape()).apply {
            intrinsicWidth = size
            intrinsicHeight = size
            paint.color = Color.TRANSPARENT
        }
    }

    /**
     * Updates the participants list. If there are more or less views than participants, the views will be added or
     * removed from the view.
     *
     * @param participants The list of participants to show on the screen.
     */
    internal fun updateParticipants(participants: List<ParticipantState>) {
        when {
            childList.size > participants.size -> {
                val diff = childList.size - participants.size
                for (index in 0 until diff) {
                    val view = childList.last()
                    participantsList.removeView(view)
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
                    participantsList.addView(view)
                }
            }
        }

        childList.forEachIndexed { index, view ->
            val participant = participants[index]
            view.setParticipant(participant)
            view.tag = participant.sessionId
        }
    }

    /**
     * Updates the current primary speaker and shows a border around the primary speaker.
     *
     * @param participant The call participant marked as a primary speaker.
     */
    internal fun updatePrimarySpeaker(participant: ParticipantState?) {
        childList.forEach {
            it.setActive(it.tag == participant?.sessionId)
        }
    }

    override fun onDetachedFromWindow() {
        stopAllJobs()
        super.onDetachedFromWindow()
    }
}
