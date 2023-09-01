package io.getstream.video.android.core.call.stats.model

sealed interface RtcMediaSourceStats : RtcStats {

    val kind: String?
    val trackIdentifier: String?

    companion object {
        const val KIND = "kind"
        const val TRACK_IDENTIFIER = "trackIdentifier"
    }
}