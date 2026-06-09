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

package io.getstream.video.android.core.analytics.call.observer

import io.getstream.video.android.core.analytics.call.observer.model.JoinReason
import io.getstream.video.android.core.analytics.reporting.ClientEventReporter
import io.getstream.video.android.core.call.RtcSession
import kotlinx.coroutines.flow.MutableStateFlow
import stream.video.sfu.models.TrackType

internal class VideoAnalytics(
    private val callId: String,
    private val callType: String,
    private val clientEventReporter: ClientEventReporter,
    private val joinAnalyticsStateHolder: JoinAnalyticsStateHolder,
    private val sfuAnalyticsStateHolder: SfuAnalyticsStateHolder,
) {

    val stageId = MutableStateFlow<String>("")

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

                    videoTrackId?.let {
                        if (stageId.value.isEmpty()) {
                            stageId.value = clientEventReporter.reportFirstVideoFrameRendered(
                                sfuAnalyticsStateHolder.sfuId.value,
                                callId,
                                callType,
                                joinStageAttemptId = joinAnalyticsStateHolder.state.value.joinStageAttemptId ?: "unknown",
                                joinReason = joinAnalyticsStateHolder.state.value.joinReason ?: JoinReason.Unknown,
                                trackId = videoTrackId,
                                callSessionId = joinAnalyticsStateHolder.state.value.callSessionId,

                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }

    fun reset() {
        stageId.value = ""
    }
}
