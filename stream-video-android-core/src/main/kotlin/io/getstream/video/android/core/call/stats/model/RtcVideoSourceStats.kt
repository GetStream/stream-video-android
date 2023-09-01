package io.getstream.video.android.core.call.stats.model

data class RtcVideoSourceStats(
    override val id: String?,
    override val type: String?,
    override val timestampUs: Double?,
    override val kind: String?,
    override val trackIdentifier: String?,
    val width: Long?,
    val height: Long?,
    val framesPerSecond: Double?,
    val frames: Long?,

) : RtcMediaSourceStats {

    companion object {
        const val KIND = RtcMediaSourceStats.KIND
        const val TRACK_IDENTIFIER = RtcMediaSourceStats.TRACK_IDENTIFIER
        const val WIDTH = "width"
        const val HEIGHT = "height"
        const val FRAMES_PER_SECOND = "framesPerSecond"
        const val FRAMES = "frames"
    }
}