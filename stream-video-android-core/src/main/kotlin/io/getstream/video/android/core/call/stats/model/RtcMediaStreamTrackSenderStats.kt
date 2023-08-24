package io.getstream.video.android.core.call.stats.model


sealed class RtcMediaStreamTrackSenderStats : RtcMediaStreamTrackStats() {
    abstract val mediaSourceId: String?
    abstract val trackIdentifier: String?
}