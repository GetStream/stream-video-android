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

package io.getstream.video.android.core

import android.os.Build
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.call.stats.model.RtcStatsReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.webrtc.CameraEnumerationAndroid
import org.webrtc.RTCStats
import stream.video.sfu.models.TrackType

data class MediaStatsInfo(
    val qualityLimit: String?,
    val jitter: Double?,
    val width: Long?,
    val height: Long?,
    val fps: Double?,
    val deviceLatency: Double?,
) {
    companion object {
        fun fromMap(map: Map<String, Any?>): MediaStatsInfo {
            val qualityLimit = map["qualityLimitationReason"] as? String
            val jitter = map["jitter"] as? Double
            val width = (map["frameWidth"] as? Long)?.takeIf { it > 0 }
            val height = (map["frameHeight"] as? Long)?.takeIf { it > 0 }
            val fps = map["framesPerSecond"] as? Double
            val deviceLatency = map["totalPacketSendDelay"] as? Double

            return MediaStatsInfo(qualityLimit, jitter, width, height, fps, deviceLatency)
        }
    }
}

public class PeerConnectionStats(scope: CoroutineScope) {
    internal var _latency: MutableStateFlow<Int> = MutableStateFlow(0)
    val latency: StateFlow<Int> = _latency

    internal val _resolution: MutableStateFlow<String> = MutableStateFlow("")
    val resolution: StateFlow<String> = _resolution

    internal val _qualityDropReason: MutableStateFlow<String> = MutableStateFlow("")
    val qualityDropReason: StateFlow<String> = _qualityDropReason

    internal val _jitterInMs: MutableStateFlow<Int> = MutableStateFlow(0)
    val jitterInMs: StateFlow<Int> = _jitterInMs

    internal val _bitrateKbps: MutableStateFlow<Float> = MutableStateFlow(0F)
    val bitrateKbps: StateFlow<Float> = _bitrateKbps

    internal val _videoCodec: MutableStateFlow<String> = MutableStateFlow("")
    val videoCodec: StateFlow<String> = _videoCodec

    internal val _audioCodec: MutableStateFlow<String> = MutableStateFlow("")
    val audioCodec: StateFlow<String> = _audioCodec
}

public data class LocalStats(
    val resolution: CameraEnumerationAndroid.CaptureFormat?,
    val availableResolutions: List<CameraEnumerationAndroid.CaptureFormat>?,
    val maxResolution: CameraEnumerationAndroid.CaptureFormat?,
    val sfu: String,
    val os: String,
    val sdkVersion: String,
    val deviceModel: String,
)

public class CallStats(val call: Call, val callScope: CoroutineScope) {
    private val logger by taggedLogger("CallStats")

    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(callScope.coroutineContext + supervisorJob)
    // TODO: cleanup the scope

    val publisher = PeerConnectionStats(scope)
    val subscriber = PeerConnectionStats(scope)
    val _local = MutableStateFlow<LocalStats?>(null)
    val local: StateFlow<LocalStats?> =
        _local.stateIn(scope, SharingStarted.WhileSubscribed(), null)

    fun updateFromRTCStats(stats: RtcStatsReport?, isPublisher: Boolean = true) {
        if (stats == null) return
        // also see https://github.com/GetStream/stream-video-js/blob/main/packages/client/src/stats/state-store-stats-reporter.ts

        val skipTypes = listOf("certificate", "data-channel")
        val trackToParticipant = call.session?.trackIdToParticipant?.value ?: emptyMap()
        val displayingAt = call.session?.trackDimensions?.value ?: emptyMap()

        val statGroups = mutableMapOf<String, MutableList<RTCStats>>()

        for (entry in stats.origin.statsMap) {
            val stat = entry.value

            val type = stat.type
            if (type in skipTypes) continue

            val statGroup = if (type == "inbound-rtp") {
                "$type:${stat.members["kind"]}"
            } else if (type == "track") {
                "$type:${stat.members["kind"]}"
            } else if (type == "outbound-rtp") {
                val rid = stat.members["rid"] ?: "missing"
                "$type:${stat.members["kind"]}:$rid"
            } else if (type == "codec") {
                (stat.members["mimeType"] as? String)?.split(
                    "/",
                )?.firstOrNull()?.let { audioOrVideo ->
                    "codec:$audioOrVideo"
                }
            } else {
                type
            }

            if (statGroup != null) {
                if (statGroup !in statGroups) {
                    statGroups[statGroup] = mutableListOf()
                }
                statGroups[statGroup]?.add(stat)
            }

            if (isPublisher) {
                // Get all outbound video qualities
                val mediaStatsF = statGroups["outbound-rtp:video:f"]?.firstOrNull()?.let {
                    MediaStatsInfo.fromMap(it.members)
                }
                val mediaStatsH = statGroups["outbound-rtp:video:h"]?.firstOrNull()?.let {
                    MediaStatsInfo.fromMap(it.members)
                }
                val mediaStatsQ = statGroups["outbound-rtp:video:q"]?.firstOrNull()?.let {
                    MediaStatsInfo.fromMap(it.members)
                }

                // Choose the quality that actually streams and has data, starting from highest one
                val mediaStats = mediaStatsF?.takeIf {
                    it.width != null && it.height != null && it.fps != null
                } ?: mediaStatsH?.takeIf {
                    it.width != null && it.height != null && it.fps != null
                } ?: mediaStatsQ?.takeIf {
                    it.width != null && it.height != null && it.fps != null
                }
                publisher._qualityDropReason.value = mediaStats?.qualityLimit ?: ""
                publisher._jitterInMs.value = mediaStats?.jitter?.times(1000)?.toInt() ?: 0
                val resolutionUpdate = mediaStats?.takeUnless {
                    it.width == null || it.height == null || it.fps == null
                }?.let {
                    "${it.width} x ${it.height} @ ${it.fps}fps"
                }
                if (resolutionUpdate != null) {
                    publisher._resolution.value = resolutionUpdate
                }
            }

            statGroups["inbound-rtp:video"]?.firstOrNull()?.let {
                val jitter = it.members["jitter"] as Double
                val width = it.members["frameWidth"] as? Long
                val height = it.members["frameHeight"] as? Long
                val fps = it.members["framesPerSecond"] as? Double
                subscriber._jitterInMs.value = (jitter * 1000).toInt()
                if (width != null && height != null && fps != null) {
                    subscriber._resolution.value = "$width x $height @ $fps fps"
                }
            }
            statGroups["candidate-pair"]?.firstOrNull()?.let {
                val latency = it.members["currentRoundTripTime"] as? Double
                val outgoingBitrate = it.members["availableOutgoingBitrate"] as? Double
                val incomingBitrate = it.members["availableIncomingBitrate"] as? Double
                latency?.let {
                    publisher._latency.value = (latency * 1000).toInt()
                }
                outgoingBitrate?.let {
                    publisher._bitrateKbps.value = (outgoingBitrate / 1000).toFloat()
                }

                incomingBitrate?.let {
                    subscriber._bitrateKbps.value = (incomingBitrate / 1000).toFloat()
                }
            }
            statGroups["track:video"]?.forEach {
                // trackIdentifier is a random UUID generated by the browser
                // map to a participant?
                val trackId = it.members["trackIdentifier"]
                val participantId = trackToParticipant[trackId]
                val participantVideo = displayingAt[participantId] ?: emptyMap()
                val visibleAt = participantVideo[TrackType.TRACK_TYPE_VIDEO]
                val freezeInSeconds = it.members["totalFreezesDuration"]
                val frameWidth = it.members["frameWidth"] as? Long
                val frameHeight = it.members["frameHeight"] as? Long
                val received = it.members["framesReceived"] as? Long
                val duration = it.members["totalFramesDuration"] as? Long
                if (participantId != null) {
                    logger.v {
                        "[stats] #manual-quality-selection; receiving video for $participantId at $frameWidth: ${it.members["frameWidth"]} and rendering it at ${visibleAt?.dimensions?.width} visible: ${visibleAt?.visible}"
                    }
                }
            }
            statGroups["codec:video"]?.firstOrNull()?.let {
                (it.members["mimeType"] as? String)?.split("/")[1]?.let { codec ->
                    publisher._videoCodec.value = codec
                }
            }
            statGroups["codec:audio"]?.firstOrNull()?.let {
                (it.members["mimeType"] as? String)?.split("/")[1]?.let { codec ->
                    publisher._audioCodec.value = codec
                }
            }
        }

        statGroups.forEach {
            logger.i { "statgroup ${it.key}:${it.value}" }
        }
    }

    fun updateLocalStats() {
        val displayingAt = call.session?.trackDimensions?.value ?: emptyMap()
        val resolution = call.camera.resolution.value
        val availableResolutions = call.camera.availableResolutions.value
        val maxResolution = availableResolutions.maxByOrNull { it.width * it.height }

        val sfu = call.session?.sfuUrl

        val version = BuildConfig.STREAM_VIDEO_VERSION
        val osVersion = Build.VERSION.RELEASE ?: ""

        val vendor = Build.MANUFACTURER ?: ""
        val model = Build.MODEL ?: ""
        val deviceModel = ("$vendor $model").trim()

        val local = LocalStats(
            resolution = resolution,
            availableResolutions = availableResolutions,
            maxResolution = maxResolution,
            sfu = sfu ?: "",
            os = osVersion,
            sdkVersion = version,
            deviceModel = deviceModel,
        )
        _local.value = local
    }
}
