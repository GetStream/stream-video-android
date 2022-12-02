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
import androidx.core.view.children
import androidx.core.view.isVisible
import io.getstream.log.StreamLog
import io.getstream.video.android.model.VideoTrack
import io.getstream.video.android.ui.TextureViewRenderer
import io.getstream.video.android.ui.xml.R
import io.getstream.video.android.ui.xml.utils.extensions.constrainViewToParent
import io.getstream.video.android.ui.xml.utils.extensions.constrainViewToParentBySide
import io.getstream.video.android.ui.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.ui.xml.utils.extensions.dpToPx
import io.getstream.video.android.ui.xml.utils.extensions.updateConstraints
import io.getstream.video.android.ui.xml.widget.avatar.AvatarView

public class CallParticipantView : ConstraintLayout {

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

    private val logger = StreamLog.getLogger("Call:ParticipantView")

    private val videoContainer = FrameLayout(context).apply {
        this.id = View.generateViewId()
        this.layoutParams = LayoutParams(
            LayoutParams.MATCH_CONSTRAINT,
            LayoutParams.MATCH_CONSTRAINT
        )
    }

    private val nameView = TextView(context).apply {
        this.id = View.generateViewId()
        this.text = text
        textSize = 24f
        setBackgroundColor(Color.GRAY)
        setTextColor(Color.WHITE)
        setPadding(8.dpToPx(), 4.dpToPx(), 8.dpToPx(), 4.dpToPx())
        this.layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = 16.dpToPx()
            topMargin = 16.dpToPx()
        }
    }

    private val avatarView = AvatarView(context).apply {
        this.id = View.generateViewId()
        this.layoutParams = LayoutParams(
            96.dpToPx(),
            96.dpToPx()
        )
    }

    private var rendererInitializer: RendererInitializer? = null

    private val activeSpeakerView = View(context).apply {
        this.id = View.generateViewId()
        this.isVisible = false
        setBackgroundResource(R.drawable.rect_active_speaker)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
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

    public fun setData(imageUrl: String, name: String) {
        avatarView.setData(imageUrl, name)
        nameView.text = name.ifBlank { "Empty" }
    }

    public fun setActive(isActive: Boolean) {
        activeSpeakerView.isVisible = isActive
    }

    public fun setRendererInitializer(rendererInitializer: RendererInitializer) {
        this.rendererInitializer = rendererInitializer
        rendererInitializer.initialize()
    }

    public fun setTrack(track: VideoTrack) {
        val renderer = TextureViewRenderer(context).apply {
            this.id = View.generateViewId()
            this.videoTrack = InitializableTrack(delegate = track)
            this.layoutParams = LayoutParams(
                LayoutParams.MATCH_CONSTRAINT,
                LayoutParams.MATCH_CONSTRAINT
            )
        }
        track.video.addSink(renderer)
        videoContainer.videoRenderer = renderer
        rendererInitializer.initialize()
    }

    private fun RendererInitializer?.initialize() {
        val videoRenderer = videoContainer.videoRenderer ?: return
        val videoTrack = videoRenderer.videoTrack ?: return
        if (videoTrack.initialized) return
        logger.i { "[initialize] videoTrack: '$videoTrack'" }
        this?.initRenderer(videoRenderer, videoTrack.streamId) { /* no-op */ }
        videoTrack.initialized = true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        logger.i { "[onDetachedFromWindow] participant: '${nameView.text}'" }
        videoContainer.videoRenderer?.also { videoRenderer ->
            videoRenderer.videoTrack?.video?.removeSink(videoRenderer)
            videoRenderer.release()
        }
        videoContainer.videoRenderer = null
    }

    private var FrameLayout.videoRenderer: TextureViewRenderer?
        get() = children.firstOrNull() as? TextureViewRenderer
        set(value) {
            removeAllViews()
            if (value != null) {
                addView(
                    value,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
            }
        }

    private var TextureViewRenderer.videoTrack: InitializableTrack?
        get() = tag as? InitializableTrack
        set(value) {
            tag = value
        }

    private data class InitializableTrack(
        var initialized: Boolean = false,
        private val delegate: VideoTrack,
    ) {
        val streamId = delegate.streamId
        val video = delegate.video
    }
}
