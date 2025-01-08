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
import io.getstream.video.android.core.call.stats.model.RtcInboundRtpAudioStreamStats
import io.getstream.video.android.core.call.stats.model.RtcInboundRtpVideoStreamStats
import io.getstream.video.android.core.call.stats.model.RtcOutboundRtpAudioStreamStats
import io.getstream.video.android.core.call.stats.model.RtcOutboundRtpVideoStreamStats
import io.getstream.video.android.core.call.stats.model.RtcRemoteInboundRtpAudioStreamStats
import io.getstream.video.android.core.call.stats.model.RtcRemoteInboundRtpVideoStreamStats
import io.getstream.video.android.core.call.stats.model.RtcRemoteOutboundRtpAudioStreamStats
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
            .background(color = Color(0x80B9B9B9)),
    ) {
        val stats by call.statsReport.collectAsStateWithLifecycle()
        val configuration = LocalConfiguration.current
        if (configuration.orientation == ORIENTATION_PORTRAIT) {
            Column {
                IconButton(onClick = onCloseClick) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                }
                LazyColumn {
                    call.state.participants.value.forEach {
                        val name = if (it.isLocal) "Me" else it.userNameOrId.value
                        val trackId = it.trackLookupPrefix
                        item {
                            Row {
                                Text(text = "$name: ", color = Color.Yellow)
                                Text(text = trackId, color = Color.Cyan)
                            }
                        }
                    }
                    PublisherDiagnosticsContent(
                        call = call,
                        stats = stats?.publisher?.parsed.orEmpty(),
                        showRemoteInbound = false,
                    )
                    SubscriberDiagnosticsContent(
                        call = call,
                        stats = stats?.subscriber?.parsed.orEmpty(),
                        showRemoteOutbound = false,
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
                        showRemoteInbound = false,
                    )
                }
                LazyColumn(modifier = Modifier.weight(1f)) {
                    SubscriberDiagnosticsContent(
                        call = call,
                        stats = stats?.subscriber?.parsed.orEmpty(),
                        showRemoteOutbound = false,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.PublisherDiagnosticsContent(
    call: Call,
    stats: Map<RtcReportType, Set<RtcStats>>,
    showRemoteInbound: Boolean,
) {
    item {
        Spacer(modifier = Modifier.height(16.dp))
        Text("PUBLISHER", color = Color.Orange, fontWeight = FontWeight.Bold)
    }
    if (stats.isEmpty()) return
    val codecs = stats[RtcReportType.CODEC]?.associate { it.id to (it as? RtcCodecStats) }.orEmpty()
    item {
        val candidatePair = stats[RtcReportType.CANDIDATE_PAIR]
            ?.firstOrNull() as? RtcIceCandidatePairStats
        val localCandidate = stats[RtcReportType.LOCAL_CANDIDATE]?.find {
            candidatePair?.localCandidateId == it.id
        } as? RtcIceCandidateStats
        if (localCandidate != null && candidatePair?.localCandidateId == localCandidate.id) {
            IceCandidate(localCandidate)
        } else {
            Text("No local candidate")
        }
    }

    item {
        val videoSource = stats[RtcReportType.MEDIA_SOURCE]
            ?.filterIsInstance<RtcVideoSourceStats>()
            ?.firstOrNull()
        VideoSource(videoSource)
    }

    val videOutboundRtp = stats[RtcReportType.OUTBOUND_RTP]
        ?.filterIsInstance<RtcOutboundRtpVideoStreamStats>()
        ?.sortedBy { it.rid }
        ?: emptyList()

    val videRemoteInboundRtpMap = stats[RtcReportType.REMOTE_INBOUND_RTP]
        ?.filterIsInstance<RtcRemoteInboundRtpVideoStreamStats>()
        ?.associate { it.localId to it }
        ?: emptyMap()

    videOutboundRtp.forEach {
        item {
            VideoOutboundRtp(
                it,
                codecs[it.codecId],
                videRemoteInboundRtpMap[it.id],
                showRemoteInbound,
            )
        }
    }

    item {
        val audioSource = stats[RtcReportType.MEDIA_SOURCE]?.filterIsInstance<RtcAudioSourceStats>()
            ?.firstOrNull()
        AudioSource(audioSource)
    }

    val audioOutboundRtp = stats[RtcReportType.OUTBOUND_RTP]
        ?.filterIsInstance<RtcOutboundRtpAudioStreamStats>()
        ?.sortedBy { it.mid }
        ?: emptyList()

    val audioRemoteInboundRtpMap = stats[RtcReportType.REMOTE_INBOUND_RTP]
        ?.filterIsInstance<RtcRemoteInboundRtpAudioStreamStats>()
        ?.associate { it.localId to it }
        ?: emptyMap()

    audioOutboundRtp.forEach {
        item {
            AudioOutboundRtp(
                it,
                codecs[it.codecId],
                audioRemoteInboundRtpMap[it.id],
                showRemoteInbound,
            )
        }
    }
}

private fun LazyListScope.SubscriberDiagnosticsContent(
    call: Call,
    stats: Map<RtcReportType, Set<RtcStats>>,
    showRemoteOutbound: Boolean,
) {
    item {
        Spacer(modifier = Modifier.height(16.dp))
        Text("SUBSCRIBER", color = Color.Orange, fontWeight = FontWeight.Bold)
    }
    if (stats.isEmpty()) return
    val videoToParticipant = try {
        call.state.remoteParticipants.value.associate {
            it.videoTrack.value?.video?.id() to it.userNameOrId.value
        }
    } catch (e: Exception) {
        emptyMap()
    }
    val audioToParticipant = call.state.remoteParticipants.value.associate {
        it.audioTrack.value?.audio?.id() to it.userNameOrId.value
    }
    val codecs = stats[RtcReportType.CODEC]?.associate { it.id to (it as? RtcCodecStats) }.orEmpty()
    item {
        val candidatePair =
            stats[RtcReportType.CANDIDATE_PAIR]?.firstOrNull() as? RtcIceCandidatePairStats
        val remoteCandidate =
            stats[RtcReportType.REMOTE_CANDIDATE]?.find {
                candidatePair?.remoteCandidateId == it.id
            } as? RtcIceCandidateStats
        if (remoteCandidate != null && candidatePair?.remoteCandidateId == remoteCandidate.id) {
            IceCandidate(remoteCandidate)
        } else {
            Text("No remote candidate")
        }
    }
    val videInboundRtp = stats[RtcReportType.INBOUND_RTP]
        ?.filterIsInstance<RtcInboundRtpVideoStreamStats>()
        ?.sortedBy { it.trackIdentifier }
        ?: emptyList()

    val videRemoteOutboundRtpMap = stats[RtcReportType.REMOTE_OUTBOUND_RTP]
        ?.filterIsInstance<RtcRemoteOutboundRtpVideoStreamStats>()
        ?.associate { it.localId to it }
        ?: emptyMap()

    videInboundRtp.forEach {
        item {
            VideoInboundRtp(
                it,
                videoToParticipant[it.trackIdentifier],
                codecs[it.codecId],
                videRemoteOutboundRtpMap[it.id],
                showRemoteOutbound,
            )
        }
    }

    val audioInboundRtp = stats[RtcReportType.INBOUND_RTP]
        ?.filterIsInstance<RtcInboundRtpAudioStreamStats>()
        ?.sortedBy { it.trackIdentifier }
        ?: emptyList()

    val audioRemoteOutboundRtpMap = stats[RtcReportType.REMOTE_OUTBOUND_RTP]
        ?.filterIsInstance<RtcRemoteOutboundRtpAudioStreamStats>()
        ?.associate { it.localId to it }
        ?: emptyMap()

    audioInboundRtp.forEach {
        item {
            AudioInboundRtp(
                it,
                audioToParticipant[it.trackIdentifier],
                codecs[it.codecId],
                audioRemoteOutboundRtpMap[it.id],
                showRemoteOutbound,
            )
        }
    }
}

@Composable
private fun IceCandidate(stats: RtcIceCandidateStats) {
    stats.apply {
        Text("ice_candidate: $ip:$port")
        Text("protocol: $protocol")
        Text("candidate_type: $candidateType")
        Text("network_type: $networkType")
    }
}

@Composable
private fun AudioSource(
    source: RtcAudioSourceStats?,
) {
    source?.apply {
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Text("Audio Source: ", color = Color.Green)
            Text("$trackIdentifier", color = Color.Cyan)
        }
        Text("audio_level: $audioLevel")
        Text("total_audio_energy: $totalAudioEnergy")
        Text("echo_return_loss: $echoReturnLoss")
        Text("echo_return_loss_enhancement: $echoReturnLossEnhancement")
        Text("dropped_samples_duration: $droppedSamplesDuration")
    }
}

@Composable
private fun VideoSource(
    source: RtcVideoSourceStats?,
) {
    source?.apply {
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Text("Video Source: ", color = Color.Green)
            Text("$trackIdentifier", color = Color.Cyan)
        }
        Text("width_height: ${width}_$height")
        Text("total_frames: $frames")
        Text("frames_per_second: $framesPerSecond")
    }
}

@Composable
private fun AudioOutboundRtp(
    ora: RtcOutboundRtpAudioStreamStats,
    codec: RtcCodecStats?,
    rira: RtcRemoteInboundRtpAudioStreamStats?,
    showRemoteInbound: Boolean,
) {
    ora.apply {
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Text("Audio Outbound RTP: ", color = Color.Green)
            Text("$mid".uppercase(), color = Color.Cyan)
        }
        Codec(codec)
        Text("target_bitrate: $targetBitrate")
        Text("media_sourceId: $mediaSourceId")
        Text("remote_id: $remoteId")
        Text("outbound_id: $id")
        Text("ssrc: $ssrc")
        Text("packets_sent: $packetsSent")
        Text("retransmitted_packets_sent: $retransmittedPacketsSent")
        Text("bytes_sent: $bytesSent")
        Text("retransmitted_bytes_sent: $retransmittedBytesSent")

        rira?.also {
            if (showRemoteInbound) {
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
}

@Composable
private fun VideoOutboundRtp(
    orv: RtcOutboundRtpVideoStreamStats,
    codec: RtcCodecStats?,
    rirv: RtcRemoteInboundRtpVideoStreamStats?,
    showRemoteInbound: Boolean,
) {
    orv.apply {
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Text("Video Outbound RTP: ", color = Color.Green)
            Text("$rid".uppercase(), color = Color.Cyan)
        }
        Codec(codec)
        StatsLine("width_height", "${frameWidth}_$frameHeight")
        Text("active: $active")
        Text("target_bitrate: $targetBitrate")
        Text("media_sourceId: $mediaSourceId")
        Text("remote_id: $remoteId")
        Text("outbound_id: $id")
        Text("ssrc: $ssrc")
        Text("packets_sent: $packetsSent")
        Text("retransmitted_packets_sent: $retransmittedPacketsSent")
        Text("bytes_sent: $bytesSent")
        Text("retransmitted_bytes_sent: $retransmittedBytesSent")
        StatsLine("frames_per_second", "$framesPerSecond")
        StatsLine("frames_encoded", "$framesEncoded")
        StatsLine("frames_sent", "$framesSent")
        StatsLine("quality_limitation_reason", "$qualityLimitationReason")
        Text("quality_limitation_durations: $qualityLimitationDurations")
        Text("quality_limitation_resolution_changes: $qualityLimitationResolutionChanges")
        Text("encoder_implementation: $encoderImplementation")
        Text("scalability_mode: $scalabilityMode")

        rirv?.also {
            if (showRemoteInbound) {
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
}

@Composable
private fun AudioInboundRtp(
    ira: RtcInboundRtpAudioStreamStats,
    userNameOrId: String?,
    codec: RtcCodecStats?,
    rora: RtcRemoteOutboundRtpAudioStreamStats?,
    showRemoteOutbound: Boolean,
) {
    ira.apply {
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Text("Audio Inbound RTP: ", color = Color.Green)
            Text("$trackIdentifier", color = Color.Cyan)
        }
        StatsLine("user_name", "$userNameOrId", Color.Yellow)
        Text("audio_level: $audioLevel")
        Codec(codec)
        Text("inbound_id: $id")
        Text("ssrc: $ssrc")
        Text("packets_received: $packetsReceived")
        Text("bytes_received: $bytesReceived")
        Text("totalSamples_received: $totalSamplesReceived")
        Text("totalSamples_duration: $totalSamplesDuration")
        Text("concealed_samples: $concealedSamples")
        Text("silent_concealed_samples: $silentConcealedSamples")
        Text("concealment_events: $concealmentEvents")
        Text("inserted_samples_for_deceleration: $insertedSamplesForDeceleration")
        Text("removed_samples_for_acceleration: $removedSamplesForAcceleration")
        Text("decoder_implementation: $decoderImplementation")

        rora?.also {
            if (showRemoteOutbound) {
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
}

@Composable
private fun VideoInboundRtp(
    irv: RtcInboundRtpVideoStreamStats,
    userNameOrId: String?,
    codec: RtcCodecStats?,
    rorv: RtcRemoteOutboundRtpVideoStreamStats?,
    showRemoteOutbound: Boolean,
) {
    irv.apply {
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Text("Video Inbound RTP: ", color = Color.Green)
            Text("$trackIdentifier", color = Color.Cyan)
        }
        StatsLine("user_name", "$userNameOrId", Color.Yellow)
        StatsLine("width_height", "${frameWidth}_$frameHeight")
        Codec(codec)
        Text("inbound_id: $id")
        Text("ssrc: $ssrc")
        Text("packets_received: $packetsReceived")
        Text("bytes_received: $bytesReceived")
        Text("key_frames_decoded: $keyFramesDecoded")
        StatsLine("frames_per_second", "$framesPerSecond")
        StatsLine("frames_decoded", "$framesDecoded")
        StatsLine("frames_rendered", "$framesRendered")
        StatsLine("frames_dropped", "$framesDropped", Color.RedLight)
        StatsLine("frames_received", "$framesReceived")
        Text("decoder_implementation: $decoderImplementation")

        rorv?.also {
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

@Composable
private fun Codec(codec: RtcCodecStats?) {
    val mimeType = codec?.mimeType
    val profileId = codec?.sdpFmtpLine?.split(";")
        ?.find { it.startsWith("profile-level-id") }
        ?.split("=")
        ?.getOrNull(1)
        .orEmpty()
    val profileText = profileId.takeIf { it.isNotEmpty() }?.let { "; $it" } ?: ""
    val codecText = "$mimeType $profileText"
    StatsLine("codec", codecText)
}

@Composable
private fun StatsLine(title: String, value: String, color: Color = Color.Cyan) {
    Row {
        Text("$title: ")
        Text(value, color = color)
    }
}

private val Color.Companion.Orange get() = Color(0xFFFFA77F)
private val Color.Companion.RedLight get() = Color(0xFFFFA9A9)
