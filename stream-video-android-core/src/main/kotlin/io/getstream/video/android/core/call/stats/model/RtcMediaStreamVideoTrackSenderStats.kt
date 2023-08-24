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
data class RtcMediaStreamVideoTrackSenderStats(
    override val id: String?,
    override val type: String?,
    override val timestampUs: Double?,
    override val kind: String?,
    override val mediaSourceId: String?,
    override val trackIdentifier: String?,
    override val remoteSource: Boolean?,
    override val detached: Boolean?,
    override val ended: Boolean?,
    val frameHeight: Long?,
    val frameWidth: Long?,
    val framesSent: Long?,
    val hugeFramesSent: Long?,
) : RtcMediaStreamTrackSenderStats() {

    companion object {
        const val KIND = "kind"
        const val TRACK_IDENTIFIER = "trackIdentifier"
        const val MEDIA_SOURCE_ID = "mediaSourceId"
        const val REMOTE_SOURCE = "remoteSource"
        const val DETACHED = "detached"
        const val ENDED = "ended"
        const val FRAME_HEIGHT = "frameHeight"
        const val FRAME_WIDTH = "frameWidth"
        const val FRAMES_SENT = "framesSent"
        const val HUGE_FRAMES_SENT = "hugeFramesSent"
    }

}