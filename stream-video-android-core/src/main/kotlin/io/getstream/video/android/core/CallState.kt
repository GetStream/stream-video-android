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
import io.getstream.result.Result
import io.getstream.video.android.core.events.AudioLevelChangedEvent
import io.getstream.video.android.core.events.ChangePublishQualityEvent
import io.getstream.video.android.core.events.ConnectionQualityChangeEvent
import io.getstream.video.android.core.events.DominantSpeakerChangedEvent
import io.getstream.video.android.core.events.ErrorEvent
import io.getstream.video.android.core.events.ICETrickleEvent
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.events.ParticipantJoinedEvent
import io.getstream.video.android.core.events.ParticipantLeftEvent
import io.getstream.video.android.core.events.SFUConnectedEvent
import io.getstream.video.android.core.events.SFUHealthCheckEvent
import io.getstream.video.android.core.events.SubscriberOfferEvent
import io.getstream.video.android.core.events.TrackPublishedEvent
import io.getstream.video.android.core.events.TrackUnpublishedEvent
import io.getstream.video.android.core.model.*
import io.getstream.video.android.core.utils.mapState
import io.getstream.video.android.core.utils.toUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.openapitools.client.models.*
import org.webrtc.MediaStream
import org.openapitools.client.models.CallSettingsResponse
import org.openapitools.client.models.OwnCapability
import org.threeten.bp.Clock
import org.threeten.bp.OffsetDateTime
import stream.video.sfu.models.Participant
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

    private val _participants: MutableStateFlow<SortedMap<String, ParticipantState>> =
        MutableStateFlow(emptyMap<String, ParticipantState>().toSortedMap())
    public val participants: StateFlow<List<ParticipantState>> =
        _participants.mapState { it.values.toList() }

    /** participants who are currently speaking */
    public val activeSpeakers =
        _participants.mapState { it.values.filter { participant -> participant.speaking.value } }

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
        it.values.sortedBy {
            // TODO: implement actual sorting
            val score = 1
            score
        }
    }

    // making it a property requires cleaning up the properties of a participant
    val me: StateFlow<ParticipantState?> = _participants.mapState { it.get(user.id) }

    // TODO: implement me properly
    public val screenSharingSession: StateFlow<ScreenSharingSession?> = _screenSharingSession

    public val isScreenSharing: StateFlow<Boolean> = _screenSharingSession.mapState { it != null }

    private val _screenSharingTrack: MutableStateFlow<TrackWrapper?> = MutableStateFlow(null)


    // TODO: maybe this should just be a list of string, seems more forward compatible
    private val _ownCapabilities: MutableStateFlow<List<OwnCapability>> =
        MutableStateFlow(emptyList())
    public val ownCapabilities: StateFlow<List<OwnCapability>> = _ownCapabilities


    public fun hasPermission(permission: String): StateFlow<Boolean> {
        val flow = _ownCapabilities.mapState { it.map { it.toString() }.contains(permission) }
        // TODO: store this in a map so we don't have to create a new flow every time
        return flow
    }


    /**
     * connection shows if we've established a connection with the SFU
     */
    private val _connection: MutableStateFlow<ConnectionState> = MutableStateFlow(
        ConnectionState.PreConnect
    )
    public val connection: StateFlow<ConnectionState> = _connection

    private val _endedAt: MutableStateFlow<OffsetDateTime?> = MutableStateFlow(null)
    val endedAt: StateFlow<OffsetDateTime?> = _endedAt
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
                _endedAt.value = OffsetDateTime.now(Clock.systemUTC())
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

            is ConnectedEvent -> {
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
                call.session!!.handleEvent(event)
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

            is SubscriberOfferEvent -> TODO()
            is TrackPublishedEvent -> TODO()
            is TrackUnpublishedEvent -> TODO()
            is SFUConnectedEvent -> {
                _connection.value = ConnectionState.Connected
            }
            is CallMemberAddedEvent -> TODO()
            is CallMemberUpdatedPermissionEvent -> TODO()
            is CallReactionEvent -> TODO()
            is ConnectedEvent -> TODO()
        }
    }

    private fun updateFromJoinResponse(event: JoinCallResponseEvent) {
        // creates the participants
        val participantStates = event.callState.participants.map {
            getOrCreateParticipant(it)
        }
        // TODO: update more fields
        upsertParticipants(participantStates)
    }

    private fun removeParticipant(sessionId: String) {
        val new = _participants.value.toSortedMap()
        new.remove(sessionId)
        _participants.value = new
    }

    private fun upsertParticipants(participants: List<ParticipantState>) {
        val new = _participants.value.toSortedMap()
        participants.forEach {
            new[it.sessionId] = it
        }
        _participants.value = new
    }

    private fun getOrCreateParticipants(participants: List<Participant>): List<ParticipantState> {
        // get or create the participant and update them
        val participantStates = participants.map {
            val participantState = getOrCreateParticipant(it.session_id, it.user_id)
            participantState.updateFromParticipantInfo(it)
            participantState
        }

        upsertParticipants(participantStates)
        return participantStates
    }

    private fun getOrCreateParticipant(participant: Participant): ParticipantState {
        // get or create the participant and update them

        val participantState = getOrCreateParticipant(participant.session_id, participant.user_id)
        participantState.updateFromParticipantInfo(participant)

        upsertParticipants(listOf(participantState))

        return participantState
    }

    fun getOrCreateParticipant(
        sessionId: String,
        userId: String,
        user: User? = null,
        updateFlow: Boolean = false
    ): ParticipantState {
        val participantMap = _participants.value.toSortedMap()
        val participantState = if (participantMap.contains(sessionId)) {
            participantMap[sessionId]!!
        } else {
            ParticipantState(sessionId = sessionId, call = call, initialUser = user ?: User(userId))
        }
        if (updateFlow) {
            upsertParticipants(listOf(participantState))
        }
        return participantState
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
        return getParticipant(sessionId)
            ?: throw IllegalStateException("No participant with sessionId $sessionId")
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

    internal fun disconnect() {
        logger.i { "[disconnect] #sfu; no args" }
        // audioHandler.stop()
        val participants = _participants.value
        _participants.value = emptyMap<String, ParticipantState>().toSortedMap()

//        participants.values.forEach {
//            val track = it.videoTrackWrapper
//            it.videoTrackWrapper = null
//            track?.video?.dispose()
//        }

    }

    private val _backstage: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val backstage: StateFlow<Boolean> = _backstage

    private val _broadcasting: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val broadcasting: StateFlow<Boolean> = _broadcasting

    private val _transcribing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val transcribing: StateFlow<Boolean> = _transcribing

    private val _startsAt: MutableStateFlow<OffsetDateTime?> = MutableStateFlow(null)
    val startsAt: StateFlow<OffsetDateTime?> = _startsAt

    private val _updatedAt: MutableStateFlow<OffsetDateTime?> = MutableStateFlow(null)
    val updatedAt: StateFlow<OffsetDateTime?> = _updatedAt

    private val _createdAt: MutableStateFlow<OffsetDateTime?> = MutableStateFlow(null)
    val createdAt: StateFlow<OffsetDateTime?> = _createdAt

    private val _blockedUserIds: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    val blockedUserIds: StateFlow<List<String>> = _blockedUserIds

    private val _custom: MutableStateFlow<Map<String,Any>> = MutableStateFlow(emptyMap())
    val custom: StateFlow<Map<String,Any>> = _custom

    private val _team: MutableStateFlow<String?> = MutableStateFlow(null)
    val team: StateFlow<String?> = _team

    private val _createdBy: MutableStateFlow<User?> = MutableStateFlow(null)
    val createdBy: StateFlow<User?> = _createdBy

    private val _ingress: MutableStateFlow<CallIngressResponse?> = MutableStateFlow(null)
    val ingress: StateFlow<CallIngressResponse?> = _ingress

    fun updateFromResponse(response: CallResponse) {
        _backstage.value = response.backstage
        _blockedUserIds.value = response.blockedUserIds
        _broadcasting.value = response.broadcasting
        _createdAt.value = response.createdAt
        _updatedAt.value = response.updatedAt
        _endedAt.value = response.endedAt
        _startsAt.value = response.startsAt
        _createdBy.value = response.createdBy.toUser()
        _custom.value = response.custom
        _ingress.value = response.ingress
        _ownCapabilities.value = response.ownCapabilities
        _recording.value = response.recording
        _settings.value = response.settings
        _transcribing.value = response.transcribing
        _team.value = response.team

    }

    fun updateFromResponse(response: GetOrCreateCallResponse) {
        val members = response.members
        val callResponse = response.call
        updateFromResponse(callResponse)
    }

    fun updateFromResponse(response: UpdateCallResponse) {
        val callResponse = response.call
        updateFromResponse(callResponse)
    }

    /**
     * @see MemberResponse
     * @see CallUser
     */
    fun updateFromResponse(response: CallData) {
        // note that the member response is different
        // @see MemberResponse





    }

    fun updateFromResponse(response: GetCallResponse) {
        updateFromResponse(response.call)
    }
}
