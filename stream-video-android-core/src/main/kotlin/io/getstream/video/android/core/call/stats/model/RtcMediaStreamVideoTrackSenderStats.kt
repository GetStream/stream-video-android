package io.getstream.video.android.core.call.stats.model

// {
//  id: RTCMediaStreamTrack_sender_2,
//  type: track,
//  timestamp: 1679425535359288.0,
//  kind: video,
//  frameHeight: 1280,
//  frameWidth: 720,
//  framesSent: 159,
//  hugeFramesSent: 0,
//  detached: false,
//  ended: false,
//  remoteSource: false,
//  trackIdentifier: 503d949a-f020-49cc-9a23-61bc1e04e89b,
//  mediaSourceId: RTCVideoSource_2
// }
// https://www.w3.org/TR/2023/CRD-webrtc-stats-20230427/#dom-rtcmediastreamtrackstats
@Deprecated("Was deprecated in 11 May 2023")
data class RtcMediaStreamVideoTrackSenderStats(
    override val id: String?,
    override val type: String?,
    override val timestampUs: Double?,
    override val trackIdentifier: String?,
    override val ended: Boolean?,
    override val kind: String?,
    override val priority: String?,
    override val remoteSource: Boolean?,
    override val detached: Boolean?,
    override val mediaSourceId: String?,
    override val frameHeight: Long?,
    override val frameWidth: Long?,
    override val framesPerSecond: Double?,
    val keyFramesSent: Long?,
    val framesCaptured: Long?,
    val framesSent: Long?,
    val hugeFramesSent: Long?,
) : RtcMediaStreamTrackSenderStats, RtcMediaStreamVideoTrackStats {

    companion object {
        const val TRACK_IDENTIFIER = "trackIdentifier"
        const val ENDED = "ended"
        const val KIND = "kind"
        const val PRIORITY = "priority"
        const val REMOTE_SOURCE = "remoteSource"
        const val DETACHED = "detached"
        const val MEDIA_SOURCE_ID = "mediaSourceId"
        const val FRAME_HEIGHT = "frameHeight"
        const val FRAME_WIDTH = "frameWidth"
        const val FRAMES_PER_SECOND = "framesPerSecond"
        const val KEY_FRAMES_SENT = "keyFramesSent"
        const val FRAMES_CAPTURED = "framesCaptured"
        const val FRAMES_SENT = "framesSent"
        const val HUGE_FRAMES_SENT = "hugeFramesSent"
    }

}