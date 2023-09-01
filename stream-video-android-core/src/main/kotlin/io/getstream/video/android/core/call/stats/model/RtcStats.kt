package io.getstream.video.android.core.call.stats.model

sealed interface RtcStats {
    val id: String?
    val type: String?
    val timestampUs: Double?
}