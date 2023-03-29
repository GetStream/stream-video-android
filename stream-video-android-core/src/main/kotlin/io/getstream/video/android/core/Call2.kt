package io.getstream.video.android.core

import io.getstream.video.android.core.events.*
import io.getstream.video.android.core.model.*
import io.getstream.video.android.core.utils.Result
import kotlinx.coroutines.flow.*
import org.openapitools.client.models.GoLiveResponse
import org.openapitools.client.models.MemberResponse
import org.openapitools.client.models.UpdateCallResponse

public data class SFUConnection(internal val callUrl: String,
                                internal val sfuToken: SfuToken,
                                internal val iceServers: List<IceServer>) {

}

public open class ParticipantState(user: User) {
    /**
     * The user
     */
    private val _user: MutableStateFlow<User> = MutableStateFlow(user)
    val user: StateFlow<User> = _user

    /**
     * State that indicates whether the camera is capturing and sending video or not.
     */
    private val _videoEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val videoEnabled: StateFlow<Boolean> = _videoEnabled
    /**
     * State that indicates whether the mic is capturing and sending the audio or not.
     */
    private val _isAudioEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val audioEnabled: StateFlow<Boolean> = _isAudioEnabled

    /**
     * State that indicates whether the speakerphone is on or not.
     */
    private val _isSpeakerPhoneEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val speakerPhoneEnabled: StateFlow<Boolean> = _isSpeakerPhoneEnabled

}

public open class LocalParticipantState(user: User): ParticipantState(user) {

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
    private val _recording: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording

    fun handleEvent(event: VideoEvent) {
        println("updating call state yolo")
        when (event) {
            is BlockedUserEvent -> TODO()
            is CallAcceptedEvent -> TODO()
            is CallCancelledEvent -> TODO()
            is CallCreatedEvent -> TODO()
            is CallEndedEvent -> TODO()
            is CallMembersDeletedEvent -> TODO()
            is CallMembersUpdatedEvent -> TODO()
            is CallRejectedEvent -> TODO()
            is CallUpdatedEvent -> TODO()
            is ConnectedEvent -> TODO()
            is CustomEvent -> TODO()
            is HealthCheckEvent -> TODO()
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
            is UpdatedCallPermissionsEvent -> TODO()
            is AudioLevelChangedEvent -> TODO()
            is ChangePublishQualityEvent -> TODO()
            is ConnectionQualityChangeEvent -> TODO()
            is DominantSpeakerChangedEvent -> TODO()
            is ErrorEvent -> TODO()
            HealthCheckResponseEvent -> TODO()
            is ICETrickleEvent -> TODO()
            is JoinCallResponseEvent -> TODO()
            is ParticipantJoinedEvent -> TODO()
            is ParticipantLeftEvent -> TODO()
            is PublisherAnswerEvent -> TODO()
            is SubscriberOfferEvent -> TODO()
            is TrackPublishedEvent -> TODO()
            is TrackUnpublishedEvent -> TODO()
            is VideoQualityChangedEvent -> TODO()
        }
    }

    // TODO: SFU Connection

    private val _participants: MutableStateFlow<List<CallParticipantState>> =
        MutableStateFlow(emptyList())
    public val participants: StateFlow<List<CallParticipantState>> = _participants

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
    private val type: String,
    private val id: String,
    private val token: String="",
    private val user: User,
    ) {
    val cid = "$type:$id"
    val state = CallState(user)

    public var custom : Map<String, Any>? = null

    // should be a stateflow
    private var sfuConnection: SFUConnection? = null

    suspend fun join(): Result<JoinedCall> {
        return client.joinCall(
            type,
            id,
            emptyList(),
            false
        )
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
