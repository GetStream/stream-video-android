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

package io.getstream.video.android.core.analytics.observer

import android.util.Log
import io.getstream.video.android.core.analytics.reporting.ClientEventReporter
import io.getstream.video.android.core.call.RtcSession
import stream.video.sfu.models.TrackType

internal class VideoObserver(
    private val callId: String,
    private val callType: String,
    private val clientEventReporter: ClientEventReporter,
    private val onSfuId: () -> String,
    val getJoinStageAttemptId: () -> String,
) {

    var stageId: String = ""

    fun firstVideoFrameRendered(
        trackType: TrackType,
        width: Int,
        height: Int,
        rtcSession: RtcSession?,
        videoSessionId: String,
        callSessionId: String,
    ) {
        when (trackType) {
            TrackType.TRACK_TYPE_VIDEO, TrackType.TRACK_TYPE_SCREEN_SHARE -> {
                if (videoSessionId != callSessionId) {
                    val videoTrackId = rtcSession?.subscriber?.value?.getTrack(
                        videoSessionId,
                        TrackType.TRACK_TYPE_VIDEO,
                    )?.asVideoTrack()?.video?.id()
                    Log.d(
                        "VideoObserver",
                        "noob [firstVideoFrameRendered]: $trackType, w:$width, h:$height, videoTrackId:$videoTrackId, videoSessionId:$videoSessionId, callSessionId:$callSessionId",
                    )
                    videoTrackId?.let {
                        if (stageId.isEmpty()) {
                            stageId = clientEventReporter.reportFirstVideoFrameRendered(
                                onSfuId(),
                                callId,
                                callType,
                                getJoinStageAttemptId(),
                                videoTrackId,
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }

    fun reset() {
        stageId = ""
    }
}
