/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

import androidx.compose.runtime.Stable
import io.getstream.android.video.generated.models.MuteUsersResponse
import io.getstream.log.taggedLogger
import io.getstream.result.Result
import io.getstream.video.android.core.internal.InternalStreamVideoApi
import io.getstream.video.android.core.model.AudioTrack
import io.getstream.video.android.core.model.MediaTrack
import io.getstream.video.android.core.model.NetworkQuality
import io.getstream.video.android.core.model.Reaction
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.core.model.VisibilityOnScreenState
import io.getstream.video.android.core.utils.combineStates
import io.getstream.video.android.core.utils.mapState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import stream.video.sfu.models.Participant
import stream.video.sfu.models.ParticipantSource
import stream.video.sfu.models.TrackType

/**
 * Represents the state of a participant in a call.
 *
 * * A list of participants is shared when you join a call the SFU send you the participant joined event.
 *
 */
@Stable
public data class ParticipantState(
    /** The SFU returns a session id for each participant. This session id is unique */
    var sessionId: String = "",
    /** The call actions interface for performing operations on this participant */
    private val callActions: CallActions,
    /** The current version of the user, this is the start for participant.user stateflow */
    private val initialUserId: String,
    val source: ParticipantSource = ParticipantSource.PARTICIPANT_SOURCE_WEBRTC_UNSPECIFIED,
    /** A prefix to identify tracks, internal */
    @InternalStreamVideoApi
    var trackLookupPrefix: String = "",
) {
    /** The coroutine scope for StateFlows - accessed via callActions.stateScope */
    private val scope: CoroutineScope
        get() = callActions.stateScope

    private val logger by taggedLogger("ParticipantState")

    val isLocal by lazy {
        callActions.isLocalParticipant(sessionId)
    }

    /** video track */
    internal val _videoTrack = MutableStateFlow<VideoTrack?>(null)
    val videoTrack: StateFlow<VideoTrack?> = _videoTrack

    internal val _visibleOnScreen = MutableStateFlow(VisibilityOnScreenState.UNKNOWN)
    val visibleOnScreen: StateFlow<VisibilityOnScreenState> = _visibleOnScreen

    /**
     * State that indicates whether the camera is capturing and sending video or not.
     */
    // todo: videoAvailable might be more descriptive
    internal val _videoEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val videoEnabled: StateFlow<Boolean> = _videoEnabled

    internal val _videoPaused: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /**
     * State that indicates whether the video is paused or not.
     */
    val videoPaused: StateFlow<Boolean> = _videoPaused

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

    internal val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name

    internal val _image = MutableStateFlow("")
    val image: StateFlow<String> = _image

    internal val _userId = MutableStateFlow(initialUserId)
    val userId: StateFlow<String> = _userId

    // Could also be a property on the user
    val userNameOrId: StateFlow<String> = _name.mapState { it.ifEmpty { _userId.value } }

    /**
     * When you joined the call
     */
    internal val _joinedAt: MutableStateFlow<OffsetDateTime?> = MutableStateFlow(null)
    val joinedAt: StateFlow<OffsetDateTime?> = _joinedAt

    /**
     * The audio level of the participant, single float value
     */
    internal val _audioLevel: MutableStateFlow<Float> = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel

    /**
     * The last 5 values for the audio level. This list easier to work with for some audio visualizations
     */
    internal val _audioLevels: MutableStateFlow<List<Float>> =
        MutableStateFlow(listOf(0f, 0f, 0f, 0f, 0f))
    val audioLevels: StateFlow<List<Float>> = _audioLevels

    /**
     * The video quality of the participant
     */
    internal val _networkQuality: MutableStateFlow<NetworkQuality> =
        MutableStateFlow(NetworkQuality.UnSpecified())
    val networkQuality: StateFlow<NetworkQuality> = _networkQuality

    internal val _dominantSpeaker: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val dominantSpeaker: StateFlow<Boolean> = _dominantSpeaker

    internal val _speaking: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking

    internal val _lastSpeakingAt: MutableStateFlow<OffsetDateTime?> = MutableStateFlow(null)
    val lastSpeakingAt: StateFlow<OffsetDateTime?> = _lastSpeakingAt

    internal val _reactions = MutableStateFlow<List<Reaction>>(emptyList())
    val reactions: StateFlow<List<Reaction>> = _reactions

    val video: StateFlow<Video?> = combine(
        _videoTrack,
        _videoEnabled,
        _videoPaused,
    ) { track, enabled, paused ->
        Video(
            sessionId = sessionId,
            track = track,
            enabled = enabled,
            paused = paused,
        )
    }.stateIn(scope, SharingStarted.Lazily, null)

    val audio: StateFlow<Audio?> = combineStates(_audioTrack, _audioEnabled) { track, enabled ->
        Audio(
            sessionId = sessionId,
            track = track,
            enabled = enabled,
        )
    }

    val screenSharing: StateFlow<ScreenSharing?> =
        combineStates(_screenSharingTrack, _screenSharingEnabled) { track, enabled ->
            ScreenSharing(
                sessionId = sessionId,
                track = track,
                enabled = enabled,
            )
        }

    suspend fun muteAudio(): Result<MuteUsersResponse> {
        return callActions.muteUserAudio(_userId.value)
    }

    suspend fun muteVideo(): Result<MuteUsersResponse> {
        return callActions.muteUserVideo(_userId.value)
    }

    suspend fun muteScreenshare(): Result<MuteUsersResponse> {
        return callActions.muteUserScreenShare(_userId.value)
    }

    suspend fun pin() {
        callActions.pinParticipant(this.userId.value, this.sessionId)
    }

    suspend fun unpin() {
        callActions.unpinParticipant(this.sessionId)
    }

    fun updateAudioLevel(audioLevel: Float) {
        val currentAudio = _audioLevels.value.toMutableList()
        currentAudio.removeAt(0)
        currentAudio.add(audioLevel)
        if (currentAudio[0] == 0f && currentAudio[2] == 0f) {
            currentAudio[0] = audioLevel
            currentAudio[2] = audioLevel
        }
        _audioLevels.value = currentAudio.toList()
        _audioLevel.value = audioLevel
    }

    internal val _roles = MutableStateFlow<List<String>>(emptyList())
    val roles: StateFlow<List<String>> = _roles

    fun setVideoTrack(track: VideoTrack?) {
        logger.i { "[setVideoTrack] #sfu; #track; userId: ${userId.value} track: $track" }
        _videoTrack.value = track
    }

    fun updateFromParticipantInfo(participant: Participant) {
        sessionId = participant.session_id

        val joinedAtMilli =
            participant.joined_at?.toEpochMilli() ?: OffsetDateTime.now().toEpochSecond()
        val instant = Instant.ofEpochSecond(joinedAtMilli)
        _joinedAt.value = OffsetDateTime.ofInstant(instant, ZoneOffset.UTC)

        trackLookupPrefix = participant.track_lookup_prefix
        _networkQuality.value = NetworkQuality.fromConnectionQuality(participant.connection_quality)
        _speaking.value = participant.is_speaking
        // _dominantSpeaker.value = participant.is_dominant_speaker. we ignore this and only handle the event
        updateAudioLevel(participant.audio_level)
        _audioEnabled.value = participant.published_tracks.contains(TrackType.TRACK_TYPE_AUDIO)
        _videoEnabled.value = participant.published_tracks.contains(TrackType.TRACK_TYPE_VIDEO)
        _screenSharingEnabled.value =
            participant.published_tracks.contains(TrackType.TRACK_TYPE_SCREEN_SHARE)
        _roles.value = participant.roles

        _name.value = participant.name
        _image.value = participant.image
    }

    public fun consumeReaction(reaction: Reaction) {
        val newReactions = _reactions.value.toMutableList()
        newReactions.remove(reaction)
        _reactions.value = newReactions
    }

    @Stable
    public sealed class Media(
        public open val sessionId: String,
        public open val track: MediaTrack?,
        public open val enabled: Boolean,
        public open val paused: Boolean,
        public val type: TrackType = TrackType.TRACK_TYPE_UNSPECIFIED,
    )

    @Stable
    public data class Video(
        public override val sessionId: String,
        public override val track: VideoTrack?,
        public override val enabled: Boolean,
        public override val paused: Boolean,
    ) : Media(
        sessionId = sessionId,
        track = track,
        enabled = enabled,
        paused = paused,
        type = TrackType.TRACK_TYPE_VIDEO,
    )

    @Stable
    public data class Audio(
        public override val sessionId: String,
        public override val track: AudioTrack?,
        public override val enabled: Boolean,
    ) : Media(
        sessionId = sessionId,
        track = track,
        enabled = enabled,
        paused = false,
        type = TrackType.TRACK_TYPE_AUDIO,
    )

    @Stable
    public data class ScreenSharing(
        public override val sessionId: String,
        public override val track: VideoTrack?,
        public override val enabled: Boolean,
    ) : Media(
        sessionId = sessionId,
        track = track,
        enabled = enabled,
        paused = false,
        type = TrackType.TRACK_TYPE_SCREEN_SHARE,
    )
}
