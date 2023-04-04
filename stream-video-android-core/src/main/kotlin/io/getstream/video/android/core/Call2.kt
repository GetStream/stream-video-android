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

import io.getstream.log.TaggedLogger
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.call.ActiveSFUSession
import io.getstream.video.android.core.events.*
import io.getstream.video.android.core.events.BlockedUserEvent
import io.getstream.video.android.core.events.CallAcceptedEvent
import io.getstream.video.android.core.events.CallCancelledEvent
import io.getstream.video.android.core.events.CallCreatedEvent
import io.getstream.video.android.core.events.CallEndedEvent
import io.getstream.video.android.core.events.CallRejectedEvent
import io.getstream.video.android.core.events.CallUpdatedEvent
import io.getstream.video.android.core.events.PermissionRequestEvent
import io.getstream.video.android.core.events.UnblockedUserEvent
import io.getstream.video.android.core.events.UpdatedCallPermissionsEvent
import stream.video.sfu.models.Participant as ParticipantData
import io.getstream.video.android.core.model.*
import io.getstream.video.android.core.utils.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.openapitools.client.models.*
import java.util.*

public data class SFUConnection(
    internal val callUrl: String,
    internal val sfuToken: SfuToken,
    internal val iceServers: List<IceServer>
)

/**
 *
 */
public class CallState(val call: Call2, user: User) {
    private val logger by taggedLogger("CallState")

    private val memberMap: MutableMap<String, MemberState> = mutableMapOf()
    private val _recording: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording

    /**
     * connectionState shows if we've established a connection with the coordinator
     */
    private val _connection: MutableStateFlow<io.getstream.video.android.core.ConnectionState> = MutableStateFlow(
        io.getstream.video.android.core.ConnectionState.PreConnect)
    public val connection: StateFlow<io.getstream.video.android.core.ConnectionState> = _connection

    private val _endedAt: MutableStateFlow<Date?> = MutableStateFlow(null)
    val endedAt: StateFlow<Date?> = _endedAt
    private val _endedByUser: MutableStateFlow<User?> = MutableStateFlow(null)
    val endedByUser: StateFlow<User?> = _endedByUser

    private val participantMap = mutableMapOf<String, ParticipantState>()

    private val _capabilitiesByRole: MutableStateFlow<Map<String, List<String>>> = MutableStateFlow(emptyMap())
    val capabilitiesByRole: StateFlow<Map<String, List<String>>> = _capabilitiesByRole



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
                me._ownCapabilities.value=event.ownCapabilities
            }
            is ConnectedEvent -> TODO()
            is CustomEvent -> TODO()
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
            is ChangePublishQualityEvent -> TODO()
            is ErrorEvent -> TODO()
            SFUHealthCheckEvent -> TODO()
            is ICETrickleEvent -> TODO()
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

    private fun getOrCreateParticipant(participant: ParticipantData): ParticipantState {
        // TODO: update some fields
        val participantState = getOrCreateParticipant(participant.user_id)
        participantState.updateFromData(participant)

        participantState._speaking.value = participant.is_speaking
        return participantState
    }

    private fun getOrCreateParticipant(user: User): ParticipantState {
        // TODO: maybe update some fields
        return getOrCreateParticipant(user.id)
    }

    private fun getOrCreateParticipant(userId: String): ParticipantState {
        return if (participantMap.contains(userId)) {
            participantMap[userId]!!
        } else {
            val participant = ParticipantState(call, User(id=userId))
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
            val member = MemberState(User(id=userId))
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
            me._ownCapabilities.value=event.ownCapabilities
            // update the capabilities by role
            _capabilitiesByRole.value = event.capabilitiesByRole
            // update call info fields
        }

    }

    // TODO: SFU Connection

    private val _participants: MutableStateFlow<List<ParticipantState>> =
        MutableStateFlow(emptyList())
    public val participants: StateFlow<List<ParticipantState>> = _participants

    private val _dominantSpeaker: MutableStateFlow<ParticipantState?> =
        MutableStateFlow(null)
    public val dominantSpeaker: StateFlow<ParticipantState?> = _dominantSpeaker


    /**
     * TODO:
     * - activeSpeakers
     * - sortedSpeakers
     * - primarySpeaker
     * - screenSharingSessions
     */

    private val _members: MutableStateFlow<List<ParticipantState>> =
        MutableStateFlow(emptyList())
    public val members: StateFlow<List<ParticipantState>> = _members

    val me = LocalParticipantState(call, user)
}

public class Call2(
    private val client: StreamVideo,
    val type: String,
    val id: String,
    private val token: String = "",
    val user: User,
) {
    private val clientImpl = client as StreamVideoImpl
    var activeSession: ActiveSFUSession? = null
    val cid = "$type:$id"
    val state = CallState(this, user)

    public var custom: Map<String, Any>? = null

    // should be a stateflow
    private var sfuConnection: SFUConnection? = null

    suspend fun join(): Result<ActiveSFUSession> {

        /**
         * Alright, how to make this solid
         *
         * - There are 2 methods.
         * -- Client.JoinCall which makes the API call and gets a response
         * -- The whole join process. Which measures latency, uploads it etc
         *
         * Latency measurement needs to be changed
         *
         */

        // step 1. call the join endpoint to get a list of SFUs
        val result = client.joinCall(type, id)
        if (result !is Success) {
            return result as Failure
        }

        // step 2. measure latency
        // TODO: setup the initial call state based on this
        println(result.data.call.settings)

        val edgeUrls = result.data.edges.map { it.latencyUrl }
        // measure latency in parallel
        val measurements = clientImpl.measureLatency(edgeUrls)

        // upload our latency measurements to the server
        val selectEdgeServerResult = client.selectEdgeServer(
            type = type,
            id = id,
            request = GetCallEdgeServerRequest(
                latencyMeasurements = measurements.associate { it.latencyUrl to it.measurements }
            )
        )
        if (selectEdgeServerResult !is Success) {
            return result as Failure
        }

        val credentials = selectEdgeServerResult.data.credentials
        val url = credentials.server.url
        val iceServers =
            selectEdgeServerResult
                .data
                .credentials
                .iceServers
                .map { it.toIceServer() }

        activeSession = ActiveSFUSession(
            client=client, call2=this,
            SFUUrl =url, SFUToken=credentials.token,
            connectionModule = (client as StreamVideoImpl).connectionModule,
            remoteIceServers=iceServers, latencyResults=measurements.associate { it.latencyUrl to it.measurements }
        )
        return Success<ActiveSFUSession>(data= activeSession!!)

    }

    suspend fun sendReaction(data: SendReactionData): Result<SendReactionResponse> {
        return client.sendReaction(type, id, data)
    }

    suspend fun goLive(): Result<GoLiveResponse> {
        return client.goLive(type, id)
    }
    suspend fun stopLive(): Result<StopLiveResponse> {
        return client.stopLive(type, id)
    }

    fun leave() {
    }

    suspend fun end(): Result<Unit> {
        return client.endCall(type, id)
    }

    /** Basic crud operations */
    suspend fun get(): Result<CallMetadata> {
        return client.getOrCreateCall(type, id)
    }
    suspend fun create(): Result<CallMetadata> {
        return client.getOrCreateCall(type, id)
    }
    suspend fun update(): Result<UpdateCallResponse> {
        return client.updateCall(type, id, custom ?: emptyMap())
    }

    /** Permissions */
    suspend fun requestPermissions(permissions: List<String>): Result<Unit> {
        return client.requestPermissions(type, id, permissions)
    }

    suspend fun startRecording(): Result<Any> {
        return client.startRecording(type, id)
    }
    suspend fun stopRecording(): Result<Any> {
        return client.stopRecording(type, id)
    }

    suspend fun startBroadcasting(): Result<Any> {
        return client.startBroadcasting(type, id)
    }
    suspend fun stopBroadcasting(): Result<Any> {
        return client.stopBroadcasting(type, id)
    }

    private var subscriptions = mutableSetOf<EventSubscription>()

    public fun subscribeFor(
        vararg eventTypes: Class<out VideoEvent>,
        listener: VideoEventListener<VideoEvent>,
    ): EventSubscription {
        val filter = { event: VideoEvent ->
            eventTypes.any { type -> type.isInstance(event) }
        }
        val sub = EventSubscription(listener, filter)
        subscriptions.add(sub)
        return sub
    }

    public fun subscribe(
        listener: VideoEventListener<VideoEvent>
    ): EventSubscription {
        val sub = EventSubscription(listener)
        subscriptions.add(sub)
        return sub
    }

    fun fireEvent(event: VideoEvent) {
        subscriptions.forEach { sub ->
            if (!sub.isDisposed) {
                // subs without filters should always fire
                if (sub.filter == null) {
                    sub.listener.onEvent(event)
                }

                // if there is a filter, check it and fire if it matches
                sub.filter?.let {
                    if (it.invoke(event)) {
                        sub.listener.onEvent(event)
                    }
                }
            }
        }
    }
}
