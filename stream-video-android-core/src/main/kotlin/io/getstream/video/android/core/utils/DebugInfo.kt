package io.getstream.video.android.core.utils

import io.getstream.log.taggedLogger

internal data class Timer(val name: String, val start: Long) {
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

    fun finish(s: String?=null): Long {
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
internal class DebugInfo () {
    private val logger by taggedLogger("DebugInfo")
    // timers to help track performance issues in prod
    val timers = mutableListOf<Timer>()
    // last 20 events

    // phone type
    val phoneModel = android.os.Build.MODEL;

    // android version
    val version = android.os.Build.VERSION.SDK_INT

    // how many times the network dropped

    // how often the sockets reconnected

    // supported codecs

    fun log() {
        logger.i { "Debug info ${phoneModel} runnin android ${version}" }
        timers.forEach {
            logger.i { "${it.name} took ${it.duration}"}
            it.durations.forEach { (s, t) ->
                logger.i {" - ${it.name}:$s took $t"}
            }
        }
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