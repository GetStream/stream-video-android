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
import io.getstream.video.android.core.model.AudioTrack
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.core.utils.mapState
import io.getstream.video.android.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.openapitools.client.models.MuteUsersResponse
import org.openapitools.client.models.ReactionResponse
import stream.video.sfu.models.ConnectionQuality
import stream.video.sfu.models.Participant
import stream.video.sfu.models.TrackType
import java.util.*

/**
 * Represents the state of a participant in a call.
 *
 * * A list of participants is shared when you join a call the SFU send you the participant joined event.
 *
 */
public data class ParticipantState(
    /** The SFU returns a session id for each participant. This session id is unique */
    var sessionId: String = "",
    /** The call object */
    val call: Call,
    /** The current version of the user, this is the start for participant.user stateflow */
    val initialUser: User,
    /** A prefix to identify tracks, internal */
    internal var trackLookupPrefix: String = "",
) {

    val isLocal by lazy {
        sessionId == call.session?.sessionId
    }

    /** video track */
    internal val _videoTrack = MutableStateFlow<VideoTrack?>(null)
    val videoTrack: StateFlow<VideoTrack?> = _videoTrack

    /**
     * State that indicates whether the camera is capturing and sending video or not.
     */
    // todo: videoAvailable might be more descriptive
    internal val _videoEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val videoEnabled: StateFlow<Boolean> = _videoEnabled

    /**
     * State that indicates whether the mic is capturing and sending the audio or not.
     */
    internal val _audioEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val audioEnabled: StateFlow<Boolean> = _audioEnabled

    internal val _audioTrack = MutableStateFlow<AudioTrack?>(null)
    val audioTrack: StateFlow<AudioTrack?> = _audioTrack

    internal val _screenSharingEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val screenSharingEnabled: StateFlow<Boolean> = _screenSharingEnabled

    internal val _screenSharingTrack = MutableStateFlow<VideoTrack?>(null)
    val screenSharingTrack: StateFlow<VideoTrack?> = _screenSharingTrack

    /**
     * The user, automatically updates when we receive user events
     */
    internal val _user: MutableStateFlow<User> =
        MutableStateFlow(initialUser)
    val user: StateFlow<User> = _user

    // Could also be a property on the user
    val userNameOrId: StateFlow<String> = _user.mapState { it.name.ifEmpty { it.id } }

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

    internal val _dominantSpeaker: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val dominantSpeaker: StateFlow<Boolean> = _dominantSpeaker

    internal val _speaking: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking

    internal val _lastSpeakingAt: MutableStateFlow<Date?> = MutableStateFlow(null)
    val lastSpeakingAt: StateFlow<Date?> = _lastSpeakingAt

    internal val _pinnedAt: MutableStateFlow<Date?> = MutableStateFlow(null)
    val pinnedAt: StateFlow<Date?> = _pinnedAt

    internal val _reactions = MutableStateFlow<List<ReactionResponse>>(emptyList())
    val reactions: StateFlow<List<ReactionResponse>> = _reactions

    suspend fun muteAudio(): Result<MuteUsersResponse> {
        // how do i mute another user?
        return call.muteUser(user.value.id, audio = true, video = false, screenShare = false)
    }

    suspend fun muteVideo(): Result<MuteUsersResponse> {
        return call.muteUser(user.value.id, audio = false, video = true, screenShare = false)
    }

    suspend fun muteScreenshare(): Result<MuteUsersResponse> {
        return call.muteUser(user.value.id, audio = false, video = false, screenShare = true)
    }

    suspend fun pin() {
        return call.state.pin(this.sessionId)
    }

    suspend fun unpin() {
        return call.state.unpin(this.sessionId)
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
        _audioEnabled.value = participant.published_tracks.contains(TrackType.TRACK_TYPE_AUDIO)
        _videoEnabled.value = participant.published_tracks.contains(TrackType.TRACK_TYPE_VIDEO)
        _screenSharingEnabled.value = participant.published_tracks.contains(TrackType.TRACK_TYPE_SCREEN_SHARE)

        val currentUser = _user.value
        _user.value = currentUser.copy(
            name = participant.name,
            image = participant.image,
            // custom = participant.custom,
            role = participant.roles.firstOrNull() ?: ""
        )
    }
}
