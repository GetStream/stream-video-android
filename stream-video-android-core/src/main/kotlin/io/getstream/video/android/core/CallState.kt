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

import io.getstream.log.taggedLogger
import io.getstream.video.android.core.call.utils.stringify
import io.getstream.video.android.core.events.AudioLevelChangedEvent
import io.getstream.video.android.core.events.BlockedUserEvent
import io.getstream.video.android.core.events.CallAcceptedEvent
import io.getstream.video.android.core.events.CallCancelledEvent
import io.getstream.video.android.core.events.CallCreatedEvent
import io.getstream.video.android.core.events.CallEndedEvent
import io.getstream.video.android.core.events.CallMembersDeletedEvent
import io.getstream.video.android.core.events.CallMembersUpdatedEvent
import io.getstream.video.android.core.events.CallRejectedEvent
import io.getstream.video.android.core.events.CallUpdatedEvent
import io.getstream.video.android.core.events.ChangePublishQualityEvent
import io.getstream.video.android.core.events.ConnectedEvent
import io.getstream.video.android.core.events.ConnectionQualityChangeEvent
import io.getstream.video.android.core.events.CoordinatorHealthCheckEvent
import io.getstream.video.android.core.events.CustomEvent
import io.getstream.video.android.core.events.DominantSpeakerChangedEvent
import io.getstream.video.android.core.events.ErrorEvent
import io.getstream.video.android.core.events.ICETrickleEvent
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.events.ParticipantJoinedEvent
import io.getstream.video.android.core.events.ParticipantLeftEvent
import io.getstream.video.android.core.events.PermissionRequestEvent
import io.getstream.video.android.core.events.PublisherAnswerEvent
import io.getstream.video.android.core.events.RecordingStartedEvent
import io.getstream.video.android.core.events.RecordingStoppedEvent
import io.getstream.video.android.core.events.SFUConnectedEvent
import io.getstream.video.android.core.events.SFUHealthCheckEvent
import io.getstream.video.android.core.events.SubscriberOfferEvent
import io.getstream.video.android.core.events.TrackPublishedEvent
import io.getstream.video.android.core.events.TrackUnpublishedEvent
import io.getstream.video.android.core.events.UnblockedUserEvent
import io.getstream.video.android.core.events.UnknownEvent
import io.getstream.video.android.core.events.UpdatedCallPermissionsEvent
import io.getstream.video.android.core.events.VideoEvent
import io.getstream.video.android.core.events.VideoQualityChangedEvent
import io.getstream.video.android.core.model.CallUser
import io.getstream.video.android.core.model.ScreenSharingSession
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.core.utils.mapState
import io.getstream.video.android.core.utils.updateValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.openapitools.client.models.CallSettingsResponse
import org.openapitools.client.models.OwnCapability
import org.webrtc.MediaStream
import stream.video.sfu.models.Participant
import stream.video.sfu.models.TrackType
import java.util.Date

/**
 *
 */
public class CallState(val call: Call, user: User) {
    private val logger by taggedLogger("CallState")

    private val _ringingState: MutableStateFlow<RingingState?> = MutableStateFlow(null)
    public val ringingState: MutableStateFlow<RingingState?> = _ringingState

    private val _settings: MutableStateFlow<CallSettingsResponse?> = MutableStateFlow(null)
    public val settings: MutableStateFlow<CallSettingsResponse?> = _settings

    private val memberMap: MutableMap<String, MemberState> = mutableMapOf()
    private val _recording: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording

    private val _screenSharingSession: MutableStateFlow<ScreenSharingSession?> =
        MutableStateFlow(null)

    public val screenSharingSession: StateFlow<ScreenSharingSession?> = _screenSharingSession

    public val isScreenSharing: StateFlow<Boolean> = _screenSharingSession.mapState { it != null }

    private val _screenSharingTrack: MutableStateFlow<VideoTrack?> = MutableStateFlow(null)

    private val _ownCapabilities: MutableStateFlow<List<OwnCapability>> =
        MutableStateFlow(emptyList())
    public val ownCapabilities: StateFlow<List<OwnCapability>> = _ownCapabilities

    /**
     * connectionState shows if we've established a connection with the coordinator
     */
    private val _connection: MutableStateFlow<ConnectionState> = MutableStateFlow(
        ConnectionState.PreConnect
    )
    public val connection: StateFlow<ConnectionState> = _connection

    private val _endedAt: MutableStateFlow<Date?> = MutableStateFlow(null)
    val endedAt: StateFlow<Date?> = _endedAt
    private val _endedByUser: MutableStateFlow<User?> = MutableStateFlow(null)
    val endedByUser: StateFlow<User?> = _endedByUser

    private val participantMap = mutableMapOf<String, ParticipantState>()

    private val _capabilitiesByRole: MutableStateFlow<Map<String, List<String>>> =
        MutableStateFlow(emptyMap())
    val capabilitiesByRole: StateFlow<Map<String, List<String>>> = _capabilitiesByRole

    private val _members: MutableStateFlow<List<ParticipantState>> =
        MutableStateFlow(emptyList())
    public val members: StateFlow<List<ParticipantState>> = _members

    // TODO: does this need to be a stateflow, or can it be a property?
    // making it a property requires cleaning up the properties of a participant
    val _me = MutableStateFlow(ParticipantState(call, user))
    val me: StateFlow<ParticipantState> = _me

    private fun replaceTrackIfNeeded(mediaStream: MediaStream, streamId: String?): VideoTrack? {

        return if (streamId == null || streamId != mediaStream.id) {
            mediaStream.videoTracks?.firstOrNull()?.let { track ->
                VideoTrack(
                    streamId = mediaStream.id,
                    video = track
                )
            }
        } else {
            null
        }
    }

    internal fun addStream(mediaStream: MediaStream) {
        val streamsToProcess: MutableList<MediaStream> = mutableListOf()
        val participants = _participants.value
        if (participants.isEmpty()) {
            streamsToProcess.add(mediaStream)
            return
        }

        logger.i { "[] #sfu; mediaStream: $mediaStream" }
        if (mediaStream.audioTracks.isNotEmpty()) {
            mediaStream.audioTracks.forEach { track ->
                logger.v { "[addStream] #sfu; audioTrack: ${track.stringify()}" }
                track.setEnabled(true)
            }
        }

        if (mediaStream.videoTracks.isEmpty()) {
            return
        }

        var screenSharingSession: ScreenSharingSession? = null

        val updatedList = _participants.value.updateValue(
            predicate = { it.trackLookupPrefix in mediaStream.id },
            transformer = {
                val track = replaceTrackIfNeeded(mediaStream, it.videoTrack?.streamId)

                if (track != null) {
                    logger.d { "[addStream] updating users with track $track" }
                    track.video.setEnabled(true)

                    val streamId = mediaStream.id
                    val videoTrack =
                        if (TrackType.TRACK_TYPE_VIDEO.name in streamId) track else it.videoTrack
                    val screenShareTrack =
                        if (TrackType.TRACK_TYPE_SCREEN_SHARE.name in streamId) track else it.screenSharingTrack

                    val tracks = it.publishedTracks.toMutableSet()

                    if (videoTrack != null) {
                        tracks.add(TrackType.TRACK_TYPE_VIDEO)
                    }

                    if (screenShareTrack != null) {
                        tracks.add(TrackType.TRACK_TYPE_SCREEN_SHARE)
                    }

                    val updatedParticipant = it.copy(
                        videoTrack = videoTrack,
                        screenSharingTrack = screenShareTrack,
                        publishedTracks = tracks
                    )

                    if (screenShareTrack != null) {
                        screenSharingSession =
                            ScreenSharingSession(
                                track = screenShareTrack,
                                participant = updatedParticipant
                            )
                    }

                    updatedParticipant
                } else {
                    it
                }
            }
        )

        logger.d { "[addStream] updated list $updatedList" }
        screenSharingSession?.let { _screenSharingSession.value = it }
        _participants.value = updatedList
    }

    public fun updateParticipantTrackSize(
        sessionId: String,
        measuredWidth: Int,
        measuredHeight: Int
    ) {
        logger.v { "[updateParticipantTrackSize] SessionId: $sessionId, width:$measuredWidth, height:$measuredHeight" }
        val oldState = _participants.value

        // TODO: should use a Map instead of a list for participants and sessionIds
        val newState = oldState.updateValue(
            predicate = { it.sessionId == sessionId },
            transformer = { it.copy(videoTrackSize = measuredWidth to measuredHeight) }
        )

        _participants.value = newState
    }

    fun handleEvent(event: VideoEvent) {
        logger.d { "Updating call state with event $event" }
        when (event) {
            is BlockedUserEvent -> TODO()
            is CallAcceptedEvent -> {
                val participant = getOrCreateParticipant(event.sentByUserId)
                participant._acceptedAt.value = Date()
            }

            is CallRejectedEvent -> {
                val participant = getOrCreateParticipant(event.user)
                participant._rejectedAt.value = Date()
            }

            is CallCancelledEvent -> TODO()
            is CallEndedEvent -> {
                _endedAt.value = Date()
                _endedByUser.value = event.endedByUser
            }

            is CallMembersUpdatedEvent -> {
                event.details.members.forEach { entry ->
                    getOrCreateMember(entry.value)
                }
            }

            is CallMembersDeletedEvent -> TODO()

            is CallCreatedEvent -> {
                // this is handled by the client
            }

            is CallUpdatedEvent -> {
                updateFromEvent(event)
            }

            is UpdatedCallPermissionsEvent -> {
                _ownCapabilities.value = event.ownCapabilities
            }

            is ConnectedEvent -> {
                // this is handled by the client
            }

            is CustomEvent -> {
                // safe to ignore, app level custom event
            }

            is CoordinatorHealthCheckEvent -> TODO()
            is PermissionRequestEvent -> TODO()
            is RecordingStartedEvent -> {
                _recording.value = true
            }

            is RecordingStoppedEvent -> {
                _recording.value = false
            }

            is UnblockedUserEvent -> TODO()
            UnknownEvent -> TODO()

            is AudioLevelChangedEvent -> {
                event.levels.forEach { entry ->
                    val participant = getOrCreateParticipant(entry.key)
                    participant._speaking.value = entry.value.isSpeaking
                    participant._audioLevel.value = entry.value.audioLevel
                }
            }

            is DominantSpeakerChangedEvent -> {
                _dominantSpeaker.value = getOrCreateParticipant(event.userId)
            }

            is ConnectionQualityChangeEvent -> {
                event.updates.forEach { entry ->
                    val participant = getOrCreateParticipant(entry.user_id)
                    participant._connectionQuality.value = entry.connection_quality
                }
            }

            is ChangePublishQualityEvent -> {
                call.activeSession!!.handleEvent(event)
            }

            is ErrorEvent -> TODO()
            SFUHealthCheckEvent -> {
                // we don't do anythign with this
            }

            is ICETrickleEvent -> {
                call.activeSession!!.handleEvent(event)
            }

            is JoinCallResponseEvent -> TODO()
            is ParticipantJoinedEvent -> {
                getOrCreateParticipant(event.participant)
            }

            is ParticipantLeftEvent -> {
                removeParticipant(event.participant.user_id)
            }

            is PublisherAnswerEvent -> TODO()
            is SubscriberOfferEvent -> TODO()
            is TrackPublishedEvent -> TODO()
            is TrackUnpublishedEvent -> TODO()
            is VideoQualityChangedEvent -> TODO()
            is SFUConnectedEvent -> {
                _connection.value = ConnectionState.Connected
            }
        }
    }

    private fun removeParticipant(userId: String) {
        participantMap.remove(userId)
        // TODO: connect map and participant list nicely
    }

    private fun getOrCreateParticipant(participant: Participant): ParticipantState {
        // TODO: update some fields
        val participantState = getOrCreateParticipant(participant.user_id)
        participantState.updateFromParticipantInfo(participant)

        participantState._speaking.value = participant.is_speaking
        return participantState
    }

    private fun getOrCreateParticipant(user: User): ParticipantState {
        // TODO: maybe update some fields
        return getOrCreateParticipant(user.id)
    }

    fun getOrCreateParticipant(userId: String): ParticipantState {
        return if (participantMap.contains(userId)) {
            participantMap[userId]!!
        } else {
            val participant = ParticipantState(call, User(id = userId))
            participantMap[userId] = participant
            participant
        }
    }

    private fun getOrCreateMember(callUser: CallUser): MemberState {
        // TODO: update fields :)
        return getOrCreateMember(callUser.id)
    }

    private fun getOrCreateMember(userId: String): MemberState {
        return if (memberMap.contains(userId)) {
            memberMap[userId]!!
        } else {
            val member = MemberState(User(id = userId))
            memberMap[userId] = member
            member
        }
    }

    fun getParticipant(userId: String): ParticipantState? {
        return participantMap[userId]
    }

    fun updateFromEvent(event: VideoEvent) {

        if (event is CallCreatedEvent) {
            // TODO fix this
            // update the own capabilities
//            me._ownCapabilities.value=event.call.ownCapabilities
//            // update the capabilities by role
//            _capabilitiesByRole.value = event.capabilitiesByRole
            // update call info fields
        } else if (event is CallUpdatedEvent) {
            // update the own capabilities
            _ownCapabilities.value = event.ownCapabilities
            // update the capabilities by role
            _capabilitiesByRole.value = event.capabilitiesByRole
            // update call info fields
        }
    }

    // TODO: SFU Connection

    private val _participants: MutableStateFlow<List<ParticipantState>> =
        MutableStateFlow(emptyList())
    public val participants: StateFlow<List<ParticipantState>> = _participants

    /** participants who are currently speaking */
    public val activeSpeakers =
        _participants.mapState { it.filter { participant -> participant.speaking.value } }

    private val _dominantSpeaker: MutableStateFlow<ParticipantState?> =
        MutableStateFlow(null)
    public val dominantSpeaker: StateFlow<ParticipantState?> = _dominantSpeaker

    /**
     * Sorted participants gives you the list of participants sorted by
     * * anyone who is pinned
     * * dominant speaker
     * * last speaking at
     * * all other video participants by when they joined
     * * audio only participants by when they joined
     *
     */
    public val sortedParticipants = _participants.mapState {
        it.sortedBy {
            // TODO: implement actual sorting
            val score = 1
            score
        }
    }

    internal fun disconnect() {
        logger.i { "[disconnect] #sfu; no args" }
        // audioHandler.stop()
        val participants = _participants.value
        _participants.value = emptyList()

        participants.forEach {
            val track = it.videoTrack
            it.videoTrack = null
            track?.video?.dispose()
        }
    }

    // local video track update...
    internal fun updateLocalVideoTrack(localVideoTrack: org.webrtc.VideoTrack) {

        logger.d { "[updateLocalVideoTrack] #sfu; localVideoTrack: $localVideoTrack, localParticipant: $_me.value" }
        val videoTrack = VideoTrack(
            video = localVideoTrack,
            streamId = "${call.client.userId}:${localVideoTrack.id()}"
        )

        // start by updating the local participant state (specialized version of Participant State)
        val localParticipant = _me.value
        val updatedParticipant = localParticipant.copy(
            videoTrack = videoTrack
        )
        _me.value = updatedParticipant

        // next update the list of participants
        val allParticipants = _participants.value
        val updated = allParticipants.updateValue(
            predicate = { it.user.value.id == call.client.userId },
            transformer = {
                it.copy(videoTrack = videoTrack)
            }
        )
        _participants.value = updated

        logger.d { "[updateLocalVideoTrack] #sfu; localParticipant: $updatedParticipant, callParticipants: ${_participants.value}" }
    }

    // TODO: move to active SFU session
    internal fun updateMuteState(
        userId: String,
        sessionId: String,
        trackType: TrackType,
        isEnabled: Boolean
    ) {

        logger.d { "[updateMuteState] #sfu; userId: $userId, sessionId: $sessionId, isEnabled: $isEnabled" }
        val currentParticipants = _participants.value

        val updatedList = currentParticipants.updateValue(
            predicate = { it.sessionId == sessionId },
            transformer = {
                val videoTrackSize = if (trackType == TrackType.TRACK_TYPE_VIDEO) {
                    if (isEnabled) {
                        it.videoTrackSize
                    } else {
                        0 to 0
                    }
                } else {
                    it.videoTrackSize
                }

                val screenShareTrack = if (trackType == TrackType.TRACK_TYPE_SCREEN_SHARE) {
                    if (isEnabled) {
                        it.screenSharingTrack
                    } else {
                        null
                    }
                } else {
                    it.screenSharingTrack
                }

                it.copy(
                    videoTrackSize = videoTrackSize,
                    screenSharingTrack = screenShareTrack,
                    publishedTracks = if (isEnabled) it.publishedTracks + trackType else it.publishedTracks - trackType
                )
            }
        )

        _participants.value = updatedList
        logger.d { "[updateMuteState] #sfu; updatedList: $updatedList" }

        if (trackType == TrackType.TRACK_TYPE_SCREEN_SHARE && !isEnabled) {
            _screenSharingSession.value = null
        }
    }

    // TODO : move to media manager and listen for changes to update local state

    public fun setCameraEnabled(isEnabled: Boolean) {
        logger.d { "[setCameraEnabled] #sfu; isEnabled: $isEnabled" }
        val localParticipant = _me.value ?: return
        val track = TrackType.TRACK_TYPE_VIDEO
        val tracks = localParticipant.publishedTracks

        val newTracks = if (isEnabled) tracks + track else tracks - track
        val updatedLocal = localParticipant.copy(
            publishedTracks = newTracks
        )
        _me.value = updatedLocal

        val updatedList = _participants.value.updateValue(
            predicate = { it.user.value.id == call.client.userId },
            transformer = { it.copy(publishedTracks = newTracks) }
        )

        _participants.value = updatedList
    }

    public fun setMicrophoneEnabled(isEnabled: Boolean) {
        logger.d { "[setMicrophoneEnabled] #sfu; isEnabled: $isEnabled" }
        val localParticipant = _me.value ?: return
        val track = TrackType.TRACK_TYPE_AUDIO
        val tracks = localParticipant.publishedTracks

        val newTracks = if (isEnabled) tracks + track else tracks - track
        val updatedLocal = localParticipant.copy(publishedTracks = newTracks)
        _me.value = updatedLocal

        val updatedList = _participants.value.updateValue(
            predicate = { it.user.value.id == call.client.userId },
            transformer = { it.copy(publishedTracks = newTracks) }
        )

        _participants.value = updatedList
    }
}
