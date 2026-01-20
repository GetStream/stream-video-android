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

// {
//   id: RTCRemoteInboundRtpVideoStream_3784832368,
//   type: remote-inbound-rtp,
//   timestamp: 1679505082075000.0,
//   transportId: RTCTransport_0_1,
//   kind: video,
//   localId: RTCOutboundRTPVideoStream_3784832368,
//   codecId: RTCCodec_0_Outbound_98,
//   ssrc: 3784832368
//   jitter: 0.004722222222222222,
//   roundTripTimeMeasurements: 72,
//   roundTripTime: 0.162,
//   totalRoundTripTime: 11.728,
//   fractionLost: 0.0,
//   packetsLost: 0,
// }

// https://www.w3.org/TR/webrtc-stats/#remoteinboundrtpstats-dict*
sealed interface RtcRemoteInboundRtpStreamStats : RtcReceivedRtpStreamStats {

    val localId: String?
    val roundTripTime: Double?
    val totalRoundTripTime: Double?
    val fractionLost: Double?
    val roundTripTimeMeasurements: Int?

    companion object {
        const val SSRC = RtcReceivedRtpStreamStats.SSRC
        const val KIND = RtcReceivedRtpStreamStats.KIND
        const val TRANSPORT_ID = RtcReceivedRtpStreamStats.TRANSPORT_ID
        const val CODEC_ID = RtcReceivedRtpStreamStats.CODEC_ID
        const val PACKETS_RECEIVED = RtcReceivedRtpStreamStats.PACKETS_RECEIVED
        const val PACKETS_LOST = RtcReceivedRtpStreamStats.PACKETS_LOST
        const val JITTER = RtcReceivedRtpStreamStats.JITTER
        const val LOCAL_ID = "localId"
        const val ROUND_TRIP_TIME = "roundTripTime"
        const val TOTAL_ROUND_TRIP_TIME = "totalRoundTripTime"
        const val FRACTION_LOST = "fractionLost"
        const val ROUND_TRIP_TIME_MEASUREMENTS = "roundTripTimeMeasurements"
    }
}
