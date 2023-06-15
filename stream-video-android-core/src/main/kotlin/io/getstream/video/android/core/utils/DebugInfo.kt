/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.utils

import io.getstream.log.taggedLogger
import io.getstream.video.android.core.StreamVideoImpl
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.RTCStats
import org.webrtc.RTCStatsReport

internal data class Timer(val name: String, val start: Long = System.currentTimeMillis()) {
    var end: Long = 0
    var duration: Long = 0
    var splits: List<Pair<String, Long>> = mutableListOf<Pair<String, Long>>()
    var durations: List<Pair<String, Long>> = mutableListOf<Pair<String, Long>>()

    fun split(s: String) {
        val now = System.currentTimeMillis()
        val last = splits.lastOrNull()?.second ?: start
        splits += s to now
        durations += s to (now - last)
    }

    fun finish(s: String? = null): Long {
        s?.let {
            split(s)
        }
        end = System.currentTimeMillis()
        duration = end - start
        return duration
    }
}

/**
 * Handy helper gathering all relevant debug information
 */
internal class DebugInfo(val client: StreamVideoImpl) {
    private var job: Job? = null
    val scope = CoroutineScope(DispatcherProvider.IO)

    private val logger by taggedLogger("DebugInfo")

    // timers to help track performance issues in prod
    val timers = mutableListOf<Timer>()
    // last 20 events

    // phone type
    val phoneModel = android.os.Build.MODEL

    // android version
    val version = android.os.Build.VERSION.SDK_INT

    // how many times the network dropped

    // how often the sockets reconnected

    // supported codecs
    // resolution
    val resolution by lazy { }
    val availableResolutions by lazy { }

    fun start() {
        if (client.developmentMode) {
            job = scope.launch {
                while (true) {
                    delay(20000)
                    log()
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
    }

    fun log() {
        val call = client.state.activeCall.value
        val sessionId = call?.sessionId
        val subscriber = call?.session?.subscriber
        val publisher = call?.session?.publisher
        val resolution = call?.camera?.resolution?.value
        val availableResolutions = call?.camera?.availableResolutions?.value
        val maxResolution = availableResolutions?.maxByOrNull { it.width * it.height }

        val publisherIce = publisher?.connection?.iceConnectionState()
        val subIce = subscriber?.connection?.iceConnectionState()

        val videoTrackState = call?.mediaManager?.videoTrack?.state()
        val coordinatorSocket = client.socketImpl.connectionState.value.javaClass.name
        val sfuSocket =
            call?.session?.sfuConnectionModule?.sfuSocket?.connectionState?.value?.javaClass?.name

        // good spot to attach your debugger

        logger.i { "Debug info $phoneModel running android $version" }
        logger.i { "Active call is ${call?.cid}, session id $sessionId" }
        logger.i { "video quality: current resolution $resolution max resolution for camera is $maxResolution" }
        logger.i { "Coordinator socket: $coordinatorSocket, SFU socket: $sfuSocket Subscriber: $publisherIce Publisher: $subIce" }
        logger.i { "Performance details" }
        timers.forEach {
            logger.i { "${it.name} took ${it.duration}" }
            it.durations.forEach { (s, t) ->
                logger.i { " - ${it.name}:$s took $t" }
            }
        }
        /*
        Stats wishlist
        - selected sfu
        - max resolution & fps capture
        - incoming, rendering at resolution vs receiving resolution
        - jitter & latency
        - fir, pli, nack etc
        - video limit reasons
        - selected resolution
        - TCP instead of UDP
         */
        localStats()
        publisher?.let {
            val stats = it.getStats().value
            processPubStats(stats)
            logger.i { "Publisher stats. video quality: $stats" }
        }
        subscriber?.let {
            val stats = it.getStats().value
            processSubStats(stats)
            logger.i { "Subscriber stats. video quality: $stats" }
        }
    }

    fun localStats() {
        val call = client.state.activeCall.value
        val resolution = call?.camera?.resolution?.value
        val availableResolutions = call?.camera?.availableResolutions?.value
        val maxResolution = availableResolutions?.maxByOrNull { it.width * it.height }

        val displayingAt = call?.session?.trackDimensions?.value

        val sfu = call?.session?.sfuUrl

        logger.i { "stat123 with $sfu ${resolution}, ${maxResolution}, displaying external video at ${displayingAt}" }

    }


    fun processStats(stats: RTCStatsReport?) {
        if (stats == null) return

        val skipTypes = listOf("codec", "certificate", "data-channel")

        val statGroups = mutableMapOf<String, MutableList<RTCStats>>()

        for (entry in stats.statsMap) {
            val stat = entry.value

            val type = stat.type
            if (type in skipTypes) continue

            val statGroup = if (type=="inbound-rtp") {
                "$type:${stat.members["kind"]}"
            } else if (type=="track") {
                "$type:${stat.members["kind"]}"
            } else if (type=="outbound-rtp") {
                val rid = stat.members["rid"] ?: "missing"
                "$type:${stat.members["kind"]}:$rid"
            } else {
                type
            }

            if (statGroup != null ) {
                if (statGroup !in statGroups) {
                    statGroups[statGroup] = mutableListOf()
                }
                statGroups[statGroup]?.add(stat)
            }
        }

        statGroups.forEach {
            logger.i { "stat123 $${it.key}:${it.value}" }
        }


    }

    fun processPubStats(stats: RTCStatsReport?) {
        processStats(stats)

    }

    fun processSubStats(stats: RTCStatsReport?) {
        processStats(stats)

    }

    fun listCodecs() {
        // see https://developer.android.com/reference/kotlin/android/media/MediaCodecInfo
    }

    fun trackTime(s: String): Timer {
        val timer = Timer(s, System.currentTimeMillis())
        timers += timer
        return timer
    }
}
