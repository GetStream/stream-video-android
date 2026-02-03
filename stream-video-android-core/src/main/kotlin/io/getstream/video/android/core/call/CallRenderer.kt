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

package io.getstream.video.android.core.call

import io.getstream.log.taggedLogger
import io.getstream.webrtc.android.ui.VideoTextureViewRenderer
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import kotlin.getValue

internal class CallRenderer {

    private val logger by taggedLogger("CallRenderer")

    internal fun initRenderer(
        videoRenderer: VideoTextureViewRenderer,
        sessionId: String,
        trackType: TrackType,
        eglBase: EglBase,
        session: RtcSession?,
        onRendered: (VideoTextureViewRenderer) -> Unit = {},
        viewportId: String = sessionId,
    ) {
        logger.d { "[initRenderer] #sfu; #track; sessionId: $sessionId" }

        // Note this comes from the shared eglBase
        videoRenderer.init(
            eglBase.eglBaseContext,
            object : RendererCommon.RendererEvents {
                override fun onFirstFrameRendered() {
                    val width = videoRenderer.measuredWidth
                    val height = videoRenderer.measuredHeight
                    logger.i {
                        "[initRenderer.onFirstFrameRendered] #sfu; #track; " +
                            "trackType: $trackType, dimension: ($width - $height), " +
                            "sessionId: $sessionId"
                    }
                    if (trackType != TrackType.TRACK_TYPE_SCREEN_SHARE) {
                        session?.updateTrackDimensions(
                            sessionId,
                            trackType,
                            true,
                            VideoDimension(width, height),
                            viewportId,
                        )
                    }
                    onRendered(videoRenderer)
                }

                override fun onFrameResolutionChanged(
                    videoWidth: Int,
                    videoHeight: Int,
                    rotation: Int,
                ) {
                    val width = videoRenderer.measuredWidth
                    val height = videoRenderer.measuredHeight
                    logger.v {
                        "[initRenderer.onFrameResolutionChanged] #sfu; #track; " +
                            "trackType: $trackType, " +
                            "viewport size: ($width - $height), " +
                            "video size: ($videoWidth - $videoHeight), " +
                            "sessionId: $sessionId"
                    }

                    if (trackType != TrackType.TRACK_TYPE_SCREEN_SHARE) {
                        session?.updateTrackDimensions(
                            sessionId,
                            trackType,
                            true,
                            VideoDimension(width, height),
                            viewportId,
                        )
                    }
                }
            },
        )
    }
}
