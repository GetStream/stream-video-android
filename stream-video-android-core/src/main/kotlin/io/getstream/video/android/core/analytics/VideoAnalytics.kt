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

package io.getstream.video.android.core.analytics

import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.events.reporting.ClientEventReporter
import stream.video.sfu.models.TrackType

internal class VideoAnalytics(
    private val callId: String,
    private val callType: String,
    private val clientEventReporter: ClientEventReporter,
    private val onSfuId: () -> String,
    val getJoinStageAttemptId: () -> String,
) {

    var stageId: String = ""

    fun firstVideoFrameRendered(
        trackType: TrackType,
        rtcSession: RtcSession?,
        videoSessionId: String,
        callSessionId: String,
    ) {
        if (trackType == TrackType.TRACK_TYPE_VIDEO && videoSessionId != callSessionId) {
            val videoTrackId = rtcSession?.subscriber?.value?.getTrack(
                videoSessionId,
                TrackType.TRACK_TYPE_VIDEO,
            )?.asVideoTrack()?.video?.id()

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

    fun reset() {}
}
