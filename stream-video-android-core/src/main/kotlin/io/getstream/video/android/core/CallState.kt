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
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.events.AudioLevelChangedEvent
import io.getstream.video.android.core.events.ChangePublishQualityEvent
import io.getstream.video.android.core.events.ConnectionQualityChangeEvent
import io.getstream.video.android.core.events.DominantSpeakerChangedEvent
import io.getstream.video.android.core.events.ErrorEvent
import io.getstream.video.android.core.events.ICETrickleEvent
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.events.ParticipantCount
import io.getstream.video.android.core.events.ParticipantJoinedEvent
import io.getstream.video.android.core.events.ParticipantLeftEvent
import io.getstream.video.android.core.events.SFUHealthCheckEvent
import io.getstream.video.android.core.events.SubscriberOfferEvent
import io.getstream.video.android.core.events.TrackPublishedEvent
import io.getstream.video.android.core.events.TrackUnpublishedEvent
import io.getstream.video.android.core.model.Ingress
import io.getstream.video.android.core.model.NetworkQuality
import io.getstream.video.android.core.model.RTMP
import io.getstream.video.android.core.model.Reaction
import io.getstream.video.android.core.model.ScreenSharingSession
import io.getstream.video.android.core.permission.PermissionRequest
import io.getstream.video.android.core.utils.mapState
import io.getstream.video.android.core.utils.toUser
import io.getstream.video.android.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.internal.toImmutableList
import org.openapitools.client.models.BlockedUserEvent
import org.openapitools.client.models.CallAcceptedEvent
import org.openapitools.client.models.CallCreatedEvent
import org.openapitools.client.models.CallEndedEvent
import org.openapitools.client.models.CallIngressResponse
import org.openapitools.client.models.CallMemberAddedEvent
import org.openapitools.client.models.CallMemberRemovedEvent
import org.openapitools.client.models.CallMemberUpdatedEvent
import org.openapitools.client.models.CallMemberUpdatedPermissionEvent
import org.openapitools.client.models.CallParticipantResponse
import org.openapitools.client.models.CallReactionEvent
import org.openapitools.client.models.CallRecordingStartedEvent
import org.openapitools.client.models.CallRecordingStoppedEvent
import org.openapitools.client.models.CallRejectedEvent
import org.openapitools.client.models.CallResponse
import org.openapitools.client.models.CallRingEvent
import org.openapitools.client.models.CallSessionEndedEvent
import org.openapitools.client.models.CallSessionParticipantJoinedEvent
import org.openapitools.client.models.CallSessionParticipantLeftEvent
import org.openapitools.client.models.CallSessionResponse
import org.openapitools.client.models.CallSessionStartedEvent
import org.openapitools.client.models.CallSettingsResponse
import org.openapitools.client.models.CallStateResponseFields
import org.openapitools.client.models.CallUpdatedEvent
import org.openapitools.client.models.ConnectedEvent
import org.openapitools.client.models.CustomVideoEvent
import org.openapitools.client.models.EgressResponse
import org.openapitools.client.models.GetCallResponse
import org.openapitools.client.models.GetOrCreateCallResponse
import org.openapitools.client.models.GoLiveResponse
import org.openapitools.client.models.HealthCheckEvent
import org.openapitools.client.models.JoinCallResponse
import org.openapitools.client.models.MemberResponse
import org.openapitools.client.models.OwnCapability
import org.openapitools.client.models.PermissionRequestEvent
import org.openapitools.client.models.QueryMembersResponse
import org.openapitools.client.models.ReactionResponse
import org.openapitools.client.models.StopLiveResponse
import org.openapitools.client.models.UnblockedUserEvent
import org.openapitools.client.models.UpdateCallResponse
import org.openapitools.client.models.UpdatedCallPermissionsEvent
import org.openapitools.client.models.VideoEvent
import org.threeten.bp.Clock
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import stream.video.sfu.models.Participant
import stream.video.sfu.models.TrackType
import java.util.SortedMap
import java.util.UUID
import kotlin.time.DurationUnit
import kotlin.time.toDuration

public sealed interface RealtimeConnection {
    /**
     * We start out in the PreJoin state. This is before call.join is called
     */
    public object PreJoin : RealtimeConnection

    /**
     * Join is in progress
     */
    public object InProgress : RealtimeConnection

    /**
     * We set the state to Joined as soon as the call state is available
     */
    public data class Joined(val session: RtcSession) :
        RealtimeConnection // joined, participant state is available, you can render the call. Video isn't ready yet

    /**
     * True when the peer connections are ready
     */
    public object Connected : RealtimeConnection // connected to RTC, able to receive and send video

    /**
     * Reconnecting is true whenever Rtc isn't available and trying to recover
     * If the subscriber peer connection breaks we'll reconnect
     * If the publisher peer connection breaks we'll reconnect
     * Also if the network provider from the OS says that internet is down we'll set it to reconnecting
     */
    public object Reconnecting : RealtimeConnection // reconnecting to recover from temporary issues
    public data class Failed(val error: Any) : RealtimeConnection // permanent failure
    public object Disconnected : RealtimeConnection // normal disconnect by the app
}

/**
 * The CallState class keeps all state for a call
 * It's available on every call object
 *
 * @sample
 *
 * val call = client.call("default", "123")
 * call.get() // or create or join
 * call.state.participants // list of participants
 *
 *
 */
public class CallState(
    private val client: StreamVideo,
    private val call: Call,
    private val user: User,
    internal val scope: CoroutineScope
) {

    private val logger by taggedLogger("CallState")

    internal val _connection = MutableStateFlow<RealtimeConnection>(RealtimeConnection.PreJoin)
    public val connection: StateFlow<RealtimeConnection> = _connection

    public val isReconnecting: StateFlow<Boolean> = _connection.mapState {
        it is RealtimeConnection.Reconnecting
    }

    private val _participants: MutableStateFlow<SortedMap<String, ParticipantState>> =
        MutableStateFlow(emptyMap<String, ParticipantState>().toSortedMap())

    /** Participants returns a list of participant state object. @see [ParticipantState] */
    public val participants: StateFlow<List<ParticipantState>> =
        _participants.mapState { it.values.toList() }

    private val _startedAt: MutableStateFlow<OffsetDateTime?> = MutableStateFlow(null)
    public val startedAt: StateFlow<OffsetDateTime?> = _startedAt

    private val _participantCounts: MutableStateFlow<ParticipantCount?> = MutableStateFlow(null)
    val participantCounts: StateFlow<ParticipantCount?> = _participantCounts

    /** Your own participant state */
    public val me: StateFlow<ParticipantState?> = _participants.mapState {
        it[call.clientImpl.sessionId]
    }

    /** participants who are currently speaking */
    private val _activeSpeakers: MutableStateFlow<List<ParticipantState>> =
        MutableStateFlow(emptyList())
    public val activeSpeakers: StateFlow<List<ParticipantState>> = _activeSpeakers

    /** participants other than yourself */
    public val remoteParticipants: StateFlow<List<ParticipantState>> =
        _participants.mapState { it.filterKeys { key -> key != call.clientImpl.sessionId }.values.toList() }

    /** the dominant speaker */
    private val _dominantSpeaker: MutableStateFlow<ParticipantState?> = MutableStateFlow(null)
    public val dominantSpeaker: StateFlow<ParticipantState?> = _dominantSpeaker

    /**
     * Sorted participants gives you the list of participants sorted by
     * * anyone who is pinned
     * * dominant speaker
     * * if you are screensharing
     * * last speaking at
     * * all other video participants by when they joined
     * * audio only participants by when they joined
     *
     */
    internal val _pinnedParticipants: MutableStateFlow<Map<String, OffsetDateTime>> =
        MutableStateFlow(emptyMap())
    val pinnedParticipants: StateFlow<Map<String, OffsetDateTime>> = _pinnedParticipants

    val stats = CallStats(call, scope)

    internal val sortedParticipantsFlow = channelFlow {
        // uses a channel flow to handle concurrency and 3 things updating: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/channel-flow.html

        fun emitSorted() {
            val participants = participants.value
            val pinned = _pinnedParticipants.value
            var lastParticipants: List<ParticipantState>? = null
            val sorted = participants.sortedWith(
                compareBy(
                    { pinned.containsKey(it.sessionId) },
                    { it.dominantSpeaker.value },
                    { it.screenSharingEnabled.value },
                    { it.lastSpeakingAt.value },
                    { it.videoEnabled.value },
                    { it.joinedAt.value }
                )
            ).reversed()

            scope.launch {
                if (lastParticipants != sorted) {
                    send(sorted)
                    lastParticipants = sorted
                }
            }
        }

        scope.launch {
            _participants.collect {
                emitSorted()
            }
        }
        // Since participant state exposes it's own stateflows this is a little harder to do than usual
        // we need to listen to the events and update the flow when it changes

        // emit the sorted list
        emitSorted()

        // TODO: could optimize performance by subscribing only to relevant events
        call.subscribe {
            emitSorted()
        }

        scope.launch {
            _pinnedParticipants.collect {
                emitSorted()
            }
        }

        awaitClose {}
    }

    /**
     * Sorted participants based on
     * - Pinned
     * - Dominant Speaker
     * - Screensharing
     * - Last speaking at
     * - Video enabled
     * - Call joined at
     *
     * Debounced 100ms to avoid rapid changes
     */
    val sortedParticipants = sortedParticipantsFlow.debounce(100)
        .stateIn(scope, SharingStarted.WhileSubscribed(10000L), emptyList())

    /** Members contains the list of users who are permanently associated with this call. This includes users who are currently not active in the call
     * As an example if you invite "john", "bob" and "jane" to a call and only Jane joins.
     * All 3 of them will be members, but only Jane will be a participant
     */
    private val _members: MutableStateFlow<SortedMap<String, MemberState>> =
        MutableStateFlow(emptyMap<String, MemberState>().toSortedMap())
    public val members: StateFlow<List<MemberState>> =
        _members.mapState { it.values.toList() }

    /** if someone is sharing their screen */
    private val _screenSharingSession: MutableStateFlow<ScreenSharingSession?> =
        MutableStateFlow(null)
    public val screenSharingSession: StateFlow<ScreenSharingSession?> = _screenSharingSession

    /** if the call is being recorded */
    private val _speakingWhileMuted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val speakingWhileMuted: StateFlow<Boolean> = _speakingWhileMuted

    /** if the call is being recorded */
    private val _recording: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording

    /** The list of users that are blocked from joining this call */
    private val _blockedUsers: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    val blockedUsers: StateFlow<Set<String>> = _blockedUsers

    /** Specific to ringing calls, additional state about incoming, outgoing calls */
    private val _ringingState: MutableStateFlow<RingingState> = MutableStateFlow(RingingState.Idle)
    public val ringingState: StateFlow<RingingState> = _ringingState

    /** The settings for the call */
    private val _settings: MutableStateFlow<CallSettingsResponse?> = MutableStateFlow(null)
    public val settings: StateFlow<CallSettingsResponse?> = _settings

    private val _durationInMs = flow {
        while (currentCoroutineContext().isActive) {
            delay(1000)
            val started = _session.value?.startedAt
            val ended = _session.value?.endedAt ?: OffsetDateTime.now()
            val difference = if (started == null) null else {
                ended.toInstant().toEpochMilli() - started.toInstant().toEpochMilli()
            }
            emit(difference)
        }
    }

    /** how long the call has been running, null if the call didn't start yet */
    public val duration: StateFlow<kotlin.time.Duration?> =
        _durationInMs.transform { emit((it ?: 0L).toDuration(DurationUnit.MILLISECONDS)) }.stateIn(scope, SharingStarted.WhileSubscribed(10000L), null)

    /** how many milliseconds the call has been running, null if the call didn't start yet */
    public val durationInMs: StateFlow<Long?> =
        _durationInMs.stateIn(scope, SharingStarted.WhileSubscribed(10000L), null)

    /** Check if you have permissions to do things like share your audio, video, screen etc */
    public fun hasPermission(permission: String): StateFlow<Boolean> {
        // store this in a map so we don't have to create a new flow every time
        return if (_hasPermissionMap.containsKey(permission)) {
            _hasPermissionMap[permission]!!
        } else {
            val flow = _ownCapabilities.mapState { it.map { it.toString() }.contains(permission) }
            _hasPermissionMap[permission] = flow
            flow
        }
    }

    private val _ownCapabilities: MutableStateFlow<List<OwnCapability>> =
        MutableStateFlow(emptyList())
    public val ownCapabilities: StateFlow<List<OwnCapability>> = _ownCapabilities

    internal val _permissionRequests = MutableStateFlow<List<PermissionRequest>>(emptyList())
    val permissionRequests: StateFlow<List<PermissionRequest>> = _permissionRequests

    private val _capabilitiesByRole: MutableStateFlow<Map<String, List<String>>> =
        MutableStateFlow(emptyMap())
    val capabilitiesByRole: StateFlow<Map<String, List<String>>> = _capabilitiesByRole

    private val _backstage: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /** if we are in backstage mode or not */
    val backstage: StateFlow<Boolean> = _backstage

    /** the opposite of backstage, if we are live or not */
    val live: StateFlow<Boolean> = _backstage.mapState { !it }

    /** how many milliseconds the call has been running, null if the call didn't start yet */
    public val liveDurationInMs: StateFlow<Long?> =
        _durationInMs
            .map {
                if (live.value) {
                    it
                } else {
                    null
                }
            }
            .stateIn(scope, SharingStarted.WhileSubscribed(10000L), null)

    private val _egress: MutableStateFlow<EgressResponse?> = MutableStateFlow(null)
    val egress: StateFlow<EgressResponse?> = _egress

    private val _broadcasting: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /** if the call is being broadcasted to HLS */
    val broadcasting: StateFlow<Boolean> = _broadcasting

    /** if transcribing is on or not */
    private val _transcribing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val transcribing: StateFlow<Boolean> = _transcribing

    private val _acceptedBy: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    val acceptedBy: StateFlow<Set<String>> = _acceptedBy

    private val _rejectedBy: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    val rejectedBy: StateFlow<Set<String>> = _rejectedBy

    internal val _session = MutableStateFlow<CallSessionResponse?>(null)
    val session: StateFlow<CallSessionResponse?> = _session

    /** startsAt */
    private val _startsAt: MutableStateFlow<OffsetDateTime?> = MutableStateFlow(null)
    val startsAt: StateFlow<OffsetDateTime?> = _startsAt

    private val _updatedAt: MutableStateFlow<OffsetDateTime?> = MutableStateFlow(null)

    /** updatedAt */
    val updatedAt: StateFlow<OffsetDateTime?> = _updatedAt

    private val _createdAt: MutableStateFlow<OffsetDateTime?> = MutableStateFlow(null)
    val createdAt: StateFlow<OffsetDateTime?> = _createdAt

    private val _blockedUserIds: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    val blockedUserIds: StateFlow<List<String>> = _blockedUserIds

    private val _custom: MutableStateFlow<Map<String, Any?>> = MutableStateFlow(emptyMap())
    val custom: StateFlow<Map<String, Any?>> = _custom

    private val _team: MutableStateFlow<String?> = MutableStateFlow(null)
    val team: StateFlow<String?> = _team

    private val _createdBy: MutableStateFlow<User?> = MutableStateFlow(null)
    val createdBy: StateFlow<User?> = _createdBy

    private val _ingress: MutableStateFlow<CallIngressResponse?> = MutableStateFlow(null)
    val ingress: StateFlow<Ingress?> = _ingress.mapState {
        // TODO: Remove runBlocking and use standard Flow instead for mapping or use other approach
        val token = runBlocking { call.clientImpl.dataStore.userToken.firstOrNull() }
        val apiKey = runBlocking { call.clientImpl.dataStore.apiKey.firstOrNull() }
        val streamKey = "$apiKey/$token"
        // TODO: use the address when the server is updated
        val overwriteUrl =
            "rtmps://video-ingress-frankfurt-vi1.stream-io-video.com:443/${call.type}/${call.id}"
        Ingress(rtmp = RTMP(address = overwriteUrl ?: "", streamKey = streamKey))
    }

    private val userToSessionIdMap = participants.mapState { participants ->
        participants.associate { it.user.value.id to it.sessionId }
    }

    internal val _hasPermissionMap = mutableMapOf<String, StateFlow<Boolean>>()

    private val _endedAt: MutableStateFlow<OffsetDateTime?> = MutableStateFlow(null)
    val endedAt: StateFlow<OffsetDateTime?> = _endedAt
    private val _endedByUser: MutableStateFlow<User?> = MutableStateFlow(null)
    val endedByUser: StateFlow<User?> = _endedByUser

    internal val _reactions = MutableStateFlow<List<ReactionResponse>>(emptyList())
    val reactions: StateFlow<List<ReactionResponse>> = _reactions

    private val _errors: MutableStateFlow<List<ErrorEvent>> =
        MutableStateFlow(emptyList())
    public val errors: StateFlow<List<ErrorEvent>> = _errors

    private var speakingWhileMutedResetJob: Job? = null
    private var autoJoiningCall: Job? = null
    private var ringingTimerJob: Job? = null

    fun handleEvent(event: VideoEvent) {
        logger.d { "Updating call state with event ${event::class.java}" }
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
                val newAcceptedBy = _acceptedBy.value.toMutableSet()
                newAcceptedBy.add(event.user.id)
                _acceptedBy.value = newAcceptedBy.toSet()
                updateRingingState()

                // auto-join the call if it's an outgoing call and someone has accepted
                // do not auto-join if it's already accepted by us
                val callRingState = _ringingState.value
                if (callRingState is RingingState.Outgoing &&
                    callRingState.acceptedByCallee &&
                    _acceptedBy.value.findLast { it == client.userId } == null &&
                    client.state.activeCall.value == null &&
                    autoJoiningCall == null
                ) {
                    autoJoiningCall = scope.launch {
                        // errors are handled inside the join function
                        call.join()
                        autoJoiningCall = null
                    }
                }
            }

            is CallRejectedEvent -> {
                val new = _rejectedBy.value.toMutableSet()
                new.add(event.user.id)
                _rejectedBy.value = new.toSet()
                updateRingingState()
            }

            is CallEndedEvent -> {
                _endedAt.value = OffsetDateTime.now(Clock.systemUTC())
                _endedByUser.value = event.user?.toUser()

                // leave the call
                call.leave()
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
                updateFromResponse(event.call)
                getOrCreateMembers(event.members)
            }

            is CallRingEvent -> {
                updateFromResponse(event.call)
                getOrCreateMembers(event.members)
            }

            is CallUpdatedEvent -> {
                updateFromResponse(event.call)
                _capabilitiesByRole.value = event.capabilitiesByRole
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
                newRequests.add(PermissionRequest(call, event))
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
                        val reaction = Reaction(
                            id = UUID.randomUUID().toString(),
                            response = event.reaction,
                            createdAt = System.currentTimeMillis(),
                        )
                        newReactions.add(reaction)
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
                    participant.updateAudioLevel(entry.value.audioLevel)
                }

                _activeSpeakers.value = participants.value
                    .filter { it.speaking.value }
                    .sortedByDescending { it.audioLevel.value }
            }

            is DominantSpeakerChangedEvent -> {
                val lastDominantSpeaker = dominantSpeaker.value
                val lastDominantSpeakerId = lastDominantSpeaker?.sessionId
                if (lastDominantSpeakerId != event.sessionId) {
                    val newSpeaker = getOrCreateParticipant(event.sessionId, event.userId)
                    newSpeaker._dominantSpeaker.value = true
                    _dominantSpeaker.value = newSpeaker
                    lastDominantSpeaker?._dominantSpeaker?.value = false
                }
            }

            is ConnectionQualityChangeEvent -> {
                event.updates.forEach { entry ->
                    val participant = getOrCreateParticipant(entry.session_id, entry.user_id)
                    participant._networkQuality.value =
                        NetworkQuality.fromConnectionQuality(entry.connection_quality)
                }
            }

            is ChangePublishQualityEvent -> {
                call.session!!.handleEvent(event)
            }

            is ErrorEvent -> {
                _errors.value = errors.value + event
            }

            is SFUHealthCheckEvent -> {
                call.state._participantCounts.value = event.participantCount
            }

            is ICETrickleEvent -> {
                // handled by ActiveSFUSession
            }

            is JoinCallResponseEvent -> {
                // time to update call state based on the join response
                updateFromJoinResponse(event)
                updateRingingState()
            }

            is ParticipantJoinedEvent -> {
                getOrCreateParticipant(event.participant)
            }

            is ParticipantLeftEvent -> {
                removeParticipant(event.participant.session_id)

                // clean up - stop screen-sharing session if it was still running
                val current = _screenSharingSession.value
                if (current?.participant?.sessionId == event.participant.session_id) {
                    _screenSharingSession.value = null
                }
            }

            is SubscriberOfferEvent -> {
                // handled by ActiveSFUSession
            }

            is TrackPublishedEvent -> {
                // handled by ActiveSFUSession
                val participant = getOrCreateParticipant(event.sessionId, event.userId)
                if (event.trackType == TrackType.TRACK_TYPE_AUDIO) {
                    participant._audioEnabled.value = true
                } else if (event.trackType == TrackType.TRACK_TYPE_VIDEO) {
                    participant._videoEnabled.value = true
                } else if (event.trackType == TrackType.TRACK_TYPE_SCREEN_SHARE) {
                    // mark the screen share enabled
                    // create the _screenSharingSession
                    participant._screenSharingEnabled.value = true
                    _screenSharingSession.value = ScreenSharingSession(
                        participant,
                    )
                }
            }

            is TrackUnpublishedEvent -> {
                // handled by ActiveSFUSession
                val participant = getOrCreateParticipant(event.sessionId, event.userId)
                if (event.trackType == TrackType.TRACK_TYPE_AUDIO) {
                    participant._audioEnabled.value = false
                } else if (event.trackType == TrackType.TRACK_TYPE_VIDEO) {
                    participant._videoEnabled.value = false
                } else if (event.trackType == TrackType.TRACK_TYPE_SCREEN_SHARE) {
                    // mark the screen share enabled
                    // create the _screenSharingSession
                    participant._screenSharingEnabled.value = false
                    val current = _screenSharingSession.value
                    if (current?.participant?.sessionId == participant.sessionId) {
                        _screenSharingSession.value = null
                    }
                }
            }

            is ConnectedEvent -> {
                // handled by socket
            }

            is CallSessionStartedEvent -> {
                event.call.session?.let { session ->
                    _session.value = session
                }
            }

            is CallSessionEndedEvent -> {
                _session.value = event.call.session
            }

            is CallSessionParticipantLeftEvent -> {
                _session.value?.let { callSessionResponse ->
                    val newList = callSessionResponse.participants.toMutableList()
                    newList.removeIf { it.userSessionId == event.participant.userSessionId }
                    _session.value = callSessionResponse.copy(
                        participants = newList.toImmutableList()
                    )
                }
            }

            is CallSessionParticipantJoinedEvent -> {
                _session.value?.let { callSessionResponse ->
                    val newList = callSessionResponse.participants.toMutableList()
                    val participant = CallParticipantResponse(
                        user = event.participant.user,
                        joinedAt = event.createdAt,
                        role = "user",
                        userSessionId = event.participant.userSessionId
                    )
                    val index = newList.indexOfFirst { user.id == event.participant.user.id }
                    if (index == -1) {
                        newList.add(participant)
                    } else {
                        newList[index] = participant
                    }
                    _session.value = callSessionResponse.copy(
                        participants = newList.toImmutableList()
                    )
                }
                updateRingingState()
            }
        }
    }

    private fun updateRingingState() {
        // this is only true when we are in the session (we have accepted/joined the call)
        val userIsParticipant =
            _session.value?.participants?.find { it.user.id == client.userId } != null
        val outgoingMembersCount = _members.value.filter { it.value.user.id != client.userId }.size
        val rejectedBy = _rejectedBy.value
        val acceptedBy = _acceptedBy.value
        val createdBy = _createdBy.value
        val members = _members.value
        val acceptedByMe = _acceptedBy.value.findLast { it == client.userId }
        val hasActiveCall = client.state.activeCall.value != null
        val hasRingingCall = client.state.ringingCall.value != null

        // no members - call is empty, we can join
        val state: RingingState = if (hasActiveCall) {
            RingingState.Active
        } else if (rejectedBy.isNotEmpty() && acceptedBy.isEmpty() && rejectedBy.size >= outgoingMembersCount) {
            // Call was rejected. Listener should leave the call with call.leave()
            RingingState.RejectedByAll
        } else if (hasRingingCall && createdBy?.id != client.userId) {
            // Member list is not empty, it's not rejected - it's an incoming call
            // If it's already accepted by me then we are in an Active call
            if (userIsParticipant) {
                RingingState.Active
            } else {
                RingingState.Incoming(acceptedByMe = acceptedByMe != null)
            }
        } else if (hasRingingCall && createdBy?.id == client.userId) {
            // The call is created by us
            if (acceptedBy.isEmpty()) {
                // no one accepted the call
                RingingState.Outgoing(acceptedByCallee = false)
            } else if (!userIsParticipant) {
                // someone already accepted the call, but it's not us (client needs to do call.join)
                RingingState.Outgoing(acceptedByCallee = true)
            } else {
                // call is accepted and we are already in the call
                RingingState.Active
            }
        } else {
            RingingState.Idle
        }

        if (_ringingState.value != state) {
            logger.d { "Updating ringing state ${_ringingState.value} -> $state" }

            // handle the auto-cancel for outgoing ringing calls
            if (state is RingingState.Outgoing && !state.acceptedByCallee) {
                startRingingTimer()
            } else {
                ringingTimerJob?.cancel()
                ringingTimerJob = null
            }

            // stop the call ringing timer if it's running
        }
        _ringingState.value = state
    }

    private fun updateFromJoinResponse(event: JoinCallResponseEvent) {
        // update the participant count
        _participantCounts.value = event.participantCount

        val instant = Instant.ofEpochSecond(event.callState.started_at?.epochSecond!!, 0)
        _startedAt.value = OffsetDateTime.ofInstant(instant, ZoneOffset.UTC)
        // creates the participants
        val participantStates = event.callState.participants.map {
            getOrCreateParticipant(it)
        }
        upsertParticipants(participantStates)
    }

    fun markSpeakingAsMuted() {
        _speakingWhileMuted.value = true
        speakingWhileMutedResetJob?.cancel()
        speakingWhileMutedResetJob = scope.launch {
            delay(2000)
            _speakingWhileMuted.value = false
        }
    }

    private fun startRingingTimer() {
        ringingTimerJob?.cancel()
        ringingTimerJob = scope.launch {

            val autoCancelTimeout = settings.value?.ring?.autoCancelTimeoutMs

            if (autoCancelTimeout != null && autoCancelTimeout > 0) {
                delay(autoCancelTimeout.toLong())

                // double check that we are still in Outgoing call state and call is not active
                if (_ringingState.value is RingingState.Outgoing && client.state.activeCall.value == null) {
                    call.reject()
                    call.leave()
                }
            } else {
                logger.w { "[startRingingTimer] No autoCancelTimeoutMs set - call ring with no timeout" }
            }
        }
    }

    private fun removeParticipant(sessionId: String) {
        val new = _participants.value.toSortedMap()
        new.remove(sessionId)
        _participants.value = new
    }

    public fun upsertParticipants(participants: List<ParticipantState>) {
        val new = _participants.value.toSortedMap()
        val screensharing = mutableListOf<ParticipantState>()
        participants.forEach {
            new[it.sessionId] = it

            if (it.screenSharingEnabled.value) {
                screensharing.add(it)
            }
        }
        _participants.value = new

        if (screensharing.isNotEmpty()) {
            _screenSharingSession.value = ScreenSharingSession(
                screensharing[0],
            )
        }
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
            ParticipantState(
                sessionId = sessionId,
                call = call,
                initialUser = user ?: User(userId),
            )
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

        updateRingingState()
    }

    fun getParticipantBySessionId(sessionId: String): ParticipantState? {
        return _participants.value[sessionId]
    }

    fun updateParticipant(participant: ParticipantState) {
        val new = _participants.value.toSortedMap()
        new[participant.sessionId] = participant
        _participants.value = new
    }

    fun clearParticipants() {
        _participants.value = emptyMap<String, ParticipantState>().toSortedMap()
    }

    fun updateFromResponse(response: CallResponse) {
        _backstage.value = response.backstage
        _blockedUserIds.value = response.blockedUserIds
        _egress.value = response.egress
        _broadcasting.value = response.egress.broadcasting
        _session.value = response.session
        _rejectedBy.value = response.session?.rejectedBy?.keys?.toSet() ?: emptySet()
        _acceptedBy.value = response.session?.acceptedBy?.keys?.toSet() ?: emptySet()
        _createdAt.value = response.createdAt
        _updatedAt.value = response.updatedAt
        _endedAt.value = response.endedAt
        _startsAt.value = response.startsAt
        _createdBy.value = response.createdBy.toUser()
        _custom.value = response.custom
        _ingress.value = response.ingress
        _recording.value = response.recording
        _settings.value = response.settings
        _transcribing.value = response.transcribing
        _team.value = response.team

        updateRingingState()
    }

    fun updateFromResponse(response: GetOrCreateCallResponse) {
        val members = response.members
        updateFromResponse(members)
        _ownCapabilities.value = response.ownCapabilities
        val callResponse = response.call
        updateFromResponse(callResponse)
    }

    private fun updateFromResponse(members: List<MemberResponse>) {
        getOrCreateMembers(members)
    }

    fun updateFromResponse(response: UpdateCallResponse) {
        updateFromResponse(response.call)
        _ownCapabilities.value = response.ownCapabilities
        updateFromResponse(response.members)
    }

    fun updateFromResponse(response: GetCallResponse) {
        updateFromResponse(response.call)
        _ownCapabilities.value = response.ownCapabilities
        updateFromResponse(response.members)
    }

    fun updateFromResponse(response: JoinCallResponse) {
        updateFromResponse(response.call)
        _ownCapabilities.value = response.ownCapabilities
        updateFromResponse(response.members)
    }

    fun getMember(userId: String): MemberState? {
        return _members.value[userId]
    }

    fun updateFromResponse(callData: CallStateResponseFields) {
        updateFromResponse(callData.call)
        updateFromResponse(callData.members)
    }

    fun updateFromResponse(it: QueryMembersResponse) {
        updateFromResponse(it.members)
    }

    fun pin(sessionId: String) {
        val pins = _pinnedParticipants.value.toMutableMap()
        pins[sessionId] = OffsetDateTime.now(Clock.systemUTC())
        _pinnedParticipants.value = pins
    }

    fun unpin(sessionId: String) {
        val pins = _pinnedParticipants.value.toMutableMap()
        pins.remove(sessionId)
        _pinnedParticipants.value = pins
    }

    fun updateFromResponse(result: StopLiveResponse) {
        updateFromResponse(result.call)
    }

    fun updateFromResponse(result: GoLiveResponse) {
        updateFromResponse(result.call)
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
