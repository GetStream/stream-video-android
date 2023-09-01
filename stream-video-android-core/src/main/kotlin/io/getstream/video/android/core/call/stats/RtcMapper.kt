package io.getstream.video.android.core.call.stats

import io.getstream.log.StreamLog
import io.getstream.video.android.core.call.stats.model.RtcAudioSourceStats
import io.getstream.video.android.core.call.stats.model.RtcCodecStats
import io.getstream.video.android.core.call.stats.model.RtcIceCandidatePairStats
import io.getstream.video.android.core.call.stats.model.RtcIceCandidateStats
import io.getstream.video.android.core.call.stats.model.RtcInboundRtpAudioStreamStats
import io.getstream.video.android.core.call.stats.model.RtcInboundRtpStreamStats
import io.getstream.video.android.core.call.stats.model.RtcInboundRtpVideoStreamStats
import io.getstream.video.android.core.call.stats.model.discriminator.RtcMediaKind
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
import io.getstream.video.android.core.call.stats.model.discriminator.RtcReportType
import io.getstream.video.android.core.call.stats.model.RtcStats
import io.getstream.video.android.core.call.stats.model.RtcVideoSourceStats
import io.getstream.video.android.core.call.stats.model.discriminator.RtcQualityLimitationReason
import java.math.BigInteger
import org.webrtc.RTCStats
import org.webrtc.RTCStatsReport

fun RTCStatsReport.toRtcStats() {

    for (report in statsMap.values) {
        report.toRtcStats()
    }
}

private fun RTCStats.toRtcStats(): RtcStats? {
    return when (RtcReportType.fromAlias(type)) {
        RtcReportType.CODEC -> toRtcCodecStats().also {
            StreamLog.i("RtcParser") { "[toRtcStats] codec: $it" }
        }

        RtcReportType.CANDIDATE_PAIR -> toRtcIceCandidatePairStats().also {
            StreamLog.i("RtcParser") { "[toRtcStats] candidatePair: $it" }
        }

        RtcReportType.LOCAL_CANDIDATE -> toRtcIceCandidateStats().also {
            StreamLog.i("RtcParser") { "[toRtcStats] localCandidate: $it" }
        }

        RtcReportType.REMOTE_CANDIDATE -> toRtcIceCandidateStats().also {
            StreamLog.i("RtcParser") { "[toRtcStats] remoteCandidate: $it" }
        }

        RtcReportType.MEDIA_SOURCE -> {
            when (getStringOrNull(RtcMediaSourceStats.KIND)?.let { RtcMediaKind.fromAlias(it) }) {
                RtcMediaKind.AUDIO -> toRtcAudioSourceStats().also {
                    StreamLog.i("RtcParser") { "[toRtcStats] audioSource: $it" }
                }

                RtcMediaKind.VIDEO -> toRtcVideoSourceStats().also {
                    StreamLog.i("RtcParser") { "[toRtcStats] videoSource: $it" }
                }

                else -> {
                    StreamLog.e("RtcParser") { "[toRtcStats] unexpected mediaSource: $type($id)" }
                    null
                }
            }
        }

        RtcReportType.INBOUND_RTP -> {
            when (getStringOrNull(RtcInboundRtpStreamStats.KIND)?.let { RtcMediaKind.fromAlias(it) }) {
                RtcMediaKind.AUDIO -> toRtcInboundRtpAudioStreamStats().also {
                    StreamLog.i("RtcParser") { "[toRtcStats] inboundRtpAudio: $it" }
                }

                RtcMediaKind.VIDEO -> toRtcInboundRtpVideoStreamStats().also {
                    StreamLog.i("RtcParser") { "[toRtcStats] inboundRtpVideo: $it" }
                }

                else -> {
                    StreamLog.e("RtcParser") { "[toRtcStats] unexpected inboundRtp: $type($id)" }
                    null
                }
            }
        }

        RtcReportType.OUTBOUND_RTP -> {
            when (getStringOrNull(RtcOutboundRtpStreamStats.KIND)?.let { RtcMediaKind.fromAlias(it) }) {
                RtcMediaKind.AUDIO -> toRtcOutboundRtpAudioStreamStats().also {
                    StreamLog.i("RtcParser") { "[toRtcStats] outboundRtpAudio: $it" }
                }

                RtcMediaKind.VIDEO -> toRtcOutboundRtpVideoStreamStats().also {
                    StreamLog.i("RtcParser") { "[toRtcStats] outboundRtpVideo: $it" }
                }

                else -> {
                    StreamLog.e("RtcParser") { "[toRtcStats] unexpected outboundRtp: $type($id)" }
                    null
                }
            }
        }

        RtcReportType.REMOTE_INBOUND_RTP -> {
            when (getStringOrNull(RtcRemoteInboundRtpStreamStats.KIND)?.let { RtcMediaKind.fromAlias(it) }) {
                RtcMediaKind.AUDIO -> toRtcRemoteInboundRtpAudioStreamStats().also {
                    StreamLog.i("RtcParser") { "[toRtcStats] remoteInboundRtpAudio: $it" }
                }

                RtcMediaKind.VIDEO -> toRtcRemoteInboundRtpVideoStreamStats().also {
                    StreamLog.i("RtcParser") { "[toRtcStats] remoteInboundRtpVideo: $it" }
                }

                else -> {
                    StreamLog.e("RtcParser") { "[toRtcStats] unexpected remoteInboundRtp: $type($id)" }
                    null
                }
            }
        }

        RtcReportType.REMOTE_OUTBOUND_RTP -> {
            when (getStringOrNull(RtcRemoteOutboundRtpStreamStats.KIND)?.let { RtcMediaKind.fromAlias(it) }) {
                RtcMediaKind.AUDIO -> toRtcRemoteOutboundRtpAudioStreamStats().also {
                    StreamLog.i("RtcParser") { "[toRtcStats] remoteOutboundRtpAudio: $it" }
                }

                RtcMediaKind.VIDEO -> toRtcRemoteOutboundRtpVideoStreamStats().also {
                    StreamLog.i("RtcParser") { "[toRtcStats] remoteOutboundRtpVideo: $it" }
                }

                else -> {
                    StreamLog.e("RtcParser") { "[toRtcStats] unexpected remoteOutboundRtp: $type($id)" }
                    null
                }
            }
        }

        RtcReportType.TRACK -> {
            when (getStringOrNull(RtcMediaStreamTrackStats.KIND)?.let { RtcMediaKind.fromAlias(it) }) {
                RtcMediaKind.AUDIO -> when (getBooleanOrNull(RtcMediaStreamTrackStats.REMOTE_SOURCE)) {
                    true -> toRtcMediaStreamAudioTrackReceiverStats().also {
                        StreamLog.i("RtcParser") { "[toRtcStats] audioTrackReceiver: $it" }
                    }
                    false -> toRtcMediaStreamAudioTrackSenderStats().also {
                        StreamLog.i("RtcParser") { "[toRtcStats] audioTrackSender: $it" }
                    }
                    else -> {
                        StreamLog.e("RtcParser") { "[toRtcStats] unexpected audioTrack: $type($id)" }
                        null
                    }
                }

                RtcMediaKind.VIDEO -> when (getBooleanOrNull(RtcMediaStreamTrackStats.REMOTE_SOURCE)) {
                    true -> toRtcMediaStreamVideoTrackReceiverStats().also {
                        StreamLog.i("RtcParser") { "[toRtcStats] videoTrackReceiver: $it" }
                    }
                    false -> toRtcMediaStreamVideoTrackSenderStats().also {
                        StreamLog.i("RtcParser") { "[toRtcStats] videoTrackSender: $it" }
                    }
                    else -> {
                        StreamLog.e("RtcParser") { "[toRtcStats] unexpected videoTrack: $type($id)" }
                        null
                    }
                }

                else -> {
                    StreamLog.e("RtcParser") { "[toRtcStats] unexpected outboundRtp: $type($id)" }
                    null
                }
            }
        }

        else -> {
            StreamLog.e("RtcParser") { "[toRtcStats] unexpected reportType: $type; ${toString()}" }
            null
        }
    }
}

private fun RTCStats.toRtcCodecStats() = RtcCodecStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    sdpFmtpLine = getStringOrNull(RtcCodecStats.SDP_FMTP_LINE),
    payloadType = getLongOrNull(RtcCodecStats.PAYLOAD_TYPE),
    transportId = getStringOrNull(RtcCodecStats.TRANSPORT_ID),
    mimeType = getStringOrNull(RtcCodecStats.MIME_TYPE),
    clockRate = getLongOrNull(RtcCodecStats.CLOCL_RATE),
)


private fun RTCStats.toRtcIceCandidatePairStats() = RtcIceCandidatePairStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    transportId = getStringOrNull(RtcCodecStats.TRANSPORT_ID),
    requestsSent = getBigIntegerOrNull(RtcIceCandidatePairStats.REQUESTS_SENT),
    localCandidateId = getStringOrNull(RtcIceCandidatePairStats.LOCAL_CANDIDATE_ID),
    bytesSent = getBigIntegerOrNull(RtcIceCandidatePairStats.BYTES_SENT),
    bytesDiscardedOnSend = getBigIntegerOrNull(RtcIceCandidatePairStats.BYTES_DISCARDED_ON_SEND),
    priority = getBigIntegerOrNull(RtcIceCandidatePairStats.PRIORITY),
    requestsReceived = getBigIntegerOrNull(RtcIceCandidatePairStats.REQUESTS_RECEIVED),
    writable = getBooleanOrNull(RtcIceCandidatePairStats.WRITABLE),
    remoteCandidateId = getStringOrNull(RtcIceCandidatePairStats.REMOTE_CANDIDATE_ID),
    bytesReceived = getBigIntegerOrNull(RtcIceCandidatePairStats.BYTES_RECEIVED),
    packetsReceived = getBigIntegerOrNull(RtcIceCandidatePairStats.PACKETS_RECEIVED),
    responsesSent = getBigIntegerOrNull(RtcIceCandidatePairStats.RESPONSES_SENT),
    packetsDiscardedOnSend = getBigIntegerOrNull(RtcIceCandidatePairStats.PACKETS_DISCARDED_ON_SEND),
    nominated = getBooleanOrNull(RtcIceCandidatePairStats.NOMINATED),
    packetsSent = getBigIntegerOrNull(RtcIceCandidatePairStats.PACKETS_SENT),
    totalRoundTripTime = getDoubleOrNull(RtcIceCandidatePairStats.TOTAL_ROUND_TRIP_TIME),
    responsesReceived = getBigIntegerOrNull(RtcIceCandidatePairStats.RESPONSES_RECEIVED),
    state = getStringOrNull(RtcIceCandidatePairStats.STATE),
    consentRequestsSent = getBigIntegerOrNull(RtcIceCandidatePairStats.CONSENT_REQUESTS_SENT),
)

private fun RTCStats.toRtcIceCandidateStats() = RtcIceCandidateStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    transportId = getStringOrNull(RtcCodecStats.TRANSPORT_ID),
    candidateType = getStringOrNull(RtcIceCandidateStats.CANDIDATE_TYPE),
    protocol = getStringOrNull(RtcIceCandidateStats.PROTOCOL),
    address = getStringOrNull(RtcIceCandidateStats.ADDRESS),
    port = getIntOrNull(RtcIceCandidateStats.PORT),
    vpn = getBooleanOrNull(RtcIceCandidateStats.VPN),
    isRemote = getBooleanOrNull(RtcIceCandidateStats.IS_REMOTE),
    ip = getStringOrNull(RtcIceCandidateStats.IP),
    networkAdapterType = getStringOrNull(RtcIceCandidateStats.NETWORK_ADAPTER_TYPE),
    networkType = getStringOrNull(RtcIceCandidateStats.NETWORK_TYPE),
    priority = getIntOrNull(RtcIceCandidateStats.PRIORITY),
    url = getStringOrNull(RtcIceCandidateStats.URL),
    relayProtocol = getStringOrNull(RtcIceCandidateStats.RELAY_PROTOCOL),
)

private fun RTCStats.toRtcAudioSourceStats() = RtcAudioSourceStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    kind = getStringOrNull(RtcAudioSourceStats.KIND),
    trackIdentifier = getStringOrNull(RtcAudioSourceStats.TRACK_IDENTIFIER),
    audioLevel = getDoubleOrNull(RtcAudioSourceStats.AUDIO_LEVEL),
    totalAudioEnergy = getDoubleOrNull(RtcAudioSourceStats.TOTAL_AUDIO_ENERGY),
    totalSamplesDuration = getDoubleOrNull(RtcAudioSourceStats.TOTAL_SAMPLES_DURATION),
    echoReturnLoss = getDoubleOrNull(RtcAudioSourceStats.ECHO_RETURN_LOSS),
    echoReturnLossEnhancement = getDoubleOrNull(RtcAudioSourceStats.ECHO_RETURN_LOSS_ENHANCEMENT),
    droppedSamplesDuration = getDoubleOrNull(RtcAudioSourceStats.DROPPED_SAMPLES_DURATION),
    droppedSamplesEvents = getLongOrNull(RtcAudioSourceStats.DROPPED_SAMPLES_EVENTS),
    totalCaptureDelay = getDoubleOrNull(RtcAudioSourceStats.TOTAL_CAPTURE_DELAY),
    totalSamplesCaptured = getLongOrNull(RtcAudioSourceStats.TOTAL_SAMPLES_CAPTURED),
)

private fun RTCStats.toRtcVideoSourceStats() = RtcVideoSourceStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    kind = getStringOrNull(RtcVideoSourceStats.KIND),
    trackIdentifier = getStringOrNull(RtcVideoSourceStats.TRACK_IDENTIFIER),
    width = getLongOrNull(RtcVideoSourceStats.WIDTH),
    height = getLongOrNull(RtcVideoSourceStats.HEIGHT),
    framesPerSecond = getDoubleOrNull(RtcVideoSourceStats.FRAMES_PER_SECOND),
    frames = getLongOrNull(RtcVideoSourceStats.FRAMES),
)

private fun RTCStats.toRtcInboundRtpVideoStreamStats() = RtcInboundRtpVideoStreamStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    ssrc = getLongOrNull(RtcInboundRtpStreamStats.SSRC),
    kind = getStringOrNull(RtcInboundRtpStreamStats.KIND),
    transportId = getStringOrNull(RtcInboundRtpStreamStats.TRANSPORT_ID),
    codecId = getStringOrNull(RtcInboundRtpStreamStats.CODEC_ID),
    packetsReceived = getLongOrNull(RtcInboundRtpStreamStats.PACKETS_RECEIVED),
    packetsLost = getIntOrNull(RtcInboundRtpStreamStats.PACKETS_LOST),
    jitter = getDoubleOrNull(RtcInboundRtpStreamStats.JITTER),
    trackIdentifier = getStringOrNull(RtcInboundRtpStreamStats.TRACK_IDENTIFIER),
    mid = getStringOrNull(RtcInboundRtpStreamStats.MID),
    remoteId = getStringOrNull(RtcInboundRtpStreamStats.REMOTE_ID),
    lastPacketReceivedTimestamp = getDoubleOrNull(RtcInboundRtpStreamStats.LAST_PACKET_RECEIVED_TIMESTAMP),
    headerBytesReceived = getBigIntegerOrNull(RtcInboundRtpStreamStats.HEADER_BYTES_RECEIVED),
    packetsDiscarded = getBigIntegerOrNull(RtcInboundRtpStreamStats.PACKETS_DISCARDED),
    fecBytesReceived = getBigIntegerOrNull(RtcInboundRtpStreamStats.FEC_BYTES_RECEIVED),
    fecPacketsReceived = getBigIntegerOrNull(RtcInboundRtpStreamStats.FEC_PACKETS_RECEIVED),
    fecPacketsDiscarded = getBigIntegerOrNull(RtcInboundRtpStreamStats.FEC_PACKETS_DISCARDED),
    bytesReceived = getBigIntegerOrNull(RtcInboundRtpStreamStats.BYTES_RECEIVED),
    nackCount = getLongOrNull(RtcInboundRtpStreamStats.NACK_COUNT),
    totalProcessingDelay = getDoubleOrNull(RtcInboundRtpStreamStats.TOTAL_PROCESSING_DELAY),
    estimatedPlayoutTimestamp = getDoubleOrNull(RtcInboundRtpStreamStats.ESTIMATED_PLAYOUT_TIMESTAMP),
    jitterBufferDelay = getDoubleOrNull(RtcInboundRtpStreamStats.JITTER_BUFFER_DELAY),
    jitterBufferTargetDelay = getDoubleOrNull(RtcInboundRtpStreamStats.JITTER_BUFFER_TARGET_DELAY),
    jitterBufferEmittedCount = getBigIntegerOrNull(RtcInboundRtpStreamStats.JITTER_BUFFER_EMITTED_COUNT),
    jitterBufferMinimumDelay = getDoubleOrNull(RtcInboundRtpStreamStats.JITTER_BUFFER_MINIMUM_DELAY),
    decoderImplementation = getStringOrNull(RtcInboundRtpStreamStats.DECODER_IMPLEMENTATION),
    playoutId = getStringOrNull(RtcInboundRtpStreamStats.PLAYOUT_ID),
    powerEfficientDecoder = getBooleanOrNull(RtcInboundRtpStreamStats.POWER_EFFICIENT_DECODER),
    retransmittedPacketsReceived = getBigIntegerOrNull(RtcInboundRtpStreamStats.RETRANSMITTED_PACKETS_RECEIVED),
    retransmittedBytesReceived = getBigIntegerOrNull(RtcInboundRtpStreamStats.RETRANSMITTED_BYTES_RECEIVED),
    rtxSsrc = getLongOrNull(RtcInboundRtpStreamStats.RTX_SSRC),
    fecSsrc = getLongOrNull(RtcInboundRtpStreamStats.FEC_SSRC),
    framesDecoded = getLongOrNull(RtcInboundRtpVideoStreamStats.FRAMES_DECODED),
    keyFramesDecoded = getLongOrNull(RtcInboundRtpVideoStreamStats.KEY_FRAMES_DECODED),
    framesRendered = getLongOrNull(RtcInboundRtpVideoStreamStats.FRAMES_RENDERED),
    framesDropped = getLongOrNull(RtcInboundRtpVideoStreamStats.FRAMES_DROPPED),
    frameWidth = getLongOrNull(RtcInboundRtpVideoStreamStats.FRAME_WIDTH),
    frameHeight = getLongOrNull(RtcInboundRtpVideoStreamStats.FRAME_HEIGHT),
    framesPerSecond = getDoubleOrNull(RtcInboundRtpVideoStreamStats.FRAMES_PER_SECOND),
    qpSum = getBigIntegerOrNull(RtcInboundRtpVideoStreamStats.QP_SUM),
    totalDecodeTime = getDoubleOrNull(RtcInboundRtpVideoStreamStats.TOTAL_DECODE_TIME),
    totalInterFrameDelay = getDoubleOrNull(RtcInboundRtpVideoStreamStats.TOTAL_INTER_FRAME_DELAY),
    totalSquaredInterFrameDelay = getDoubleOrNull(RtcInboundRtpVideoStreamStats.TOTAL_SQUARED_INTER_FRAME_DELAY),
    pauseCount = getLongOrNull(RtcInboundRtpVideoStreamStats.PAUSE_COUNT),
    totalPausesDuration = getDoubleOrNull(RtcInboundRtpVideoStreamStats.TOTAL_PAUSES_DURATION),
    freezeCount = getLongOrNull(RtcInboundRtpVideoStreamStats.FREEZE_COUNT),
    totalFreezesDuration = getDoubleOrNull(RtcInboundRtpVideoStreamStats.TOTAL_FREEZES_DURATION),
    firCount = getLongOrNull(RtcInboundRtpVideoStreamStats.FIR_COUNT),
    pliCount = getLongOrNull(RtcInboundRtpVideoStreamStats.PLI_COUNT),
    framesReceived = getLongOrNull(RtcInboundRtpVideoStreamStats.FRAMES_RECEIVED),
    framesAssembledFromMultiplePackets = getLongOrNull(RtcInboundRtpVideoStreamStats.FRAMES_ASSEMBLED_FROM_MULTIPLE_PACKETS),
    totalAssemblyTime = getDoubleOrNull(RtcInboundRtpVideoStreamStats.TOTAL_ASSEMBLY_TIME),
)

private fun RTCStats.toRtcInboundRtpAudioStreamStats() = RtcInboundRtpAudioStreamStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    ssrc = getLongOrNull(RtcInboundRtpStreamStats.SSRC),
    kind = getStringOrNull(RtcInboundRtpStreamStats.KIND),
    transportId = getStringOrNull(RtcInboundRtpStreamStats.TRANSPORT_ID),
    codecId = getStringOrNull(RtcInboundRtpStreamStats.CODEC_ID),
    packetsReceived = getLongOrNull(RtcInboundRtpStreamStats.PACKETS_RECEIVED),
    packetsLost = getIntOrNull(RtcInboundRtpStreamStats.PACKETS_LOST),
    jitter = getDoubleOrNull(RtcInboundRtpStreamStats.JITTER),
    trackIdentifier = getStringOrNull(RtcInboundRtpStreamStats.TRACK_IDENTIFIER),
    mid = getStringOrNull(RtcInboundRtpStreamStats.MID),
    remoteId = getStringOrNull(RtcInboundRtpStreamStats.REMOTE_ID),
    lastPacketReceivedTimestamp = getDoubleOrNull(RtcInboundRtpStreamStats.LAST_PACKET_RECEIVED_TIMESTAMP),
    headerBytesReceived = getBigIntegerOrNull(RtcInboundRtpStreamStats.HEADER_BYTES_RECEIVED),
    packetsDiscarded = getBigIntegerOrNull(RtcInboundRtpStreamStats.PACKETS_DISCARDED),
    fecBytesReceived = getBigIntegerOrNull(RtcInboundRtpStreamStats.FEC_BYTES_RECEIVED),
    fecPacketsReceived = getBigIntegerOrNull(RtcInboundRtpStreamStats.FEC_PACKETS_RECEIVED),
    fecPacketsDiscarded = getBigIntegerOrNull(RtcInboundRtpStreamStats.FEC_PACKETS_DISCARDED),
    bytesReceived = getBigIntegerOrNull(RtcInboundRtpStreamStats.BYTES_RECEIVED),
    nackCount = getLongOrNull(RtcInboundRtpStreamStats.NACK_COUNT),
    totalProcessingDelay = getDoubleOrNull(RtcInboundRtpStreamStats.TOTAL_PROCESSING_DELAY),
    estimatedPlayoutTimestamp = getDoubleOrNull(RtcInboundRtpStreamStats.ESTIMATED_PLAYOUT_TIMESTAMP),
    jitterBufferDelay = getDoubleOrNull(RtcInboundRtpStreamStats.JITTER_BUFFER_DELAY),
    jitterBufferTargetDelay = getDoubleOrNull(RtcInboundRtpStreamStats.JITTER_BUFFER_TARGET_DELAY),
    jitterBufferEmittedCount = getBigIntegerOrNull(RtcInboundRtpStreamStats.JITTER_BUFFER_EMITTED_COUNT),
    jitterBufferMinimumDelay = getDoubleOrNull(RtcInboundRtpStreamStats.JITTER_BUFFER_MINIMUM_DELAY),
    decoderImplementation = getStringOrNull(RtcInboundRtpStreamStats.DECODER_IMPLEMENTATION),
    playoutId = getStringOrNull(RtcInboundRtpStreamStats.PLAYOUT_ID),
    powerEfficientDecoder = getBooleanOrNull(RtcInboundRtpStreamStats.POWER_EFFICIENT_DECODER),
    retransmittedPacketsReceived = getBigIntegerOrNull(RtcInboundRtpStreamStats.RETRANSMITTED_PACKETS_RECEIVED),
    retransmittedBytesReceived = getBigIntegerOrNull(RtcInboundRtpStreamStats.RETRANSMITTED_BYTES_RECEIVED),
    rtxSsrc = getLongOrNull(RtcInboundRtpStreamStats.RTX_SSRC),
    fecSsrc = getLongOrNull(RtcInboundRtpStreamStats.FEC_SSRC),
    audioLevel = getDoubleOrNull(RtcInboundRtpAudioStreamStats.AUDIO_LEVEL),
    totalAudioEnergy = getDoubleOrNull(RtcInboundRtpAudioStreamStats.TOTAL_AUDIO_ENERGY),
    totalSamplesReceived = getBigIntegerOrNull(RtcInboundRtpAudioStreamStats.TOTAL_SAMPLES_RECEIVED),
    totalSamplesDuration = getDoubleOrNull(RtcInboundRtpAudioStreamStats.TOTAL_SAMPLES_DURATION),
    concealedSamples = getBigIntegerOrNull(RtcInboundRtpAudioStreamStats.CONCEALED_SAMPLES),
    silentConcealedSamples = getBigIntegerOrNull(RtcInboundRtpAudioStreamStats.SILENT_CONCEALED_SAMPLES),
    concealmentEvents = getBigIntegerOrNull(RtcInboundRtpAudioStreamStats.CONCEALMENT_EVENTS),
    insertedSamplesForDeceleration = getBigIntegerOrNull(RtcInboundRtpAudioStreamStats.INSERTED_SAMPLES_FOR_DECELERATION),
    removedSamplesForAcceleration = getBigIntegerOrNull(RtcInboundRtpAudioStreamStats.REMOVED_SAMPLES_FOR_ACCELERATION),
)

private fun RTCStats.toRtcOutboundRtpVideoStreamStats() = RtcOutboundRtpVideoStreamStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    ssrc = getLongOrNull(RtcOutboundRtpVideoStreamStats.SSRC),
    kind = getStringOrNull(RtcOutboundRtpVideoStreamStats.KIND),
    transportId = getStringOrNull(RtcOutboundRtpVideoStreamStats.TRANSPORT_ID),
    codecId = getStringOrNull(RtcOutboundRtpVideoStreamStats.CODEC_ID),
    packetsSent = getBigIntegerOrNull(RtcOutboundRtpVideoStreamStats.PACKETS_SENT),
    bytesSent = getBigIntegerOrNull(RtcOutboundRtpVideoStreamStats.BYTES_SENT),
    mid = getStringOrNull(RtcOutboundRtpVideoStreamStats.MID),
    mediaSourceId = getStringOrNull(RtcOutboundRtpVideoStreamStats.MEDIA_SOURCE_ID),
    remoteId = getStringOrNull(RtcOutboundRtpVideoStreamStats.REMOTE_ID),
    headerBytesSent = getBigIntegerOrNull(RtcOutboundRtpVideoStreamStats.HEADER_BYTES_SENT),
    retransmittedPacketsSent = getBigIntegerOrNull(RtcOutboundRtpVideoStreamStats.RETRANSMITTED_PACKETS_SENT),
    retransmittedBytesSent = getBigIntegerOrNull(RtcOutboundRtpVideoStreamStats.RETRANSMITTED_BYTES_SENT),
    rtxSsrc = getLongOrNull(RtcOutboundRtpVideoStreamStats.RTX_SSRC),
    targetBitrate = getDoubleOrNull(RtcOutboundRtpVideoStreamStats.TARGET_BITRATE),
    totalEncodedBytesTarget = getBigIntegerOrNull(RtcOutboundRtpVideoStreamStats.TOTAL_ENCODED_BYTES_TARGET),
    totalEncodeTime = getDoubleOrNull(RtcOutboundRtpVideoStreamStats.TOTAL_ENCODE_TIME),
    totalPacketSendDelay = getDoubleOrNull(RtcOutboundRtpVideoStreamStats.TOTAL_PACKET_SEND_DELAY),
    active = getBooleanOrNull(RtcOutboundRtpVideoStreamStats.ACTIVE),
    rid = getStringOrNull(RtcOutboundRtpVideoStreamStats.RID),
    frameWidth = getLongOrNull(RtcOutboundRtpVideoStreamStats.FRAME_WIDTH),
    frameHeight = getLongOrNull(RtcOutboundRtpVideoStreamStats.FRAME_HEIGHT),
    framesPerSecond = getDoubleOrNull(RtcOutboundRtpVideoStreamStats.FRAMES_PER_SECOND),
    framesSent = getLongOrNull(RtcOutboundRtpVideoStreamStats.FRAMES_SENT),
    hugeFramesSent = getLongOrNull(RtcOutboundRtpVideoStreamStats.HUGE_FRAMES_SENT),
    framesEncoded = getLongOrNull(RtcOutboundRtpVideoStreamStats.FRAMES_ENCODED),
    keyFramesEncoded = getLongOrNull(RtcOutboundRtpVideoStreamStats.KEY_FRAMES_ENCODED),
    qpSum = getBigIntegerOrNull(RtcOutboundRtpVideoStreamStats.QP_SUM),
    qualityLimitationReason = getStringOrNull(RtcOutboundRtpVideoStreamStats.QUALITY_LIMITATION_REASON)?.let {
        RtcQualityLimitationReason.fromAlias(it)
    },
    qualityLimitationDurations = getMapOrNull<String, Double>(RtcOutboundRtpVideoStreamStats.QUALITY_LIMITATION_DURATIONS)?.map {
        RtcQualityLimitationReason.fromAlias(it.key) to it.value
    }?.toMap(),
    qualityLimitationResolutionChanges = getLongOrNull(RtcOutboundRtpVideoStreamStats.QUALITY_LIMITATION_RESOLUTION_CHANGES),
    nackCount = getLongOrNull(RtcOutboundRtpVideoStreamStats.NACK_COUNT),
    firCount = getLongOrNull(RtcOutboundRtpVideoStreamStats.FIR_COUNT),
    pliCount = getLongOrNull(RtcOutboundRtpVideoStreamStats.PLI_COUNT),
    encoderImplementation = getStringOrNull(RtcOutboundRtpVideoStreamStats.ENCODER_IMPLEMENTATION),
    powerEfficientEncoder = getBooleanOrNull(RtcOutboundRtpVideoStreamStats.POWER_EFFICIENT_ENCODER),
    scalabilityMode = getStringOrNull(RtcOutboundRtpVideoStreamStats.SCALABILITY_MODE),
)

private fun RTCStats.toRtcOutboundRtpAudioStreamStats() = RtcOutboundRtpAudioStreamStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    ssrc = getLongOrNull(RtcOutboundRtpAudioStreamStats.SSRC),
    kind = getStringOrNull(RtcOutboundRtpAudioStreamStats.KIND),
    transportId = getStringOrNull(RtcOutboundRtpAudioStreamStats.TRANSPORT_ID),
    codecId = getStringOrNull(RtcOutboundRtpAudioStreamStats.CODEC_ID),
    packetsSent = getBigIntegerOrNull(RtcOutboundRtpAudioStreamStats.PACKETS_SENT),
    bytesSent = getBigIntegerOrNull(RtcOutboundRtpAudioStreamStats.BYTES_SENT),
    mid = getStringOrNull(RtcOutboundRtpAudioStreamStats.MID),
    mediaSourceId = getStringOrNull(RtcOutboundRtpAudioStreamStats.MEDIA_SOURCE_ID),
    remoteId = getStringOrNull(RtcOutboundRtpAudioStreamStats.REMOTE_ID),
    headerBytesSent = getBigIntegerOrNull(RtcOutboundRtpAudioStreamStats.HEADER_BYTES_SENT),
    retransmittedPacketsSent = getBigIntegerOrNull(RtcOutboundRtpAudioStreamStats.RETRANSMITTED_PACKETS_SENT),
    retransmittedBytesSent = getBigIntegerOrNull(RtcOutboundRtpAudioStreamStats.RETRANSMITTED_BYTES_SENT),
    rtxSsrc = getLongOrNull(RtcOutboundRtpAudioStreamStats.RTX_SSRC),
    targetBitrate = getDoubleOrNull(RtcOutboundRtpAudioStreamStats.TARGET_BITRATE),
    totalEncodedBytesTarget = getBigIntegerOrNull(RtcOutboundRtpAudioStreamStats.TOTAL_ENCODED_BYTES_TARGET),
    totalEncodeTime = getDoubleOrNull(RtcOutboundRtpAudioStreamStats.TOTAL_ENCODE_TIME),
    totalPacketSendDelay = getDoubleOrNull(RtcOutboundRtpAudioStreamStats.TOTAL_PACKET_SEND_DELAY),
    active = getBooleanOrNull(RtcOutboundRtpAudioStreamStats.ACTIVE),
)

private fun RTCStats.toRtcRemoteInboundRtpAudioStreamStats() = RtcRemoteInboundRtpAudioStreamStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    ssrc = getLongOrNull(RtcRemoteInboundRtpStreamStats.SSRC),
    kind = getStringOrNull(RtcRemoteInboundRtpStreamStats.KIND),
    transportId = getStringOrNull(RtcRemoteInboundRtpStreamStats.TRANSPORT_ID),
    codecId = getStringOrNull(RtcRemoteInboundRtpStreamStats.CODEC_ID),
    packetsReceived = getLongOrNull(RtcRemoteInboundRtpStreamStats.PACKETS_RECEIVED),
    packetsLost = getIntOrNull(RtcRemoteInboundRtpStreamStats.PACKETS_LOST),
    jitter = getDoubleOrNull(RtcRemoteInboundRtpStreamStats.JITTER),
    localId = getStringOrNull(RtcRemoteInboundRtpStreamStats.LOCAL_ID),
    roundTripTime = getDoubleOrNull(RtcRemoteInboundRtpStreamStats.ROUND_TRIP_TIME),
    totalRoundTripTime = getDoubleOrNull(RtcRemoteInboundRtpStreamStats.TOTAL_ROUND_TRIP_TIME),
    fractionLost = getDoubleOrNull(RtcRemoteInboundRtpStreamStats.FRACTION_LOST),
    roundTripTimeMeasurements = getIntOrNull(RtcRemoteInboundRtpStreamStats.ROUND_TRIP_TIME_MEASUREMENTS),
)

private fun RTCStats.toRtcRemoteInboundRtpVideoStreamStats() = RtcRemoteInboundRtpVideoStreamStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    ssrc = getLongOrNull(RtcRemoteInboundRtpStreamStats.SSRC),
    kind = getStringOrNull(RtcRemoteInboundRtpStreamStats.KIND),
    transportId = getStringOrNull(RtcRemoteInboundRtpStreamStats.TRANSPORT_ID),
    codecId = getStringOrNull(RtcRemoteInboundRtpStreamStats.CODEC_ID),
    packetsReceived = getLongOrNull(RtcRemoteInboundRtpStreamStats.PACKETS_RECEIVED),
    packetsLost = getIntOrNull(RtcRemoteInboundRtpStreamStats.PACKETS_LOST),
    jitter = getDoubleOrNull(RtcRemoteInboundRtpStreamStats.JITTER),
    localId = getStringOrNull(RtcRemoteInboundRtpStreamStats.LOCAL_ID),
    roundTripTime = getDoubleOrNull(RtcRemoteInboundRtpStreamStats.ROUND_TRIP_TIME),
    totalRoundTripTime = getDoubleOrNull(RtcRemoteInboundRtpStreamStats.TOTAL_ROUND_TRIP_TIME),
    fractionLost = getDoubleOrNull(RtcRemoteInboundRtpStreamStats.FRACTION_LOST),
    roundTripTimeMeasurements = getIntOrNull(RtcRemoteInboundRtpStreamStats.ROUND_TRIP_TIME_MEASUREMENTS),
)

private fun RTCStats.toRtcRemoteOutboundRtpAudioStreamStats() = RtcRemoteOutboundRtpAudioStreamStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    ssrc = getLongOrNull(RtcRemoteOutboundRtpStreamStats.SSRC),
    kind = getStringOrNull(RtcRemoteOutboundRtpStreamStats.KIND),
    transportId = getStringOrNull(RtcRemoteOutboundRtpStreamStats.TRANSPORT_ID),
    codecId = getStringOrNull(RtcRemoteOutboundRtpStreamStats.CODEC_ID),
    packetsSent = getBigIntegerOrNull(RtcRemoteOutboundRtpStreamStats.PACKETS_SENT),
    bytesSent = getBigIntegerOrNull(RtcRemoteOutboundRtpStreamStats.BYTES_SENT),
    localId = getStringOrNull(RtcRemoteOutboundRtpStreamStats.LOCAL_ID),
    remoteTimestamp = getDoubleOrNull(RtcRemoteOutboundRtpStreamStats.REMOTE_TIMESTAMP),
    reportsSent = getBigIntegerOrNull(RtcRemoteOutboundRtpStreamStats.REPORTS_SENT),
    roundTripTime = getDoubleOrNull(RtcRemoteOutboundRtpStreamStats.ROUND_TRIP_TIME),
    totalRoundTripTime = getDoubleOrNull(RtcRemoteOutboundRtpStreamStats.TOTAL_ROUND_TRIP_TIME),
    roundTripTimeMeasurements = getBigIntegerOrNull(RtcRemoteOutboundRtpStreamStats.ROUND_TRIP_TIME_MEASUREMENTS),
)

private fun RTCStats.toRtcRemoteOutboundRtpVideoStreamStats() = RtcRemoteOutboundRtpVideoStreamStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    ssrc = getLongOrNull(RtcRemoteOutboundRtpStreamStats.SSRC),
    kind = getStringOrNull(RtcRemoteOutboundRtpStreamStats.KIND),
    transportId = getStringOrNull(RtcRemoteOutboundRtpStreamStats.TRANSPORT_ID),
    codecId = getStringOrNull(RtcRemoteOutboundRtpStreamStats.CODEC_ID),
    packetsSent = getBigIntegerOrNull(RtcRemoteOutboundRtpStreamStats.PACKETS_SENT),
    bytesSent = getBigIntegerOrNull(RtcRemoteOutboundRtpStreamStats.BYTES_SENT),
    localId = getStringOrNull(RtcRemoteOutboundRtpStreamStats.LOCAL_ID),
    remoteTimestamp = getDoubleOrNull(RtcRemoteOutboundRtpStreamStats.REMOTE_TIMESTAMP),
    reportsSent = getBigIntegerOrNull(RtcRemoteOutboundRtpStreamStats.REPORTS_SENT),
    roundTripTime = getDoubleOrNull(RtcRemoteOutboundRtpStreamStats.ROUND_TRIP_TIME),
    totalRoundTripTime = getDoubleOrNull(RtcRemoteOutboundRtpStreamStats.TOTAL_ROUND_TRIP_TIME),
    roundTripTimeMeasurements = getBigIntegerOrNull(RtcRemoteOutboundRtpStreamStats.ROUND_TRIP_TIME_MEASUREMENTS),
)

private fun RTCStats.toRtcMediaStreamAudioTrackSenderStats() = RtcMediaStreamAudioTrackSenderStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    trackIdentifier = getStringOrNull(RtcMediaStreamAudioTrackSenderStats.TRACK_IDENTIFIER),
    ended = getBooleanOrNull(RtcMediaStreamAudioTrackSenderStats.ENDED),
    kind = getStringOrNull(RtcMediaStreamAudioTrackSenderStats.KIND),
    priority = getStringOrNull(RtcMediaStreamAudioTrackSenderStats.PRIORITY),
    remoteSource = getBooleanOrNull(RtcMediaStreamAudioTrackSenderStats.REMOTE_SOURCE),
    detached = getBooleanOrNull(RtcMediaStreamAudioTrackSenderStats.DETACHED),
    mediaSourceId = getStringOrNull(RtcMediaStreamAudioTrackSenderStats.MEDIA_SOURCE_ID),
    audioLevel = getDoubleOrNull(RtcMediaStreamAudioTrackSenderStats.AUDIO_LEVEL),
    totalAudioEnergy = getDoubleOrNull(RtcMediaStreamAudioTrackSenderStats.TOTAL_AUDIO_ENERGY),
    totalSamplesDuration = getDoubleOrNull(RtcMediaStreamAudioTrackSenderStats.TOTAL_SAMPLES_DURATION),
)

private fun RTCStats.toRtcMediaStreamAudioTrackReceiverStats() = RtcMediaStreamAudioTrackReceiverStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    trackIdentifier = getStringOrNull(RtcMediaStreamAudioTrackReceiverStats.TRACK_IDENTIFIER),
    ended = getBooleanOrNull(RtcMediaStreamAudioTrackReceiverStats.ENDED),
    kind = getStringOrNull(RtcMediaStreamAudioTrackReceiverStats.KIND),
    priority = getStringOrNull(RtcMediaStreamAudioTrackReceiverStats.PRIORITY),
    remoteSource = getBooleanOrNull(RtcMediaStreamAudioTrackReceiverStats.REMOTE_SOURCE),
    detached = getBooleanOrNull(RtcMediaStreamAudioTrackReceiverStats.DETACHED),
    estimatedPlayoutTimestamp = getDoubleOrNull(RtcMediaStreamAudioTrackReceiverStats.ESTIMATED_PLAYOUT_TIMESTAMP),
    jitterBufferDelay = getDoubleOrNull(RtcMediaStreamAudioTrackReceiverStats.JITTER_BUFFER_DELAY),
    jitterBufferEmittedCount = getLongOrNull(RtcMediaStreamAudioTrackReceiverStats.JITTER_BUFFER_EMITTED_COUNT),
    totalAudioEnergy = getDoubleOrNull(RtcMediaStreamAudioTrackReceiverStats.TOTAL_AUDIO_ENERGY),
    totalInterruptionDuration = getDoubleOrNull(RtcMediaStreamAudioTrackReceiverStats.TOTAL_INTERRUPTION_DURATION),
    removedSamplesForAcceleration = getLongOrNull(RtcMediaStreamAudioTrackReceiverStats.REMOVED_SAMPLES_FOR_ACCELERATION),
    audioLevel = getDoubleOrNull(RtcMediaStreamAudioTrackReceiverStats.AUDIO_LEVEL),
    interruptionCount = getLongOrNull(RtcMediaStreamAudioTrackReceiverStats.INTERRUPTION_COUNT),
    relativePacketArrivalDelay = getDoubleOrNull(RtcMediaStreamAudioTrackReceiverStats.RELATIVE_PACKET_ARRIVAL_DELAY),
    jitterBufferFlushes = getLongOrNull(RtcMediaStreamAudioTrackReceiverStats.JITTER_BUFFER_FLUSHES),
    concealedSamples = getLongOrNull(RtcMediaStreamAudioTrackReceiverStats.CONCEALED_SAMPLES),
    jitterBufferTargetDelay = getDoubleOrNull(RtcMediaStreamAudioTrackReceiverStats.JITTER_BUFFER_TARGET_DELAY),
    totalSamplesDuration = getDoubleOrNull(RtcMediaStreamAudioTrackReceiverStats.TOTAL_SAMPLES_DURATION),
    insertedSamplesForDeceleration = getLongOrNull(RtcMediaStreamAudioTrackReceiverStats.INSERTED_SAMPLES_FOR_DECELERATION),
    delayedPacketOutageSamples = getLongOrNull(RtcMediaStreamAudioTrackReceiverStats.DELAYED_PACKET_OUTAGE_SAMPLES),
    totalSamplesReceived = getLongOrNull(RtcMediaStreamAudioTrackReceiverStats.TOTAL_SAMPLES_RECEIVED),
    concealmentEvents = getLongOrNull(RtcMediaStreamAudioTrackReceiverStats.CONCEALMENT_EVENTS),
    silentConcealedSamples = getLongOrNull(RtcMediaStreamAudioTrackReceiverStats.SILENT_CONCEALED_SAMPLES)
)

private fun RTCStats.toRtcMediaStreamVideoTrackReceiverStats() = RtcMediaStreamVideoTrackReceiverStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    trackIdentifier = getStringOrNull(RtcMediaStreamVideoTrackReceiverStats.TRACK_IDENTIFIER),
    ended = getBooleanOrNull(RtcMediaStreamVideoTrackReceiverStats.ENDED),
    kind = getStringOrNull(RtcMediaStreamVideoTrackReceiverStats.KIND),
    priority = getStringOrNull(RtcMediaStreamVideoTrackReceiverStats.PRIORITY),
    remoteSource = getBooleanOrNull(RtcMediaStreamVideoTrackReceiverStats.REMOTE_SOURCE),
    detached = getBooleanOrNull(RtcMediaStreamVideoTrackReceiverStats.DETACHED),
    estimatedPlayoutTimestamp = getDoubleOrNull(RtcMediaStreamVideoTrackReceiverStats.ESTIMATED_PLAYOUT_TIMESTAMP),
    jitterBufferDelay = getDoubleOrNull(RtcMediaStreamVideoTrackReceiverStats.JITTER_BUFFER_DELAY),
    jitterBufferEmittedCount = getLongOrNull(RtcMediaStreamVideoTrackReceiverStats.JITTER_BUFFER_EMITTED_COUNT),
    frameHeight = getLongOrNull(RtcMediaStreamVideoTrackReceiverStats.FRAME_HEIGHT),
    frameWidth = getLongOrNull(RtcMediaStreamVideoTrackReceiverStats.FRAME_WIDTH),
    framesPerSecond = getDoubleOrNull(RtcMediaStreamVideoTrackReceiverStats.FRAMES_PER_SECOND),
    framesReceived = getLongOrNull(RtcMediaStreamVideoTrackReceiverStats.FRAMES_RECEIVED),
    framesDecoded = getLongOrNull(RtcMediaStreamVideoTrackReceiverStats.FRAMES_DECODED),
    framesDropped = getLongOrNull(RtcMediaStreamVideoTrackReceiverStats.FRAMES_DROPPED),
    totalFramesDuration = getDoubleOrNull(RtcMediaStreamVideoTrackReceiverStats.TOTAL_FRAMES_DURATION),
    totalFreezesDuration = getDoubleOrNull(RtcMediaStreamVideoTrackReceiverStats.TOTAL_FREEZES_DURATION),
    freezeCount = getLongOrNull(RtcMediaStreamVideoTrackReceiverStats.FREEZE_COUNT),
    pauseCount = getLongOrNull(RtcMediaStreamVideoTrackReceiverStats.PAUSE_COUNT),
    totalPausesDuration = getDoubleOrNull(RtcMediaStreamVideoTrackReceiverStats.TOTAL_PAUSES_DURATION),
    sumOfSquaredFramesDuration = getDoubleOrNull(RtcMediaStreamVideoTrackReceiverStats.SUM_OF_SQUARED_FRAMES_DURATION)
)

private fun RTCStats.toRtcMediaStreamVideoTrackSenderStats() = RtcMediaStreamVideoTrackSenderStats(
    id = id,
    type = type,
    timestampUs = timestampUs,
    trackIdentifier = getStringOrNull(RtcMediaStreamVideoTrackSenderStats.TRACK_IDENTIFIER),
    ended = getBooleanOrNull(RtcMediaStreamVideoTrackSenderStats.ENDED),
    kind = getStringOrNull(RtcMediaStreamVideoTrackSenderStats.KIND),
    priority = getStringOrNull(RtcMediaStreamVideoTrackSenderStats.PRIORITY),
    remoteSource = getBooleanOrNull(RtcMediaStreamVideoTrackSenderStats.REMOTE_SOURCE),
    detached = getBooleanOrNull(RtcMediaStreamVideoTrackSenderStats.DETACHED),
    mediaSourceId = getStringOrNull(RtcMediaStreamVideoTrackSenderStats.MEDIA_SOURCE_ID),
    frameHeight = getLongOrNull(RtcMediaStreamVideoTrackSenderStats.FRAME_HEIGHT),
    frameWidth = getLongOrNull(RtcMediaStreamVideoTrackSenderStats.FRAME_WIDTH),
    framesPerSecond = getDoubleOrNull(RtcMediaStreamVideoTrackSenderStats.FRAMES_PER_SECOND),
    keyFramesSent = getLongOrNull(RtcMediaStreamVideoTrackSenderStats.KEY_FRAMES_SENT),
    framesCaptured = getLongOrNull(RtcMediaStreamVideoTrackSenderStats.FRAMES_CAPTURED),
    framesSent = getLongOrNull(RtcMediaStreamVideoTrackSenderStats.FRAMES_SENT),
    hugeFramesSent = getLongOrNull(RtcMediaStreamVideoTrackSenderStats.HUGE_FRAMES_SENT),
)

operator fun RTCStats.get(key: String): Any? = members[key]
fun RTCStats.getStringOrNull(key: String): String? = members[key] as String?
fun RTCStats.getDoubleOrNull(key: String): Double? = members[key] as Double?

fun RTCStats.getIntOrNull(key: String): Int? = members[key] as Int?
fun RTCStats.getLongOrNull(key: String): Long? = members[key] as Long?

fun RTCStats.getBigIntegerOrNull(key: String): BigInteger? = members[key] as BigInteger?
fun RTCStats.getBooleanOrNull(key: String): Boolean? = members[key] as Boolean?

fun <T, R> RTCStats.getMapOrNull(key: String): Map<T, R>? = members[key] as Map<T, R>?
