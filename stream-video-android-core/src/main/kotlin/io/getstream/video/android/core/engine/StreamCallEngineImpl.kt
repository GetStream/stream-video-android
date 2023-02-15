/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.engine

import io.getstream.log.taggedLogger
import io.getstream.video.android.core.StreamVideoConfig
import io.getstream.video.android.core.coordinator.CallCoordinatorClient
import io.getstream.video.android.core.errors.VideoError
import io.getstream.video.android.core.events.AudioLevelChangedEvent
import io.getstream.video.android.core.events.CallAcceptedEvent
import io.getstream.video.android.core.events.CallCanceledEvent
import io.getstream.video.android.core.events.CallCreatedEvent
import io.getstream.video.android.core.events.CallEndedEvent
import io.getstream.video.android.core.events.CallMembersDeletedEvent
import io.getstream.video.android.core.events.CallMembersUpdatedEvent
import io.getstream.video.android.core.events.CallRejectedEvent
import io.getstream.video.android.core.events.CallUpdatedEvent
import io.getstream.video.android.core.events.ChangePublishQualityEvent
import io.getstream.video.android.core.events.ConnectedEvent
import io.getstream.video.android.core.events.ConnectionQualityChangeEvent
import io.getstream.video.android.core.events.DominantSpeakerChangedEvent
import io.getstream.video.android.core.events.ErrorEvent
import io.getstream.video.android.core.events.HealthCheckEvent
import io.getstream.video.android.core.events.HealthCheckResponseEvent
import io.getstream.video.android.core.events.ICETrickleEvent
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.events.ParticipantJoinedEvent
import io.getstream.video.android.core.events.ParticipantLeftEvent
import io.getstream.video.android.core.events.PublisherAnswerEvent
import io.getstream.video.android.core.events.SfuDataEvent
import io.getstream.video.android.core.events.SubscriberOfferEvent
import io.getstream.video.android.core.events.TrackPublishedEvent
import io.getstream.video.android.core.events.TrackUnpublishedEvent
import io.getstream.video.android.core.events.UnknownEvent
import io.getstream.video.android.core.events.VideoEvent
import io.getstream.video.android.core.events.VideoQualityChangedEvent
import io.getstream.video.android.core.filter.InFilterObject
import io.getstream.video.android.core.filter.toMap
import io.getstream.video.android.core.model.CallEventType
import io.getstream.video.android.core.model.CallEventType.ACCEPTED
import io.getstream.video.android.core.model.CallEventType.CANCELLED
import io.getstream.video.android.core.model.CallEventType.REJECTED
import io.getstream.video.android.core.model.CallEventType.UNDEFINED
import io.getstream.video.android.core.model.CallMetadata
import io.getstream.video.android.core.model.JoinedCall
import io.getstream.video.android.core.model.StreamCallGuid
import io.getstream.video.android.core.model.StreamCallKind
import io.getstream.video.android.core.model.mapper.toConnected
import io.getstream.video.android.core.model.mapper.toConnecting
import io.getstream.video.android.core.model.merge
import io.getstream.video.android.core.model.state.DropReason
import io.getstream.video.android.core.model.state.StreamDate
import io.getstream.video.android.core.model.state.copy
import io.getstream.video.android.core.utils.Jobs
import io.getstream.video.android.core.utils.Success
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.openapitools.client.models.QueryMembersRequest
import stream.video.sfu.event.JoinRequest
import io.getstream.video.android.core.model.StreamCallCid as CallCid
import io.getstream.video.android.core.model.StreamCallId as CallId
import io.getstream.video.android.core.model.StreamCallType as CallType
import io.getstream.video.android.core.model.state.StreamCallState as State

private const val ID_TIMEOUT_ACCEPT = 1
private const val ID_TIMEOUT_SFU_JOINED = 2

private const val TIMEOUT_SFU_JOINED = 10_000L

/**
 * Should be written in pure kotlin.
 * No android imports are allowed here.
 */
internal class StreamCallEngineImpl(
    parentScope: CoroutineScope,
    private val coordinatorClient: CallCoordinatorClient,
    private val config: StreamVideoConfig,
    private inline val getCurrentUserId: () -> String,
) : StreamCallEngine {

    private val logger by taggedLogger("Call:Engine")

    private val jobs = Jobs()

    private val mutex = Mutex()

    private val scope = parentScope + Job(parentScope.coroutineContext.job) + Dispatchers.Default

    private val _callState = MutableStateFlow<State>(State.Idle)

    override val callState: StateFlow<State> = _callState

    override fun onCoordinatorEvent(event: VideoEvent) {
        if (event !is HealthCheckEvent) {
            logger.v { "[onCoordinatorEvent] event: $event" }
        }
        when (event) {
            is CallCreatedEvent -> onCallCreated(event)
            is CallMembersDeletedEvent -> {}
            is CallMembersUpdatedEvent -> {}
            is CallUpdatedEvent -> {}
            is CallAcceptedEvent -> onCallAccepted(event)
            is CallRejectedEvent -> onCallRejected(event)
            is CallEndedEvent -> onCallFinished(event)
            is CallCanceledEvent -> onCallCancelled(event)
            is ConnectedEvent -> {}
            is HealthCheckEvent -> {}
            is UnknownEvent -> {}
        }
    }

    override fun onSfuEvent(event: SfuDataEvent) {
        if (event !is HealthCheckResponseEvent) {
            logger.v { "[onSfuEvent] event: $event" }
        }
        when (event) {
            is JoinCallResponseEvent -> onSfuJoined(event)
            is ParticipantJoinedEvent -> onSfuParticipantJoined(event)
            is ParticipantLeftEvent -> onSfuParticipantLeft(event)
            is AudioLevelChangedEvent -> {}
            is ChangePublishQualityEvent -> {}
            is ConnectionQualityChangeEvent -> {}
            is DominantSpeakerChangedEvent -> {}
            is HealthCheckResponseEvent -> {}
            is ICETrickleEvent -> {}
            is SubscriberOfferEvent -> {}
            is VideoQualityChangedEvent -> {}
            is PublisherAnswerEvent -> {}
            is TrackPublishedEvent -> {}
            is TrackUnpublishedEvent -> {}
            is ErrorEvent -> {}
        }
    }

    override fun onSfuJoinSent(request: JoinRequest) = scope.launchWithLock(mutex) {
        logger.d { "[onSfuJoinSent] request: $request" }
        val state = _callState.value
        if (state !is State.Joined) {
            logger.w { "[onSfuJoinSent] rejected (state is not Connecting): $state" }
            return@launchWithLock
        }
        if (state.sfuToken != request.token) {
            logger.w {
                "[onSfuJoinSent] rejected (token is not valid);" + " expected: ${state.sfuToken}, actual: ${request.token}"
            }
            return@launchWithLock
        }
        _callState.post(
            state.toConnecting(sfuSessionId = request.session_id)
        )
        waitForSfuJoined()
    }

    private fun waitForSfuJoined() {
        jobs.add(
            ID_TIMEOUT_SFU_JOINED,
            scope.launch {
                logger.d { "[waitForSfuJoined] dropTimeout: $TIMEOUT_SFU_JOINED" }
                delay(TIMEOUT_SFU_JOINED)
                mutex.withLock {
                    val state = _callState.value
                    if (state is State.Connecting) {
                        logger.w { "[waitForSfuJoined] timed out (no SfuJoined event received)" }
                        dropCall(
                            State.Drop(
                                state.callGuid, state.callKind,
                                DropReason.Failure(
                                    VideoError(message = "no SfuJoined event received")
                                )
                            )
                        )
                    } else {
                        logger.v { "[waitForSfuJoined] SfuJoined event was accepted" }
                    }
                }
            }
        )
    }

    private fun onSfuJoined(event: JoinCallResponseEvent) = scope.launchWithLock(mutex) {
        logger.d { "[onSfuJoined] event: $event" }
        val state = _callState.value
        if (state !is State.Connecting) {
            logger.w { "[onSfuJoined] rejected (state is not Connecting): $state" }
            return@launchWithLock
        }

        jobs.cancel(ID_TIMEOUT_SFU_JOINED)

        val query = InFilterObject(
            "id", event.callState.participants.map { it.user_id }.toSet()
        ).toMap()

        val queryUsersResult = coordinatorClient.queryMembers(
            QueryMembersRequest(
                id = state.callGuid.id,
                type = state.callGuid.type,
                filterConditions = query
            )
        )

        if (queryUsersResult is Success) {
            val eventUsers = queryUsersResult.data.associateBy { it.id }

            val stateConfirmed = state.toConnected().copy(
                users = state.users merge eventUsers
            )
            _callState.post(stateConfirmed)
        }
    }

    /**
     * Called when participant joins an existing call.
     * @param event Contains information about the participant who joined. */
    private fun onSfuParticipantJoined(event: ParticipantJoinedEvent) =
        scope.launchWithLock(mutex) {
            logger.d { "[onSfuParticipantJoined] event: $event" }
            val state = _callState.value
            if (state !is State.InCall) {
                logger.w { "[onSfuParticipantJoined] rejected (state is not Connecting): $state" }
                return@launchWithLock
            }

            val callCid = event.callCid
            if (state.callGuid.cid != callCid) {
                logger.w {
                    "[onSfuParticipantJoined] rejected (callCid is not valid); " + "expected: ${state.callGuid.cid}, actual: $callCid"
                }
                return@launchWithLock
            }
            val query = InFilterObject("id", setOf(event.participant.user_id)).toMap()

            val userQueryResult = coordinatorClient.queryMembers(
                QueryMembersRequest(
                    id = state.callGuid.id,
                    type = state.callGuid.type,
                    filterConditions = query
                )
            )

            if (userQueryResult is Success) {
                val user = userQueryResult.data.first()

                _callState.post(
                    state.copy(
                        users = state.users merge user
                    )
                )
            }
        }

    /**
     * Called when participant leaves a call.
     * @param event Contains information about the participant who left.
     */
    private fun onSfuParticipantLeft(event: ParticipantLeftEvent) = scope.launchWithLock(mutex) {
        logger.d { "[onSfuParticipantLeft] event: $event" }
        val state = _callState.value
        if (state !is State.InCall) {
            logger.w { "[onSfuParticipantLeft] rejected (state is not Connecting): $state" }
            return@launchWithLock
        }

        val callCid = event.callCid
        if (state.callGuid.cid != callCid) {
            logger.w {
                "[onSfuParticipantLeft] rejected (callCid is not valid); " + "expected: ${state.callGuid.cid}, actual: $callCid"
            }
            return@launchWithLock
        }
        val users = state.users.toMutableMap()
        users.remove(event.participant.user_id)

        _callState.post(state.copy(users = users))
    }

    private fun onCallAccepted(event: CallAcceptedEvent) = scope.launchWithLock(mutex) {
        logger.d { "[onCallAccepted] event: $event" }
        val state = _callState.value
        if (state !is State.Outgoing) {
            logger.w { "[onCallAccepted] rejected (state is not Outgoing): $state" }
            return@launchWithLock
        }
        if (state.callGuid.cid != event.callCid) {
            logger.w {
                "[onCallAccepted] rejected (callCid is not valid);" + " expected: ${state.callGuid.cid}, actual: ${event.callCid}"
            }
            return@launchWithLock
        }
        if (!state.users.contains(event.sentByUserId)) {
            logger.w { "[onCallAccepted] rejected (accepted by non-Member): $event" }
            return@launchWithLock
        }
        jobs.cancel(ID_TIMEOUT_ACCEPT)
        _callState.post(
            state.copy(
                acceptedByCallee = true
            )
        )
    }

    private fun onCallRejected(event: CallRejectedEvent) = scope.launchWithLock(mutex) {
        logger.d { "[onCallRejected] event: $event" }
        val state = _callState.value
        if (state !is State.Started) {
            logger.w { "[onCallRejected] rejected (state is not Started): $state" }
            return@launchWithLock
        }
        if (state.callGuid.cid != event.callCid) {
            logger.w {
                "[onCallRejected] rejected (callCid is not valid);" + " expected: ${state.callGuid.cid}, actual: ${event.callCid}"
            }
            return@launchWithLock
        }
        if (!state.users.contains(event.sentByUserId)) {
            logger.w { "[onCallRejected] rejected (rejected by non-Member): $event" }
            return@launchWithLock
        }
        _callState.post(
            state.copy(
                broadcastingEnabled = event.info.broadcastingEnabled,
                recordingEnabled = event.info.recordingEnabled,
                createdAt = StreamDate.from(event.info.createdAt),
                updatedAt = StreamDate.from(event.info.updatedAt),
                users = event.users
            )
        )
        val otherUsers = event.users - getCurrentUserId()
        if (otherUsers.isNotEmpty()) {
            logger.w { "[onCallRejected] rejected (rejected not by all members): $event" }
            return@launchWithLock
        }
        dropCall(
            State.Drop(
                state.callGuid, state.callKind, DropReason.Rejected(event.sentByUserId)
            )
        )
    }

    override fun onCallJoined(joinedCall: JoinedCall) = scope.launchWithLock(mutex) {
        logger.d { "[onCallJoined] joinedCall: $joinedCall" }
        val state = _callState.value
        if (state !is State.Joining) {
            logger.w { "[onCallJoined] rejected (state is not Joining): $state" }
            return@launchWithLock
        }
        if (state.callGuid.cid != joinedCall.call.cid) {
            logger.w {
                "[onCallJoined] rejected (callCid is not valid); " + "expected: ${state.callGuid.cid}, actual: ${joinedCall.call.cid}"
            }
            return@launchWithLock
        }
        _callState.post(
            joinedCall.run {
                State.Joined(
                    callGuid = state.callGuid,
                    callKind = state.callKind,
                    createdByUserId = call.createdByUserId,
                    broadcastingEnabled = call.broadcastingEnabled,
                    recordingEnabled = call.recordingEnabled,
                    createdAt = StreamDate.from(call.createdAt),
                    updatedAt = StreamDate.from(call.updatedAt),
                    users = call.users,
                    callUrl = callUrl,
                    sfuToken = sfuToken,
                    iceServers = iceServers,
                    callDetails = call.callDetails,
                    callEgress = call.callEgress,
                    custom = call.custom
                )
            }
        )
    }

    override fun onCallStarting(
        type: CallType,
        id: CallId,
        participantIds: List<String>,
        ringing: Boolean,
        forcedNewCall: Boolean
    ) = scope.launchWithLock(mutex) {
        logger.d {
            "[onCallStarting] type: $type, id: $id, ringing: $ringing, forcedNewCall: $forcedNewCall, participantIds: $participantIds"
        }
        val state = _callState.value
        if (state !is State.Incoming) {
            logger.w { "[onCallStarting] rejected (state is not Incoming): $state" }
            return@launchWithLock
        }
        val callCid = CallCid(type, id)
        if (state.callGuid.cid != callCid) {
            logger.w {
                "[onCallStarting] rejected (callCid is not valid); expected: ${state.callGuid.cid}, actual: $callCid"
            }
            return@launchWithLock
        }
        _callState.post(
            state.copy(
                acceptedByMe = true
            )
        )
    }

    override fun onCallStarted(call: CallMetadata) = scope.launchWithLock(mutex) {
        logger.d { "[onCallStarted] call: $call" }
        val state = _callState.value
        if (state !is State.Idle) {
            logger.w { "[onCallStarted] rejected (state is not Starting): $state" }
            return@launchWithLock
        }
        if (call.kind != StreamCallKind.RINGING) {
            logger.w { "[onCallStarted] rejected (call.kind is not RINGING)" }
            return@launchWithLock
        }
        val callGuid = StreamCallGuid(
            type = call.type,
            id = call.id,
            cid = call.cid,
        )
        _callState.post(
            State.Outgoing(
                callGuid = callGuid,
                callKind = StreamCallKind.RINGING,
                createdByUserId = call.createdByUserId,
                broadcastingEnabled = call.broadcastingEnabled,
                recordingEnabled = call.recordingEnabled,
                createdAt = StreamDate.from(call.createdAt),
                updatedAt = StreamDate.from(call.updatedAt),
                users = call.users,
                acceptedByCallee = false,
                callDetails = call.callDetails,
                callEgress = call.callEgress,
                custom = call.custom
            )
        )
        waitForCallToBeAccepted()
    }

    private fun waitForCallToBeAccepted() {
        jobs.add(
            ID_TIMEOUT_ACCEPT,
            scope.launch {
                logger.d { "[waitForCallToBeAccepted] dropTimeout: ${config.dropTimeout}" }
                delay(config.dropTimeout)
                mutex.withLock {
                    val state = _callState.value
                    if (state is State.Outgoing && !state.acceptedByCallee) {
                        logger.w { "[waitForCallToBeAccepted] timed out (call is not accepted)" }
                        dropCall(
                            State.Drop(
                                state.callGuid,
                                state.callKind,
                                DropReason.Timeout(config.dropTimeout)
                            )
                        )
                    } else {
                        logger.v { "[waitForCallToBeAccepted] call was accepted" }
                    }
                }
            }
        )
    }

    override fun onCallEventSending(callCid: String, eventType: CallEventType) =
        scope.launchWithLock(mutex) {
            logger.d { "[onCallEventSending] callCid: $callCid, eventType: $eventType" }
            val state = _callState.value
            if (state !is State.Active) {
                logger.w { "[onCallEventSending] $eventType rejected (state is not Active): $state" }
                return@launchWithLock
            }
            if (state.callGuid.cid != callCid) {
                logger.w {
                    "[onCallEventSending] $eventType rejected (callCid is not valid);" + " expected: ${state.callGuid.cid}, actual: $callCid"
                }
                return@launchWithLock
            }
            if (eventType == ACCEPTED && state !is State.Incoming) {
                logger.w { "[onCallEventSending] $eventType rejected (state is not Incoming): $state" }
                return@launchWithLock
            }
            when (eventType) {
                REJECTED -> dropCall(
                    State.Drop(
                        state.callGuid, state.callKind, DropReason.Rejected(getCurrentUserId())
                    )
                )
                CANCELLED -> dropCall(
                    State.Drop(
                        state.callGuid, state.callKind, DropReason.Cancelled(getCurrentUserId())
                    )
                )
                ACCEPTED -> if (state is State.Incoming) {
                    _callState.post(state.copy(acceptedByMe = true))
                }
                UNDEFINED -> Unit
            }
        }

    override fun onCallEventSent(
        callCid: String,
        eventType: CallEventType
    ) = scope.launchWithLock(mutex) {
        logger.d { "[onCallEventSent] callCid: $callCid, eventType: $eventType" }
    }

    override fun onCallJoining(call: CallMetadata) = scope.launchWithLock(mutex) {
        logger.d { "[onCallJoining] call: $call" }
        val state = _callState.value
        if (state !is State.Joinable) {
            logger.w { "[onCallJoining] rejected (state is not Joinable): $state" }
            return@launchWithLock
        }
        val callGuid = StreamCallGuid(
            type = call.type,
            id = call.id,
            cid = call.cid,
        )
        _callState.post(
            State.Joining(
                callGuid = callGuid,
                createdByUserId = call.createdByUserId,
                broadcastingEnabled = call.broadcastingEnabled,
                recordingEnabled = call.recordingEnabled,
                createdAt = StreamDate.from(call.createdAt),
                updatedAt = StreamDate.from(call.updatedAt),
                users = call.users,
                callKind = call.kind,
                callDetails = call.callDetails,
                callEgress = call.callEgress,
                custom = call.custom
            )
        )
    }

    override fun onCallFailed(error: VideoError) = scope.launchWithLock(mutex) {
        logger.e { "[onCallFailed] error: $error" }
        val state = _callState.value
        if (state !is State.Active) {
            logger.w { "[onCallFailed] rejected (state is not Active): $state" }
            return@launchWithLock
        }
        dropCall(State.Drop(state.callGuid, state.callKind, DropReason.Failure(error)))
    }

    private fun onCallFinished(event: CallEndedEvent) = scope.launchWithLock(mutex) {
        val state = _callState.value
        if (state !is State.Active) {
            logger.w { "[onCallFinished] rejected (state is not Active): $state" }
            return@launchWithLock
        }
        if (state.callGuid.cid != event.callCid) {
            logger.w {
                "[onCallFinished] rejected (callCid is not valid);" + " expected: ${state.callGuid.cid}, actual: ${event.callCid}"
            }
            return@launchWithLock
        }
        logger.d { "[onCallFinished] event: $event, state: $state" }
        dropCall(State.Drop(state.callGuid, state.callKind, DropReason.Ended))
    }

    private fun onCallCancelled(event: CallCanceledEvent) = scope.launchWithLock(mutex) {
        val state = _callState.value
        if (state !is State.Active) {
            logger.w { "[onCallCancelled] rejected (state is not Active): $state" }
            return@launchWithLock
        }
        if (state.callGuid.cid != event.callCid) {
            logger.w {
                "[onCallCancelled] rejected (callCid is not valid);" + " expected: ${state.callGuid.cid}, actual: ${event.callCid}"
            }
            return@launchWithLock
        }
        if (state.callKind == StreamCallKind.MEETING) {
            logger.w { "[onCallCancelled] rejected (callKind is MEETING): $state" }
            return@launchWithLock
        }
        logger.d { "[onCallCancelled] state: $state" }
        dropCall(
            State.Drop(
                state.callGuid, state.callKind, DropReason.Cancelled(event.sentByUserId)
            )
        )
    }

    private fun onCallCreated(event: CallCreatedEvent) = scope.launchWithLock(mutex) {
        logger.v { "[onCallCreated] event: $event" }
        val state = _callState.value
        if (state !is State.Idle) {
            logger.w { "[onCallCreated] rejected (state is not Idle): $state" }
            return@launchWithLock
        }
        if (!event.ringing) {
            logger.w { "[onCallCreated] rejected (ringing is False): $event" }
            return@launchWithLock
        }
        logger.d { "[onCallCreated] state: $state" }
        _callState.post(
            event.run {
                State.Incoming(
                    callGuid = StreamCallGuid(
                        type = info.type,
                        id = info.id,
                        cid = info.cid,
                    ),
                    callKind = StreamCallKind.RINGING,
                    createdByUserId = info.createdByUserId,
                    broadcastingEnabled = info.broadcastingEnabled,
                    recordingEnabled = info.recordingEnabled,
                    createdAt = StreamDate.from(info.createdAt),
                    updatedAt = StreamDate.from(info.updatedAt),
                    users = users,
                    acceptedByMe = false,
                    callDetails = event.callDetails,
                    callEgress = event.info.callEgress,
                    custom = event.info.custom
                )
            }
        )
    }

    /**
     * Used for setting the state to a dropped call and then immediately switching to Idle.
     */
    private fun dropCall(state: State.Drop) {
        jobs.cancelAll()
        _callState.post(state)
        _callState.post(State.Idle)
    }

    private fun MutableStateFlow<State>.post(state: State) {
        if (state == value) {
            logger.w { "[post] rejected (duplicate state): $state" }
            return
        }
        logger.i { "[post] state: $state" }
        value = state
    }
}

private fun CoroutineScope.launchWithLock(
    mutex: Mutex,
    action: suspend CoroutineScope.() -> Unit
) {
    launch {
        mutex.withLock {
            action()
        }
    }
}
