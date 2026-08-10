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

package io.getstream.video.android.core.call.components

import android.graphics.Bitmap
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.analytics.call.CallAnalytics
import io.getstream.video.android.core.call.connection.Subscriber
import io.getstream.video.android.core.call.video.YuvFrame
import io.getstream.video.android.core.model.AudioTrack
import io.getstream.video.android.core.model.PreferredVideoResolution
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.webrtc.EglBase
import io.getstream.webrtc.RendererCommon
import io.getstream.webrtc.VideoSink
import io.getstream.webrtc.android.ui.VideoTextureViewRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import kotlin.coroutines.resume

/**
 * Handles binding video tracks to renderers, visibility / track-dimension updates,
 * screenshots and incoming media-quality overrides for a call.
 *
 * @param eglBase provider for the shared EGL context; invoked lazily on first render so the
 * native context isn't created until it's actually needed.
 * @param callSessionId provider for the call's (mutable, runtime-updated) session id.
 */
internal class CallRenderer(
    private val type: String,
    private val id: String,
    private val scope: CoroutineScope,
    private val sessionManager: CallSessionManager,
    private val callAnalytics: CallAnalytics,
    private val eglBase: () -> EglBase,
    private val callSessionId: () -> String,
) {
    private val logger by taggedLogger("Call:Renderer:$type:$id")

    fun setVisibility(
        sessionId: String,
        trackType: TrackType,
        visible: Boolean,
        viewportId: String = sessionId,
    ) {
        logger.i {
            "[setVisibility] #track; #sfu; viewportId: $viewportId, sessionId: $sessionId, trackType: $trackType, visible: $visible"
        }
        sessionManager.session.value?.updateTrackDimensions(
            sessionId,
            trackType,
            visible,
            Subscriber.defaultVideoDimension,
            viewportId,
        )
    }

    fun setVisibility(
        sessionId: String,
        trackType: TrackType,
        visible: Boolean,
        viewportId: String = sessionId,
        width: Int,
        height: Int,
    ) {
        logger.i {
            "[setVisibility] #track; #sfu; viewportId: $viewportId, sessionId: $sessionId, trackType: $trackType, visible: $visible"
        }
        sessionManager.session.value?.updateTrackDimensions(
            sessionId,
            trackType,
            visible,
            VideoDimension(width, height),
            viewportId,
        )
    }

    fun initRenderer(
        videoRenderer: VideoTextureViewRenderer,
        sessionId: String,
        trackType: TrackType,
        onRendered: (VideoTextureViewRenderer) -> Unit = {},
        viewportId: String = sessionId,
    ) {
        logger.d { "[initRenderer] #sfu; #track; sessionId: $sessionId" }

        // Note this comes from the shared eglBase
        videoRenderer.init(
            eglBase().eglBaseContext,
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
                        sessionManager.session.value?.updateTrackDimensions(
                            sessionId,
                            trackType,
                            true,
                            VideoDimension(width, height),
                            viewportId,
                        )
                    }
                    onRendered(videoRenderer)
                    callAnalytics.videoAnalytics.firstVideoFrameRendered(

                        trackType,
                        width,
                        height,
                        rtcSession = sessionManager.session.value,
                        sessionId,
                        callSessionId(),
                    )
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
                        sessionManager.session.value?.updateTrackDimensions(
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

    suspend fun takeScreenshot(track: VideoTrack): Bitmap? {
        return suspendCancellableCoroutine { continuation ->
            var screenshotSink: VideoSink? = null
            screenshotSink = VideoSink {
                // make sure we stop after first frame is delivered
                if (!continuation.isActive) {
                    return@VideoSink
                }
                it.retain()
                val bitmap = YuvFrame.bitmapFromVideoFrame(it)
                it.release()

                // This has to be launched asynchronously - removing the sink on the
                // same thread as the videoframe is delivered will lead to a deadlock
                // (needs investigation why)
                scope.launch {
                    track.video.removeSink(screenshotSink)
                }
                continuation.resume(bitmap)
            }

            track.video.addSink(screenshotSink)
        }
    }

    fun setPreferredIncomingVideoResolution(
        resolution: PreferredVideoResolution?,
        sessionIds: List<String>? = null,
    ) {
        sessionManager.session.value?.let { session ->
            session.trackOverridesHandler.updateOverrides(
                sessionIds = sessionIds,
                dimensions = resolution?.let { VideoDimension(it.width, it.height) },
            )
        }
    }

    fun setIncomingVideoEnabled(enabled: Boolean?, sessionIds: List<String>? = null) {
        sessionManager.session.value?.trackOverridesHandler?.updateOverrides(
            sessionIds,
            visible = enabled,
        )
    }

    fun setIncomingAudioEnabled(enabled: Boolean, sessionIds: List<String>? = null) {
        val participantTrackMap = sessionManager.session.value?.subscriber?.value?.tracks ?: return

        val targetTracks = when {
            sessionIds != null -> sessionIds.mapNotNull { participantTrackMap[it] }
            else -> participantTrackMap.values.toList()
        }

        targetTracks
            .mapNotNull { it[TrackType.TRACK_TYPE_AUDIO] as? AudioTrack }
            .forEach { it.enableAudio(enabled) }
    }
}
