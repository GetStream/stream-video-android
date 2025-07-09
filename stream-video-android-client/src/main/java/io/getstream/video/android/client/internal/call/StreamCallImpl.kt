package io.getstream.video.android.client.internal.call

import io.getstream.android.video.generated.models.BlockedUserEvent
import io.getstream.android.video.generated.models.CallAcceptedEvent
import io.getstream.android.video.generated.models.CallClosedCaptionsFailedEvent
import io.getstream.android.video.generated.models.CallClosedCaptionsStartedEvent
import io.getstream.android.video.generated.models.CallClosedCaptionsStoppedEvent
import io.getstream.android.video.generated.models.CallCreatedEvent
import io.getstream.android.video.generated.models.CallDeletedEvent
import io.getstream.android.video.generated.models.CallEndedEvent
import io.getstream.android.video.generated.models.CallHLSBroadcastingFailedEvent
import io.getstream.android.video.generated.models.CallHLSBroadcastingStartedEvent
import io.getstream.android.video.generated.models.CallHLSBroadcastingStoppedEvent
import io.getstream.android.video.generated.models.CallLiveStartedEvent
import io.getstream.android.video.generated.models.CallMemberAddedEvent
import io.getstream.android.video.generated.models.CallMemberRemovedEvent
import io.getstream.android.video.generated.models.CallMemberUpdatedEvent
import io.getstream.android.video.generated.models.CallMemberUpdatedPermissionEvent
import io.getstream.android.video.generated.models.CallMissedEvent
import io.getstream.android.video.generated.models.CallNotificationEvent
import io.getstream.android.video.generated.models.CallReactionEvent
import io.getstream.android.video.generated.models.CallRecordingFailedEvent
import io.getstream.android.video.generated.models.CallRecordingReadyEvent
import io.getstream.android.video.generated.models.CallRecordingStartedEvent
import io.getstream.android.video.generated.models.CallRecordingStoppedEvent
import io.getstream.android.video.generated.models.CallRejectedEvent
import io.getstream.android.video.generated.models.CallRequest
import io.getstream.android.video.generated.models.CallRingEvent
import io.getstream.android.video.generated.models.CallRtmpBroadcastFailedEvent
import io.getstream.android.video.generated.models.CallRtmpBroadcastStartedEvent
import io.getstream.android.video.generated.models.CallRtmpBroadcastStoppedEvent
import io.getstream.android.video.generated.models.CallSessionEndedEvent
import io.getstream.android.video.generated.models.CallSessionParticipantCountsUpdatedEvent
import io.getstream.android.video.generated.models.CallSessionParticipantJoinedEvent
import io.getstream.android.video.generated.models.CallSessionParticipantLeftEvent
import io.getstream.android.video.generated.models.CallSessionStartedEvent
import io.getstream.android.video.generated.models.CallSettingsRequest
import io.getstream.android.video.generated.models.CallTranscriptionFailedEvent
import io.getstream.android.video.generated.models.CallTranscriptionReadyEvent
import io.getstream.android.video.generated.models.CallTranscriptionStartedEvent
import io.getstream.android.video.generated.models.CallTranscriptionStoppedEvent
import io.getstream.android.video.generated.models.CallUpdatedEvent
import io.getstream.android.video.generated.models.CallUserMutedEvent
import io.getstream.android.video.generated.models.ClosedCaptionEvent
import io.getstream.android.video.generated.models.CustomVideoEvent
import io.getstream.android.video.generated.models.GetCallResponse
import io.getstream.android.video.generated.models.GetOrCreateCallRequest
import io.getstream.android.video.generated.models.GetOrCreateCallResponse
import io.getstream.android.video.generated.models.MemberRequest
import io.getstream.android.video.generated.models.MuteUsersResponse
import io.getstream.android.video.generated.models.PermissionRequestEvent
import io.getstream.android.video.generated.models.PinResponse
import io.getstream.android.video.generated.models.SendReactionResponse
import io.getstream.android.video.generated.models.UnblockedUserEvent
import io.getstream.android.video.generated.models.UnpinResponse
import io.getstream.android.video.generated.models.UpdateCallResponse
import io.getstream.android.video.generated.models.UpdatedCallPermissionsEvent
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.android.video.generated.models.WSCallEvent
import io.getstream.log.TaggedLogger
import io.getstream.video.android.client.api.StreamCall
import io.getstream.video.android.client.api.listeners.StreamVideoEventListener
import io.getstream.video.android.client.api.state.state.StreamCallState
import io.getstream.video.android.client.api.subscribe.StreamSubscription
import io.getstream.video.android.client.internal.common.StreamSubscriptionManager
import io.getstream.video.android.client.internal.generated.apis.ProductVideoApi
import io.getstream.video.android.client.internal.log.provideLogger
import io.getstream.video.android.client.internal.socket.coordinator.ConnectionState
import io.getstream.video.android.client.internal.socket.coordinator.CoordinatorSocket
import io.getstream.video.android.client.internal.socket.coordinator.StreamCoordinatorSocketListener
import io.getstream.video.android.client.model.StreamCallCreateOptions
import io.getstream.video.android.core.model.QueriedMembers
import io.getstream.video.android.core.model.SortField
import org.threeten.bp.OffsetDateTime

internal class StreamCallImpl(
    private val type: String,
    private val id: String,
    private val productVideoApi: ProductVideoApi,
    private val coordinatorSocket: CoordinatorSocket,
    private val subscriptionManager: StreamSubscriptionManager<StreamVideoEventListener>,
    private val callState: StreamCallState,
    private val logger: TaggedLogger = provideLogger(tag = "StreamCall:$type:$id"),
) : StreamCall {

    private val cid = "$type:$id"
    private val coordinatorSubscription: StreamSubscription? = null
    private val coordinatorListener = object : StreamCoordinatorSocketListener {

        override fun onEvent(event: VideoEvent) {
            val callEvent = event as? WSCallEvent
            if (callEvent?.getCallCID() == cid) {
                handleEvent(event)
                subscriptionManager.forEach {
                    it.onEvent(event)
                }
            }
        }
    }

    // region API

    override suspend fun get(): Result<GetCallResponse> = runCatching {
        productVideoApi.getCall(type, id)
    }

    override suspend fun create(
        memberIds: List<String>?,
        members: List<MemberRequest>?,
        custom: Map<String, Any>?,
        settings: CallSettingsRequest?,
        startsAt: OffsetDateTime?,
        team: String?,
        ring: Boolean,
        notify: Boolean
    ): Result<GetOrCreateCallResponse> = runCatching {
        productVideoApi.getOrCreateCall(
            type,
            id,
            null,
            GetOrCreateCallRequest(
                data = CallRequest(
                    members = members,
                    custom = custom,
                    settingsOverride = settings,
                    startsAt = startsAt,
                    team = team,
                ),
                ring = ring,
                notify = notify,
            ),
        )
    }

    override suspend fun update(
        custom: Map<String, Any>?,
        settingsOverride: CallSettingsRequest?,
        startsAt: OffsetDateTime?
    ): Result<UpdateCallResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun join(
        create: Boolean,
        createOptions: StreamCallCreateOptions?,
        ring: Boolean,
        notify: Boolean
    ): Result<StreamCall> {
        coordinatorSocket.subscribe(coordinatorListener)
        return Result.success(this)
    }

    override suspend fun leave(): Result<Unit> = runCatching {
        coordinatorSubscription?.cancel()
    }

    override suspend fun end(): Result<Unit> = runCatching {
        productVideoApi.endCall(type, id)
    }

    override suspend fun pinForEveryone(sessionId: String, userId: String): Result<PinResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun unpinForEveryone(
        sessionId: String,
        userId: String
    ): Result<UnpinResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun sendReaction(
        type: String,
        emoji: String?,
        custom: Map<String, Any>?
    ): Result<SendReactionResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun queryMembers(
        filter: Map<String, Any>,
        sort: List<SortField>,
        limit: Int,
        prev: String?,
        next: String?
    ): Result<QueriedMembers> {
        TODO("Not yet implemented")
    }

    override suspend fun muteAllUsers(
        audio: Boolean,
        video: Boolean,
        screenShare: Boolean
    ): Result<MuteUsersResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun unmuteAllUsers(
        audio: Boolean,
        video: Boolean,
        screenShare: Boolean
    ): Result<MuteUsersResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun muteUsers(
        userIds: List<String>,
        audio: Boolean,
        video: Boolean,
        screenShare: Boolean
    ): Result<MuteUsersResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun unmuteUsers(
        userIds: List<String>,
        audio: Boolean,
        video: Boolean,
        screenShare: Boolean
    ): Result<MuteUsersResponse> {
        TODO("Not yet implemented")
    }

    override fun subscribe(listener: StreamVideoEventListener): Result<StreamSubscription> =
        subscriptionManager.subscribe(listener)

    // region end: API

    private fun handleEvent(event: WSCallEvent) {
        logger.d { "[handleEvent] Received event: $event" }
        when (event) {
            else -> {

            }
        }
    }
}