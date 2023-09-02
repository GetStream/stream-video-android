package io.getstream.video.android.compose.ui.components.call.diagnostics

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.stats.model.RtcAudioSourceStats
import io.getstream.video.android.core.call.stats.model.RtcCodecStats
import io.getstream.video.android.core.call.stats.model.RtcIceCandidatePairStats
import io.getstream.video.android.core.call.stats.model.RtcIceCandidateStats
import io.getstream.video.android.core.call.stats.model.RtcInboundRtpVideoStreamStats
import io.getstream.video.android.core.call.stats.model.RtcOutboundRtpVideoStreamStats
import io.getstream.video.android.core.call.stats.model.RtcRemoteInboundRtpVideoStreamStats
import io.getstream.video.android.core.call.stats.model.RtcRemoteOutboundRtpVideoStreamStats
import io.getstream.video.android.core.call.stats.model.RtcStats
import io.getstream.video.android.core.call.stats.model.RtcVideoSourceStats
import io.getstream.video.android.core.call.stats.model.discriminator.RtcReportType

@Composable
public fun CallDiagnosticsContent(
    call: Call,
    onCloseClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(color = Color(0x80000000))
    ) {
        val stats by call.statsReport.collectAsStateWithLifecycle()
        val configuration = LocalConfiguration.current
        if (configuration.orientation == ORIENTATION_PORTRAIT) {
            Column {
                IconButton(onClick = onCloseClick) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                }
                LazyColumn() {
                    call.state.participants.value.forEach {
                        val name = it.userNameOrId.value.orEmpty()
                        val trackId = it.trackLookupPrefix
                        item {
                            Row {
                                Text(text = "${name}: ", color = Color.Yellow)
                                Text(text = trackId, color = Color.Cyan)
                            }
                        }
                    }
                    PublisherDiagnosticsContent(
                        call = call,
                        stats = stats?.publisher?.parsed.orEmpty(),
                    )
                    SubscriberDiagnosticsContent(
                        call = call,
                        stats = stats?.subscriber?.parsed.orEmpty(),
                    )
                    item {
                        Spacer(modifier = Modifier.height(128.dp))
                    }
                }
            }
        } else {
            Row {
                IconButton(onClick = onCloseClick) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                }
                LazyColumn(modifier = Modifier.weight(1f)) {
                    PublisherDiagnosticsContent(
                        call = call,
                        stats = stats?.publisher?.parsed.orEmpty(),
                    )
                }
                LazyColumn(modifier = Modifier.weight(1f)) {
                    SubscriberDiagnosticsContent(
                        call = call,
                        stats = stats?.subscriber?.parsed.orEmpty(),
                    )
                }
            }
        }
    }
}

private fun LazyListScope.PublisherDiagnosticsContent(
    call: Call,
    stats: Map<RtcReportType, Set<RtcStats>>,
) {
    item {
        Spacer(modifier = Modifier.height(16.dp))
        Text("PUBLISHER", color = Color.Orange, fontWeight = FontWeight.Bold)
    }
    if (stats.isEmpty()) return
    item {

        val codecs =
            stats[RtcReportType.CODEC]?.associate { it.id to (it as? RtcCodecStats) }.orEmpty()
        val candidatePair =
            stats[RtcReportType.CANDIDATE_PAIR]?.firstOrNull() as? RtcIceCandidatePairStats
        val localCandidate =
            stats[RtcReportType.LOCAL_CANDIDATE]?.find { candidatePair?.localCandidateId == it.id } as? RtcIceCandidateStats
        if (localCandidate != null && candidatePair?.localCandidateId == localCandidate.id) {
            localCandidate.apply {
                Text("ice_candidate: ${ip}:${port}")
                Text("protocol: $protocol")
                Text("candidate_type: $candidateType")
                Text("network_type: $networkType")
            }
        } else {
            Text("No local candidate")
        }

        val videoSource = stats[RtcReportType.MEDIA_SOURCE]?.filterIsInstance<RtcVideoSourceStats>()
            ?.firstOrNull()
        videoSource?.apply {
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Text("Video Source: ", color = Color.Green)
                Text("$trackIdentifier", color = Color.Cyan)
            }
            Text("width_height: ${width}_$height")
            Text("total_frames: $frames")
            Text("frames_per_second: $framesPerSecond")
        }

        val videOutboundRtp = stats[RtcReportType.OUTBOUND_RTP]
            ?.filterIsInstance<RtcOutboundRtpVideoStreamStats>()
            ?.sortedBy { it.rid }
            ?: emptyList()

        val videRemoteInboundRtpMap = stats[RtcReportType.REMOTE_INBOUND_RTP]
            ?.filterIsInstance<RtcRemoteInboundRtpVideoStreamStats>()
            ?.associate { it.localId to it }
            ?: emptyMap()

        videOutboundRtp.forEach { vor ->
            vor.apply {
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Text("Video Outbound RTP: ", color = Color.Green)
                    Text("$rid".uppercase(), color = Color.Cyan)
                }
                Text("width_height: ${frameWidth}_$frameHeight")
                Text("codec: ${codecs[codecId]?.mimeType}")
                Text("outbound_id: $id")
                Text("ssrc: $ssrc")
                Text("packets_sent: $packetsSent")
                Text("bytes_sent: $bytesSent")
                Text("frames_per_second: $framesPerSecond")
                Text("frames_encoded: $framesEncoded")
                Text("frames_sent: $framesSent")

                videRemoteInboundRtpMap[id]?.also {
                    Text("packets_received: ${it.packetsReceived}")
                    Text("packets_lost: ${it.packetsLost}")
                    Text("jitter: ${it.jitter}")
                    Text("fraction_lost: ${it.fractionLost}")
                    Text("round_trip_time: ${it.roundTripTime}")
                    Text("total_round_trip_time: ${it.totalRoundTripTime}")
                    Text("round_trip_time_measurements: ${it.roundTripTimeMeasurements}")
                }
            }
        }

        /*val videRemoteInboundRtp = stats[RtcReportType.REMOTE_INBOUND_RTP]
            ?.filterIsInstance<RtcRemoteInboundRtpVideoStreamStats>()
            ?.sortedBy { it.localId }
            ?: emptyList()
        videRemoteInboundRtp.forEach {
            it.apply {
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Text("Video Remote Inbound RTP: ", color = Color.Green)
                    Text("$localId", color = Color.Cyan)
                }
                Text("codec: ${codecs[codecId]?.mimeType}")
                Text("remote_outbound_id: $id")
                Text("ssrc: $ssrc")
                Text("packets_received: $packetsReceived")
                Text("packets_lost: $packetsLost")
                Text("jitter: $jitter")
                Text("fraction_lost: $fractionLost")
                Text("round_trip_time: $roundTripTime")
                Text("total_round_trip_time: $totalRoundTripTime")
                Text("round_trip_time_measurements: $roundTripTimeMeasurements")
            }
        }*/

        val audioSource = stats[RtcReportType.MEDIA_SOURCE]?.filterIsInstance<RtcAudioSourceStats>()
            ?.firstOrNull()

    }
}

private fun LazyListScope.SubscriberDiagnosticsContent(
    call: Call,
    stats: Map<RtcReportType, Set<RtcStats>>,
) {
    item {
        Spacer(modifier = Modifier.height(16.dp))
        Text("SUBSCRIBER", color = Color.Orange, fontWeight = FontWeight.Bold)
    }
    if (stats.isEmpty()) return
    val videoToParticipant =
        call.state.remoteParticipants.value.associate { it.videoTrack.value?.video?.id() to it.userNameOrId.value }
    val audioToParticipant =
        call.state.remoteParticipants.value.associate { it.audioTrack.value?.audio?.id() to it.userNameOrId.value }

    item {

        val codecs =
            stats[RtcReportType.CODEC]?.associate { it.id to (it as? RtcCodecStats) }.orEmpty()
        val candidatePair =
            stats[RtcReportType.CANDIDATE_PAIR]?.firstOrNull() as? RtcIceCandidatePairStats
        val remoteCandidate =
            stats[RtcReportType.REMOTE_CANDIDATE]?.find { candidatePair?.remoteCandidateId == it.id } as? RtcIceCandidateStats
        if (remoteCandidate != null && candidatePair?.remoteCandidateId == remoteCandidate.id) {
            remoteCandidate.apply {
                Text("IceCandidate: ${ip}:${port}")
                Text("Protocol: $protocol")
                Text("CandidateType: $candidateType")
                Text("NetworkType: $networkType")
            }
        } else {
            Text("No remote candidate")
        }

        val videInboundRtp = stats[RtcReportType.INBOUND_RTP]
            ?.filterIsInstance<RtcInboundRtpVideoStreamStats>()
            ?.sortedBy { it.trackIdentifier }
            ?: emptyList()

        val videRemoteOutboundRtpMap = stats[RtcReportType.REMOTE_OUTBOUND_RTP]
            ?.filterIsInstance<RtcRemoteOutboundRtpVideoStreamStats>()
            ?.associate { it.localId to it }
            ?: emptyMap()

        videInboundRtp.forEach { vor ->
            vor.apply {
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Text("Video Inbound RTP: ", color = Color.Green)
                    Text("$trackIdentifier", color = Color.Cyan)
                }
                Row {
                    Text("user_name: ")
                    Text("${videoToParticipant[trackIdentifier]}", color = Color.Yellow)
                }
                Text("width_height: ${frameWidth}_$frameHeight")
                Text("codec: ${codecs[codecId]?.mimeType}")
                Text("inbound_id: $id")
                Text("ssrc: $ssrc")
                Text("packets_received: $packetsReceived")
                Text("bytes_received: $bytesReceived")
                Text("key_frames_decoded: $keyFramesDecoded")
                Text("frames_per_second: $framesPerSecond")
                Text("frames_decoded: $framesDecoded")
                Text("frames_rendered: $framesRendered")
                Text("frames_dropped: $framesDropped")
                Text("frames_received: $framesReceived")

                videRemoteOutboundRtpMap[id]?.also {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("packets_sent: ${it.packetsSent}")
                    Text("bytes_sent: ${it.bytesSent}")
                    Text("reports_sent: ${it.reportsSent}")
                    Text("round_trip_time: ${it.roundTripTime}")
                    Text("total_round_trip_time: ${it.totalRoundTripTime}")
                    Text("round_trip_time_measurements: ${it.roundTripTimeMeasurements}")
                }
            }
        }

        val videRemoteOutboundRtp = stats[RtcReportType.OUTBOUND_RTP]
            ?.filterIsInstance<RtcRemoteOutboundRtpVideoStreamStats>()
            ?.sortedBy { it.localId }
            ?: emptyList()
        /*videRemoteOutboundRtp.forEach {
            it.apply {
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Text("Video Remote Outbound RTP: ", color = Color.Green)
                    Text("$localId", color = Color.Cyan)
                }
                Text("codec: ${codecs[codecId]?.mimeType}")
                Text("remote_outbound_id: $id")
                Text("ssrc: $ssrc")
                Text("packets_sent: $packetsSent")
                Text("reports_sent: $reportsSent")
                Text("remote_timestamp: $remoteTimestamp")
                Text("bytes_sent: $bytesSent")
                Text("round_trip_time: $roundTripTime")
                Text("total_round_trip_time: $totalRoundTripTime")
                Text("round_trip_time_measurements: $roundTripTimeMeasurements")
            }
        }*/

    }
}

private val Color.Companion.Orange get() = Color(0xFFFFCC99)