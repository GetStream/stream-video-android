package io.getstream.video.android.core.call.stats.model


// https://www.w3.org/TR/2023/CRD-webrtc-stats-20230427/#dom-rtcmediastreamtrackstats
@Deprecated("Was deprecated in 11 May 2023")
sealed interface RtcMediaStreamVideoTrackStats : RtcMediaStreamTrackStats {
    val frameHeight: Long?
    val frameWidth: Long?
    val framesPerSecond: Double?

    companion object {
        const val FRAME_HEIGHT = "frameHeight"
        const val FRAME_WIDTH = "frameWidth"
        const val FRAMES_PER_SECOND = "framesPerSecond"
    }
}