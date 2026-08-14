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

package io.getstream.video.android.core.call.components

import android.os.Build
import android.os.PowerManager
import android.os.PowerManager.THERMAL_STATUS_CRITICAL
import android.os.PowerManager.THERMAL_STATUS_EMERGENCY
import android.os.PowerManager.THERMAL_STATUS_LIGHT
import android.os.PowerManager.THERMAL_STATUS_MODERATE
import android.os.PowerManager.THERMAL_STATUS_NONE
import android.os.PowerManager.THERMAL_STATUS_SEVERE
import android.os.PowerManager.THERMAL_STATUS_SHUTDOWN
import io.getstream.log.taggedLogger
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.video.android.core.BuildConfig
import io.getstream.video.android.core.CallStatsReport
import io.getstream.video.android.core.call.connection.utils.safeApiCall
import io.getstream.video.android.core.call.stats.model.RtcStatsReport
import io.getstream.video.android.core.internal.module.SfuConnectionModule
import io.getstream.video.android.core.model.StreamPeerType
import io.getstream.video.android.core.toJson
import io.getstream.video.android.core.trace.TraceSlice
import io.getstream.video.android.core.trace.Tracer
import io.getstream.video.android.core.trace.TracerManager
import io.getstream.video.android.core.trace.serialize
import io.getstream.video.android.core.utils.safeCallWithDefault
import stream.video.sfu.models.AndroidState
import stream.video.sfu.models.AndroidThermalState
import stream.video.sfu.models.WebsocketReconnectStrategy
import stream.video.sfu.signal.Reconnection
import stream.video.sfu.signal.SendStatsRequest
import stream.video.sfu.signal.Telemetry

/**
 * Collects publisher/subscriber WebRTC stats for one RTC session and posts them to the SFU,
 * including device thermal / power-saver telemetry and connection / reconnection timings.
 */
internal class RtcStatsCollector(
    private val powerManager: PowerManager?,
    private val sessionId: String,
    private val sessionManager: CallSessionManager,
    private val peerConnections: PeerConnections,
    private val sfuConnectionModule: () -> SfuConnectionModule,
    private val tracerManager: TracerManager,
    private val publisherTracer: Tracer,
    private val subscriberTracer: Tracer,
    private val sfuTracer: Tracer,
    private val statsReporter: CallStatsReporter,
) {
    private val logger by taggedLogger("Video:RtcStatsCollector")

    suspend fun getPublisherStats(): RtcStatsReport? = peerConnections.publisher.value?.getStats()

    suspend fun getSubscriberStats(): RtcStatsReport? = peerConnections.subscriber.value?.getStats()

    suspend fun sendConnectionTimeStats(reconnectStrategy: WebsocketReconnectStrategy? = null) {
        if (reconnectStrategy == null) {
            sendCallStats(
                report = statsReporter.collectStats(),
                connectionTimeSeconds = sessionManager.connectionTimeSeconds(),
            )
        } else {
            sendCallStats(
                report = statsReporter.collectStats(),
                reconnectionTimeSeconds = Pair(
                    sessionManager.reconnectionTimeSeconds(),
                    reconnectStrategy,
                ),
            )
        }
    }

    suspend fun sendCallStats(
        report: CallStatsReport? = null,
        connectionTimeSeconds: Float? = null,
        reconnectionTimeSeconds: Pair<Float, WebsocketReconnectStrategy>? = null,
    ) {
        val result = safeApiCall {
            val androidThermalState =
                safeCallWithDefault(AndroidThermalState.ANDROID_THERMAL_STATE_UNSPECIFIED) {
                    val thermalState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        powerManager?.currentThermalStatus
                    } else {
                        AndroidThermalState.ANDROID_THERMAL_STATE_UNSPECIFIED
                    }
                    logger.d { "[sendCallStats] #thermals state: $thermalState" }
                    when (thermalState) {
                        THERMAL_STATUS_NONE -> AndroidThermalState.ANDROID_THERMAL_STATE_NONE
                        THERMAL_STATUS_LIGHT -> AndroidThermalState.ANDROID_THERMAL_STATE_LIGHT
                        THERMAL_STATUS_MODERATE -> AndroidThermalState.ANDROID_THERMAL_STATE_MODERATE
                        THERMAL_STATUS_SEVERE -> AndroidThermalState.ANDROID_THERMAL_STATE_SEVERE
                        THERMAL_STATUS_CRITICAL -> AndroidThermalState.ANDROID_THERMAL_STATE_CRITICAL
                        THERMAL_STATUS_EMERGENCY -> AndroidThermalState.ANDROID_THERMAL_STATE_EMERGENCY
                        THERMAL_STATUS_SHUTDOWN -> AndroidThermalState.ANDROID_THERMAL_STATE_SHUTDOWN
                        else -> AndroidThermalState.ANDROID_THERMAL_STATE_UNSPECIFIED
                    }
                }
            val powerSaving = safeCallWithDefault(false) {
                val powerSaveMode = powerManager?.isPowerSaveMode
                logger.d { "[sendCallStats] #powerSaveMode state: $powerSaveMode" }
                powerSaveMode ?: false
            }

            val publisherRtcStats = peerConnections.publisher.value?.stats()
            val subscriberRtcStats = peerConnections.subscriber.value?.stats()
            publisherTracer.trace("getstats", publisherRtcStats?.delta)
            subscriberTracer.trace("getstats", subscriberRtcStats?.delta)

            val tracerSlices = mutableListOf<TraceSlice>()

            val rtcStats = tracerManager.tracers().flatMap { tracer ->
                val slice = tracer.take()
                tracerSlices.add(slice)
                slice.snapshot.map { it.serialize() }
            }.toMutableList().toJsonArray()

            logger.d { "[sendCallStats] #sfu; #track; rtc_stats: $rtcStats" }

            val sendStatsRequest = SendStatsRequest(
                session_id = sessionId,
                sdk = "stream-android",
                unified_session_id = sessionManager.unifiedSessionId,
                sdk_version = BuildConfig.STREAM_VIDEO_VERSION,
                webrtc_version = BuildConfig.STREAM_WEBRTC_VERSION,
                publisher_stats = report?.toJson(StreamPeerType.PUBLISHER) ?: "",
                subscriber_stats = report?.toJson(StreamPeerType.SUBSCRIBER) ?: "",
                rtc_stats = rtcStats,
                encode_stats = publisherRtcStats?.performanceStats ?: emptyList(),
                decode_stats = subscriberRtcStats?.performanceStats ?: emptyList(),
                android = AndroidState(
                    thermal_state = androidThermalState,
                    is_power_saver_mode = powerSaving,
                ),
                telemetry = safeCallWithDefault(null) {
                    if (connectionTimeSeconds != null) {
                        Telemetry(
                            connection_time_seconds = connectionTimeSeconds.toFloat(),
                        )
                    } else if (reconnectionTimeSeconds != null) {
                        Telemetry(
                            reconnection = Reconnection(
                                time_seconds = reconnectionTimeSeconds.first.toFloat(),
                                strategy = reconnectionTimeSeconds.second,
                            ),
                        )
                    } else {
                        null
                    }
                },
            )
            logger.d { "[sendCallStats] #sfu; #track; request: $rtcStats" }
            try {
                sfuConnectionModule().api.sendStats(
                    sendStatsRequest = sendStatsRequest,
                )
            } catch (e: Exception) {
                sfuTracer.trace("send-stats-failed", "${e.message}")
                tracerSlices.forEach { slice -> slice.rollback() }
            }
        }

        logger.d {
            "sendStats: " + when (result) {
                is Success -> "Success. Response: ${result.value}. Telemetry: connectionTimeSeconds: $connectionTimeSeconds, reconnectionTimeSeconds: ${reconnectionTimeSeconds?.first}, strategy: ${reconnectionTimeSeconds?.second}"
                is Failure -> "Failure. Reason: ${result.value.message}"
            }
        }
    }

    private fun List<Array<Any?>>.toJsonArray(): String {
        // Manual encoding: android.jar stubs used by JVM unit tests return null from
        // JSONArray.toString(), which crashes Kotlin's non-null String bridge.
        return buildString {
            append('[')
            this@toJsonArray.forEachIndexed { index, inner ->
                if (index > 0) append(',')
                append('[')
                inner.forEachIndexed { i, element ->
                    if (i > 0) append(',')
                    when (element) {
                        null -> append("null")
                        is Number, is Boolean -> append(element.toString())
                        else -> {
                            append('"')
                            append(
                                element.toString()
                                    .replace("\\", "\\\\")
                                    .replace("\"", "\\\""),
                            )
                            append('"')
                        }
                    }
                }
                append(']')
            }
            append(']')
        }
    }
}
