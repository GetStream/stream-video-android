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

import io.getstream.video.android.core.events.AudioLevelChangedEvent
import io.getstream.video.android.core.events.BlockedUserEvent
import io.getstream.video.android.core.events.CallAcceptedEvent
import io.getstream.video.android.core.events.CallCancelledEvent
import stream.video.sfu.models.Participant as ParticipantData
import io.getstream.video.android.core.events.CallCreatedEvent
import io.getstream.video.android.core.events.CallEndedEvent
import io.getstream.video.android.core.events.CallMembersDeletedEvent
import io.getstream.video.android.core.events.CallMembersUpdatedEvent
import io.getstream.video.android.core.events.CallRejectedEvent
import io.getstream.video.android.core.events.CallUpdatedEvent
import io.getstream.video.android.core.events.ChangePublishQualityEvent
import io.getstream.video.android.core.events.ConnectedEvent
import io.getstream.video.android.core.events.ConnectionQualityChangeEvent
import io.getstream.video.android.core.events.CustomEvent
import io.getstream.video.android.core.events.DominantSpeakerChangedEvent
import io.getstream.video.android.core.events.ErrorEvent
import io.getstream.video.android.core.events.CoordinatorHealthCheckEvent
import io.getstream.video.android.core.events.SFUHealthCheckEvent
import io.getstream.video.android.core.events.ICETrickleEvent
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.events.ParticipantJoinedEvent
import io.getstream.video.android.core.events.ParticipantLeftEvent
import io.getstream.video.android.core.events.PermissionRequestEvent
import io.getstream.video.android.core.events.PublisherAnswerEvent
import io.getstream.video.android.core.events.RecordingStartedEvent
import io.getstream.video.android.core.events.RecordingStoppedEvent
import io.getstream.video.android.core.events.SubscriberOfferEvent
import io.getstream.video.android.core.events.TrackPublishedEvent
import io.getstream.video.android.core.events.TrackUnpublishedEvent
import io.getstream.video.android.core.events.UnblockedUserEvent
import io.getstream.video.android.core.events.UnknownEvent
import io.getstream.video.android.core.events.UpdatedCallPermissionsEvent
import io.getstream.video.android.core.events.VideoEvent
import io.getstream.video.android.core.events.VideoQualityChangedEvent
import io.getstream.video.android.core.model.*
import io.getstream.video.android.core.utils.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.openapitools.client.models.GetCallEdgeServerRequest
import org.openapitools.client.models.GoLiveResponse
import org.openapitools.client.models.OwnCapability
import org.openapitools.client.models.UpdateCallResponse
import stream.video.sfu.models.ConnectionQuality
import java.util.*

public data class SFUConnection(
    internal val callUrl: String,
    internal val sfuToken: SfuToken,
    internal val iceServers: List<IceServer>
)

public open class ActiveSFUSession(
    client: StreamVideo,
    call2: Call2,
    url: String,
    token: String,
    iceServers: List<IceServer>,
    latencyResults: Map<String, List<Float>>
) {

}


public open class ParticipantState(user: User) {

    /**
     * The user
     */
    internal val _user: MutableStateFlow<User> = MutableStateFlow(user)
    val user: StateFlow<User> = _user

    internal val _acceptedAt: MutableStateFlow<Date?> = MutableStateFlow(null)
    val acceptedAt: StateFlow<Date?> = _acceptedAt

    internal val _rejectedAt: MutableStateFlow<Date?> = MutableStateFlow(null)
    val rejectedAt: StateFlow<Date?> = _rejectedAt

    internal val _joinedAt: MutableStateFlow<Date?> = MutableStateFlow(null)
    val joinedAt: StateFlow<Date?> = _joinedAt

    /**
     * State that indicates whether the camera is capturing and sending video or not.
     */
    internal val _videoEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val videoEnabled: StateFlow<Boolean> = _videoEnabled

    internal val _speaking: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking

    internal val _audioLevel: MutableStateFlow<Float> = MutableStateFlow(0F)
    val audioLevel: StateFlow<Float> = _audioLevel

    internal val _connectionQuality: MutableStateFlow<ConnectionQuality> = MutableStateFlow(ConnectionQuality.CONNECTION_QUALITY_UNSPECIFIED)
    val connectionQuality: StateFlow<ConnectionQuality> = _connectionQuality
    /**
     * State that indicates whether the mic is capturing and sending the audio or not.
     */
    internal val _isAudioEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val audioEnabled: StateFlow<Boolean> = _isAudioEnabled

    /**
     * State that indicates whether the speakerphone is on or not.
     */
    internal val _isSpeakerPhoneEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val speakerPhoneEnabled: StateFlow<Boolean> = _isSpeakerPhoneEnabled
}

public open class LocalParticipantState(user: User) : ParticipantState(user) {
    internal val _ownCapabilities: MutableStateFlow<List<OwnCapability>> = MutableStateFlow(
        emptyList()
    )
    val ownCapabilities: StateFlow<List<OwnCapability>> = _ownCapabilities
}

public class MemberState(user: User) {
    /**
     * If you are a participant or not
     */
    private val _isParticipant: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val videoEnabled: StateFlow<Boolean> = _isParticipant
}

/**
 *
 */
public class CallState(user: User) {
    private val memberMap: MutableMap<String, MemberState> = mutableMapOf()
    private val _recording: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording

    private val _endedAt: MutableStateFlow<Date?> = MutableStateFlow(null)
    val endedAt: StateFlow<Date?> = _endedAt
    private val _endedByUser: MutableStateFlow<User?> = MutableStateFlow(null)
    val endedByUser: StateFlow<User?> = _endedByUser

    private val participantMap = mutableMapOf<String, ParticipantState>()

    private val _capabilitiesByRole: MutableStateFlow<Map<String, List<String>>> = MutableStateFlow(emptyMap())
    val capabilitiesByRole: StateFlow<Map<String, List<String>>> = _capabilitiesByRole

    fun handleEvent(event: VideoEvent) {
        println("updating call state yolo")
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
            is CallCreatedEvent -> TODO()
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


            is CallUpdatedEvent -> {
                // update the own capabilities
                me._ownCapabilities.value=event.ownCapabilities
                // update the capabilities by role
                _capabilitiesByRole.value = event.capabilitiesByRole
                // update call info fields
            }
            is UpdatedCallPermissionsEvent -> {
                me._ownCapabilities.value=event.ownCapabilities
            }
            is ConnectedEvent -> TODO()
            is CustomEvent -> TODO()
            is CoordinatorHealthCheckEvent -> TODO()
            is PermissionRequestEvent -> TODO()
            is RecordingStartedEvent -> {
                println("RecordingStartedEvent")
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
        }
    }

    private fun removeParticipant(userId: String) {
        participantMap.remove(userId)
        // TODO: connect map and participant list nicely
    }

    private fun getOrCreateParticipant(participant: ParticipantData): ParticipantState {
        // TODO: update some fields
        val participantState = getOrCreateParticipant(participant.user_id)

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
            val participant = ParticipantState(User(id=userId))
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

    // TODO: SFU Connection

    private val _participants: MutableStateFlow<List<CallParticipantState>> =
        MutableStateFlow(emptyList())
    public val participants: StateFlow<List<CallParticipantState>> = _participants

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

    private val _members: MutableStateFlow<List<CallParticipantState>> =
        MutableStateFlow(emptyList())
    public val members: StateFlow<List<CallParticipantState>> = _members

    val me = LocalParticipantState(user)
}

public class Call2(
    private val client: StreamVideo,
    val type: String,
    val id: String,
    private val token: String = "",
    val user: User,
) {
    private var activeSession: ActiveSFUSession? = null
    val cid = "$type:$id"
    val state = CallState(user)

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
        // TODO: this should run in parallel and be configurable
        val latencyResults = result.data.edges.associate {
            it.name to getLatencyMeasurements(it.latencyUrl)
        }
        // upload our latency measurements to the server
        val selectEdgeServerResult = client.selectEdgeServer(
            type = type,
            id = id,
            request = GetCallEdgeServerRequest(
                latencyMeasurements = latencyResults
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

        activeSession = ActiveSFUSession(client, this, url, credentials.token, iceServers, latencyResults)
        return Success<ActiveSFUSession>(data= activeSession!!)

    }

    suspend fun goLive(): Result<GoLiveResponse> {
        return client.goLive(type, id)
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
}
