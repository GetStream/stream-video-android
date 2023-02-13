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

public class ScreenSharingView : ConstraintLayout, VideoRenderer {

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
    )

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
            release()
        }
        track = null
    }
}