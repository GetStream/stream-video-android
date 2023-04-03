package io.getstream.video.android.core

import io.getstream.video.android.core.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import stream.video.sfu.models.ConnectionQuality
import stream.video.sfu.models.TrackType
import java.util.*

/**
 * TODO: Line 297. Where do we get the call settings from?
 */



public open class ParticipantState(open val call: Call2, user: User) {

    open fun muteAudio() {
        // how do i mute another user?
    }

    open fun muteVideo() {
        // how do i mute another user?
    }

    open val videoTrack by lazy {
        call.activeSession?.getParticipant(user.id)?.videoTrack
    }
    open val audioTrack by lazy {
        call.activeSession?.getParticipant(user.id)?.publishedTracks?.filter { it == TrackType.TRACK_TYPE_AUDIO }
    }

    /**
     * The user
     */
    internal val _user: MutableStateFlow<User> = MutableStateFlow(user)
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

    internal val _speaking: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking

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