/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.xml.widget.screenshare

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.getstream.video.android.core.model.ScreenSharingSession
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.xml.databinding.ViewScreenShareBinding
import io.getstream.video.android.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.xml.utils.extensions.streamThemeInflater
import io.getstream.video.android.xml.widget.participant.RendererInitializer
import io.getstream.video.android.xml.widget.renderer.VideoRenderer
import stream.video.sfu.models.TrackType

public class ScreenShareView : ConstraintLayout, VideoRenderer {

    private lateinit var style: ScreenShareStyle

    private val binding = ViewScreenShareBinding.inflate(streamThemeInflater, this)

    /**
     * Flag that notifies if we initialised the renderer for this view or not.
     */
    private var wasRendererInitialised: Boolean = false

    /**
     * The track of the current screen share.
     */
    private var track: VideoTrack? = null

    /**
     * Handler when the video renders.
     */
    public var onRender: (View) -> Unit = {}

    private lateinit var rendererInitializer: RendererInitializer

    public constructor(context: Context) : this(context, null, 0)
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context.createStreamThemeWrapper(),
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        style = ScreenShareStyle(context, attrs)

        // TODO fullscreen and orientation support will be added in later pr
        // https://github.com/GetStream/stream-video-android/issues/150
        binding.changeOrientationButton.apply {
            setImageDrawable(style.landscapeIcon)
            setColorFilter(style.controlButtonIconTint)
            background.setTint(style.controlButtonBackgroundTint)
        }

        binding.fullscreenButton.apply {
            setImageDrawable(style.fullscreenIcon)
            setColorFilter(style.controlButtonIconTint)
            background.setTint(style.controlButtonBackgroundTint)
        }
    }

    override fun setRendererInitializer(rendererInitializer: RendererInitializer) {
        this.rendererInitializer = rendererInitializer
        initRenderer()
    }

    /**
     * Initialises the renderer for this view if it was not initialised.
     */
    private fun initRenderer() {
        if (!wasRendererInitialised) {
            if (::rendererInitializer.isInitialized) {
                track?.let {
                    rendererInitializer.initRenderer(
                        binding.screenShare,
                        it.streamId,
                        TrackType.TRACK_TYPE_VIDEO
                    ) { onRender(it) }
                    wasRendererInitialised = true
                }
            }
        }
    }

    /**
     * Sets the current screen sharing session.
     *
     * @param screenSharingSession The currently active screen sharing session.
     */
    public fun setScreenSharingSession(screenSharingSession: ScreenSharingSession) {
        setTrack(screenSharingSession.track)
    }

    /**
     * Updates the current track which contains the current participants video.
     *
     * @param track The [VideoTrack] of the participant.
     */
    private fun setTrack(track: VideoTrack?) {
        if (this.track == track) return

        this.track?.video?.removeSink(binding.screenShare)
        this.track = track

        if (track == null) return

        this.track!!.video.addSink(binding.screenShare)
        initRenderer()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        binding.screenShare.apply {
            track?.video?.removeSink(this)
        }
        track = null
    }
}
