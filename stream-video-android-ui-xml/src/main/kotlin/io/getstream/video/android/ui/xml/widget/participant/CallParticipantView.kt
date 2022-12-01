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
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import io.getstream.video.android.model.CallParticipantState
import io.getstream.video.android.ui.xml.R
import io.getstream.video.android.ui.xml.utils.extensions.constrainViewToParent
import io.getstream.video.android.ui.xml.utils.extensions.constrainViewToParentBySide
import io.getstream.video.android.ui.xml.utils.extensions.updateConstraints
import io.getstream.video.android.ui.xml.widget.avatar.AvatarView
import org.webrtc.SurfaceViewRenderer

public class CallParticipantView : ConstraintLayout {

    public constructor(context: Context) : super(context)
    public constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    )

    // TODO delete
    private val tempColors = listOf(
        Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA, Color.YELLOW, Color.CYAN
    )

    private val videoContainer = FrameLayout(context).apply {
        this.id = View.generateViewId()
        this.layoutParams = LayoutParams(
            LayoutParams.MATCH_CONSTRAINT,
            LayoutParams.MATCH_CONSTRAINT
        )
        setBackgroundColor(tempColors.random())
    }

    private val nameView = TextView(context).apply {
        this.id = View.generateViewId()
        this.text = text
        textSize = 24f
        setBackgroundColor(Color.DKGRAY)
        setTextColor(Color.WHITE)
        this.layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = 50
            topMargin = 50
        }
    }

    private val avatarView = AvatarView(context).apply {
        this.id = View.generateViewId()
        this.layoutParams = LayoutParams(
            300,
            300
        )
        setData(
            imageUrl = "https://getstream.io/static/a4ba18b7dc1eedfa3ea4edbac74ce5e4/a3911/kanat-kiialbaev.webp",
            name = "Kanat Kia"
        )
    }

    private val activeSpeakerView = View(context).apply {
        this.id = View.generateViewId()
        this.isVisible = false
        setBackgroundResource(R.drawable.rect_active_speaker)
    }

    init {
        addView(videoContainer)
        addView(nameView)
        addView(avatarView)
        addView(activeSpeakerView)

        updateConstraints {
            constrainViewToParent(videoContainer)
            constrainViewToParent(activeSpeakerView)
            constrainViewToParent(avatarView)

            constrainViewToParentBySide(nameView, ConstraintSet.START)
            constrainViewToParentBySide(nameView, ConstraintSet.TOP)
        }
    }

    public fun setName(name: String) {
        nameView.text = name.ifBlank { "John" }
    }

    public fun setActive(isActive: Boolean) {
        activeSpeakerView.isVisible = isActive
    }

    private var rendererInitializer: ((SurfaceViewRenderer, String, (View) -> Unit) -> Unit)? = null

    public fun setRendererInitializer(rendererInitializer: (SurfaceViewRenderer, String, (View) -> Unit) -> Unit) {
        this.rendererInitializer = rendererInitializer
    }

    public fun set(
        participant: CallParticipantState,
    ) {
        val renderer = SurfaceViewRenderer(context).apply {
            this.id = SurfaceViewRenderer.generateViewId()
            this.layoutParams = LayoutParams(
                LayoutParams.MATCH_CONSTRAINT,
                LayoutParams.MATCH_CONSTRAINT
            )
        }
        rendererInitializer?.invoke(renderer, participant.track?.streamId.orEmpty()) { /* no-op */ }
        participant.track?.video?.addSink(renderer)

        videoContainer.addView(
            renderer,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
    }
}
