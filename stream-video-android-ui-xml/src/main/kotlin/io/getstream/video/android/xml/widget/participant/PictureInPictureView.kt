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

package io.getstream.video.android.xml.widget.participant

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.children
import io.getstream.video.android.xml.R
import io.getstream.video.android.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.xml.utils.extensions.getFirstViewInstance
import io.getstream.video.android.xml.widget.renderer.VideoRenderer
import io.getstream.video.android.xml.widget.screenshare.ScreenShareView
import io.getstream.video.android.xml.widget.view.CallCardView

/**
 * View to ve shown when inside a call and the app enter picture in picture mode. Will show the primary speaker.
 */
public class PictureInPictureView : CallCardView, VideoRenderer {

    private lateinit var style: PictureInPictureStyle

    /**
     * Handler to initialise the renderer. If the participant view was created before the renderer has been initialised
     * will take care of init.
     */
    private var rendererInitializer: RendererInitializer? = null

    public constructor(context: Context) : this(context, null)
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context.createStreamThemeWrapper(),
        attrs,
        defStyleAttr,
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        style = PictureInPictureStyle(context, attrs)
    }

    /**
     * Used to set the [RendererInitializer] for the [CallParticipantView].
     *
     * @param rendererInitializer The [RendererInitializer] used to initialize the renderer.
     */
    override fun setRendererInitializer(rendererInitializer: RendererInitializer) {
        this.rendererInitializer = rendererInitializer
        getVideoRenderer()?.let {
            it.setRendererInitializer(rendererInitializer)
        }
    }

    /**
     * Populates the PiP ui with the [CallParticipantView] to preview the primary speaker.
     */
    public fun setCallParticipantView(onViewInitialized: (CallParticipantView) -> Unit) {
        if (getFirstViewInstance<CallParticipantView>() != null) return

        removeAllViews()
        CallParticipantView(
            context = context,
            attrs = null,
            defStyleAttr = R.attr.streamVideoPictureInPictureCallParticipantViewStyle,
            defStyleRes = style.callParticipantStyle,
        ).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            rendererInitializer?.let { setRendererInitializer(it) }
            this@PictureInPictureView.addView(this)
            onViewInitialized(this)
        }
    }

    /**
     * Populates the PiP ui with the [ScreenShareView] to preview the primary speaker.
     */
    public fun setScreenShareView(onViewInitialized: (ScreenShareView) -> Unit) {
        if (getFirstViewInstance<ScreenShareView>() != null) return

        ScreenShareView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            rendererInitializer?.let { setRendererInitializer(it) }
            this@PictureInPictureView.addView(this)
            onViewInitialized(this)
        }
    }

    /**
     * Retrieves the [VideoRenderer] from the content holder if it is currently being used.
     *
     * @return The [VideoRenderer] if one is inflated in the layout.
     */
    private fun getVideoRenderer(): VideoRenderer? {
        return children.firstOrNull { it is VideoRenderer } as? VideoRenderer
    }
}
