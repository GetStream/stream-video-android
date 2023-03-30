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
import io.getstream.video.android.core.events.HealthCheckEvent
import io.getstream.video.android.core.events.HealthCheckResponseEvent
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
import io.getstream.video.android.core.model.CallMetadata
import io.getstream.video.android.core.model.CallParticipantState
import io.getstream.video.android.core.model.IceServer
import io.getstream.video.android.core.model.JoinedCall
import io.getstream.video.android.core.model.SfuToken
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.utils.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.openapitools.client.models.GoLiveResponse
import org.openapitools.client.models.UpdateCallResponse

public data class SFUConnection(
    internal val callUrl: String,
    internal val sfuToken: SfuToken,
    internal val iceServers: List<IceServer>
)

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

public open class LocalParticipantState(user: User) : ParticipantState(user)

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
    private val token: String = "",
    private val user: User,
) {
    val cid = "$type:$id"
    val state = CallState(user)

    public var custom: Map<String, Any>? = null

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
