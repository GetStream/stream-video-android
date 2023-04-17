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
import io.getstream.video.android.core.model.ScreenSharingSession
import io.getstream.video.android.core.model.TrackWrapper
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.utils.mapState
import io.getstream.video.android.core.utils.toUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.openapitools.client.models.BlockedUserEvent
import org.openapitools.client.models.CallAcceptedEvent
import org.openapitools.client.models.CallCreatedEvent
import org.openapitools.client.models.CallEndedEvent
import org.openapitools.client.models.CallIngressResponse
import org.openapitools.client.models.CallMemberAddedEvent
import org.openapitools.client.models.CallMemberRemovedEvent
import org.openapitools.client.models.CallMemberUpdatedEvent
import org.openapitools.client.models.CallMemberUpdatedPermissionEvent
import org.openapitools.client.models.CallReactionEvent
import org.openapitools.client.models.CallRecordingStartedEvent
import org.openapitools.client.models.CallRecordingStoppedEvent
import org.openapitools.client.models.CallRejectedEvent
import org.openapitools.client.models.CallResponse
import org.openapitools.client.models.CallSettingsResponse
import org.openapitools.client.models.CallStateResponseFields
import org.openapitools.client.models.CallUpdatedEvent
import org.openapitools.client.models.ConnectedEvent
import org.openapitools.client.models.CustomVideoEvent
import org.openapitools.client.models.GetCallResponse
import org.openapitools.client.models.GetOrCreateCallResponse
import org.openapitools.client.models.HealthCheckEvent
import org.openapitools.client.models.JoinCallResponse
import org.openapitools.client.models.MemberResponse
import org.openapitools.client.models.OwnCapability
import org.openapitools.client.models.PermissionRequestEvent
import org.openapitools.client.models.ReactionResponse
import org.openapitools.client.models.UnblockedUserEvent
import org.openapitools.client.models.UpdateCallResponse
import org.openapitools.client.models.UpdatedCallPermissionsEvent
import org.openapitools.client.models.VideoEvent
import org.threeten.bp.Clock
import org.threeten.bp.OffsetDateTime
import stream.video.sfu.models.Participant
import java.util.SortedMap

/**
 *
 */
public class CallState(private val call: Call, user: User) {
    private val logger by taggedLogger("CallState")

    private val _blockedUsers: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    val blockedUsers: StateFlow<Set<String>> = _blockedUsers

    private val _ringingState: MutableStateFlow<RingingState?> = MutableStateFlow(null)
    public val ringingState: MutableStateFlow<RingingState?> = _ringingState

    private val _settings: MutableStateFlow<CallSettingsResponse?> = MutableStateFlow(null)
    public val settings: MutableStateFlow<CallSettingsResponse?> = _settings

    private val _members: MutableStateFlow<SortedMap<String, MemberState>> =
        MutableStateFlow(emptyMap<String, MemberState>().toSortedMap())
    public val members: StateFlow<List<MemberState>> =
        _members.mapState { it.values.toList() }

    private val _participants: MutableStateFlow<SortedMap<String, ParticipantState>> =
        MutableStateFlow(emptyMap<String, ParticipantState>().toSortedMap())
    public val participants: StateFlow<List<ParticipantState>> =
        _participants.mapState { it.values.toList() }

    private val _recording: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording

    private val _screenSharingSession: MutableStateFlow<ScreenSharingSession?> =
        MutableStateFlow(null)

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

    public val screenSharingSession: StateFlow<ScreenSharingSession?> = _screenSharingSession

    public val isScreenSharing: StateFlow<Boolean> = _screenSharingSession.mapState { it != null }

    private val _screenSharingTrack: MutableStateFlow<TrackWrapper?> = MutableStateFlow(null)

    private val userToSessionIdMap = participants.mapState { participants ->
        participants.map { it.user.value.id to it.sessionId }.toMap()
    }

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

    internal val _reactions = MutableStateFlow<List<ReactionResponse>>(emptyList())
    val reactions: StateFlow<List<ReactionResponse>> = _reactions

    internal val _permissionRequests = MutableStateFlow<List<PermissionRequestEvent>>(emptyList())
    val permissionRequests: StateFlow<List<PermissionRequestEvent>> = _permissionRequests

    private val _errors: MutableStateFlow<List<ErrorEvent>> =
        MutableStateFlow(emptyList())
    public val errors: StateFlow<List<ErrorEvent>> = _errors

    public fun updateParticipantTrackSize(
        sessionId: String,
        measuredWidth: Int,
        measuredHeight: Int
    ) {
        logger.v { "[updateParticipantTrackSize] SessionId: $sessionId, width:$measuredWidth, height:$measuredHeight" }
        val participant = getParticipantBySessionId(sessionId)
        participant?.let {
            val updated = participant.copy(videoTrackSize = measuredWidth to measuredHeight)
            updateParticipant(updated)
        }
    }

    fun handleEvent(event: VideoEvent) {
        logger.d { "Updating call state with event $event" }
        when (event) {
            is BlockedUserEvent -> {
                val newBlockedUsers = _blockedUsers.value.toMutableSet()
                newBlockedUsers.add(event.user.id)
                _blockedUsers.value = newBlockedUsers
            }

            is UnblockedUserEvent -> {
                val newBlockedUsers = _blockedUsers.value.toMutableSet()
                newBlockedUsers.remove(event.user.id)
                _blockedUsers.value = newBlockedUsers
            }

            is CallAcceptedEvent -> {
                val member = getMember(event.user.id)
                val newMember = member?.copy(acceptedAt = OffsetDateTime.now(Clock.systemUTC()))
                val newMembersMap = _members.value.toSortedMap()
                newMember?.let {
                    newMembersMap[event.user.id] = it
                    _members.value = newMembersMap
                }
            }

            is CallRejectedEvent -> {
                val member = getMember(event.user.id)
                val newMember = member?.copy(rejectedAt = OffsetDateTime.now(Clock.systemUTC()))
                val newMembersMap = _members.value.toSortedMap()
                newMember?.let {
                    newMembersMap[event.user.id] = it
                    _members.value = newMembersMap
                }
            }

            is CallEndedEvent -> {
                _endedAt.value = OffsetDateTime.now(Clock.systemUTC())
                _endedByUser.value = event.user?.toUser()
            }

            is CallMemberUpdatedEvent -> {
                getOrCreateMembers(event.members)
            }

            is CallMemberRemovedEvent -> {
                val newMembersMap = _members.value.toSortedMap()
                event.members.forEach {
                    newMembersMap.remove(it)
                }
                _members.value = newMembersMap
            }

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

            is HealthCheckEvent -> {
                // we don't do anything with this, handled by the socket
            }

            is PermissionRequestEvent -> {
                val newRequests = _permissionRequests.value.toMutableList()
                newRequests.add(event)
                _permissionRequests.value = newRequests
            }

            is CallMemberUpdatedPermissionEvent -> {
                _capabilitiesByRole.value = event.capabilitiesByRole
            }

            is CallMemberAddedEvent -> {
                getOrCreateMembers(event.members)
            }

            is CallReactionEvent -> {
                val reactions = _reactions.value.toMutableList()
                reactions.add(event.reaction)
                _reactions.value = reactions
                val user = event.reaction.user
                // get the participants for this user
                val userToSessionIdMap = userToSessionIdMap.value
                val sessionId = userToSessionIdMap[user.id]
                sessionId?.let {
                    val participant = getParticipantBySessionId(sessionId)
                    participant?.let {
                        val newReactions = participant._reactions.value.toMutableList()
                        newReactions.add(event.reaction)
                        participant._reactions.value = newReactions
                    }
                }
            }

            is CallRecordingStartedEvent -> {
                _recording.value = true
            }

            is CallRecordingStoppedEvent -> {
                _recording.value = false
            }

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

            is SubscriberOfferEvent -> {
                // handled by ActiveSFUSession
            }

            is TrackPublishedEvent -> {
                // handled by ActiveSFUSession
            }

            is TrackUnpublishedEvent -> {
                // handled by ActiveSFUSession
            }

            is SFUConnectedEvent -> {
                _connection.value = ConnectionState.Connected
            }

            is ConnectedEvent -> {
                // handled by socket
            }
        }
    }

    private fun updateFromJoinResponse(event: JoinCallResponseEvent) {
        // creates the participants
        val participantStates = event.callState.participants.map {
            getOrCreateParticipant(it)
        }
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

    internal fun getOrCreateParticipant(participant: Participant): ParticipantState {
        // get or create the participant and update them
        if (participant.session_id.isEmpty()) {
            throw IllegalStateException("Participant session id is empty")
        }

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

    internal fun getOrCreateMembers(members: List<MemberResponse>) {
        val memberMap = _members.value.toSortedMap()

        val memberStates = members.map {
            val userId = it.user.id
            val memberState = if (memberMap.contains(userId)) {
                memberMap[userId]!!
            } else {
                val member = it.toMemberState()
                memberMap[userId] = member
                member
            }
        }
        _members.value = memberMap
    }

    fun getParticipantBySessionId(sessionId: String): ParticipantState? {
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

    private val _custom: MutableStateFlow<Map<String, Any>> = MutableStateFlow(emptyMap())
    val custom: StateFlow<Map<String, Any>> = _custom

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
        updateFromResponse(members)
        val callResponse = response.call
        updateFromResponse(callResponse)
    }

    private fun updateFromResponse(members: List<MemberResponse>) {
        getOrCreateMembers(members)
    }

    fun updateFromResponse(response: UpdateCallResponse) {
        updateFromResponse(response.call)
    }

    fun updateFromResponse(response: GetCallResponse) {
        updateFromResponse(response.call)
        updateFromResponse(response.members)
    }

    fun updateFromResponse(response: JoinCallResponse) {
        updateFromResponse(response.call)
        updateFromResponse(response.members)
    }

    fun getMember(userId: String): MemberState? {
        return _members.value[userId]
    }

    fun updateFromResponse(callData: CallStateResponseFields) {
        updateFromResponse(callData.call)
        updateFromResponse(callData.members)
        // TODO: what about the blocked users?
    }
}

private fun MemberResponse.toMemberState(): MemberState {

    return MemberState(
        user = user.toUser(),
        custom = custom,
        role = role,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
