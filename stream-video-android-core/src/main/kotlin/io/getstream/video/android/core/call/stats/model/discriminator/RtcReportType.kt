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

package io.getstream.video.android.core.call.stats.model.discriminator

enum class RtcReportType(
    private val alias: String,
) {
    CERTIFICATE("certificate"),
    CODEC("codec"),
    CANDIDATE_PAIR("candidate-pair"),
    REMOTE_CANDIDATE("remote-candidate"),
    LOCAL_CANDIDATE("local-candidate"),
    REMOTE_INBOUND_RTP("remote-inbound-rtp"),
    REMOTE_OUTBOUND_RTP("remote-outbound-rtp"),
    INBOUND_RTP("inbound-rtp"),
    OUTBOUND_RTP("outbound-rtp"),
    TRACK("track"),
    MEDIA_SOURCE("media-source"),
    STREAM("stream"),
    PEER_CONNECTION("peer-connection"),
    TRANSPORT("transport"),
    UNKNOWN("unknown"),
    ;

    override fun toString(): String = alias

    companion object {
        fun fromAlias(alias: String?): RtcReportType {
            return RtcReportType.values().firstOrNull {
                it.alias == alias
            } ?: UNKNOWN
        }
    }
}
