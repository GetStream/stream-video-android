/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.call.stats

import io.getstream.log.StreamLog
import io.getstream.video.android.core.call.stats.model.RtcAudioSourceStats
import io.getstream.video.android.core.call.stats.model.RtcCodecStats
import io.getstream.video.android.core.call.stats.model.RtcIceCandidatePairStats
import io.getstream.video.android.core.call.stats.model.RtcIceCandidateStats
import io.getstream.video.android.core.call.stats.model.RtcInboundRtpAudioStreamStats
import io.getstream.video.android.core.call.stats.model.RtcInboundRtpStreamStats
import io.getstream.video.android.core.call.stats.model.RtcInboundRtpVideoStreamStats
import io.getstream.video.android.core.call.stats.model.RtcMediaSourceStats
import io.getstream.video.android.core.call.stats.model.RtcMediaStreamAudioTrackReceiverStats
import io.getstream.video.android.core.call.stats.model.RtcMediaStreamAudioTrackSenderStats
import io.getstream.video.android.core.call.stats.model.RtcMediaStreamTrackStats
import io.getstream.video.android.core.call.stats.model.RtcMediaStreamVideoTrackReceiverStats
import io.getstream.video.android.core.call.stats.model.RtcMediaStreamVideoTrackSenderStats
import io.getstream.video.android.core.call.stats.model.RtcOutboundRtpAudioStreamStats
import io.getstream.video.android.core.call.stats.model.RtcOutboundRtpStreamStats
import io.getstream.video.android.core.call.stats.model.RtcOutboundRtpVideoStreamStats
import io.getstream.video.android.core.call.stats.model.RtcRemoteInboundRtpAudioStreamStats
import io.getstream.video.android.core.call.stats.model.RtcRemoteInboundRtpStreamStats
import io.getstream.video.android.core.call.stats.model.RtcRemoteInboundRtpVideoStreamStats
import io.getstream.video.android.core.call.stats.model.RtcRemoteOutboundRtpAudioStreamStats
import io.getstream.video.android.core.call.stats.model.RtcRemoteOutboundRtpStreamStats
import io.getstream.video.android.core.call.stats.model.RtcRemoteOutboundRtpVideoStreamStats
import io.getstream.video.android.core.call.stats.model.RtcStats
import io.getstream.video.android.core.call.stats.model.RtcVideoSourceStats
import io.getstream.video.android.core.call.stats.model.discriminator.RtcMediaKind
import io.getstream.video.android.core.call.stats.model.discriminator.RtcQualityLimitationReason
import io.getstream.video.android.core.call.stats.model.discriminator.RtcReportType
import org.webrtc.RTCStats
import org.webrtc.RTCStatsReport

private const val TAG = "RtcParser"
private const val DEBUG = false

fun RTCStatsReport.toRtcStats(): Map<RtcReportType, Set<RtcStats>> {
    val result = hashMapOf<RtcReportType, MutableSet<RtcStats>>()
    for (report in statsMap.values) {
        val type = RtcReportType.fromAlias(report.type)
        val rtcStats = report.toRtcStats(type) ?: continue
        result.getOrPut(type) { hashSetOf() }.add(rtcStats)
    }
    return result
}

private fun RTCStats.toRtcStats(type: RtcReportType): RtcStats? {
    return when (type) {
        RtcReportType.CODEC -> toRtcCodecStats().also {
            if (DEBUG) StreamLog.i(TAG) { "[toRtcStats] codec: $it" }
        }

        RtcReportType.CANDIDATE_PAIR -> toRtcIceCandidatePairStats().also {
            if (DEBUG) StreamLog.i(TAG) { "[toRtcStats] candidatePair: $it" }
        }

        RtcReportType.LOCAL_CANDIDATE -> toRtcIceCandidateStats().also {
            if (DEBUG) StreamLog.i(TAG) { "[toRtcStats] localCandidate: $it" }
        }

        RtcReportType.REMOTE_CANDIDATE -> toRtcIceCandidateStats().also {
            if (DEBUG) StreamLog.i(TAG) { "[toRtcStats] remoteCandidate: $it" }
        }

        RtcReportType.MEDIA_SOURCE -> {
            when (getOrNull<String>(RtcMediaSourceStats.KIND)?.let { RtcMediaKind.fromAlias(it) }) {
                RtcMediaKind.AUDIO -> toRtcAudioSourceStats().also {
                    if (DEBUG) StreamLog.i(TAG) { "[toRtcStats] audioSource: $it" }
                }

                RtcMediaKind.VIDEO -> toRtcVideoSourceStats().also {
                    if (DEBUG) StreamLog.i(TAG) { "[toRtcStats] videoSource: $it" }
                }

                else -> {
                    if (DEBUG) {
                        StreamLog.v(
                            TAG,
                        ) { "[toRtcStats] unexpected mediaSource: $type($id)" }
                    }
                    null
                }
            }
        }

        RtcReportType.INBOUND_RTP -> {
            when (
                getOrNull<String>(
                    RtcInboundRtpStreamStats.KIND,
                )?.let { RtcMediaKind.fromAlias(it) }
            ) {
                RtcMediaKind.AUDIO -> toRtcInboundRtpAudioStreamStats().also {
                    if (DEBUG) StreamLog.i(TAG) { "[toRtcStats] inboundRtpAudio: $it" }
                }

                RtcMediaKind.VIDEO -> toRtcInboundRtpVideoStreamStats().also {
                    if (DEBUG) StreamLog.i(TAG) { "[toRtcStats] inboundRtpVideo: $it" }
                }

                else -> {
                    if (DEBUG) StreamLog.e(TAG) { "[toRtcStats] unexpected inboundRtp: $type($id)" }
                    null
                }
            }
        }

        RtcReportType.OUTBOUND_RTP -> {
            when (
                getOrNull<String>(
                    RtcOutboundRtpStreamStats.KIND,
                )?.let { RtcMediaKind.fromAlias(it) }
            ) {
                RtcMediaKind.AUDIO -> toRtcOutboundRtpAudioStreamStats().also {
                    if (DEBUG) StreamLog.i(TAG) { "[toRtcStats] outboundRtpAudio: $it" }
                }

                RtcMediaKind.VIDEO -> toRtcOutboundRtpVideoStreamStats().also {
                    if (DEBUG) StreamLog.i(TAG) { "[toRtcStats] outboundRtpVideo: $it" }
                }

                else -> {
                    if (DEBUG) {
                        StreamLog.e(
                            TAG,
                        ) { "[toRtcStats] unexpected outboundRtp: $type($id)" }
                    }
                    null
                }
            }
        }

        RtcReportType.REMOTE_INBOUND_RTP -> {
            when (
                getOrNull<String>(RtcRemoteInboundRtpStreamStats.KIND)?.let {
                    RtcMediaKind.fromAlias(
                        it,
                    )
                }
            ) {
                RtcMediaKind.AUDIO -> toRtcRemoteInboundRtpAudioStreamStats().also {
                    if (DEBUG) StreamLog.i(TAG) { "[toRtcStats] remoteInboundRtpAudio: $it" }
                }

                RtcMediaKind.VIDEO -> toRtcRemoteInboundRtpVideoStreamStats().also {
                    if (DEBUG) StreamLog.i(TAG) { "[toRtcStats] remoteInboundRtpVideo: $it" }
                }

                else -> {
                    if (DEBUG) {
                        StreamLog.e(
                            TAG,
                        ) { "[toRtcStats] unexpected remoteInboundRtp: $type($id)" }
                    }
                    null
                }
            }
        }

        RtcReportType.REMOTE_OUTBOUND_RTP -> {
            when (
                getOrNull<String>(RtcRemoteOutboundRtpStreamStats.KIND)?.let {
                    RtcMediaKind.fromAlias(
                        it,
                    )
                }
            ) {
                RtcMediaKind.AUDIO -> toRtcRemoteOutboundRtpAudioStreamStats().also {
                    if (DEBUG) StreamLog.i(TAG) { "[toRtcStats] remoteOutboundRtpAudio: $it" }
                }

                RtcMediaKind.VIDEO -> toRtcRemoteOutboundRtpVideoStreamStats().also {
                    if (DEBUG) StreamLog.i(TAG) { "[toRtcStats] remoteOutboundRtpVideo: $it" }
                }

                else -> {
                    if (DEBUG) {
                        StreamLog.e(
                            TAG,
                        ) { "[toRtcStats] unexpected remoteOutboundRtp: $type($id)" }
                    }
                    null
                }
            }
        }

        RtcReportType.TRACK -> {
            when (
                getOrNull<String>(
                    RtcMediaStreamTrackStats.KIND,
                )?.let { RtcMediaKind.fromAlias(it) }
            ) {
                RtcMediaKind.AUDIO -> when (
                    getOrNull<Boolean>(
                        RtcMediaStreamTrackStats.REMOTE_SOURCE,
                    )
                ) {
                    true -> toRtcMediaStreamAudioTrackReceiverStats().also {
                        if (DEBUG) StreamLog.i(TAG) { "[toRtcStats] audioTrackReceiver: $it" }
                    }

                    false -> toRtcMediaStreamAudioTrackSenderStats().also {
                        if (DEBUG) StreamLog.i(TAG) { "[toRtcStats] audioTrackSender: $it" }
                    }

                    else -> {
                        if (DEBUG) {
                            StreamLog.e(
                                TAG,
                            ) { "[toRtcStats] unexpected audioTrack: $type($id)" }
                        }
                        null
                    }
                }

                RtcMediaKind.VIDEO -> when (
                    getOrNull<Boolean>(
                        RtcMediaStreamTrackStats.REMOTE_SOURCE,
                    )
                ) {
                    true -> toRtcMediaStreamVideoTrackReceiverStats().also {
                        if (DEBUG) StreamLog.i(TAG) { "[toRtcStats] videoTrackReceiver: $it" }
                    }

                    false -> toRtcMediaStreamVideoTrackSenderStats().also {
                        if (DEBUG) StreamLog.i(TAG) { "[toRtcStats] videoTrackSender: $it" }
                    }

                    else -> {
                        if (DEBUG) {
                            StreamLog.e(
                                TAG,
                            ) { "[toRtcStats] unexpected videoTrack: $type($id)" }
                        }
                        null
                    }
                }

                else -> {
                    if (DEBUG) {
                        StreamLog.e(
                            TAG,
                        ) { "[toRtcStats] unexpected outboundRtp: $type($id)" }
                    }
                    null
                }
            }
        }
        else -> {
            if (DEBUG) {
                StreamLog.e(
                    TAG,
                ) { "[toRtcStats] unexpected reportType: $type; ${toString()}" }
            }
            null
        }
    }
}

internal fun RTCStats.toRtcCodecStats() = RtcCodecStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    sdpFmtpLine = getOrNull(RtcCodecStats.SDP_FMTP_LINE),
    payloadType = getOrNull(RtcCodecStats.PAYLOAD_TYPE),
    transportId = getOrNull(RtcCodecStats.TRANSPORT_ID),
    mimeType = getOrNull(RtcCodecStats.MIME_TYPE),
    clockRate = getOrNull(RtcCodecStats.CLOCL_RATE),
)

internal fun RTCStats.toRtcIceCandidatePairStats() = RtcIceCandidatePairStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    transportId = getOrNull(RtcCodecStats.TRANSPORT_ID),
    requestsSent = getOrNull(RtcIceCandidatePairStats.REQUESTS_SENT),
    localCandidateId = getOrNull(RtcIceCandidatePairStats.LOCAL_CANDIDATE_ID),
    bytesSent = getOrNull(RtcIceCandidatePairStats.BYTES_SENT),
    bytesDiscardedOnSend = getOrNull(RtcIceCandidatePairStats.BYTES_DISCARDED_ON_SEND),
    priority = getOrNull(RtcIceCandidatePairStats.PRIORITY),
    requestsReceived = getOrNull(RtcIceCandidatePairStats.REQUESTS_RECEIVED),
    writable = getOrNull(RtcIceCandidatePairStats.WRITABLE),
    remoteCandidateId = getOrNull(RtcIceCandidatePairStats.REMOTE_CANDIDATE_ID),
    bytesReceived = getOrNull(RtcIceCandidatePairStats.BYTES_RECEIVED),
    packetsReceived = getOrNull(RtcIceCandidatePairStats.PACKETS_RECEIVED),
    responsesSent = getOrNull(RtcIceCandidatePairStats.RESPONSES_SENT),
    packetsDiscardedOnSend = getOrNull(RtcIceCandidatePairStats.PACKETS_DISCARDED_ON_SEND),
    nominated = getOrNull(RtcIceCandidatePairStats.NOMINATED),
    packetsSent = getOrNull(RtcIceCandidatePairStats.PACKETS_SENT),
    totalRoundTripTime = getOrNull(RtcIceCandidatePairStats.TOTAL_ROUND_TRIP_TIME),
    responsesReceived = getOrNull(RtcIceCandidatePairStats.RESPONSES_RECEIVED),
    state = getOrNull(RtcIceCandidatePairStats.STATE),
    consentRequestsSent = getOrNull(RtcIceCandidatePairStats.CONSENT_REQUESTS_SENT),
)

internal fun RTCStats.toRtcIceCandidateStats() = RtcIceCandidateStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    transportId = getOrNull(RtcCodecStats.TRANSPORT_ID),
    candidateType = getOrNull(RtcIceCandidateStats.CANDIDATE_TYPE),
    protocol = getOrNull(RtcIceCandidateStats.PROTOCOL),
    address = getOrNull(RtcIceCandidateStats.ADDRESS),
    port = getOrNull(RtcIceCandidateStats.PORT),
    vpn = getOrNull(RtcIceCandidateStats.VPN),
    isRemote = getOrNull(RtcIceCandidateStats.IS_REMOTE),
    ip = getOrNull(RtcIceCandidateStats.IP),
    networkAdapterType = getOrNull(RtcIceCandidateStats.NETWORK_ADAPTER_TYPE),
    networkType = getOrNull(RtcIceCandidateStats.NETWORK_TYPE),
    priority = getOrNull(RtcIceCandidateStats.PRIORITY),
    url = getOrNull(RtcIceCandidateStats.URL),
    relayProtocol = getOrNull(RtcIceCandidateStats.RELAY_PROTOCOL),
)

internal fun RTCStats.toRtcAudioSourceStats() = RtcAudioSourceStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    kind = getOrNull(RtcAudioSourceStats.KIND),
    trackIdentifier = getOrNull(RtcAudioSourceStats.TRACK_IDENTIFIER),
    audioLevel = getOrNull(RtcAudioSourceStats.AUDIO_LEVEL),
    totalAudioEnergy = getOrNull(RtcAudioSourceStats.TOTAL_AUDIO_ENERGY),
    totalSamplesDuration = getOrNull(RtcAudioSourceStats.TOTAL_SAMPLES_DURATION),
    echoReturnLoss = getOrNull(RtcAudioSourceStats.ECHO_RETURN_LOSS),
    echoReturnLossEnhancement = getOrNull(RtcAudioSourceStats.ECHO_RETURN_LOSS_ENHANCEMENT),
    droppedSamplesDuration = getOrNull(RtcAudioSourceStats.DROPPED_SAMPLES_DURATION),
    droppedSamplesEvents = getOrNull(RtcAudioSourceStats.DROPPED_SAMPLES_EVENTS),
    totalCaptureDelay = getOrNull(RtcAudioSourceStats.TOTAL_CAPTURE_DELAY),
    totalSamplesCaptured = getOrNull(RtcAudioSourceStats.TOTAL_SAMPLES_CAPTURED),
)

internal fun RTCStats.toRtcVideoSourceStats() = RtcVideoSourceStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    kind = getOrNull(RtcVideoSourceStats.KIND),
    trackIdentifier = getOrNull(RtcVideoSourceStats.TRACK_IDENTIFIER),
    width = getOrNull(RtcVideoSourceStats.WIDTH),
    height = getOrNull(RtcVideoSourceStats.HEIGHT),
    framesPerSecond = getOrNull(RtcVideoSourceStats.FRAMES_PER_SECOND),
    frames = getOrNull(RtcVideoSourceStats.FRAMES),
)

internal fun RTCStats.toRtcInboundRtpVideoStreamStats() = RtcInboundRtpVideoStreamStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    ssrc = getOrNull(RtcInboundRtpStreamStats.SSRC),
    kind = getOrNull(RtcInboundRtpStreamStats.KIND),
    transportId = getOrNull(RtcInboundRtpStreamStats.TRANSPORT_ID),
    codecId = getOrNull(RtcInboundRtpStreamStats.CODEC_ID),
    packetsReceived = getOrNull(RtcInboundRtpStreamStats.PACKETS_RECEIVED),
    packetsLost = getOrNull(RtcInboundRtpStreamStats.PACKETS_LOST),
    jitter = getOrNull(RtcInboundRtpStreamStats.JITTER),
    trackIdentifier = getOrNull(RtcInboundRtpStreamStats.TRACK_IDENTIFIER),
    mid = getOrNull(RtcInboundRtpStreamStats.MID),
    remoteId = getOrNull(RtcInboundRtpStreamStats.REMOTE_ID),
    lastPacketReceivedTimestamp = getOrNull(
        RtcInboundRtpStreamStats.LAST_PACKET_RECEIVED_TIMESTAMP,
    ),
    headerBytesReceived = getOrNull(RtcInboundRtpStreamStats.HEADER_BYTES_RECEIVED),
    packetsDiscarded = getOrNull(RtcInboundRtpStreamStats.PACKETS_DISCARDED),
    fecBytesReceived = getOrNull(RtcInboundRtpStreamStats.FEC_BYTES_RECEIVED),
    fecPacketsReceived = getOrNull(RtcInboundRtpStreamStats.FEC_PACKETS_RECEIVED),
    fecPacketsDiscarded = getOrNull(RtcInboundRtpStreamStats.FEC_PACKETS_DISCARDED),
    bytesReceived = getOrNull(RtcInboundRtpStreamStats.BYTES_RECEIVED),
    nackCount = getOrNull(RtcInboundRtpStreamStats.NACK_COUNT),
    totalProcessingDelay = getOrNull(RtcInboundRtpStreamStats.TOTAL_PROCESSING_DELAY),
    estimatedPlayoutTimestamp = getOrNull(RtcInboundRtpStreamStats.ESTIMATED_PLAYOUT_TIMESTAMP),
    jitterBufferDelay = getOrNull(RtcInboundRtpStreamStats.JITTER_BUFFER_DELAY),
    jitterBufferTargetDelay = getOrNull(RtcInboundRtpStreamStats.JITTER_BUFFER_TARGET_DELAY),
    jitterBufferEmittedCount = getOrNull(RtcInboundRtpStreamStats.JITTER_BUFFER_EMITTED_COUNT),
    jitterBufferMinimumDelay = getOrNull(RtcInboundRtpStreamStats.JITTER_BUFFER_MINIMUM_DELAY),
    decoderImplementation = getOrNull(RtcInboundRtpStreamStats.DECODER_IMPLEMENTATION),
    playoutId = getOrNull(RtcInboundRtpStreamStats.PLAYOUT_ID),
    powerEfficientDecoder = getOrNull(RtcInboundRtpStreamStats.POWER_EFFICIENT_DECODER),
    retransmittedPacketsReceived = getOrNull(
        RtcInboundRtpStreamStats.RETRANSMITTED_PACKETS_RECEIVED,
    ),
    retransmittedBytesReceived = getOrNull(RtcInboundRtpStreamStats.RETRANSMITTED_BYTES_RECEIVED),
    rtxSsrc = getOrNull(RtcInboundRtpStreamStats.RTX_SSRC),
    fecSsrc = getOrNull(RtcInboundRtpStreamStats.FEC_SSRC),
    framesDecoded = getOrNull(RtcInboundRtpVideoStreamStats.FRAMES_DECODED),
    keyFramesDecoded = getOrNull(RtcInboundRtpVideoStreamStats.KEY_FRAMES_DECODED),
    framesRendered = getOrNull(RtcInboundRtpVideoStreamStats.FRAMES_RENDERED),
    framesDropped = getOrNull(RtcInboundRtpVideoStreamStats.FRAMES_DROPPED),
    frameWidth = getOrNull(RtcInboundRtpVideoStreamStats.FRAME_WIDTH),
    frameHeight = getOrNull(RtcInboundRtpVideoStreamStats.FRAME_HEIGHT),
    framesPerSecond = getOrNull(RtcInboundRtpVideoStreamStats.FRAMES_PER_SECOND),
    qpSum = getOrNull(RtcInboundRtpVideoStreamStats.QP_SUM),
    totalDecodeTime = getOrNull(RtcInboundRtpVideoStreamStats.TOTAL_DECODE_TIME),
    totalInterFrameDelay = getOrNull(RtcInboundRtpVideoStreamStats.TOTAL_INTER_FRAME_DELAY),
    totalSquaredInterFrameDelay = getOrNull(
        RtcInboundRtpVideoStreamStats.TOTAL_SQUARED_INTER_FRAME_DELAY,
    ),
    pauseCount = getOrNull(RtcInboundRtpVideoStreamStats.PAUSE_COUNT),
    totalPausesDuration = getOrNull(RtcInboundRtpVideoStreamStats.TOTAL_PAUSES_DURATION),
    freezeCount = getOrNull(RtcInboundRtpVideoStreamStats.FREEZE_COUNT),
    totalFreezesDuration = getOrNull(RtcInboundRtpVideoStreamStats.TOTAL_FREEZES_DURATION),
    firCount = getOrNull(RtcInboundRtpVideoStreamStats.FIR_COUNT),
    pliCount = getOrNull(RtcInboundRtpVideoStreamStats.PLI_COUNT),
    framesReceived = getOrNull(RtcInboundRtpVideoStreamStats.FRAMES_RECEIVED),
    framesAssembledFromMultiplePackets = getOrNull(
        RtcInboundRtpVideoStreamStats.FRAMES_ASSEMBLED_FROM_MULTIPLE_PACKETS,
    ),
    totalAssemblyTime = getOrNull(RtcInboundRtpVideoStreamStats.TOTAL_ASSEMBLY_TIME),
)

internal fun RTCStats.toRtcInboundRtpAudioStreamStats() = RtcInboundRtpAudioStreamStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    ssrc = getOrNull(RtcInboundRtpStreamStats.SSRC),
    kind = getOrNull(RtcInboundRtpStreamStats.KIND),
    transportId = getOrNull(RtcInboundRtpStreamStats.TRANSPORT_ID),
    codecId = getOrNull(RtcInboundRtpStreamStats.CODEC_ID),
    packetsReceived = getOrNull(RtcInboundRtpStreamStats.PACKETS_RECEIVED),
    packetsLost = getOrNull(RtcInboundRtpStreamStats.PACKETS_LOST),
    jitter = getOrNull(RtcInboundRtpStreamStats.JITTER),
    trackIdentifier = getOrNull(RtcInboundRtpStreamStats.TRACK_IDENTIFIER),
    mid = getOrNull(RtcInboundRtpStreamStats.MID),
    remoteId = getOrNull(RtcInboundRtpStreamStats.REMOTE_ID),
    lastPacketReceivedTimestamp = getOrNull(
        RtcInboundRtpStreamStats.LAST_PACKET_RECEIVED_TIMESTAMP,
    ),
    headerBytesReceived = getOrNull(RtcInboundRtpStreamStats.HEADER_BYTES_RECEIVED),
    packetsDiscarded = getOrNull(RtcInboundRtpStreamStats.PACKETS_DISCARDED),
    fecBytesReceived = getOrNull(RtcInboundRtpStreamStats.FEC_BYTES_RECEIVED),
    fecPacketsReceived = getOrNull(RtcInboundRtpStreamStats.FEC_PACKETS_RECEIVED),
    fecPacketsDiscarded = getOrNull(RtcInboundRtpStreamStats.FEC_PACKETS_DISCARDED),
    bytesReceived = getOrNull(RtcInboundRtpStreamStats.BYTES_RECEIVED),
    nackCount = getOrNull(RtcInboundRtpStreamStats.NACK_COUNT),
    totalProcessingDelay = getOrNull(RtcInboundRtpStreamStats.TOTAL_PROCESSING_DELAY),
    estimatedPlayoutTimestamp = getOrNull(RtcInboundRtpStreamStats.ESTIMATED_PLAYOUT_TIMESTAMP),
    jitterBufferDelay = getOrNull(RtcInboundRtpStreamStats.JITTER_BUFFER_DELAY),
    jitterBufferTargetDelay = getOrNull(RtcInboundRtpStreamStats.JITTER_BUFFER_TARGET_DELAY),
    jitterBufferEmittedCount = getOrNull(RtcInboundRtpStreamStats.JITTER_BUFFER_EMITTED_COUNT),
    jitterBufferMinimumDelay = getOrNull(RtcInboundRtpStreamStats.JITTER_BUFFER_MINIMUM_DELAY),
    decoderImplementation = getOrNull(RtcInboundRtpStreamStats.DECODER_IMPLEMENTATION),
    playoutId = getOrNull(RtcInboundRtpStreamStats.PLAYOUT_ID),
    powerEfficientDecoder = getOrNull(RtcInboundRtpStreamStats.POWER_EFFICIENT_DECODER),
    retransmittedPacketsReceived = getOrNull(
        RtcInboundRtpStreamStats.RETRANSMITTED_PACKETS_RECEIVED,
    ),
    retransmittedBytesReceived = getOrNull(RtcInboundRtpStreamStats.RETRANSMITTED_BYTES_RECEIVED),
    rtxSsrc = getOrNull(RtcInboundRtpStreamStats.RTX_SSRC),
    fecSsrc = getOrNull(RtcInboundRtpStreamStats.FEC_SSRC),
    audioLevel = getOrNull(RtcInboundRtpAudioStreamStats.AUDIO_LEVEL),
    totalAudioEnergy = getOrNull(RtcInboundRtpAudioStreamStats.TOTAL_AUDIO_ENERGY),
    totalSamplesReceived = getOrNull(RtcInboundRtpAudioStreamStats.TOTAL_SAMPLES_RECEIVED),
    totalSamplesDuration = getOrNull(RtcInboundRtpAudioStreamStats.TOTAL_SAMPLES_DURATION),
    concealedSamples = getOrNull(RtcInboundRtpAudioStreamStats.CONCEALED_SAMPLES),
    silentConcealedSamples = getOrNull(RtcInboundRtpAudioStreamStats.SILENT_CONCEALED_SAMPLES),
    concealmentEvents = getOrNull(RtcInboundRtpAudioStreamStats.CONCEALMENT_EVENTS),
    insertedSamplesForDeceleration = getOrNull(
        RtcInboundRtpAudioStreamStats.INSERTED_SAMPLES_FOR_DECELERATION,
    ),
    removedSamplesForAcceleration = getOrNull(
        RtcInboundRtpAudioStreamStats.REMOVED_SAMPLES_FOR_ACCELERATION,
    ),
)

internal fun RTCStats.toRtcOutboundRtpVideoStreamStats() = RtcOutboundRtpVideoStreamStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    ssrc = getOrNull(RtcOutboundRtpVideoStreamStats.SSRC),
    kind = getOrNull(RtcOutboundRtpVideoStreamStats.KIND),
    transportId = getOrNull(RtcOutboundRtpVideoStreamStats.TRANSPORT_ID),
    codecId = getOrNull(RtcOutboundRtpVideoStreamStats.CODEC_ID),
    packetsSent = getOrNull(RtcOutboundRtpVideoStreamStats.PACKETS_SENT),
    bytesSent = getOrNull(RtcOutboundRtpVideoStreamStats.BYTES_SENT),
    mid = getOrNull(RtcOutboundRtpVideoStreamStats.MID),
    mediaSourceId = getOrNull(RtcOutboundRtpVideoStreamStats.MEDIA_SOURCE_ID),
    remoteId = getOrNull(RtcOutboundRtpVideoStreamStats.REMOTE_ID),
    headerBytesSent = getOrNull(RtcOutboundRtpVideoStreamStats.HEADER_BYTES_SENT),
    retransmittedPacketsSent = getOrNull(RtcOutboundRtpVideoStreamStats.RETRANSMITTED_PACKETS_SENT),
    retransmittedBytesSent = getOrNull(RtcOutboundRtpVideoStreamStats.RETRANSMITTED_BYTES_SENT),
    rtxSsrc = getOrNull(RtcOutboundRtpVideoStreamStats.RTX_SSRC),
    targetBitrate = getOrNull(RtcOutboundRtpVideoStreamStats.TARGET_BITRATE),
    totalEncodedBytesTarget = getOrNull(RtcOutboundRtpVideoStreamStats.TOTAL_ENCODED_BYTES_TARGET),
    totalEncodeTime = getOrNull(RtcOutboundRtpVideoStreamStats.TOTAL_ENCODE_TIME),
    totalPacketSendDelay = getOrNull(RtcOutboundRtpVideoStreamStats.TOTAL_PACKET_SEND_DELAY),
    active = getOrNull(RtcOutboundRtpVideoStreamStats.ACTIVE),
    rid = getOrNull(RtcOutboundRtpVideoStreamStats.RID),
    frameWidth = getOrNull(RtcOutboundRtpVideoStreamStats.FRAME_WIDTH),
    frameHeight = getOrNull(RtcOutboundRtpVideoStreamStats.FRAME_HEIGHT),
    framesPerSecond = getOrNull(RtcOutboundRtpVideoStreamStats.FRAMES_PER_SECOND),
    framesSent = getOrNull(RtcOutboundRtpVideoStreamStats.FRAMES_SENT),
    hugeFramesSent = getOrNull(RtcOutboundRtpVideoStreamStats.HUGE_FRAMES_SENT),
    framesEncoded = getOrNull(RtcOutboundRtpVideoStreamStats.FRAMES_ENCODED),
    keyFramesEncoded = getOrNull(RtcOutboundRtpVideoStreamStats.KEY_FRAMES_ENCODED),
    qpSum = getOrNull(RtcOutboundRtpVideoStreamStats.QP_SUM),
    qualityLimitationReason = getOrNull<String>(
        RtcOutboundRtpVideoStreamStats.QUALITY_LIMITATION_REASON,
    )?.let {
        RtcQualityLimitationReason.fromAlias(it)
    },
    qualityLimitationDurations = getOrNull<Map<String, Double>>(
        RtcOutboundRtpVideoStreamStats.QUALITY_LIMITATION_DURATIONS,
    )?.map {
        RtcQualityLimitationReason.fromAlias(it.key) to it.value
    }?.toMap(),
    qualityLimitationResolutionChanges = getOrNull(
        RtcOutboundRtpVideoStreamStats.QUALITY_LIMITATION_RESOLUTION_CHANGES,
    ),
    nackCount = getOrNull(RtcOutboundRtpVideoStreamStats.NACK_COUNT),
    firCount = getOrNull(RtcOutboundRtpVideoStreamStats.FIR_COUNT),
    pliCount = getOrNull(RtcOutboundRtpVideoStreamStats.PLI_COUNT),
    encoderImplementation = getOrNull(RtcOutboundRtpVideoStreamStats.ENCODER_IMPLEMENTATION),
    powerEfficientEncoder = getOrNull(RtcOutboundRtpVideoStreamStats.POWER_EFFICIENT_ENCODER),
    scalabilityMode = getOrNull(RtcOutboundRtpVideoStreamStats.SCALABILITY_MODE),
)

internal fun RTCStats.toRtcOutboundRtpAudioStreamStats() = RtcOutboundRtpAudioStreamStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    ssrc = getOrNull(RtcOutboundRtpAudioStreamStats.SSRC),
    kind = getOrNull(RtcOutboundRtpAudioStreamStats.KIND),
    transportId = getOrNull(RtcOutboundRtpAudioStreamStats.TRANSPORT_ID),
    codecId = getOrNull(RtcOutboundRtpAudioStreamStats.CODEC_ID),
    packetsSent = getOrNull(RtcOutboundRtpAudioStreamStats.PACKETS_SENT),
    bytesSent = getOrNull(RtcOutboundRtpAudioStreamStats.BYTES_SENT),
    mid = getOrNull(RtcOutboundRtpAudioStreamStats.MID),
    mediaSourceId = getOrNull(RtcOutboundRtpAudioStreamStats.MEDIA_SOURCE_ID),
    remoteId = getOrNull(RtcOutboundRtpAudioStreamStats.REMOTE_ID),
    headerBytesSent = getOrNull(RtcOutboundRtpAudioStreamStats.HEADER_BYTES_SENT),
    retransmittedPacketsSent = getOrNull(RtcOutboundRtpAudioStreamStats.RETRANSMITTED_PACKETS_SENT),
    retransmittedBytesSent = getOrNull(RtcOutboundRtpAudioStreamStats.RETRANSMITTED_BYTES_SENT),
    rtxSsrc = getOrNull(RtcOutboundRtpAudioStreamStats.RTX_SSRC),
    targetBitrate = getOrNull(RtcOutboundRtpAudioStreamStats.TARGET_BITRATE),
    totalEncodedBytesTarget = getOrNull(RtcOutboundRtpAudioStreamStats.TOTAL_ENCODED_BYTES_TARGET),
    totalEncodeTime = getOrNull(RtcOutboundRtpAudioStreamStats.TOTAL_ENCODE_TIME),
    totalPacketSendDelay = getOrNull(RtcOutboundRtpAudioStreamStats.TOTAL_PACKET_SEND_DELAY),
    active = getOrNull(RtcOutboundRtpAudioStreamStats.ACTIVE),
)

internal fun RTCStats.toRtcRemoteInboundRtpAudioStreamStats() = RtcRemoteInboundRtpAudioStreamStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    ssrc = getOrNull(RtcRemoteInboundRtpStreamStats.SSRC),
    kind = getOrNull(RtcRemoteInboundRtpStreamStats.KIND),
    transportId = getOrNull(RtcRemoteInboundRtpStreamStats.TRANSPORT_ID),
    codecId = getOrNull(RtcRemoteInboundRtpStreamStats.CODEC_ID),
    packetsReceived = getOrNull(RtcRemoteInboundRtpStreamStats.PACKETS_RECEIVED),
    packetsLost = getOrNull(RtcRemoteInboundRtpStreamStats.PACKETS_LOST),
    jitter = getOrNull(RtcRemoteInboundRtpStreamStats.JITTER),
    localId = getOrNull(RtcRemoteInboundRtpStreamStats.LOCAL_ID),
    roundTripTime = getOrNull(RtcRemoteInboundRtpStreamStats.ROUND_TRIP_TIME),
    totalRoundTripTime = getOrNull(RtcRemoteInboundRtpStreamStats.TOTAL_ROUND_TRIP_TIME),
    fractionLost = getOrNull(RtcRemoteInboundRtpStreamStats.FRACTION_LOST),
    roundTripTimeMeasurements = getOrNull(
        RtcRemoteInboundRtpStreamStats.ROUND_TRIP_TIME_MEASUREMENTS,
    ),
)

internal fun RTCStats.toRtcRemoteInboundRtpVideoStreamStats() = RtcRemoteInboundRtpVideoStreamStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    ssrc = getOrNull(RtcRemoteInboundRtpStreamStats.SSRC),
    kind = getOrNull(RtcRemoteInboundRtpStreamStats.KIND),
    transportId = getOrNull(RtcRemoteInboundRtpStreamStats.TRANSPORT_ID),
    codecId = getOrNull(RtcRemoteInboundRtpStreamStats.CODEC_ID),
    packetsReceived = getOrNull(RtcRemoteInboundRtpStreamStats.PACKETS_RECEIVED),
    packetsLost = getOrNull(RtcRemoteInboundRtpStreamStats.PACKETS_LOST),
    jitter = getOrNull(RtcRemoteInboundRtpStreamStats.JITTER),
    localId = getOrNull(RtcRemoteInboundRtpStreamStats.LOCAL_ID),
    roundTripTime = getOrNull(RtcRemoteInboundRtpStreamStats.ROUND_TRIP_TIME),
    totalRoundTripTime = getOrNull(RtcRemoteInboundRtpStreamStats.TOTAL_ROUND_TRIP_TIME),
    fractionLost = getOrNull(RtcRemoteInboundRtpStreamStats.FRACTION_LOST),
    roundTripTimeMeasurements = getOrNull(
        RtcRemoteInboundRtpStreamStats.ROUND_TRIP_TIME_MEASUREMENTS,
    ),
)

internal fun RTCStats.toRtcRemoteOutboundRtpAudioStreamStats() =
    RtcRemoteOutboundRtpAudioStreamStats(
        id = id,
        type = type,
        timestampUs = timestampUs,
        ssrc = getOrNull(RtcRemoteOutboundRtpStreamStats.SSRC),
        kind = getOrNull(RtcRemoteOutboundRtpStreamStats.KIND),
        transportId = getOrNull(RtcRemoteOutboundRtpStreamStats.TRANSPORT_ID),
        codecId = getOrNull(RtcRemoteOutboundRtpStreamStats.CODEC_ID),
        packetsSent = getOrNull(RtcRemoteOutboundRtpStreamStats.PACKETS_SENT),
        bytesSent = getOrNull(RtcRemoteOutboundRtpStreamStats.BYTES_SENT),
        localId = getOrNull(RtcRemoteOutboundRtpStreamStats.LOCAL_ID),
        remoteTimestamp = getOrNull(RtcRemoteOutboundRtpStreamStats.REMOTE_TIMESTAMP),
        reportsSent = getOrNull(RtcRemoteOutboundRtpStreamStats.REPORTS_SENT),
        roundTripTime = getOrNull(RtcRemoteOutboundRtpStreamStats.ROUND_TRIP_TIME),
        totalRoundTripTime = getOrNull(RtcRemoteOutboundRtpStreamStats.TOTAL_ROUND_TRIP_TIME),
        roundTripTimeMeasurements = getOrNull(
            RtcRemoteOutboundRtpStreamStats.ROUND_TRIP_TIME_MEASUREMENTS,
        ),
    )

internal fun RTCStats.toRtcRemoteOutboundRtpVideoStreamStats() =
    RtcRemoteOutboundRtpVideoStreamStats(
        id = id,
        type = type,
        timestampUs = timestampUs,
        ssrc = getOrNull(RtcRemoteOutboundRtpStreamStats.SSRC),
        kind = getOrNull(RtcRemoteOutboundRtpStreamStats.KIND),
        transportId = getOrNull(RtcRemoteOutboundRtpStreamStats.TRANSPORT_ID),
        codecId = getOrNull(RtcRemoteOutboundRtpStreamStats.CODEC_ID),
        packetsSent = getOrNull(RtcRemoteOutboundRtpStreamStats.PACKETS_SENT),
        bytesSent = getOrNull(RtcRemoteOutboundRtpStreamStats.BYTES_SENT),
        localId = getOrNull(RtcRemoteOutboundRtpStreamStats.LOCAL_ID),
        remoteTimestamp = getOrNull(RtcRemoteOutboundRtpStreamStats.REMOTE_TIMESTAMP),
        reportsSent = getOrNull(RtcRemoteOutboundRtpStreamStats.REPORTS_SENT),
        roundTripTime = getOrNull(RtcRemoteOutboundRtpStreamStats.ROUND_TRIP_TIME),
        totalRoundTripTime = getOrNull(RtcRemoteOutboundRtpStreamStats.TOTAL_ROUND_TRIP_TIME),
        roundTripTimeMeasurements = getOrNull(
            RtcRemoteOutboundRtpStreamStats.ROUND_TRIP_TIME_MEASUREMENTS,
        ),
    )

internal fun RTCStats.toRtcMediaStreamAudioTrackSenderStats() = RtcMediaStreamAudioTrackSenderStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    trackIdentifier = getOrNull(RtcMediaStreamAudioTrackSenderStats.TRACK_IDENTIFIER),
    ended = getOrNull(RtcMediaStreamAudioTrackSenderStats.ENDED),
    kind = getOrNull(RtcMediaStreamAudioTrackSenderStats.KIND),
    priority = getOrNull(RtcMediaStreamAudioTrackSenderStats.PRIORITY),
    remoteSource = getOrNull(RtcMediaStreamAudioTrackSenderStats.REMOTE_SOURCE),
    detached = getOrNull(RtcMediaStreamAudioTrackSenderStats.DETACHED),
    mediaSourceId = getOrNull(RtcMediaStreamAudioTrackSenderStats.MEDIA_SOURCE_ID),
    audioLevel = getOrNull(RtcMediaStreamAudioTrackSenderStats.AUDIO_LEVEL),
    totalAudioEnergy = getOrNull(RtcMediaStreamAudioTrackSenderStats.TOTAL_AUDIO_ENERGY),
    totalSamplesDuration = getOrNull(RtcMediaStreamAudioTrackSenderStats.TOTAL_SAMPLES_DURATION),
)

internal fun RTCStats.toRtcMediaStreamAudioTrackReceiverStats() =
    RtcMediaStreamAudioTrackReceiverStats(
        id = id,
        type = type,
        timestampUs = timestampUs,
        trackIdentifier = getOrNull(RtcMediaStreamAudioTrackReceiverStats.TRACK_IDENTIFIER),
        ended = getOrNull(RtcMediaStreamAudioTrackReceiverStats.ENDED),
        kind = getOrNull(RtcMediaStreamAudioTrackReceiverStats.KIND),
        priority = getOrNull(RtcMediaStreamAudioTrackReceiverStats.PRIORITY),
        remoteSource = getOrNull(RtcMediaStreamAudioTrackReceiverStats.REMOTE_SOURCE),
        detached = getOrNull(RtcMediaStreamAudioTrackReceiverStats.DETACHED),
        estimatedPlayoutTimestamp = getOrNull(
            RtcMediaStreamAudioTrackReceiverStats.ESTIMATED_PLAYOUT_TIMESTAMP,
        ),
        jitterBufferDelay = getOrNull(RtcMediaStreamAudioTrackReceiverStats.JITTER_BUFFER_DELAY),
        jitterBufferEmittedCount = getOrNull(
            RtcMediaStreamAudioTrackReceiverStats.JITTER_BUFFER_EMITTED_COUNT,
        ),
        totalAudioEnergy = getOrNull(RtcMediaStreamAudioTrackReceiverStats.TOTAL_AUDIO_ENERGY),
        totalInterruptionDuration = getOrNull(
            RtcMediaStreamAudioTrackReceiverStats.TOTAL_INTERRUPTION_DURATION,
        ),
        removedSamplesForAcceleration = getOrNull(
            RtcMediaStreamAudioTrackReceiverStats.REMOVED_SAMPLES_FOR_ACCELERATION,
        ),
        audioLevel = getOrNull(RtcMediaStreamAudioTrackReceiverStats.AUDIO_LEVEL),
        interruptionCount = getOrNull(RtcMediaStreamAudioTrackReceiverStats.INTERRUPTION_COUNT),
        relativePacketArrivalDelay = getOrNull(
            RtcMediaStreamAudioTrackReceiverStats.RELATIVE_PACKET_ARRIVAL_DELAY,
        ),
        jitterBufferFlushes = getOrNull(
            RtcMediaStreamAudioTrackReceiverStats.JITTER_BUFFER_FLUSHES,
        ),
        concealedSamples = getOrNull(RtcMediaStreamAudioTrackReceiverStats.CONCEALED_SAMPLES),
        jitterBufferTargetDelay = getOrNull(
            RtcMediaStreamAudioTrackReceiverStats.JITTER_BUFFER_TARGET_DELAY,
        ),
        totalSamplesDuration = getOrNull(
            RtcMediaStreamAudioTrackReceiverStats.TOTAL_SAMPLES_DURATION,
        ),
        insertedSamplesForDeceleration = getOrNull(
            RtcMediaStreamAudioTrackReceiverStats.INSERTED_SAMPLES_FOR_DECELERATION,
        ),
        delayedPacketOutageSamples = getOrNull(
            RtcMediaStreamAudioTrackReceiverStats.DELAYED_PACKET_OUTAGE_SAMPLES,
        ),
        totalSamplesReceived = getOrNull(
            RtcMediaStreamAudioTrackReceiverStats.TOTAL_SAMPLES_RECEIVED,
        ),
        concealmentEvents = getOrNull(RtcMediaStreamAudioTrackReceiverStats.CONCEALMENT_EVENTS),
        silentConcealedSamples = getOrNull(
            RtcMediaStreamAudioTrackReceiverStats.SILENT_CONCEALED_SAMPLES,
        ),
    )

internal fun RTCStats.toRtcMediaStreamVideoTrackReceiverStats() =
    RtcMediaStreamVideoTrackReceiverStats(
        id = id,
        type = type,
        timestampUs = timestampUs,
        trackIdentifier = getOrNull(RtcMediaStreamVideoTrackReceiverStats.TRACK_IDENTIFIER),
        ended = getOrNull(RtcMediaStreamVideoTrackReceiverStats.ENDED),
        kind = getOrNull(RtcMediaStreamVideoTrackReceiverStats.KIND),
        priority = getOrNull(RtcMediaStreamVideoTrackReceiverStats.PRIORITY),
        remoteSource = getOrNull(RtcMediaStreamVideoTrackReceiverStats.REMOTE_SOURCE),
        detached = getOrNull(RtcMediaStreamVideoTrackReceiverStats.DETACHED),
        estimatedPlayoutTimestamp = getOrNull(
            RtcMediaStreamVideoTrackReceiverStats.ESTIMATED_PLAYOUT_TIMESTAMP,
        ),
        jitterBufferDelay = getOrNull(RtcMediaStreamVideoTrackReceiverStats.JITTER_BUFFER_DELAY),
        jitterBufferEmittedCount = getOrNull(
            RtcMediaStreamVideoTrackReceiverStats.JITTER_BUFFER_EMITTED_COUNT,
        ),
        frameHeight = getOrNull(RtcMediaStreamVideoTrackReceiverStats.FRAME_HEIGHT),
        frameWidth = getOrNull(RtcMediaStreamVideoTrackReceiverStats.FRAME_WIDTH),
        framesPerSecond = getOrNull(RtcMediaStreamVideoTrackReceiverStats.FRAMES_PER_SECOND),
        framesReceived = getOrNull(RtcMediaStreamVideoTrackReceiverStats.FRAMES_RECEIVED),
        framesDecoded = getOrNull(RtcMediaStreamVideoTrackReceiverStats.FRAMES_DECODED),
        framesDropped = getOrNull(RtcMediaStreamVideoTrackReceiverStats.FRAMES_DROPPED),
        totalFramesDuration = getOrNull(
            RtcMediaStreamVideoTrackReceiverStats.TOTAL_FRAMES_DURATION,
        ),
        totalFreezesDuration = getOrNull(
            RtcMediaStreamVideoTrackReceiverStats.TOTAL_FREEZES_DURATION,
        ),
        freezeCount = getOrNull(RtcMediaStreamVideoTrackReceiverStats.FREEZE_COUNT),
        pauseCount = getOrNull(RtcMediaStreamVideoTrackReceiverStats.PAUSE_COUNT),
        totalPausesDuration = getOrNull(
            RtcMediaStreamVideoTrackReceiverStats.TOTAL_PAUSES_DURATION,
        ),
        sumOfSquaredFramesDuration = getOrNull(
            RtcMediaStreamVideoTrackReceiverStats.SUM_OF_SQUARED_FRAMES_DURATION,
        ),
    )

internal fun RTCStats.toRtcMediaStreamVideoTrackSenderStats() = RtcMediaStreamVideoTrackSenderStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    trackIdentifier = getOrNull(RtcMediaStreamVideoTrackSenderStats.TRACK_IDENTIFIER),
    ended = getOrNull(RtcMediaStreamVideoTrackSenderStats.ENDED),
    kind = getOrNull(RtcMediaStreamVideoTrackSenderStats.KIND),
    priority = getOrNull(RtcMediaStreamVideoTrackSenderStats.PRIORITY),
    remoteSource = getOrNull(RtcMediaStreamVideoTrackSenderStats.REMOTE_SOURCE),
    detached = getOrNull(RtcMediaStreamVideoTrackSenderStats.DETACHED),
    mediaSourceId = getOrNull(RtcMediaStreamVideoTrackSenderStats.MEDIA_SOURCE_ID),
    frameHeight = getOrNull(RtcMediaStreamVideoTrackSenderStats.FRAME_HEIGHT),
    frameWidth = getOrNull(RtcMediaStreamVideoTrackSenderStats.FRAME_WIDTH),
    framesPerSecond = getOrNull(RtcMediaStreamVideoTrackSenderStats.FRAMES_PER_SECOND),
    keyFramesSent = getOrNull(RtcMediaStreamVideoTrackSenderStats.KEY_FRAMES_SENT),
    framesCaptured = getOrNull(RtcMediaStreamVideoTrackSenderStats.FRAMES_CAPTURED),
    framesSent = getOrNull(RtcMediaStreamVideoTrackSenderStats.FRAMES_SENT),
    hugeFramesSent = getOrNull(RtcMediaStreamVideoTrackSenderStats.HUGE_FRAMES_SENT),
)

internal inline fun <reified T> RTCStats.getOrNull(key: String): T? {
    val found = members[key] ?: return null
    if (found !is T) {
        StreamLog.w(TAG) {
            "[toRtcStats] '$key' => expected ${T::class.java.simpleName} " +
                "but found ${found::class.java.simpleName}"
        }
        return null
    }
    return found
}
