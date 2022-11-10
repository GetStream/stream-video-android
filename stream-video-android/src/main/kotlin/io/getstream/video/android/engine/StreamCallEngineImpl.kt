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

package io.getstream.video.android.engine

import io.getstream.logging.StreamLog
import io.getstream.video.android.StreamVideoConfig
import io.getstream.video.android.errors.VideoError
import io.getstream.video.android.events.AudioLevelChangedEvent
import io.getstream.video.android.events.AudioMutedEvent
import io.getstream.video.android.events.AudioUnmutedEvent
import io.getstream.video.android.events.CallAcceptedEvent
import io.getstream.video.android.events.CallCanceledEvent
import io.getstream.video.android.events.CallCreatedEvent
import io.getstream.video.android.events.CallEndedEvent
import io.getstream.video.android.events.CallMembersDeletedEvent
import io.getstream.video.android.events.CallMembersUpdatedEvent
import io.getstream.video.android.events.CallRejectedEvent
import io.getstream.video.android.events.CallUpdatedEvent
import io.getstream.video.android.events.ChangePublishQualityEvent
import io.getstream.video.android.events.ConnectedEvent
import io.getstream.video.android.events.ConnectionQualityChangeEvent
import io.getstream.video.android.events.DominantSpeakerChangedEvent
import io.getstream.video.android.events.HealthCheckEvent
import io.getstream.video.android.events.HealthCheckResponseEvent
import io.getstream.video.android.events.ICETrickleEvent
import io.getstream.video.android.events.JoinCallResponseEvent
import io.getstream.video.android.events.LocalDeviceChangeEvent
import io.getstream.video.android.events.MuteStateChangeEvent
import io.getstream.video.android.events.ParticipantJoinedEvent
import io.getstream.video.android.events.ParticipantLeftEvent
import io.getstream.video.android.events.PublisherCandidateEvent
import io.getstream.video.android.events.SfuDataEvent
import io.getstream.video.android.events.SfuParticipantJoinedEvent
import io.getstream.video.android.events.SfuParticipantLeftEvent
import io.getstream.video.android.events.SubscriberCandidateEvent
import io.getstream.video.android.events.SubscriberOfferEvent
import io.getstream.video.android.events.UnknownEvent
import io.getstream.video.android.events.VideoEvent
import io.getstream.video.android.events.VideoQualityChangedEvent
import io.getstream.video.android.events.VideoStartedEvent
import io.getstream.video.android.events.VideoStoppedEvent
import io.getstream.video.android.model.CallEventType
import io.getstream.video.android.model.CallEventType.ACCEPTED
import io.getstream.video.android.model.CallEventType.CANCELLED
import io.getstream.video.android.model.CallEventType.REJECTED
import io.getstream.video.android.model.CallEventType.UNDEFINED
import io.getstream.video.android.model.CallMetadata
import io.getstream.video.android.model.JoinedCall
import io.getstream.video.android.model.User
import io.getstream.video.android.model.state.DropReason
import io.getstream.video.android.model.state.StreamCallGuid
import io.getstream.video.android.model.state.StreamCallKind
import io.getstream.video.android.model.state.StreamDate
import io.getstream.video.android.model.state.copy
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
import stream.video.sfu.event.JoinRequest
import io.getstream.video.android.model.StreamCallCid as CallCid
import io.getstream.video.android.model.StreamCallId as CallId
import io.getstream.video.android.model.StreamCallType as CallType
import io.getstream.video.android.model.state.StreamCallState as State

/**
 * Should be written in pure kotlin.
 * No android imports are allowed here.
 */
internal class StreamCallEngineImpl(
    parentScope: CoroutineScope,
    private val config: StreamVideoConfig,
    private val getCurrentUser: () -> User,
) : StreamCallEngine {

    private val logger = StreamLog.getLogger("Call:Engine")

    private val getCurrentUserId = { getCurrentUser().id }

    private val mutex = Mutex()

    private val scope = parentScope + Job(parentScope.coroutineContext.job) + Dispatchers.Default

    private val _callState = MutableStateFlow<State>(State.Idle)

    override val callState: StateFlow<State> = _callState

    override fun onCoordinatorEvent(event: VideoEvent) {
        if (event !is HealthCheckEvent) {
            logger.i { "[onCoordinatorEvent] event: $event" }
        }
        when (event) {
            is AudioMutedEvent -> {}
            is AudioUnmutedEvent -> {}
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
            is ParticipantJoinedEvent -> {}
            is ParticipantLeftEvent -> {}
            is VideoStartedEvent -> {}
            is VideoStoppedEvent -> {}
            is UnknownEvent -> {}
        }
    }

    override fun onSfuEvent(event: SfuDataEvent) {
        if (event !is HealthCheckResponseEvent) {
            logger.i { "[onSfuEvent] event: $event" }
        }
        when (event) {
            is AudioLevelChangedEvent -> { }
            is ChangePublishQualityEvent -> { }
            is ConnectionQualityChangeEvent -> { }
            is DominantSpeakerChangedEvent -> { }
            is HealthCheckResponseEvent -> { }
            is ICETrickleEvent -> { }
            is JoinCallResponseEvent -> { }
            is LocalDeviceChangeEvent -> { }
            is MuteStateChangeEvent -> { }
            is PublisherCandidateEvent -> { }
            is SfuParticipantJoinedEvent -> { }
            is SfuParticipantLeftEvent -> { }
            is SubscriberCandidateEvent -> { }
            is SubscriberOfferEvent -> { }
            is VideoQualityChangedEvent -> {}
        }
    }

    override fun onSfuJoinSent(request: JoinRequest) = scope.launchWithLock(mutex) {
        logger.i { "[onSfuJoinSent] request: $request" }
    }

    private fun onCallAccepted(event: CallAcceptedEvent) = scope.launchWithLock(mutex) {
        val state = _callState.value
        if (state !is State.Outgoing) {
            logger.w { "[onCallAccepted] rejected (state is not Outgoing): $state" }
            return@launchWithLock
        }
        if (state.callGuid.cid != event.callCid) {
            logger.w {
                "[onCallAccepted] rejected (callCid is not valid);" +
                    " expected: ${state.callGuid.cid}, actual: ${event.callCid}"
            }
            return@launchWithLock
        }
        if (!state.members.contains(event.sentByUserId)) {
            logger.w { "[onCallAccepted] rejected (accepted by non-Member): $event" }
            return@launchWithLock
        }
        logger.d { "[onCallAccepted] state: $state" }
        _callState.post(
            state.copy(
                acceptedByCallee = true
            )
        )
    }

    private fun onCallRejected(event: CallRejectedEvent) = scope.launchWithLock(mutex) {
        val state = _callState.value
        if (state !is State.Started) {
            logger.w { "[onCallRejected] rejected (state is not Started): $state" }
            return@launchWithLock
        }
        if (state.callGuid.cid != event.callCid) {
            logger.w {
                "[onCallRejected] rejected (callCid is not valid);" +
                    " expected: ${state.callGuid.cid}, actual: ${event.callCid}"
            }
            return@launchWithLock
        }
        if (!state.members.contains(event.sentByUserId)) {
            logger.w { "[onCallRejected] rejected (rejected by non-Member): $event" }
            return@launchWithLock
        }
        if (!state.members.contains(event.sentByUserId)) {
            logger.w { "[onCallRejected] rejected (rejected by non-Member): $event" }
            return@launchWithLock
        }
        logger.d { "[onCallRejected] state: $state" }
        _callState.post(
            state.copy(
                broadcastingEnabled = event.info.broadcastingEnabled,
                recordingEnabled = event.info.recordingEnabled,
                createdAt = StreamDate.from(event.info.createdAt),
                updatedAt = StreamDate.from(event.info.updatedAt),
                members = event.details.members,
                users = event.users
            )
        )
        if (event.details.members.isNotEmpty()) {
            logger.w { "[onCallRejected] rejected (rejected not by all members): $event" }
            return@launchWithLock
        }
        logger.d { "[onCallRejected] state: $state" }
        dropCall(State.Drop(state.callGuid, state.callKind, DropReason.Rejected(event.sentByUserId)))
    }

    override fun onCallJoined(joinedCall: JoinedCall) = scope.launchWithLock(mutex) {
        val state = _callState.value
        if (state !is State.Joining) {
            logger.w { "[onCallJoined] rejected (state is not Joining): $state" }
            return@launchWithLock
        }
        if (state.callGuid.cid != joinedCall.call.cid) {
            logger.w {
                "[onCallJoined] rejected (callCid is not valid); " +
                    "expected: ${state.callGuid.cid}, actual: ${joinedCall.call.cid}"
            }
            return@launchWithLock
        }
        logger.d { "[onCallJoined] joinedCall: $joinedCall, state: $state" }
        _callState.post(
            joinedCall.run {

                // TODO it should be Connecting until a Connected state from ICE candidates comes
                State.Connected(
                    callGuid = state.callGuid,
                    callKind = state.callKind,
                    createdByUserId = call.createdByUserId,
                    broadcastingEnabled = call.broadcastingEnabled,
                    recordingEnabled = call.recordingEnabled,
                    createdAt = StreamDate.from(call.createdAt),
                    updatedAt = StreamDate.from(call.updatedAt),
                    users = call.users,
                    members = call.members,
                    callUrl = callUrl,
                    userToken = userToken,
                    iceServers = iceServers,
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
        val state = _callState.value
        if (state !is State.Idle && state !is State.Incoming) {
            logger.w { "[onCallStarting] rejected (state is not Idle/Incoming): $state" }
            return@launchWithLock
        }
        val callCid = CallCid(type, id)
        if (state is State.Incoming && state.callGuid.cid != callCid) {
            logger.w {
                "[onCallStarting] rejected (callCid is not valid); expected: ${state.callGuid.cid}, actual: $callCid"
            }
            return@launchWithLock
        }
        logger.d {
            "[onCallStarting] type: $type, id: $id, ringing: $ringing, forcedNewCall: $forcedNewCall, " +
                "participantIds: $participantIds, state: $state"
        }
        if (state is State.Idle) {
            _callState.post(
                State.Starting(
                    callGuid = StreamCallGuid(
                        type = type,
                        id = id,
                        cid = CallCid(type, id),
                    ),
                    callKind = if (ringing) StreamCallKind.RINGING else StreamCallKind.MEETING,
                    memberUserIds = participantIds
                )
            )
        } else if (state is State.Incoming) {
            _callState.post(
                state.copy(
                    acceptedByMe = true
                )
            )
        }
    }

    override fun onCallStarted(call: CallMetadata) = scope.launchWithLock(mutex) {
        val state = _callState.value
        if (state !is State.Starting) {
            logger.w { "[onCallStarted] rejected (state is not Starting): $state" }
            return@launchWithLock
        }
        if (state.callGuid.cid != call.cid) {
            logger.w {
                "[onCallStarted] rejected (callCid is not valid); expected: ${state.callGuid.cid}, actual: ${call.cid}"
            }
            return@launchWithLock
        }
        logger.d { "[onCallStarted] call: $call, state: $state" }
        _callState.post(
            State.Outgoing(
                callGuid = state.callGuid,
                callKind = state.callKind,
                createdByUserId = call.createdByUserId,
                broadcastingEnabled = call.broadcastingEnabled,
                recordingEnabled = call.recordingEnabled,
                createdAt = StreamDate.from(call.createdAt),
                updatedAt = StreamDate.from(call.updatedAt),
                users = call.users,
                members = call.members,
                acceptedByCallee = false
            )
        )
        if (state.callKind == StreamCallKind.RINGING) {
            waitForCallToBeAccepted()
        }
    }

    private fun waitForCallToBeAccepted() {
        scope.launch {
            logger.d { "[waitForCallToBeAccepted] dropTimeout: ${config.dropTimeout}" }
            delay(config.dropTimeout)
            mutex.withLock {
                val state = _callState.value
                if (state is State.Outgoing && !state.acceptedByCallee) {
                    logger.w { "[waitForCallToBeAccepted] timed out (call is not accepted)" }
                    dropCall(State.Drop(state.callGuid, state.callKind, DropReason.Timeout(config.dropTimeout)))
                } else {
                    logger.v { "[waitForCallToBeAccepted] call was accepted" }
                }
            }
        }
    }

    override fun onCallEventSending(callCid: String, eventType: CallEventType) = scope.launchWithLock(mutex) {
        val state = _callState.value
        if (state !is State.Active) {
            logger.w { "[onCallEventSending] $eventType rejected (state is not Active): $state" }
            return@launchWithLock
        }
        if (state.callGuid.cid != callCid) {
            logger.w {
                "[onCallEventSending] $eventType rejected (callCid is not valid);" +
                    " expected: ${state.callGuid.cid}, actual: $callCid"
            }
            return@launchWithLock
        }
        if (eventType == ACCEPTED && state !is State.Incoming) {
            logger.w { "[onCallEventSending] $eventType rejected (state is not Incoming): $state" }
            return@launchWithLock
        }
        logger.d { "[onCallEventSending] callCid: $callCid, eventType: $eventType, state: $state" }
        when (eventType) {
            REJECTED -> dropCall(State.Drop(state.callGuid, state.callKind, DropReason.Rejected(getCurrentUserId())))
            CANCELLED -> dropCall(State.Drop(state.callGuid, state.callKind, DropReason.Cancelled(getCurrentUserId())))
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
        val state = _callState.value
        logger.d { "[onCallEventSent] callCid: $callCid, eventType: $eventType, state: $state" }
    }

    override fun onCallJoining(call: CallMetadata) = scope.launchWithLock(mutex) {
        val state = _callState.value
        if (state !is State.Joinable) {
            logger.w { "[onCallJoining] rejected (state is not Joinable): $state" }
            return@launchWithLock
        }
        if (state.callGuid.cid != call.cid) {
            logger.w {
                "[onCallJoining] rejected (callCid is not valid);" +
                    " expected: ${state.callGuid.cid}, actual: $${call.cid}"
            }
            return@launchWithLock
        }
        logger.d { "[onCallJoining] call: $call, state: $state" }
        _callState.post(
            State.Joining(
                callGuid = state.callGuid,
                createdByUserId = call.createdByUserId,
                broadcastingEnabled = call.broadcastingEnabled,
                recordingEnabled = call.recordingEnabled,
                createdAt = StreamDate.from(call.createdAt),
                updatedAt = StreamDate.from(call.updatedAt),
                users = call.users,
                members = call.members,
                callKind = state.callKind
            )
        )
    }

    override fun onCallFailed(error: VideoError) = scope.launchWithLock(mutex) {
        val state = _callState.value
        if (state !is State.Active) {
            logger.w { "[onCallFailed] rejected (state is not Active): $state" }
            return@launchWithLock
        }
        logger.e { "[onCallFailed] error: $error, state: $state" }
        dropCall(State.Drop(state.callGuid, state.callKind, DropReason.Failure(error)))
    }

    private fun onCallFinished(event: CallEndedEvent) = scope.launchWithLock(mutex) {
        val state = _callState.value
        logger.d { "[onCallFinished] state: $state" }
        if (state !is State.Active) {
            logger.w { "[onCallFinished] rejected (state is not Active): $state" }
            return@launchWithLock
        }
        if (state.callGuid.cid != event.callCid) {
            logger.w {
                "[onCallFinished] rejected (callCid is not valid);" +
                    " expected: ${state.callGuid.cid}, actual: ${event.callCid}"
            }
            return@launchWithLock
        }
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
                "[onCallCancelled] rejected (callCid is not valid);" +
                    " expected: ${state.callGuid.cid}, actual: ${event.callCid}"
            }
            return@launchWithLock
        }
        if (state.callKind == StreamCallKind.MEETING) {
            logger.w { "[onCallCancelled] rejected (callKind is MEETING): $state" }
            return@launchWithLock
        }
        logger.d { "[onCallCancelled] event: $event, state: $state" }
        dropCall(State.Drop(state.callGuid, state.callKind, DropReason.Cancelled(event.sentByUserId)))
    }

    private fun onCallCreated(event: CallCreatedEvent) = scope.launchWithLock(mutex) {
        val state = _callState.value
        if (state !is State.Idle) {
            logger.w { "[onCallCreated] rejected (state is not Idle): $state" }
            return@launchWithLock
        }
        if (!event.ringing) {
            logger.w { "[onCallCreated] rejected (ringing is False): $event" }
            return@launchWithLock
        }
        logger.d { "[onCallCreated] event: $event, state: $state" }
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
                    members = details.members,
                    acceptedByMe = false
                )
            }
        )
    }

    /**
     * Used for setting the state to a dropped call and then immediately switching to Idle.
     */
    private fun dropCall(state: State.Drop) {
        _callState.post(state)
        _callState.post(State.Idle)
    }

    private fun MutableStateFlow<State>.post(state: State) {
        if (state == value) {
            logger.w { "[post] rejected (duplicate state): $state" }
            return
        }
        logger.i { "[post] $state <= $value" }
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
