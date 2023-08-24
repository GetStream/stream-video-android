package io.getstream.video.android.core.call.stats.model

// {
//  id: RTCMediaStreamTrack_sender_3,
//  type: track,
//  timestamp: 1679425535359288.0,
//  kind: audio,
//  mediaSourceId: RTCAudioSource_3
//  trackIdentifier: 3137ee0d-6f04-4e53-a45a-20fbdcdcd1ca,
//  remoteSource: false,
//  detached: false,
//  ended: false,
// }

data class RtcMediaStreamAudioTrackSenderStats(
    override val id: String?,
    override val type: String?,
    override val timestampUs: Double?,
    override val kind: String?,
    override val mediaSourceId: String?,
    override val trackIdentifier: String?,
    override val remoteSource: Boolean?,
    override val detached: Boolean?,
    override val ended: Boolean?,
) : RtcMediaStreamTrackSenderStats() {

    companion object {
        const val KIND = "kind"
        const val TRACK_IDENTIFIER = "trackIdentifier"
        const val MEDIA_SOURCE_ID = "mediaSourceId"
        const val REMOTE_SOURCE = "remoteSource"
        const val DETACHED = "detached"
        const val ENDED = "ended"
    }

}