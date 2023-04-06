/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-video-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.video.android.core

import io.getstream.result.Result
import io.getstream.video.android.core.model.MuteUsersData
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.model.VideoTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import stream.video.sfu.models.ConnectionQuality
import stream.video.sfu.models.Participant
import stream.video.sfu.models.TrackType
import java.util.*

/**
 * Represents the state of a participant in a call.
 *
 * * A list of participants is shared when you join a call
 * * The SFU sens you the participant joined event
 *
 * @see ParticipantJoinedEvent which shares the basic participant info found in
 * @see Participant
 *
 * TODO: we need equality functions here , data class will ignore the stateflows
 */
public data class ParticipantState(
    /** The SFU returns a session id for each participant. This session id is unique */
    var sessionId: String = "",
    /** The call object */
    val call: Call,
    /** The current version of the user, this is the start for participant.user stateflow */
    val initialUser: User,
    /** If this participant is the you/ the local participant */
    val isLocal: Boolean = false,
    /** video track and size */
    var videoTrack: VideoTrack? = null,
    var videoTrackSize: Pair<Int, Int> = Pair(0, 0),
    /** screen sharing track and size */
    var screenSharingTrack: VideoTrack? = null,
    var screenSharingTrackSize: Pair<Int, Int> = Pair(0, 0),
    /** all published tracks including audio */
    var publishedTracks: Set<TrackType> = emptySet(),
    /** A prefix to identify tracks, internal */
    internal var trackLookupPrefix: String = "",

) {
    /**
     * The user, automatically updates when we receive user events
     */
    internal val _user: MutableStateFlow<User> = MutableStateFlow(initialUser)
    val user: StateFlow<User> = _user

    /**
     * When you joined the call
     */
    internal val _joinedAt: MutableStateFlow<Date?> = MutableStateFlow(null)
    val joinedAt: StateFlow<Date?> = _joinedAt

    /**
     * The audio level of the participant
     */
    internal val _audioLevel: MutableStateFlow<Float> = MutableStateFlow(0F)
    val audioLevel: StateFlow<Float> = _audioLevel

    /**
     * The video quality of the participant
     */
    internal val _connectionQuality: MutableStateFlow<ConnectionQuality> =
        MutableStateFlow(ConnectionQuality.CONNECTION_QUALITY_UNSPECIFIED)
    val connectionQuality: StateFlow<ConnectionQuality> = _connectionQuality

    /**
     * For ringing calls we track when the participant accepted or rejected
     */
    internal val _acceptedAt: MutableStateFlow<Date?> = MutableStateFlow(null)
    val acceptedAt: StateFlow<Date?> = _acceptedAt

    internal val _rejectedAt: MutableStateFlow<Date?> = MutableStateFlow(null)
    val rejectedAt: StateFlow<Date?> = _rejectedAt

    /**
     * State that indicates whether the camera is capturing and sending video or not.
     */
    internal val _videoEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val videoEnabled: StateFlow<Boolean> = _videoEnabled

    /**
     * State that indicates whether the mic is capturing and sending the audio or not.
     */
    internal val _audioEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val audioEnabled: StateFlow<Boolean> = _audioEnabled

    /**
     * State that indicates whether the speakerphone is on or not.
     */
    internal val _speakerPhoneEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val speakerPhoneEnabled: StateFlow<Boolean> = _speakerPhoneEnabled

    internal val _dominantSpeaker: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val dominantSpeaker: StateFlow<Boolean> = _dominantSpeaker

    internal val _speaking: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking

    internal val _lastSpeakingAt: MutableStateFlow<Date?> = MutableStateFlow(null)
    val lastSpeakingAt: StateFlow<Date?> = _lastSpeakingAt

    internal val _pinnedAt: MutableStateFlow<Date?> = MutableStateFlow(null)
    val pinnedAt: StateFlow<Date?> = _pinnedAt

    open val audioTrack by lazy {
        publishedTracks?.filter { it == TrackType.TRACK_TYPE_AUDIO }
    }

    open suspend fun muteAudio(): Result<Unit> {
        // how do i mute another user?
        return call.muteUsers(MuteUsersData(audio = true, users = listOf(user.value.id)))
    }

    open suspend fun muteVideo(): Result<Unit> {
        return call.muteUsers(MuteUsersData(video = true, users = listOf(user.value.id)))
    }

    open suspend fun muteScreenshare(): Result<Unit> {
        return call.muteUsers(MuteUsersData(screenShare = true, users = listOf(user.value.id)))
    }

    fun updateFromParticipantInfo(participant: Participant) {
        sessionId = participant.session_id
        _joinedAt.value = participant.joined_at?.toEpochMilli()?.let { Date(it) }
            ?: Date() // convert instant to date
        trackLookupPrefix = participant.track_lookup_prefix
        _connectionQuality.value = participant.connection_quality
        _speaking.value = participant.is_speaking
        _dominantSpeaker.value = participant.is_dominant_speaker
        _audioLevel.value = participant.audio_level
        val currentUser = _user.value
        _user.value = currentUser.copy(
            name = participant.name,
            imageUrl=participant.image,
            //custom = participant.custom,
            role=participant.roles.first()
        )

        publishedTracks = participant.published_tracks.toSet()
    }
}
