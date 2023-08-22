package io.getstream.video.android.core.call.stats.model

sealed class RtcStats {
    abstract val id: String?
    abstract val type: String?
    abstract val timestampUs: Double?
}