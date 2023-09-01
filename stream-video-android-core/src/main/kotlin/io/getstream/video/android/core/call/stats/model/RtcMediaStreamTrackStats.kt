package io.getstream.video.android.core.call.stats.model

// https://www.w3.org/TR/2023/CRD-webrtc-stats-20230427/#dom-rtcmediastreamtrackstats
@Deprecated("Was deprecated in 11 May 2023")
sealed interface RtcMediaStreamTrackStats : RtcStats {
    val trackIdentifier: String?
    val ended: Boolean?
    val kind: String?
    val priority: String?
    val remoteSource: Boolean?
    val detached: Boolean?

    companion object {
        const val TRACK_IDENTIFIER = "trackIdentifier"
        const val KIND = "kind"
        const val ENDED = "ended"
        const val PRIORITY = "priority"
        const val REMOTE_SOURCE = "remoteSource"
        const val DETACHED = "detached"
    }
}