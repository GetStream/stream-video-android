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

package io.getstream.video.android.xml.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.CallParticipantState
import io.getstream.video.android.model.VideoTrack
import io.getstream.video.android.xml.R
import io.getstream.video.android.xml.ui.utils.extensions.constrainViewToParent
import io.getstream.video.android.xml.ui.utils.extensions.constrainViewToParentBySide
import io.getstream.video.android.xml.ui.utils.extensions.updateConstraints
import io.getstream.video.android.xml.ui.widget.avatar.AvatarView
import org.webrtc.SurfaceViewRenderer

public class ParticipantView : ConstraintLayout {

    public constructor(context: Context) : super(context)
    public constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    )

    private val nameView = TextView(context).apply {
        this.id = View.generateViewId()
        this.text = text
        textSize = 24f
        this.layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = 50
            bottomMargin = 50
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

    init {
        val activeSpeakerView = View(context).apply {
            this.id = View.generateViewId()
            setBackgroundResource(R.drawable.rect_active_speaker)
        }

        addView(nameView)
        addView(avatarView)
        addView(activeSpeakerView)

        updateConstraints {
            constrainViewToParent(activeSpeakerView)
            constrainViewToParent(avatarView)

            constrainViewToParentBySide(nameView, ConstraintSet.START)
            constrainViewToParentBySide(nameView, ConstraintSet.BOTTOM)
        }
    }

    public fun setName(name: String) {
        nameView.text = name
    }

    public fun set(
        call: Call,
        participant: CallParticipantState,
        track: VideoTrack?,
    ) {
        val renderer = SurfaceViewRenderer(context).apply {
            this.id = SurfaceViewRenderer.generateViewId()
            this.layoutParams = LayoutParams(
                LayoutParams.MATCH_CONSTRAINT,
                LayoutParams.MATCH_CONSTRAINT
            )
        }
        call.initRenderer(renderer, track?.streamId.orEmpty(), onRender = { /* no-op */ })
        track?.video?.addSink(renderer)

        addView(renderer)

        updateConstraints {
            constrainViewToParent(renderer)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }
}
