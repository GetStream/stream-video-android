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

package io.getstream.video.android.compose.ui.components.video

import android.content.Context
import android.util.AttributeSet
import android.view.View
import io.getstream.log.StreamLog
import io.getstream.video.android.compose.ui.components.video.VideoScalingType.Companion.toCommonScalingType
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.model.MediaTrack
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.model.StreamCallId
import io.getstream.webrtc.android.ui.VideoTextureViewRenderer
import java.util.UUID

/**
 * Renders a single video track based on the call state.
 */
public class VideoRendererView : VideoTextureViewRenderer {

    private var cid: StreamCallId? = null

    private var onRendered: (View) -> Unit = {}

    private var videoScalingType: VideoScalingType = VideoScalingType.SCALE_ASPECT_BALANCED

    public constructor(context: Context) : this(context, null)

    public constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    /**
     * The video track that should be drawn.
     */
    private val video: ParticipantState.Media? = null

    private val viewportId = UUID.randomUUID().toString()

    /**
     * An interface that will be invoked when the video is rendered.
     */
    public fun onRendered(onRendered: (View) -> Unit) {
        this.onRendered = onRendered
    }

    /**
     * Setup the video scale type of this renderer.
     */
    public fun setVideoScalingType(videoScalingType: VideoScalingType) {
        this.videoScalingType = videoScalingType
    }

    /**
     * Set the video track with [ParticipantState.Media] for the given [streamCallId].
     */
    public fun setVideo(streamCallId: StreamCallId, video: ParticipantState.Media?) {
        if (video == null || !video.enabled) return

        this.cid = streamCallId
        val mediaTrack = video.track
        val sessionId = video.sessionId
        val trackType = video.type

        // get a call form the call id
        val call = StreamVideo.instance().call(type = streamCallId.type, id = streamCallId.id)

        call.initRenderer(
            videoRenderer = this,
            sessionId = sessionId,
            trackType = trackType,
            onRendered = onRendered,
            viewportId = viewportId,
        )
        setScalingType(scalingType = videoScalingType.toCommonScalingType())
        setupVideo(mediaTrack)

        // inform the call that we want to render this video track. (this will trigger a subscription to the track)
        call.setVisibility(sessionId, trackType, true, viewportId)
    }

    private fun setupVideo(mediaTrack: MediaTrack?) {
        cleanTrack()

        try {
            if (mediaTrack is VideoTrack) {
                mediaTrack.video.addSink(this)
            }
        } catch (e: Exception) {
            StreamLog.d("VideoRendererView") { e.message.toString() }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        cleanTrack()
    }

    private fun cleanTrack() {
        val track = video?.track
        if (track is VideoTrack) {
            track.video.removeSink(this)
        }

        val streamCallId = this.cid
        if (streamCallId != null && video != null) {
            val sessionId = video.sessionId
            val trackType = video.type

            // inform the call that we no longer want to render this video track
            val call = StreamVideo.instance().call(type = streamCallId.type, id = streamCallId.id)
            call.setVisibility(sessionId, trackType, false, viewportId)
        }
    }
}
