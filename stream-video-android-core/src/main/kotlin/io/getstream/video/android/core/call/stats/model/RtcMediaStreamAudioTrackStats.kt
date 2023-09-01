package io.getstream.video.android.core.call.stats.model


// https://www.w3.org/TR/2023/CRD-webrtc-stats-20230427/#dom-rtcmediastreamtrackstats
@Deprecated("Was deprecated in 11 May 2023")
sealed interface RtcMediaStreamAudioTrackStats : RtcMediaStreamTrackStats {
    val audioLevel: Double?
    val totalAudioEnergy: Double?
    val totalSamplesDuration: Double?
}