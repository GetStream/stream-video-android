package io.getstream.video.android.core.call.stats.model

sealed class RtcMediaStreamTrackStats : RtcStats() {
    abstract val kind: String?
    abstract val remoteSource: Boolean?
    abstract val detached: Boolean?
    abstract val ended: Boolean?

    companion object {
        const val KIND = "kind"
        const val REMOTE_SOURCE = "remoteSource"
        const val DETACHED = "detached"
        const val ENDED = "ended"
    }
}