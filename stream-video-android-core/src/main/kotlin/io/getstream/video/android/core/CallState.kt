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

import android.app.Notification
import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.core.app.NotificationManagerCompat
import io.getstream.android.video.generated.models.BlockedUserEvent
import io.getstream.android.video.generated.models.CallAcceptedEvent
import io.getstream.android.video.generated.models.CallClosedCaption
import io.getstream.android.video.generated.models.CallClosedCaptionsStartedEvent
import io.getstream.android.video.generated.models.CallClosedCaptionsStoppedEvent
import io.getstream.android.video.generated.models.CallCreatedEvent
import io.getstream.android.video.generated.models.CallEndedEvent
import io.getstream.android.video.generated.models.CallIngressResponse
import io.getstream.android.video.generated.models.CallLiveStartedEvent
import io.getstream.android.video.generated.models.CallMemberAddedEvent
import io.getstream.android.video.generated.models.CallMemberRemovedEvent
import io.getstream.android.video.generated.models.CallMemberUpdatedEvent
import io.getstream.android.video.generated.models.CallMemberUpdatedPermissionEvent
import io.getstream.android.video.generated.models.CallMissedEvent
import io.getstream.android.video.generated.models.CallModerationBlurEvent
import io.getstream.android.video.generated.models.CallParticipantResponse
import io.getstream.android.video.generated.models.CallReactionEvent
import io.getstream.android.video.generated.models.CallRecordingStartedEvent
import io.getstream.android.video.generated.models.CallRecordingStoppedEvent
import io.getstream.android.video.generated.models.CallRejectedEvent
import io.getstream.android.video.generated.models.CallResponse
import io.getstream.android.video.generated.models.CallRingEvent
import io.getstream.android.video.generated.models.CallSessionEndedEvent
import io.getstream.android.video.generated.models.CallSessionParticipantCountsUpdatedEvent
import io.getstream.android.video.generated.models.CallSessionParticipantJoinedEvent
import io.getstream.android.video.generated.models.CallSessionParticipantLeftEvent
import io.getstream.android.video.generated.models.CallSessionResponse
import io.getstream.android.video.generated.models.CallSessionStartedEvent
import io.getstream.android.video.generated.models.CallSettingsResponse
import io.getstream.android.video.generated.models.CallStateResponseFields
import io.getstream.android.video.generated.models.CallTranscriptionFailedEvent
import io.getstream.android.video.generated.models.CallTranscriptionStartedEvent
import io.getstream.android.video.generated.models.CallTranscriptionStoppedEvent
import io.getstream.android.video.generated.models.CallUpdatedEvent
import io.getstream.android.video.generated.models.ClosedCaptionEvent
import io.getstream.android.video.generated.models.ConnectedEvent
import io.getstream.android.video.generated.models.CustomVideoEvent
import io.getstream.android.video.generated.models.EgressHLSResponse
import io.getstream.android.video.generated.models.EgressResponse
import io.getstream.android.video.generated.models.GetCallResponse
import io.getstream.android.video.generated.models.GetOrCreateCallResponse
import io.getstream.android.video.generated.models.GoLiveResponse
import io.getstream.android.video.generated.models.HealthCheckEvent
import io.getstream.android.video.generated.models.JoinCallResponse
import io.getstream.android.video.generated.models.LocalCallAcceptedPostEvent
import io.getstream.android.video.generated.models.LocalCallMissedEvent
import io.getstream.android.video.generated.models.LocalCallRejectedPostEvent
import io.getstream.android.video.generated.models.MemberResponse
import io.getstream.android.video.generated.models.MuteUsersResponse
import io.getstream.android.video.generated.models.OwnCapability
import io.getstream.android.video.generated.models.PermissionRequestEvent
import io.getstream.android.video.generated.models.QueryCallMembersResponse
import io.getstream.android.video.generated.models.ReactionResponse
import io.getstream.android.video.generated.models.StartHLSBroadcastingResponse
import io.getstream.android.video.generated.models.StopLiveResponse
import io.getstream.android.video.generated.models.TranscriptionSettingsResponse.ClosedCaptionMode
import io.getstream.android.video.generated.models.UnblockedUserEvent
import io.getstream.android.video.generated.models.UpdateCallResponse
import io.getstream.android.video.generated.models.UpdatedCallPermissionsEvent
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.log.taggedLogger
import io.getstream.result.Result
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.closedcaptions.ClosedCaptionManager
import io.getstream.video.android.core.closedcaptions.ClosedCaptionsSettings
import io.getstream.video.android.core.events.AudioLevelChangedEvent
import io.getstream.video.android.core.events.CallEndedSfuEvent
import io.getstream.video.android.core.events.ChangePublishQualityEvent
import io.getstream.video.android.core.events.ConnectionQualityChangeEvent
import io.getstream.video.android.core.events.DominantSpeakerChangedEvent
import io.getstream.video.android.core.events.ErrorEvent
import io.getstream.video.android.core.events.ICETrickleEvent
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.events.ParticipantCount
import io.getstream.video.android.core.events.ParticipantJoinedEvent
import io.getstream.video.android.core.events.ParticipantLeftEvent
import io.getstream.video.android.core.events.PinUpdate
import io.getstream.video.android.core.events.PinsUpdatedEvent
import io.getstream.video.android.core.events.SFUHealthCheckEvent
import io.getstream.video.android.core.events.SubscriberOfferEvent
import io.getstream.video.android.core.events.TrackPublishedEvent
import io.getstream.video.android.core.events.TrackUnpublishedEvent
import io.getstream.video.android.core.internal.InternalStreamVideoApi
import io.getstream.video.android.core.model.Ingress
import io.getstream.video.android.core.model.NetworkQuality
import io.getstream.video.android.core.model.RTMP
import io.getstream.video.android.core.model.Reaction
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.model.ScreenSharingSession
import io.getstream.video.android.core.model.VisibilityOnScreenState
import io.getstream.video.android.core.moderations.ModerationManager
import io.getstream.video.android.core.notifications.IncomingNotificationData
import io.getstream.video.android.core.notifications.NotificationType
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.core.notifications.internal.telecom.jetpack.JetpackTelecomRepository
import io.getstream.video.android.core.permission.PermissionRequest
import io.getstream.video.android.core.pinning.PinType
import io.getstream.video.android.core.pinning.PinUpdateAtTime
import io.getstream.video.android.core.socket.common.scope.ClientScope
import io.getstream.video.android.core.socket.common.scope.UserScope
import io.getstream.video.android.core.sorting.SortedParticipantsState
import io.getstream.video.android.core.utils.ScheduleConfig
import io.getstream.video.android.core.utils.TaskSchedulerWithDebounce
import io.getstream.video.android.core.utils.mapState
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.core.utils.toUser
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.threeten.bp.Clock
import org.threeten.bp.OffsetDateTime
import stream.video.sfu.models.Participant
import stream.video.sfu.models.ParticipantSource
import stream.video.sfu.models.TrackType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.SortedMap
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Stable
public sealed interface RealtimeConnection {
    /**
     * We start out in the PreJoin state. This is before call.join is called
     */
    public data object PreJoin : RealtimeConnection

    /**
     * Join is in progress
     */
    public data object InProgress : RealtimeConnection

    /**
     * We set the state to Joined as soon as the call state is available
     */
    public data class Joined(val session: RtcSession) :
        RealtimeConnection // joined, participant state is available, you can render the call. Video isn't ready yet

    /**
     * True when the peer connections are ready
     */
    public data object Connected :
        RealtimeConnection // connected to RTC, able to receive and send video

    /**
     * Reconnecting is true whenever Rtc isn't available and trying to recover
     * If the subscriber peer connection breaks we'll reconnect
     * If the publisher peer connection breaks we'll reconnect
     * Also if the network provider from the OS says that internet is down we'll set it to reconnecting
     */
    public data object Reconnecting :
        RealtimeConnection // reconnecting to recover from temporary issues

    public data object Migrating : RealtimeConnection
    public data class Failed(val error: Any) : RealtimeConnection // permanent failure
    public data object Disconnected : RealtimeConnection // normal disconnect by the app
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
@Stable
public class CallState(
    private val client: StreamVideo,
    private val call: Call,
    private val user: User,
    @InternalStreamVideoApi
    val scope: CoroutineScope,
) {

    private val logger by taggedLogger("CallState")
    private var participantsVisibilityMonitor: Job? = null

    // Create a CallActions implementation that delegates to the Call object
    @InternalStreamVideoApi
    val callActions = object : CallActions {
        override suspend fun muteUserAudio(userId: String): Result<MuteUsersResponse> {
            return call.muteUser(userId, audio = true, video = false, screenShare = false)
        }

        override suspend fun muteUserVideo(userId: String): Result<MuteUsersResponse> {
            return call.muteUser(userId, audio = false, video = true, screenShare = false)
        }

        override suspend fun muteUserScreenShare(userId: String): Result<MuteUsersResponse> {
            return call.muteUser(userId, audio = false, video = false, screenShare = true)
        }

        override suspend fun pinParticipant(userId: String, sessionId: String) {
            call.state.pin(userId, sessionId)
        }

        override suspend fun unpinParticipant(sessionId: String) {
            call.state.unpin(sessionId)
        }

        override fun isLocalParticipant(sessionId: String): Boolean {
            return sessionId == call.sessionId
        }
    }

    internal val _connection = MutableStateFlow<RealtimeConnection>(RealtimeConnection.PreJoin)
    public val connection: StateFlow<RealtimeConnection> = _connection

    public val isReconnecting: StateFlow<Boolean> = _connection.mapState {
        it is RealtimeConnection.Reconnecting
    }

    private val internalParticipants = ConcurrentHashMap<String, ParticipantState>()
    private val _participants = MutableStateFlow<Map<String, ParticipantState>>(emptyMap())

    /** Participants returns a list of participant state object. @see [ParticipantState] */
    public val participants: StateFlow<List<ParticipantState>> =
        _participants.mapState { it.values.toList() }

    private val _startedAt: MutableStateFlow<OffsetDateTime?> = MutableStateFlow(null)

    /**
     * Will return always null with current SFU implementation - it may be fixed later. For now
     * do not use it.
     */
    public val startedAt: StateFlow<OffsetDateTime?> = _startedAt

    private val _participantCounts: MutableStateFlow<ParticipantCount?> = MutableStateFlow(null)
    val participantCounts: StateFlow<ParticipantCount?> = _participantCounts

    /** a count of the total number of participants. */
    val totalParticipants = _participantCounts.mapState { it?.total ?: 0 }

    /** Your own participant state */
    public val me: StateFlow<ParticipantState?> = _participants.mapState { map ->
        map[call.sessionId] ?: participants.value.find { it.isLocal }
    }

    /** Your own participant state */
    public val localParticipant = me

    /** participants who are currently speaking */
    private val _activeSpeakers: MutableStateFlow<List<ParticipantState>> =
        MutableStateFlow(emptyList())
    public val activeSpeakers: StateFlow<List<ParticipantState>> = _activeSpeakers

    /** participants other than yourself */
    public val remoteParticipants: StateFlow<List<ParticipantState>> =
        _participants.mapState { it.filterKeys { key -> key != call.sessionId }.values.toList() }

    /** the dominant speaker */
    private val _dominantSpeaker: MutableStateFlow<ParticipantState?> = MutableStateFlow(null)
    public val dominantSpeaker: StateFlow<ParticipantState?> = _dominantSpeaker

    internal val _localPins: MutableStateFlow<Map<String, PinUpdateAtTime>> =
        MutableStateFlow(emptyMap())
    internal val _serverPins: MutableStateFlow<Map<String, PinUpdateAtTime>> =
        MutableStateFlow(emptyMap())

    internal val _pinnedParticipants: StateFlow<Map<String, OffsetDateTime>> =
        combine(_localPins, _serverPins) { local, server ->
            val combined = mutableMapOf<String, PinUpdateAtTime>()
            combined.putAll(local)
            combined.putAll(server)
            combined.toMap().asIterable().associate {
                Pair(it.key, it.value.at)
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    /**
     * Pinned participants, combined value both from server and local pins.
     */
    val pinnedParticipants: StateFlow<Map<String, OffsetDateTime>> = _pinnedParticipants

    val stats = CallStats(call, scope)

    private val participantsUpdate = TaskSchedulerWithDebounce()
    private val participantsUpdateConfig = ScheduleConfig(
        debounce = {
            val participantCount = participants.value.size
            if (participantCount < 8) {
                0
            } else if (participantCount < 25) {
                250
            } else if (participantCount < 50) {
                500
            } else {
                1000
            }
        },
    )

    private val livestreamFlow: Flow<ParticipantState.Video?> = channelFlow {
        fun emitLivestreamVideo() {
            val participants = participants.value
            val filteredVideo = participants.firstOrNull {
                it.video.value?.enabled == true
            }?.video?.value
            scope.launch {
                if (_backstage.value) {
                    send(null)
                } else {
                    send(filteredVideo)
                }
            }
        }

        scope.launch {
            _participants.collect {
                logger.v {
                    "[livestreamFlow] #track; participants: ${it.size} =>" + "${it.map { "${it.value.userId.value} - ${it.value.video.value?.enabled}" }}"
                }
                emitLivestreamVideo()
            }
        }

        // The caller i.e. `livestream` is deprecated as well
        call.subscribe {
            logger.v { "[livestreamFlow] #track; event.type: ${it.getEventType()}" }
            if (it is TrackPublishedEvent) {
                val participant = getOrCreateParticipant(it.sessionId, it.userId)

                if (it.trackType == TrackType.TRACK_TYPE_VIDEO) {
                    participant._videoEnabled.value = true
                } else if (it.trackType == TrackType.TRACK_TYPE_AUDIO) {
                    participant._audioEnabled.value = true
                }
            }

            if (it is TrackUnpublishedEvent) {
                val participant = getOrCreateParticipant(it.sessionId, it.userId)

                if (it.trackType == TrackType.TRACK_TYPE_VIDEO) {
                    participant._videoEnabled.value = false
                } else if (it.trackType == TrackType.TRACK_TYPE_AUDIO) {
                    participant._audioEnabled.value = false
                }
            }

            emitLivestreamVideo()
        }

        // emit livestream Video
        logger.d { "[livestreamFlow] #track; no args" }
        emitLivestreamVideo()

        awaitClose { }
    }

    @Deprecated(
        message = "The correct approach is to find the participant with video from the participants list or if the id of the user who is host is known query that one directly.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith(
            """
                            call.state.participants.flatMapLatest { participants ->
                                combine(participants.map { p -> p.videoEnabled.map { enabled -> p to enabled } }) { pairs ->
                                    pairs.filter { (_, e) -> e }.map { (p, _) -> p }
                                }
                            }
                         """,
        ),
    )
    val livestream: StateFlow<ParticipantState.Video?> =
        livestreamFlow.debounce(1000).stateIn(scope, SharingStarted.WhileSubscribed(10_000L), null)

    private var _sortedParticipantsState = SortedParticipantsState(
        scope,
        call,
        _participants,
        _pinnedParticipants,
    )

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
    val sortedParticipants = _sortedParticipantsState.asFlow().debounce(100)

    /**
     * Update participant sorting order
     *
     * @param comparator a new comparator to be used in [sortedParticipants] flow.
     */
    fun updateParticipantSortingOrder(
        comparator: Comparator<ParticipantState>,
    ) = _sortedParticipantsState.updateComparator(comparator)

    /** Members contains the list of users who are permanently associated with this call. This includes users who are currently not active in the call
     * As an example if you invite "john", "bob" and "jane" to a call and only Jane joins.
     * All 3 of them will be members, but only Jane will be a participant
     */
    private val _members: MutableStateFlow<SortedMap<String, MemberState>> =
        MutableStateFlow(emptyMap<String, MemberState>().toSortedMap())
    public val members: StateFlow<List<MemberState>> = _members.mapState { it.values.toList() }

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

    private val _individualRecording: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val individualRecording: StateFlow<Boolean> = _individualRecording

    private val _rawRecording: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val rawRecording: StateFlow<Boolean> = _rawRecording

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
            val difference = if (started == null) {
                null
            } else {
                ended.toInstant().toEpochMilli() - started.toInstant().toEpochMilli()
            }
            emit(difference)
        }
    }

    /** how long the call has been running, rounded to seconds, null if the call didn't start yet */
    public val duration: StateFlow<Duration?> =
        _durationInMs.transform { emit(((it ?: 0L) / 1000L).toDuration(DurationUnit.SECONDS)) }
            .stateIn(scope, SharingStarted.WhileSubscribed(10000L), null)

    /** how many milliseconds the call has been running, null if the call didn't start yet */
    public val durationInMs: StateFlow<Long?> =
        _durationInMs.stateIn(scope, SharingStarted.WhileSubscribed(10000L), null)

    /** how many milliseconds the call has been running in the simple date format. */
    public val durationInDateFormat: StateFlow<String?> = durationInMs.mapState { durationInMs ->
        if (durationInMs == null) {
            null
        } else {
            val date = Date(durationInMs)
            val dateFormat = SimpleDateFormat("HH:MM:SS", Locale.US)
            dateFormat.format(date)
        }
    }

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

    /**
     * How long the call has been live for, in milliseconds, or null if the call hasn't been live yet.
     * Keeps its value when live ends and resets when live starts again.
     *
     * @see [liveDuration]
     */
    public val liveDurationInMs = flow {
        while (currentCoroutineContext().isActive) {
            delay(1000)

            val liveStartedAt = _session.value?.liveStartedAt
            val liveEndedAt = _session.value?.liveEndedAt ?: OffsetDateTime.now()

            liveStartedAt?.let {
                val duration = liveEndedAt.toInstant().toEpochMilli() - liveStartedAt.toInstant()
                    .toEpochMilli()
                emit(duration)
            }
        }
    }.distinctUntilChanged().stateIn(scope, SharingStarted.WhileSubscribed(10000L), null)

    /**
     * How long the call has been live for, represented as [Duration], or null if the call hasn't been live yet.
     * Keeps its value when live ends and resets when live starts again.
     *
     * @see [liveDurationInMs]
     */
    public val liveDuration = liveDurationInMs.mapState { durationInMs ->
        durationInMs?.takeIf { it >= 1000 }?.let { (it / 1000).toDuration(DurationUnit.SECONDS) }
    }

    private val _egress: MutableStateFlow<EgressResponse?> = MutableStateFlow(null)
    val egress: StateFlow<EgressResponse?> = _egress

    public val egressPlayListUrl: StateFlow<String?> = egress.mapState {
        it?.hls?.playlistUrl
    }

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

    private val _rejectActionBundle: MutableStateFlow<Bundle?> = MutableStateFlow(null)
    val rejectActionBundle: StateFlow<Bundle?> = _rejectActionBundle

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
        if (it != null) {
            val token = call.clientImpl.token
            val apiKey = call.clientImpl.apiKey
            Ingress(rtmp = RTMP(address = it.rtmp.address, streamKey = token))
        } else {
            null
        }
    }

    private val userToSessionIdMap = participants.mapState { participants ->
        participants.associate { it.userId.value to it.sessionId }
    }

    internal val _hasPermissionMap = mutableMapOf<String, StateFlow<Boolean>>()

    private val _endedAt: MutableStateFlow<OffsetDateTime?> = MutableStateFlow(null)
    val endedAt: StateFlow<OffsetDateTime?> = _endedAt
    private val _endedByUser: MutableStateFlow<User?> = MutableStateFlow(null)
    val endedByUser: StateFlow<User?> = _endedByUser

    internal val _reactions = MutableStateFlow<List<ReactionResponse>>(emptyList())
    val reactions: StateFlow<List<ReactionResponse>> = _reactions

    private val _errors: MutableStateFlow<List<ErrorEvent>> = MutableStateFlow(emptyList())
    public val errors: StateFlow<List<ErrorEvent>> = _errors

    internal val _participantVideoEnabledOverrides =
        MutableStateFlow<Map<String, Boolean?>>(emptyMap())
    val participantVideoEnabledOverrides = _participantVideoEnabledOverrides.asStateFlow()

    private var speakingWhileMutedResetJob: Job? = null
    private var autoJoiningCall: Job? = null
    private var ringingTimerJob: Job? = null

    internal var acceptedOnThisDevice: Boolean = false

    /**
     * This [ClosedCaptionManager] is responsible for handling closed captions during the call.
     * This includes processing events related to closed captions and maintaining their state.
     */
    internal val closedCaptionManager = ClosedCaptionManager()

    public val moderationManager = ModerationManager(call)

    /**
     * Tracks whether closed captioning is currently active for the call.
     * True if captioning is ongoing, false otherwise.
     */
    public val isCaptioning: StateFlow<Boolean> = closedCaptionManager.closedCaptioning

    /**
     * Holds the current list of closed captions. This list is updated dynamically
     * and contains at most [ClosedCaptionsSettings.maxVisibleCaptions] captions.
     */
    public val closedCaptions: StateFlow<List<CallClosedCaption>> =
        closedCaptionManager.closedCaptions

    /**
     *  Holds the current closed caption mode for the video call. This object contains information about closed
     *  captioning feature availability. This state is updated dynamically based on the server's transcription
     *  setting which is [io.getstream.android.video.generated.models.TranscriptionSettingsResponse.closedCaptionMode]
     *
     *  Possible values:
     *  - [ClosedCaptionMode.Available]: Closed captions are available and can be enabled.
     *  - [ClosedCaptionMode.Disabled]: Closed captions are explicitly disabled.
     *  - [ClosedCaptionMode.AutoOn]: Closed captions are automatically enabled as soon as user joins the call
     *  - [ClosedCaptionMode.Unknown]: Represents an unrecognized or unsupported mode.
     */
    val ccMode: StateFlow<ClosedCaptionMode> = closedCaptionManager.ccMode

    private val pendingParticipantsJoined = ConcurrentHashMap<String, Participant>()

    /**
     * We re-create notification more than 1 times, so we don't want to
     * overwrite to the notifications builder properties once it is already set
     */
    internal val atomicNotification: AtomicReference<Notification?> =
        AtomicReference<Notification?>(null)

    private var _notificationIdFlow = MutableStateFlow<Int?>(null)
    internal val notificationIdFlow: StateFlow<Int?> = _notificationIdFlow

    @InternalStreamVideoApi
    internal var jetpackTelecomRepository: JetpackTelecomRepository? = null

    internal var incomingNotificationData = IncomingNotificationData(emptyMap())

    fun handleEvent(event: VideoEvent) {
        logger.d { "[handleEvent] ${event::class.java.name.split(".").last()}" }

        when (event) {
            is BlockedUserEvent -> {
                val newBlockedUsers = _blockedUsers.value.toMutableSet()
                newBlockedUsers.add(event.user.id)
                _blockedUsers.value = newBlockedUsers
            }

            is PinsUpdatedEvent -> {
                updateServerSidePins(event.pins)
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
                if (callRingState is RingingState.Outgoing && callRingState.acceptedByCallee && _acceptedBy.value.findLast {
                        it == client.userId
                    } == null && client.state.activeCall.value == null && autoJoiningCall == null
                ) {
                    autoJoiningCall = scope.launch {
                        // errors are handled inside the join function
                        call.join()
                        autoJoiningCall = null
                    }
                } else if (callRingState is RingingState.Incoming && event.user.id == client.userId) {
                    // Call accepted by me + this device is Incoming => I accepted on another device
                    // Then leave the call on this device
                    if (!acceptedOnThisDevice) call.leave("accepted-on-another-device")
                }
                call.fireEvent(
                    LocalCallAcceptedPostEvent(
                        event.callCid,
                        event.createdAt,
                        event.call,
                        event.user,
                        event.type,
                    ),
                )
            }

            is CallMissedEvent -> {
                _createdBy.value = event.call.createdBy.toUser()
            }

            is CallRejectedEvent -> {
                _createdBy.value = event.call.createdBy.toUser()
                val new = _rejectedBy.value.toMutableSet()
                new.add(event.user.id)
                _rejectedBy.value = new.toSet()
                updateRingingState(
                    rejectReason = event.reason?.let {
                        when (it) {
                            RejectReason.Busy.alias -> RejectReason.Busy
                            RejectReason.Cancel.alias -> RejectReason.Cancel
                            RejectReason.Decline.alias -> RejectReason.Decline
                            else -> RejectReason.Custom(alias = it)
                        }
                    },
                )
                call.fireEvent(
                    LocalCallRejectedPostEvent(
                        event.callCid,
                        event.createdAt,
                        event.call,
                        event.user,
                        event.type,
                        event.reason,
                    ),
                )
            }

            is LocalCallMissedEvent -> {
                scope.launch {
                    val newRejectedBySet = _rejectedBy.value.toMutableSet()
                    StreamVideo.instanceOrNull()?.let {
                        newRejectedBySet.add(it.user.id)
                    }
                    _rejectedBy.value = newRejectedBySet.toSet()
                    _ringingState.value = RingingState.RejectedByAll
                    call.leave("LocalCallMissedEvent")

                    val activeCallExists = client.state.activeCall.value != null
                    if (activeCallExists) {
                        // Another call is active - just remove incoming notification
                        val streamCallId = StreamCallId(call.type, call.id)
                        NotificationManagerCompat.from(client.context)
                            .cancel(streamCallId.getNotificationId(NotificationType.Incoming))
                    } else {
                        // No other call - stop service
                        client.state.maybeStopForegroundService(call)
                    }
                }
            }

            is CallEndedEvent -> {
                call.state.cancelTimeout()
                updateFromResponse(event.call)
                _endedAt.value = OffsetDateTime.now(Clock.systemUTC())
                _endedByUser.value = event.user?.toUser()
                call.leave("CallEndedEvent")
            }

            is CallEndedSfuEvent -> {
                _endedAt.value = OffsetDateTime.now(Clock.systemUTC())
                call.leave("CallEndedSfuEvent")
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
                getOrCreateMembers(event.members)
                updateFromResponse(event.call)
            }

            is CallRingEvent -> {
                getOrCreateMembers(event.members)
                updateFromResponse(event.call)

                // Fill caller in members if not present
                val memberMap = _members.value.toSortedMap()
                if (!memberMap.contains(event.call.createdBy.id)) {
                    memberMap[event.call.createdBy.id] = MemberState(
                        user = event.call.createdBy.toUser(),
                        role = event.call.createdBy.role,
                        custom = emptyMap(),
                        createdAt = event.call.createdAt,
                        updatedAt = event.call.updatedAt,
                    )
                    _members.value = memberMap
                }
            }

            is CallUpdatedEvent -> {
                updateFromResponse(event.call)
                _capabilitiesByRole.value = event.capabilitiesByRole
            }

            is UpdatedCallPermissionsEvent -> {
                if (event.user.id == client.userId) {
                    _ownCapabilities.value = event.ownCapabilities
                }
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
                when (event.recordingType) {
                    CallRecordingStartedEvent.RecordingType.Individual ->
                        _individualRecording.value =
                            true

                    CallRecordingStartedEvent.RecordingType.Raw -> _rawRecording.value = true
                    CallRecordingStartedEvent.RecordingType.Composite -> _recording.value = true
                    else -> {}
                }
            }

            is CallRecordingStoppedEvent -> {
                when (event.recordingType) {
                    CallRecordingStoppedEvent.RecordingType.Individual ->
                        _individualRecording.value =
                            false

                    CallRecordingStoppedEvent.RecordingType.Raw -> _rawRecording.value = false
                    CallRecordingStoppedEvent.RecordingType.Composite -> _recording.value = false
                    else -> {}
                }
            }

            is CallLiveStartedEvent -> {
                updateFromResponse(event.call)
            }

            is AudioLevelChangedEvent -> {
                event.levels.forEach { entry ->
                    val participant = getOrCreateParticipant(entry.key, entry.value.userId)
                    participant._speaking.value = entry.value.isSpeaking
                    participant.updateAudioLevel(entry.value.audioLevel)
                }

                _activeSpeakers.value = participants.value.filter { it.speaking.value }
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
                call.session?.handleEvent(event)
            }

            is ErrorEvent -> {
                _errors.value = errors.value + event
            }

            is SFUHealthCheckEvent -> {
                updateParticipantCounts(sfuHealthCheckEvent = event)
            }

            is ICETrickleEvent -> {
                // handled by ActiveSFUSession
            }

            is JoinCallResponseEvent -> {
                // time to update call state based on the join response
                updateFromJoinResponse(event)
                if (!ringingStateUpdatesStopped) {
                    updateRingingState()
                } else {
                    _ringingState.value = RingingState.Outgoing(acceptedByCallee = true)
                }
                updateServerSidePins(
                    event.callState.pins.map {
                        PinUpdate(it.user_id, it.session_id)
                    },
                )
            }

            is ParticipantJoinedEvent -> {
                try {
                    if (participants.value.size < 8) {
                        getOrCreateParticipant(event.participant)
                    } else {
                        pendingParticipantsJoined[event.participant.session_id] = event.participant
                        participantsUpdate.schedule(scope, participantsUpdateConfig) {
                            logger.d {
                                "[ParticipantJoinedEvent] #participants; #debounce; participants: ${participants.value.size}"
                            }
                            getOrCreateParticipants(pendingParticipantsJoined.values.toList())
                        }
                    }
                } catch (e: Exception) {
                    logger.e(e) {
                        "[ParticipantJoinedEvent] #participants; #debounce; Failed to debounce, processing as usual."
                    }
                    getOrCreateParticipant(event.participant)
                }
            }

            is ParticipantLeftEvent -> {
                safeCall { pendingParticipantsJoined.remove(event.participant.session_id) }
                val sessionId = event.participant.session_id
                removeParticipant(sessionId)

                // clean up - stop screen-sharing session if it was still running
                val current = _screenSharingSession.value
                if (current?.participant?.sessionId == sessionId) {
                    _screenSharingSession.value = null
                }
                if (_localPins.value.containsKey(sessionId)) {
                    // Remove any pins for the participant
                    unpin(sessionId)
                }

                if (_serverPins.value.containsKey(sessionId)) {
                    scope.launch {
                        call.unpinForEveryone(sessionId, event.participant.user_id)
                    }
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
                updateFromResponse(event.call)
            }

            is CallSessionEndedEvent -> {
                _session.value = event.call.session
                updateFromResponse(event.call)
            }

            is CallSessionParticipantCountsUpdatedEvent -> {
                _session.value?.let {
                    _session.value = it.copy(
                        participantsCountByRole = event.participantsCountByRole,
                        anonymousParticipantCount = event.anonymousParticipantCount,
                    )
                }

                updateParticipantCounts(session = session.value)
            }

            is CallSessionParticipantLeftEvent -> {
                _session.value?.let { callSessionResponse ->
                    val newList = callSessionResponse.participants.toMutableList()
                    newList.removeIf { it.userSessionId == event.participant.userSessionId }

                    val newMap = callSessionResponse.participantsCountByRole.toMutableMap()
                    newMap.computeIfPresent(event.participant.role) { _, v -> maxOf(v - 1, 0) }
                        .also { if (it == 0) newMap.remove(event.participant.role) }

                    _session.value = callSessionResponse.copy(
                        participants = newList,
                        participantsCountByRole = newMap,
                    )
                }

                updateParticipantCounts(session = session.value)
            }

            is CallSessionParticipantJoinedEvent -> {
                _session.value?.let { callSessionResponse ->
                    val newList = callSessionResponse.participants.toMutableList()
                    val participant = CallParticipantResponse(
                        user = event.participant.user,
                        joinedAt = event.createdAt,
                        role = event.participant.user.role,
                        userSessionId = event.participant.userSessionId,
                    )
                    val newMap = callSessionResponse.participantsCountByRole.toMutableMap()
                    newMap.merge(event.participant.role, 1, Int::plus)

                    // It could happen that the backend delivers the same participant more than once.
                    // Once with the call.session_started event and once again with the
                    // call.session_participant_joined event. In this case,
                    // we should update the existing participant and prevent duplicating it.
                    val index = newList.indexOfFirst { user.id == event.participant.user.id }
                    if (index == -1) {
                        newList.add(participant)
                    } else {
                        newList[index] = participant
                    }

                    _session.value = callSessionResponse.copy(
                        participants = newList,
                        participantsCountByRole = newMap,
                    )
                }

                updateParticipantCounts(session = session.value)
                updateRingingState()
            }

            is CallTranscriptionStartedEvent -> {
                _transcribing.value = true
            }

            is CallTranscriptionStoppedEvent -> {
                _transcribing.value = false
            }

            is CallTranscriptionFailedEvent -> {
                _transcribing.value = false
            }

            is CallClosedCaptionsStartedEvent,
            is ClosedCaptionEvent,
            is CallClosedCaptionsStoppedEvent,
            -> closedCaptionManager.handleEvent(event)

            is CallModerationBlurEvent -> {
                scope.launch {
                    val callServiceConfig = StreamVideo.instanceOrNull()?.state?.callConfigRegistry?.get(call.type) ?: CallServiceConfig()
                    if (callServiceConfig.moderationConfig.videoModerationConfig.enable) {
                        call.state.moderationManager.applyVideoModeration()
                        delay(callServiceConfig.moderationConfig.videoModerationConfig.blurDuration)
                        call.state.moderationManager.clearVideoModeration()
                    }
                }
            }
        }
    }

    private fun updateServerSidePins(pins: List<PinUpdate>) {
        // Update participants that are still in the call
        val pinnedInCall = pins.filter {
            internalParticipants.containsKey(it.sessionId)
        }
        _serverPins.value = pinnedInCall.associate {
            Pair(
                it.sessionId,
                PinUpdateAtTime(it, OffsetDateTime.now(Clock.systemUTC()), PinType.Server),
            )
        }
    }

    private fun updateRingingState(rejectReason: RejectReason? = null) {
        // this is only true when we are in the session (we have accepted/joined the call)
        val rejectedBy = _rejectedBy.value
        val isRejectedByMe = _rejectedBy.value.contains(client.userId)
        val acceptedBy = _acceptedBy.value
        val isAcceptedByMe = _acceptedBy.value.contains(client.userId)
        val createdBy = _createdBy.value
        val hasActiveCall =
            client.state.activeCall.value != null && client.state.activeCall.value?.id == call.id
        val hasRingingCall =
            client.state.ringingCall.value != null && client.state.ringingCall.value?.id == call.id
        val userIsParticipant =
            _session.value?.participants?.find { it.user.id == client.userId } != null
        val outgoingMembersCount = _members.value.filter { it.value.user.id != client.userId }.size

        Log.d("RingingState", "Current: ${_ringingState.value}, call_id: ${call.cid}")
        Log.d(
            "RingingState",
            "call_id: ${call.cid}, Flags: [\n" + "acceptedByMe: $isAcceptedByMe,\n" + "rejectedByMe: $isRejectedByMe,\n" + "rejectReason: $rejectReason,\n" + "hasActiveCall: $hasActiveCall\n" + "hasRingingCall: $hasRingingCall\n" + "userIsParticipant: $userIsParticipant,\n" + "]",
        )

        // no members - call is empty, we can join
        val state: RingingState = if (hasActiveCall && !ringingStateUpdatesStopped) {
            cancelTimeout()
            RingingState.Active
        } else if ((rejectedBy.isNotEmpty() && rejectedBy.size >= outgoingMembersCount) ||
            (rejectedBy.contains(createdBy?.id) && hasRingingCall)
        ) {
            call.leave("updateRingingState-rejected")
            cancelTimeout()

            if (rejectReason?.alias == REJECT_REASON_TIMEOUT) {
                RingingState.TimeoutNoAnswer
            } else {
                RingingState.RejectedByAll
            }
        } else if (hasRingingCall && createdBy?.id != client.userId) {
            // Member list is not empty, it's not rejected - it's an incoming call
            // If it's already accepted by me then we are in an Active call
            if (userIsParticipant) {
                cancelTimeout()
                RingingState.Active
            } else {
                RingingState.Incoming(acceptedByMe = isAcceptedByMe)
            }
        } else if (hasRingingCall && createdBy?.id == client.userId) {
            // The call is created by us
            logger.d { "acceptedBy: $acceptedBy, userIsParticipant: $userIsParticipant" }
            if (acceptedBy.isEmpty()) {
                // no one accepted the call
                RingingState.Outgoing(acceptedByCallee = false)
            } else if (!userIsParticipant) {
                // someone already accepted the call, but it's not us (client needs to do call.join)
                RingingState.Outgoing(acceptedByCallee = true)
            } else {
                // call is accepted and we are already in the call
                ringingStateUpdatesStopped = false
                cancelTimeout()
                RingingState.Active
            }
        } else {
            if (_ringingState.value is RingingState.Incoming && !acceptedOnThisDevice) {
                RingingState.TimeoutNoAnswer
            } else {
                RingingState.Idle
            }
        }

        if (_ringingState.value != state) {
            logger.d { "Updating ringing state ${_ringingState.value} -> $state" }

            // handle the auto-cancel for outgoing ringing calls
            if (state is RingingState.Outgoing && !state.acceptedByCallee) {
                startRingingTimer()
            } else if (state is RingingState.Incoming && !state.acceptedByMe) {
                startRingingTimer()
            } else {
                cancelTimeout()
            }

            // stop the call ringing timer if it's running
        }
        Log.d("RingingState", "Update: $state")

        _ringingState.value = state
    }

    @InternalStreamVideoApi
    fun cancelTimeout() {
        ringingTimerJob?.cancel()
        ringingTimerJob = null
    }

    private fun updateFromJoinResponse(event: JoinCallResponseEvent) {
        // update the participant count
        _participantCounts.value = event.participantCount
        // creates the participants
        val participantStates = event.callState.participants.map {
            getOrCreateParticipant(it)
        }
        upsertParticipants(participantStates)
    }

    private fun updateParticipantCounts(
        session: CallSessionResponse? = null,
        sfuHealthCheckEvent: SFUHealthCheckEvent? = null,
    ) {
        // When in JOINED state, we should use the participant from SFU health check event, as it's more accurate.

        if (sfuHealthCheckEvent != null) {
            _participantCounts.value = sfuHealthCheckEvent.participantCount
        } else if (session != null && connection.value !is RealtimeConnection.Joined) {
            _participantCounts.value = ParticipantCount(
                total = session.anonymousParticipantCount + session.participantsCountByRole.values.sum(),
                anonymous = session.anonymousParticipantCount,
            )
        }
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
        ringingTimerJob = UserScope(ClientScope()).launch {
            val autoCancelTimeout = settings.value?.ring?.autoCancelTimeoutMs

            if (autoCancelTimeout != null && autoCancelTimeout > 0) {
                delay(autoCancelTimeout.toLong())

                // double check that we are still in Outgoing call state and call is not active
                if (_ringingState.value is RingingState.Outgoing || _ringingState.value is RingingState.Incoming && client.state.activeCall.value == null) {
                    ringingStateUpdatesStopped = false
                    call.reject(reason = RejectReason.Custom(alias = REJECT_REASON_TIMEOUT))
                    call.leave("start-ringing-timeout")
                }
            } else {
                logger.w { "[startRingingTimer] No autoCancelTimeoutMs set - call ring with no timeout" }
            }
        }
    }

    internal fun removeParticipant(sessionId: String) {
        internalParticipants.remove(sessionId)
        _participants.value = HashMap(internalParticipants)
    }

    public fun upsertParticipants(participants: List<ParticipantState>) {
        val screensharing = mutableListOf<ParticipantState>()
        participants.forEach {
            internalParticipants[it.sessionId] = it

            if (it.screenSharingEnabled.value) {
                screensharing.add(it)
            }
        }
        _participants.value = HashMap(internalParticipants)

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
        if (participant.session_id.isEmpty()) {
            // Empty session ID is technically allowed but should not happen.
            logger.w { "A user [id:${participant.user_id}] is in the call with empty session_id" }
        }

        val participantState = getOrCreateParticipant(
            participant.session_id,
            participant.user_id,
            false,
            participant.source,
        )
        participantState.updateFromParticipantInfo(participant)

        upsertParticipants(listOf(participantState))

        return participantState
    }

    fun getOrCreateParticipant(
        sessionId: String,
        userId: String,
        updateFlow: Boolean = false,
        source: ParticipantSource = ParticipantSource.PARTICIPANT_SOURCE_WEBRTC_UNSPECIFIED,
    ): ParticipantState {
        val participantState = if (internalParticipants.containsKey(sessionId)) {
            internalParticipants[sessionId]!!
        } else {
            ParticipantState(
                sessionId = sessionId,
                scope = scope,
                callActions = callActions,
                initialUserId = userId,
                source = source,
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
        return internalParticipants[sessionId]
    }

    fun updateParticipant(participant: ParticipantState) {
        internalParticipants[participant.sessionId] = participant
        _participants.value = HashMap(internalParticipants)
    }

    fun clearParticipants() {
        internalParticipants.clear()
        _participants.value = HashMap(internalParticipants)
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
        didUpdateSession(response.session)

        updateRingingState()
        closedCaptionManager.handleCallUpdate(response)
    }

    /**
     * Called after session changes
     */
    private fun didUpdateSession(newSession: CallSessionResponse?) {
        _session.value = newSession

        if (newSession?.startedAt != null) {
            _startedAt.value = newSession.startedAt
        } else if (newSession?.liveStartedAt != null) {
            _startedAt.value = newSession.liveStartedAt
        }

        if (newSession?.liveEndedAt != null) {
            _endedAt.value = newSession.liveEndedAt
        }
        // Also unify participant counts if needed:
        updateParticipantCounts(session = newSession)
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
        _ownCapabilities.value = response.ownCapabilities
        updateFromResponse(response.members)
        updateFromResponse(response.call)
    }

    fun getMember(userId: String): MemberState? {
        return _members.value[userId]
    }

    fun updateFromResponse(callData: CallStateResponseFields) {
        updateFromResponse(callData.call)
        _ownCapabilities.value = callData.ownCapabilities
        updateFromResponse(callData.members)
    }

    fun updateFromResponse(it: QueryCallMembersResponse) {
        updateFromResponse(it.members)
    }

    fun pin(userId: String, sessionId: String) {
        val pins = _localPins.value.toMutableMap()
        pins[sessionId] = PinUpdateAtTime(
            PinUpdate(userId, sessionId),
            OffsetDateTime.now(Clock.systemUTC()),
            PinType.Local,
        )
        _localPins.value = pins
    }

    fun unpin(sessionId: String) {
        val pins = _localPins.value.toMutableMap()
        pins.remove(sessionId)
        _localPins.value = pins
    }

    fun updateFromResponse(result: StopLiveResponse) {
        updateFromResponse(result.call)
    }

    fun updateFromResponse(result: GoLiveResponse) {
        updateFromResponse(result.call)
    }

    fun updateFromResponse(response: StartHLSBroadcastingResponse) {
        val curEgress = _egress.value
        logger.d { "[updateFromResponse] response: $response, curEgress: $curEgress" }
        val newEgress = curEgress?.copy(
            broadcasting = true,
            hls = curEgress.hls?.copy(
                playlistUrl = response.playlistUrl,
            ) ?: EgressHLSResponse(playlistUrl = response.playlistUrl, ""),
        ) ?: EgressResponse(
            broadcasting = true,
            rtmps = emptyList(),
            hls = EgressHLSResponse(playlistUrl = response.playlistUrl, ""),
        )
        logger.v { "[updateFromResponse] newEgress: $newEgress" }
        _egress.value = newEgress
        _broadcasting.value = true
    }

    private var ringingStateUpdatesStopped = false

    internal fun toggleRingingStateUpdates(stopped: Boolean) {
        ringingStateUpdatesStopped = stopped
    }

    /**
     * Update participants visibility on the UI.
     *
     * @param sessionId the session ID of the participant.
     * @param visibilityOnScreenState the visibility state.
     *
     * @see VisibilityOnScreenState
     * @see CallState.updateParticipantVisibilityFlow
     */
    fun updateParticipantVisibility(
        sessionId: String,
        visibilityOnScreenState: VisibilityOnScreenState,
    ) {
        internalParticipants[sessionId]?._visibleOnScreen?.value = visibilityOnScreenState
        _participants.value = HashMap(internalParticipants)
    }

    /**
     * Set a flow to update the participants visibility.
     * The flow should emit lists with currently visible participant session IDs.
     *
     * Note: If you pass null to the parameter it will just cancel the currently observing flow.
     *
     * E.g. Grid visible items info can be used to update the [CallState]
     * ```
     * val gridState = rememberLazyGridState()
     * val updateFlow = snapshotFlow {
     *      gridState.layoutInfo.visibleItemsInfo.map {
     *          it.key // Assuming keys are sessionId
     *      }
     * }
     *
     * call.state.updateParticipantVisibilityFlow(updateFlow)
     * ```
     *
     * @param flow a flow that emits updates with list of visible participants.
     *
     * @see CallState.updateParticipantVisibility
     */
    fun updateParticipantVisibilityFlow(flow: Flow<List<String>>?) {
        // Cancel any previous job.
        participantsVisibilityMonitor?.cancel()

        if (flow != null) {
            participantsVisibilityMonitor = scope.launch {
                flow.collectLatest { visibleParticipantIds ->
                    internalParticipants.forEach {
                        if (visibleParticipantIds.contains(it.key)) {
                            // If participant is in the lists its visible
                            it.value._visibleOnScreen.value = VisibilityOnScreenState.VISIBLE
                        } else {
                            // Participant is not in the list, thus invisible
                            it.value._visibleOnScreen.value = VisibilityOnScreenState.INVISIBLE
                        }
                    }
                    _participants.value = HashMap(internalParticipants)
                }
            }
        }
    }

    fun replaceParticipants(participants: List<ParticipantState>) {
        internalParticipants.clear()
        participants.forEach { participant ->
            internalParticipants[participant.sessionId] = participant
        }
        _participants.value = HashMap(internalParticipants)

        val screensharing = mutableListOf<ParticipantState>()
        participants.forEach {
            if (it.screenSharingEnabled.value) {
                screensharing.add(it)
            }
        }
        _screenSharingSession.value = if (screensharing.isNotEmpty()) {
            ScreenSharingSession(
                screensharing[0],
            )
        } else {
            null
        }
    }

    fun updateRejectedBy(userId: Set<String>) {
        _rejectedBy.value = userId
    }

    fun updateRejectActionBundle(bundle: Bundle) {
        _rejectActionBundle.value = bundle
    }

    @Deprecated("Use updateNotification(Int, Notification) instead")
    fun updateNotification(notification: Notification) {
        atomicNotification.set(notification)
    }

    fun updateNotification(notificationId: Int, notification: Notification) {
        this._notificationIdFlow.value = notificationId
        this.atomicNotification.set(notification)
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

private const val REJECT_REASON_TIMEOUT = "timeout"
