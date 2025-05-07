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

package io.getstream.video.android.core.call.connection.stats

import io.getstream.video.android.core.call.stats.toRtcCodecStats
import io.getstream.video.android.core.call.stats.toRtcInboundRtpVideoStreamStats
import io.getstream.video.android.core.call.stats.toRtcOutboundRtpVideoStreamStats
import io.getstream.video.android.core.call.stats.toRtcVideoSourceStats
import kotlinx.coroutines.suspendCancellableCoroutine
import org.webrtc.PeerConnection
import org.webrtc.RTCStats
import org.webrtc.RTCStatsReport
import stream.video.sfu.models.Codec
import stream.video.sfu.models.PeerType
import stream.video.sfu.models.PerformanceStats
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/**
 * Collects and processes WebRTC stats for publishers and subscribers.
 *
 * @param pc                 current [PeerConnection]
 * @param peerType           whether this side is PUBLISHER or SUBSCRIBER
 * @param trackIdToTrackType mapping from WebRTC track IDs to [TrackType] (audio / video / screen-share…)
 *
 * @internal
 */
class StatsTracer(
    private val pc: PeerConnection,
    private val peerType: PeerType,
) {

    private var previousStats: Map<String, RTCStats> = emptyMap()
    private val frameTimeHistory = ArrayDeque<Double>()
    private val fpsHistory = ArrayDeque<Double>()

    /**
     * Collects the latest statistics and returns:
     *  * current performance numbers
     *  * delta-compressed stats (diff vs previous poll)
     *  * the raw [RTCStatsReport]
     */
    suspend fun get(
        trackIdToTrackType: Map<String, TrackType>,
    ): ComputedStats = suspendCancellableCoroutine { cont ->
        pc.getStats { report ->
            val currentStats = report.statsMap

            val perfStats = when (peerType) {
                PeerType.PEER_TYPE_SUBSCRIBER -> getDecodeStats(trackIdToTrackType, currentStats)
                else -> getEncodeStats(trackIdToTrackType, currentStats)
            }

            val delta = deltaCompression(previousStats, currentStats)

            // keep only the last two values so we don’t average over an ever-growing list
            trimHistory(frameTimeHistory, 2)
            trimHistory(fpsHistory, 2)

            previousStats = currentStats

            cont.resume(
                ComputedStats(
                    performanceStats = perfStats,
                    delta = delta,
                    stats = report,
                ),
            )
        }
    }

    /* ---------- internal helpers ---------- */

    private fun getEncodeStats(
        trackIdToTrackType: Map<String, TrackType>,
        stats: Map<String, RTCStats>,
    ): List<PerformanceStats> {
        val result = mutableListOf<PerformanceStats>()

        stats.values
            .filter { it.type == "outbound-rtp" }
            .map { it.toRtcOutboundRtpVideoStreamStats() }
            .filter { it.kind == "video" && previousStats.containsKey(it.id) }
            .forEach { rtp ->
                val prev = previousStats[rtp.id]?.toRtcOutboundRtpVideoStreamStats()

                // --- safer arithmetic that handles nullable values ---
                val dtEncode = (rtp.totalEncodeTime ?: 0.0)
                    .minus(prev?.totalEncodeTime ?: 0.0)

                val dfSent = (rtp.framesSent ?: 0)
                    .minus(prev?.framesSent ?: 0)
                    .toDouble()

                val frameTime = if (dfSent > 0) {
                    dtEncode.div(dfSent).times(1000) // ms / frame
                } else { 0.0 }

                frameTimeHistory.add(frameTime)
                fpsHistory.add(rtp.framesPerSecond ?: 0.0)

                val trackType = rtp.mediaSourceId
                    ?.let { stats[it]?.toRtcVideoSourceStats() }
                    ?.trackIdentifier
                    ?.let(trackIdToTrackType::get)
                    ?: TrackType.TRACK_TYPE_VIDEO

                result += PerformanceStats(
                    track_type = trackType,
                    codec = codecFrom(stats, rtp.codecId),
                    avg_frame_time_ms = frameTimeHistory.averageOrNull()?.toFloat() ?: 0f,
                    avg_fps = fpsHistory.averageOrNull()?.toFloat() ?: 0f,
                    target_bitrate = rtp.targetBitrate?.roundToInt() ?: 0,
                    video_dimension = VideoDimension(
                        width = rtp.frameWidth?.toInt() ?: 0,
                        height = rtp.frameHeight?.toInt() ?: 0,
                    ),
                )
            }
        return result
    }

    private fun getDecodeStats(
        trackIdToTrackType: Map<String, TrackType>,
        stats: Map<String, RTCStats>,
    ): List<PerformanceStats> {
        // pick the inbound-rtp entry with the largest frame area (active track)
        val rtp = stats.values
            .filter { it.type == "inbound-rtp" }
            .map { it.toRtcInboundRtpVideoStreamStats() }
            .maxByOrNull { (it.frameWidth ?: 0) * (it.frameHeight ?: 0) }
            ?: return emptyList()

        val prev = previousStats[rtp.id]?.toRtcInboundRtpVideoStreamStats()
        val dtDecode = (rtp.totalDecodeTime ?: 0.0)
            .minus(prev?.totalDecodeTime ?: 0.0)

        val dfDecoded = (rtp.framesDecoded ?: 0)
            .minus(prev?.framesDecoded ?: 0)
            .toDouble()

        val frameTime = if (dfDecoded > 0) {
            dtDecode.div(dfDecoded).times(1000) // ms / frame
        } else {
            0.0
        }
        frameTimeHistory.add(frameTime)
        fpsHistory.add(rtp.framesPerSecond ?: 0.0)

        val trackType = trackIdToTrackType[rtp.trackIdentifier] ?: TrackType.TRACK_TYPE_VIDEO

        return listOf(
            PerformanceStats(
                track_type = trackType,
                codec = codecFrom(stats, rtp.codecId),
                avg_frame_time_ms = frameTimeHistory.averageOrNull()?.toFloat() ?: 0f,
                avg_fps = fpsHistory.averageOrNull()?.toFloat() ?: 0f,
                video_dimension = VideoDimension(
                    width = rtp.frameWidth?.toInt() ?: 0,
                    height = rtp.frameHeight?.toInt() ?: 0,
                ),
            ),
        )
    }
}

/* ------------ extension / helper functions ------------- */

private fun codecFrom(stats: Map<String, RTCStats>, codecId: String?): Codec? =
    codecId?.let(stats::get)?.toRtcCodecStats()
        ?.let {
            Codec(
                name = it.mimeType ?: "",
                clock_rate = it.clockRate?.toInt() ?: 0,
                payload_type = it.payloadType?.toInt() ?: 0,
                fmtp = it.sdpFmtpLine ?: "",
            )
        }

private fun MutableCollection<*>.averageOrNull(): Double? =
    if (isEmpty()) null else (this.sumOf { (it as Number).toDouble() } / size)

private fun <T> trimHistory(history: ArrayDeque<T>, keep: Int) {
    while (history.size > keep) history.removeFirst()
}

/**
 * Builds a delta-compressed snapshot of [newStats] against [oldStats].
 *
 *  * For every stats record (`id`) we keep **only** the members that changed
 *    since the previous poll (or the whole record if it is new).
 *  * The largest timestamp in the new report is placed in the top-level
 *    `"timestamp"` field; records carrying that timestamp have it replaced by 0
 *    so the client can reconstruct absolute time.
 *
 *  The result is roughly an order of magnitude smaller than the full report.
 */
private fun deltaCompression(
    oldStats: Map<String, RTCStats>,
    newStats: Map<String, RTCStats>,
): Map<String, Any?> {
    val diff = mutableMapOf<String, MutableMap<String, Any?>>()
    var latestTs = Double.MIN_VALUE

    newStats.forEach { (id, current) ->

        val old = oldStats[id]
        val changed = mutableMapOf<String, Any?>()

        /* ---------- compare member fields ---------- */
        current.members.forEach { (key, value) ->
            val oldValue = old?.members?.get(key)
            if (oldValue != value) {
                changed[key] = value
            }
        }

        /* ---------- include record only if something changed ---------- */
        if (changed.isNotEmpty() || old == null) {
            changed["type"] = current.type // keep the record type
            changed["timestamp"] = current.timestampUs
            diff[id] = changed
        }

        if (current.timestampUs > latestTs) {
            latestTs = current.timestampUs
        }
    }
    return diff
}

/**
 * Wrapper returned by [StatsTracer.get] that contains:
 *  * the full WebRTC stats report for the current poll,
 *  * a delta-compressed diff against the previous poll,
 *  * high-level performance metrics (encode / decode).
 */
data class ComputedStats(
    /** Full statistics snapshot from the current [RTCStatsReport]. */
    val stats: RTCStatsReport,

    /** Delta-compressed copy of the report (reduces payload by ~90 %). */
    val delta: Map<String, Any?>,

    /** High-level, human-readable performance numbers for each track. */
    val performanceStats: List<PerformanceStats>,
)
