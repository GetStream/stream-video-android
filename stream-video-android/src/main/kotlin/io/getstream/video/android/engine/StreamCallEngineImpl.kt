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
import io.getstream.video.android.errors.VideoError
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
import io.getstream.video.android.events.ConnectedEvent
import io.getstream.video.android.events.HealthCheckEvent
import io.getstream.video.android.events.ParticipantJoinedEvent
import io.getstream.video.android.events.ParticipantLeftEvent
import io.getstream.video.android.events.UnknownEvent
import io.getstream.video.android.events.VideoEvent
import io.getstream.video.android.events.VideoStartedEvent
import io.getstream.video.android.events.VideoStoppedEvent
import io.getstream.video.android.model.CallMetadata
import io.getstream.video.android.model.JoinedCall
import io.getstream.video.android.model.User
import io.getstream.video.android.model.state.CallGuid
import io.getstream.video.android.model.state.DropReason
import io.getstream.video.android.socket.SocketListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import stream.video.coordinator.client_v1_rpc.UserEventType
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
    private val getCurrentUser: () -> User
) : StreamCallEngine, SocketListener {

    private val logger = StreamLog.getLogger("Call:Engine")

    private val mutex = Mutex()

    private val scope = parentScope + Job(parentScope.coroutineContext.job) + Dispatchers.Default

    private val _callState = MutableStateFlow<State>(State.Idle)

    override val callState: StateFlow<State> = _callState

    override fun onEvent(event: VideoEvent) {
        if (event !is HealthCheckEvent) {
            logger.v { "[onEvent] event: $event" }
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
            is CallEndedEvent -> onCallFinished()
            is CallCanceledEvent -> onCallCancelled()
            is ConnectedEvent -> {}
            is HealthCheckEvent -> {}
            is ParticipantJoinedEvent -> {}
            is ParticipantLeftEvent -> {}
            is VideoStartedEvent -> {}
            is VideoStoppedEvent -> {}
            is UnknownEvent -> {}
        }
    }

    private fun onCallAccepted(event: CallAcceptedEvent) = scope.launchWithLock(mutex) {
        val state = _callState.value
        if (state !is State.Outgoing) {
            logger.w { "[onCallAccepted] rejected (state is not Outgoing): $state" }
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
        if (state !is State.Active) {
            return@launchWithLock
        }
        logger.d { "[onCallRejected] no args" }
        dropCall(State.Drop(state.callGuid, DropReason.Rejected))
    }

    override fun onCallJoined(joinedCall: JoinedCall) = scope.launchWithLock(mutex) {
        val state = _callState.value
        if (state !is State.Joining) {
            logger.w { "[onCallJoined] rejected (state is not Joining): $state" }
            return@launchWithLock
        }
        logger.d { "[onCallJoined] joinedCall: $joinedCall, state: $state" }
        _callState.post(
            joinedCall.run {
                // TODO it should be Connecting until a Connected state from ICE candidates
                State.Connected(
                    callGuid = CallGuid(
                        type = call.type,
                        id = call.id,
                        cid = call.cid,
                    ),
                    createdByUserId = call.createdByUserId,
                    broadcastingEnabled = call.broadcastingEnabled,
                    recordingEnabled = call.recordingEnabled,
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
                    callGuid = CallGuid(
                        type = type,
                        id = id,
                        cid = CallCid(type, id),
                    ),
                    memberUserIds = participantIds,
                    ringing = ringing
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
                callGuid = CallGuid(
                    type = call.type,
                    id = call.id,
                    cid = call.cid,
                ),
                createdByUserId = call.createdByUserId,
                broadcastingEnabled = call.broadcastingEnabled,
                recordingEnabled = call.recordingEnabled,
                users = call.users,
                members = call.members,
                acceptedByCallee = false
            )
        )
    }

    override fun onCallEventSending(type: String, id: String, eventType: UserEventType) =
        scope.launchWithLock(mutex) {
            logger.d { "[onCallEventSending] type: $type, id: $id, eventType: $UserEventType" }
        }

    override fun onCallEventSent(type: String, id: String, eventType: UserEventType) =
        scope.launchWithLock(mutex) {
            logger.d { "[onCallEventSent] type: $type, id: $id, eventType: $UserEventType" }
        }

    override fun onCallJoining(call: CallMetadata) = scope.launchWithLock(mutex) {
        val state = _callState.value
        if (state !is State.Starting && state !is State.Outgoing && state !is State.Incoming) {
            logger.w { "[onCallJoining] rejected (state is not Starting/Outgoing/Accepting): $state" }
            return@launchWithLock
        }
        logger.d { "[onCallJoining] call: $call, state: $state" }
        _callState.post(
            State.Joining(
                callGuid = CallGuid(
                    type = call.type,
                    id = call.id,
                    cid = call.cid,
                ),
                createdByUserId = call.createdByUserId,
                broadcastingEnabled = call.broadcastingEnabled,
                recordingEnabled = call.recordingEnabled,
                users = call.users,
                members = call.members
            )
        )
    }

    override fun onCallFailed(error: VideoError) = scope.launchWithLock(mutex) {
        logger.e { "[onCallFailed] error: $error" }
        val state = _callState.value
        if (state !is State.Active) {
            return@launchWithLock
        }
        dropCall(State.Drop(state.callGuid, DropReason.Failure(error)))
    }

    private fun onCallFinished() = scope.launchWithLock(mutex) {
        val state = _callState.value
        logger.d { "[onCallFinished] state: $state" }
        if (state is State.Idle) {
            return@launchWithLock
        }
        _callState.post(State.Idle)
    }

    private fun onCallCancelled() = scope.launchWithLock(mutex) {
        val state = _callState.value
        if (state !is State.Active) {
            logger.w { "[onCallCancelled] rejected (state is not Active): $state" }
            return@launchWithLock
        }
        logger.d { "[onCallCancelled] state: $state" }
        dropCall(State.Drop(state.callGuid, DropReason.Cancelled))
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
        _callState.post(
            event.run {
                State.Incoming(
                    callGuid = CallGuid(
                        type = info.type,
                        id = info.id,
                        cid = info.cid,
                    ),
                    createdByUserId = info.createdByUserId,
                    broadcastingEnabled = info.broadcastingEnabled,
                    recordingEnabled = info.recordingEnabled,
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
