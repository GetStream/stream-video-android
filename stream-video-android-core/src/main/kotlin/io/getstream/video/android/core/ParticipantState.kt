package io.getstream.video.android.core

import io.getstream.video.android.core.model.MuteUsersData
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.core.utils.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import stream.video.sfu.models.ConnectionQuality
import stream.video.sfu.models.Participant
import stream.video.sfu.models.TrackType
import java.util.*

/**
 * TODO: Line 297. Where do we get the call settings from?
 */



public open class ParticipantState(
    open val call: Call,
    open val initialUser: User,
    open var sessionId: String = "",
    open var idPrefix: String = "",
    open val isLocal: Boolean = false,
    open var isOnline: Boolean = false,

    open var videoTrack: VideoTrack? = null,
    open var screenSharingTrack: VideoTrack? = null,
    open var publishedTracks: Set<TrackType> = emptySet(),
    open var videoTrackSize: Pair<Int, Int> = Pair(0, 0)
) {

    // old fields to evaluate if we need them

    open fun copy(
        call: Call = this.call,
        initialUser: User = this.initialUser,
        sessionId: String = this.sessionId,
        idPrefix: String = this.idPrefix,
        isLocal: Boolean = this.isLocal,
        isOnline: Boolean = this.isOnline,
        videoTrack: VideoTrack? = this.videoTrack,
        screenSharingTrack: VideoTrack? = this.screenSharingTrack,
        publishedTracks: Set<TrackType> = this.publishedTracks,
        videoTrackSize: Pair<Int, Int> = this.videoTrackSize
    ): ParticipantState {
        return ParticipantState(
            call = call,
            initialUser = initialUser,
            sessionId = sessionId,
            idPrefix = idPrefix,
            isLocal = isLocal,
            isOnline = isOnline,
            videoTrack = videoTrack,
            screenSharingTrack = screenSharingTrack,
            publishedTracks = publishedTracks,
            videoTrackSize = videoTrackSize
        )
    }


    public val hasVideo: Boolean
        get() = TrackType.TRACK_TYPE_VIDEO in publishedTracks

    public val hasAudio: Boolean
        get() = TrackType.TRACK_TYPE_AUDIO in publishedTracks

    public val hasScreenShare: Boolean
        get() = TrackType.TRACK_TYPE_SCREEN_SHARE in publishedTracks && screenSharingTrack != null

    public val hasScreenShareAudio: Boolean
        get() = TrackType.TRACK_TYPE_SCREEN_SHARE_AUDIO in publishedTracks


    open suspend fun muteAudio(): Result<Unit> {
        // how do i mute another user?
        return call.muteUsers(MuteUsersData(audio = true, users= listOf(user.value.id)))
    }

    open suspend fun muteVideo(): Result<Unit> {
        return call.muteUsers(MuteUsersData(video = true, users= listOf(user.value.id)))
    }

    open suspend fun muteScreenshare(): Result<Unit> {
        return call.muteUsers(MuteUsersData(screenShare = true, users= listOf(user.value.id)))
    }

    fun updateFromData(participant: Participant) {
        sessionId = participant.session_id
        _joinedAt.value = participant.joined_at?.toEpochMilli()?.let { Date(it) } ?: Date() // convert instant to date
        idPrefix = participant.track_lookup_prefix
        _connectionQuality.value = participant.connection_quality
        _speaking.value = participant.is_speaking
        _dominantSpeaker.value = participant.is_dominant_speaker
        _audioLevel.value = participant.audio_level
        val currentUser = _user.value
        _user.value = currentUser.copy(name = participant.name)
    }

    open val audioTrack by lazy {
        publishedTracks?.filter { it == TrackType.TRACK_TYPE_AUDIO }
    }

    /**
     * The user
     */
    internal val _user: MutableStateFlow<User> = MutableStateFlow(initialUser)
    val user: StateFlow<User> = _user

    internal val _acceptedAt: MutableStateFlow<Date?> = MutableStateFlow(null)
    val acceptedAt: StateFlow<Date?> = _acceptedAt

    internal val _rejectedAt: MutableStateFlow<Date?> = MutableStateFlow(null)
    val rejectedAt: StateFlow<Date?> = _rejectedAt

    internal val _joinedAt: MutableStateFlow<Date?> = MutableStateFlow(null)
    val joinedAt: StateFlow<Date?> = _joinedAt

    /**
     * State that indicates whether the camera is capturing and sending video or not.
     */
    internal val _videoEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val videoEnabled: StateFlow<Boolean> = _videoEnabled

    internal val _dominantSpeaker: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val dominantSpeaker: StateFlow<Boolean> = _dominantSpeaker

    internal val _speaking: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking

    internal val _lastSpeakingAt: MutableStateFlow<Date?> = MutableStateFlow(null)
    val lastSpeakingAt: StateFlow<Date?> = _lastSpeakingAt

    internal val _pinnedAt: MutableStateFlow<Date?> = MutableStateFlow(null)
    val pinnedAt: StateFlow<Date?> = _pinnedAt

    internal val _audioLevel: MutableStateFlow<Float> = MutableStateFlow(0F)
    val audioLevel: StateFlow<Float> = _audioLevel

    internal val _connectionQuality: MutableStateFlow<ConnectionQuality> =
        MutableStateFlow(ConnectionQuality.CONNECTION_QUALITY_UNSPECIFIED)
    val connectionQuality: StateFlow<ConnectionQuality> = _connectionQuality
    /**
     * State that indicates whether the mic is capturing and sending the audio or not.
     */
    internal val _isAudioEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val audioEnabled: StateFlow<Boolean> = _isAudioEnabled

    /**
     * State that indicates whether the speakerphone is on or not.
     */
    internal val _isSpeakerPhoneEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val speakerPhoneEnabled: StateFlow<Boolean> = _isSpeakerPhoneEnabled
}