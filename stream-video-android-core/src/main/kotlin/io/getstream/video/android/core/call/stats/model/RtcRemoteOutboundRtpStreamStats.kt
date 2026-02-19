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

package io.getstream.video.android.core.call.stats.model

import java.math.BigInteger

// https://www.w3.org/TR/webrtc-stats/#remoteoutboundrtpstats-dict*
sealed interface RtcRemoteOutboundRtpStreamStats : RtcSentRtpStreamStats {

    val localId: String?
    val remoteTimestamp: Double?
    val reportsSent: BigInteger?
    val roundTripTime: Double?
    val totalRoundTripTime: Double?
    val roundTripTimeMeasurements: BigInteger?

    companion object {
        const val SSRC = RtcSentRtpStreamStats.SSRC
        const val KIND = RtcSentRtpStreamStats.KIND
        const val TRANSPORT_ID = RtcSentRtpStreamStats.TRANSPORT_ID
        const val CODEC_ID = RtcSentRtpStreamStats.CODEC_ID
        const val PACKETS_SENT = RtcSentRtpStreamStats.PACKETS_SENT
        const val BYTES_SENT = RtcSentRtpStreamStats.BYTES_SENT
        const val LOCAL_ID = "localId"
        const val REMOTE_TIMESTAMP = "remoteTimestamp"
        const val REPORTS_SENT = "reportsSent"
        const val ROUND_TRIP_TIME = "roundTripTime"
        const val TOTAL_ROUND_TRIP_TIME = "totalRoundTripTime"
        const val ROUND_TRIP_TIME_MEASUREMENTS = "roundTripTimeMeasurements"
    }
}
