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
import io.getstream.video.android.core.events.ChangePublishQualityEvent
import io.getstream.video.android.core.events.ConnectionQualityChangeEvent
import io.getstream.video.android.core.events.DominantSpeakerChangedEvent
import io.getstream.video.android.core.events.ErrorEvent
import io.getstream.video.android.core.events.ICETrickleEvent
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.events.ParticipantJoinedEvent
import io.getstream.video.android.core.events.ParticipantLeftEvent
import io.getstream.video.android.core.events.PublisherAnswerEvent
import io.getstream.video.android.core.events.SFUConnectedEvent
import io.getstream.video.android.core.events.SFUHealthCheckEvent
import io.getstream.video.android.core.events.SubscriberOfferEvent
import io.getstream.video.android.core.events.TrackPublishedEvent
import io.getstream.video.android.core.events.TrackUnpublishedEvent
import io.getstream.video.android.core.events.VideoEvent
import io.getstream.video.android.core.model.CallUser
import io.getstream.video.android.core.model.ScreenSharingSession
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.core.utils.mapState
import io.getstream.video.android.core.utils.toUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.openapitools.client.models.*
import org.webrtc.MediaStream
import stream.video.sfu.models.Participant
import stream.video.sfu.models.TrackType
import java.util.*

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

    // participants by session id -> participant state
    private val trackPrefixToSessionIdMap = mutableMapOf<String, String>()

    private val _participants: MutableStateFlow<SortedMap<String, ParticipantState>> =
        MutableStateFlow(emptyMap<String, ParticipantState>().toSortedMap())
    public val participants: StateFlow<List<ParticipantState>> = _participants.mapState { it.values.toList() }

    // making it a property requires cleaning up the properties of a participant
    val me : StateFlow<ParticipantState?> = _participants.mapState { it.get(user.id) }

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

    private val _capabilitiesByRole: MutableStateFlow<Map<String, List<String>>> =
        MutableStateFlow(emptyMap())
    val capabilitiesByRole: StateFlow<Map<String, List<String>>> = _capabilitiesByRole

    private val _members: MutableStateFlow<List<ParticipantState>> =
        MutableStateFlow(emptyList())
    public val members: StateFlow<List<ParticipantState>> = _members



    private val _errors: MutableStateFlow<List<ErrorEvent>> =
        MutableStateFlow(emptyList())
    public val errors: StateFlow<List<ErrorEvent>> = _errors



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

        val (trackPrefix, trackType) = mediaStream.id.split(':');
        if (trackPrefixToSessionIdMap[trackPrefix].isNullOrEmpty()) {
            logger.w { "[addStream] skipping unrecognized trackPrefix ${trackPrefix}" }
            return
        }

        val sessionId = trackPrefixToSessionIdMap[trackPrefix]!!
        val participant = getParticipant(sessionId=sessionId)!!

        val track = replaceTrackIfNeeded(mediaStream, participant.videoTrack?.streamId)

        if (track != null) {
            logger.d { "[addStream] updating users with track $track" }
            track.video.setEnabled(true)

            val streamId = mediaStream.id
            val videoTrack =
                if (TrackType.TRACK_TYPE_VIDEO.name in streamId) track else participant.videoTrack
            val screenShareTrack =
                if (TrackType.TRACK_TYPE_SCREEN_SHARE.name in streamId) track else participant.screenSharingTrack

            val tracks = participant.publishedTracks.toMutableSet()

            if (videoTrack != null) {
                tracks.add(TrackType.TRACK_TYPE_VIDEO)
            }

            if (screenShareTrack != null) {
                tracks.add(TrackType.TRACK_TYPE_SCREEN_SHARE)
            }

            val updatedParticipant = participant.copy(
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
            screenSharingSession?.let { _screenSharingSession.value = it }
            val updatedParticipantMap = _participants.value.toSortedMap()
            updatedParticipantMap[sessionId] = updatedParticipant
            _participants.value = updatedParticipantMap

        }

    }

    public fun updateParticipantTrackSize(
        sessionId: String,
        measuredWidth: Int,
        measuredHeight: Int
    ) {
        logger.v { "[updateParticipantTrackSize] SessionId: $sessionId, width:$measuredWidth, height:$measuredHeight" }
        val participant = getParticipant(sessionId)
        participant?.let {
            val updated = participant.copy(videoTrackSize = measuredWidth to measuredHeight)
            updateParticipant(updated)
        }
    }

    fun handleEvent(event: VideoEvent) {
        logger.d { "Updating call state with event $event" }
        when (event) {
            is BlockedUserEvent -> TODO()
            is CallAcceptedEvent -> {
//                val participant = getOrCreateParticipant(event.sessionId, event.sentByUserId)
//                participant._acceptedAt.value = Date()
            }

            is CallRejectedEvent -> {
                // TODO: we don't have a RTC session yet. So how do we do this?
                // TODO: these are not participants. Keep track on the member instead of participant
//                val participant = getOrCreateParticipant(event.sessionId, event.user.id, event.user)
//                participant._rejectedAt.value = Date()
            }

            is CallEndedEvent -> {
                _endedAt.value = Date()
                _endedByUser.value = event.user?.toUser()
            }

            is CallMemberUpdatedEvent -> {
                event.members.forEach { entry ->
                    getOrCreateMember(entry)
                }
            }

            is CallMemberRemovedEvent -> TODO()

            is CallCreatedEvent -> {
                // this is handled by the client
            }

            is CallUpdatedEvent -> {
                updateFromEvent(event)
            }

            is UpdatedCallPermissionsEvent -> {
                _ownCapabilities.value = event.ownCapabilities
            }

            is WSConnectedEvent -> {
                // this is handled by the client
            }

            is CustomVideoEvent -> {
                // safe to ignore, app level custom event
            }

            is HealthCheckEvent -> TODO()
            is PermissionRequestEvent -> TODO()
            is CallRecordingStartedEvent -> {
                _recording.value = true
            }

            is CallRecordingStoppedEvent -> {
                _recording.value = false
            }

            is UnblockedUserEvent -> TODO()

            is AudioLevelChangedEvent -> {
                event.levels.forEach { entry ->
                    val participant = getOrCreateParticipant(entry.key, entry.value.userId)
                    participant._speaking.value = entry.value.isSpeaking
                    participant._audioLevel.value = entry.value.audioLevel
                }
            }

            is DominantSpeakerChangedEvent -> {
                _dominantSpeaker.value = getOrCreateParticipant(event.sessionId, event.userId)
            }

            is ConnectionQualityChangeEvent -> {
                event.updates.forEach { entry ->
                    val participant = getOrCreateParticipant(entry.session_id, entry.user_id)
                    participant._connectionQuality.value = entry.connection_quality
                }
            }

            is ChangePublishQualityEvent -> {
                call.activeSession!!.handleEvent(event)
            }

            is ErrorEvent -> {
                _errors.value = errors.value + event
            }
            SFUHealthCheckEvent -> {
                // we don't do anything with this
            }

            is ICETrickleEvent -> {
                // handled by ActiveSFUSession
            }
            is JoinCallResponseEvent -> {
                // time to update call state based on the join response
                updateFromJoinResponse(event)
            }

            is ParticipantJoinedEvent -> {
                getOrCreateParticipant(event.participant)
            }

            is ParticipantLeftEvent -> {
                removeParticipant(event.participant.session_id)
            }

            is PublisherAnswerEvent -> TODO()
            is SubscriberOfferEvent -> TODO()
            is TrackPublishedEvent -> TODO()
            is TrackUnpublishedEvent -> TODO()
            is SFUConnectedEvent -> {
                _connection.value = ConnectionState.Connected
            }
            is CallMemberAddedEvent -> TODO()
            is CallMemberUpdatedPermissionEvent -> TODO()
            is CallReactionEvent -> TODO()
            is WSConnectedEvent -> TODO()
        }
    }




    private fun updateFromJoinResponse(event: JoinCallResponseEvent) {
        event.callState.participants.forEach {
            getOrCreateParticipant(it)
        }
    }


    private fun removeParticipant(sessionId: String) {
        val new = _participants.value.toSortedMap()
        new.remove(sessionId)
        _participants.value = new

    }

    private fun getOrCreateParticipant(participant: Participant): ParticipantState {
        // get or create the participant and update them

        val participantState = getOrCreateParticipant(participant.session_id, participant.user_id)
        trackPrefixToSessionIdMap[participant.track_lookup_prefix] = participant.session_id
        participantState.updateFromParticipantInfo(participant)

        participantState._speaking.value = participant.is_speaking
        return participantState
    }

    fun getOrCreateParticipant(sessionId: String, userId: String, user: User? = null): ParticipantState {
        val participantMap = _participants.value
        return if (participantMap.contains(sessionId)) {
            participantMap[sessionId]!!
        } else {
            val participant = ParticipantState(sessionId=sessionId, call=call, initialUser=user ?: User(userId))
            participantMap[sessionId] = participant
            participant
        }
    }

    private fun getOrCreateMember(entry: MemberResponse): MemberState {
        val member = getOrCreateMember(entry.userId)
        // TODO: update fields
        return member
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

    fun requireParticipant(sessionId: String): ParticipantState {
        // TODO: after development lets just log instead throwing an error
        return getParticipant(sessionId) ?: throw IllegalStateException("No participant with sessionId $sessionId")
    }

    fun getParticipant(sessionId: String): ParticipantState? {
        return _participants.value[sessionId]
    }

    fun updateParticipant(participant: ParticipantState) {
        val new = _participants.value.toSortedMap()
        new[participant.sessionId] = participant
        _participants.value = new
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
            _ownCapabilities.value = event.call.ownCapabilities
            // update the capabilities by role
            _capabilitiesByRole.value = event.capabilitiesByRole
            // update call info fields
        }
    }

    // TODO: SFU Connection



    /** participants who are currently speaking */
    public val activeSpeakers = _participants.mapState { it.values.filter { participant -> participant.speaking.value } }


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

    public val sortedParticipants = _participants.mapState { it.values.sortedBy{
        // TODO: implement actual sorting
        val score = 1
        score
    } }

    internal fun disconnect() {
        logger.i { "[disconnect] #sfu; no args" }
        // audioHandler.stop()
        val participants = _participants.value
        _participants.value = emptyMap<String, ParticipantState>().toSortedMap()

        participants.values.forEach {
            val track = it.videoTrack
            it.videoTrack = null
            track?.video?.dispose()
        }
    }

    // local video track update...
    internal fun updateLocalVideoTrack(localVideoTrack: org.webrtc.VideoTrack) {

        logger.d { "[updateLocalVideoTrack] #sfu; localVideoTrack: $localVideoTrack, localParticipant: $me.value" }
        val videoTrack = VideoTrack(
            video = localVideoTrack,
            streamId = "${call.client.userId}:${localVideoTrack.id()}"
        )

        // start by updating the local participant state (specialized version of Participant State)
        val localParticipant = me.value ?: throw IllegalStateException("Local participant is null")

        val updatedParticipant = localParticipant.copy(
            videoTrack = videoTrack
        )
        updateParticipant(updatedParticipant)

        logger.d { "[updateLocalVideoTrack] #sfu; localParticipant: $updatedParticipant, callParticipants: ${_participants.value}" }
    }

    // TODO: move to active SFU session
    internal fun updatePublishState(
        userId: String,
        sessionId: String,
        trackType: TrackType,
        isEnabled: Boolean
    ) {

        logger.d { "[updateMuteState] #sfu; userId: $userId, sessionId: $sessionId, isEnabled: $isEnabled" }

        val participant = requireParticipant(sessionId)

        val videoTrackSize = if (trackType == TrackType.TRACK_TYPE_VIDEO) {
            if (isEnabled) {
                participant.videoTrackSize
            } else {
                0 to 0
            }
        } else {
            participant.videoTrackSize
        }

        val screenShareTrack = if (trackType == TrackType.TRACK_TYPE_SCREEN_SHARE) {
            if (isEnabled) {
                participant.screenSharingTrack
            } else {
                null
            }
        } else {
            participant.screenSharingTrack
        }

        val updated = participant.copy(
            videoTrackSize = videoTrackSize,
            screenSharingTrack = screenShareTrack,
            publishedTracks = if (isEnabled) participant.publishedTracks + trackType else participant.publishedTracks - trackType
        )

        updateParticipant(updated)

        if (trackType == TrackType.TRACK_TYPE_SCREEN_SHARE && !isEnabled) {
            _screenSharingSession.value = null
        }
    }

    // TODO : move to media manager and listen for changes to update local state

    public fun setCameraEnabled(isEnabled: Boolean) {
        logger.d { "[setCameraEnabled] #sfu; isEnabled: $isEnabled" }
        val localParticipant = me.value ?: return
        val track = TrackType.TRACK_TYPE_VIDEO
        val tracks = localParticipant.publishedTracks

        val newTracks = if (isEnabled) tracks + track else tracks - track
        val updatedLocal = localParticipant.copy(
            publishedTracks = newTracks
        )
        updateParticipant(updatedLocal)
    }

    public fun setMicrophoneEnabled(isEnabled: Boolean) {
        logger.d { "[setMicrophoneEnabled] #sfu; isEnabled: $isEnabled" }
        val localParticipant = me.value ?: return
        val track = TrackType.TRACK_TYPE_AUDIO
        val tracks = localParticipant.publishedTracks

        val newTracks = if (isEnabled) tracks + track else tracks - track
        val updatedLocal = localParticipant.copy(publishedTracks = newTracks)
        updateParticipant(updatedLocal)
    }
}
