package io.getstream.video.android.core.call.stats.model

sealed class RtcMediaSourceStats : RtcStats() {

    abstract val kind: String?
    abstract val trackIdentifier: String?

    companion object {
        const val KIND = "kind"
        const val TRACK_IDENTIFIER = "trackIdentifier"
    }
}