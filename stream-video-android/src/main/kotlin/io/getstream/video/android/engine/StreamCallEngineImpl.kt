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
import io.getstream.video.android.model.state.DropReason
import io.getstream.video.android.model.toDetails
import io.getstream.video.android.model.toInfo
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
import io.getstream.video.android.model.state.StreamCallState as State

/**
 * Should be written in pure kotlin.
 * No android imports are allowed here.
 */
internal class StreamCallEngineImpl(
    parentScope: CoroutineScope
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
            is CallAcceptedEvent -> onCallAccepted()
            is CallRejectedEvent -> onCallRejected()
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

    private fun onCallAccepted() = scope.launchWithLock(mutex) {
        logger.d { "[onCallAccepted] no args" }
    }

    private fun onCallRejected() = scope.launchWithLock(mutex) {
        logger.d { "[onCallRejected] no args" }
        val state = _callState.value
        if (state is State.Idle) {
            return@launchWithLock
        }
        _callState.post(State.Drop(reason = DropReason.Rejected))
        _callState.post(State.Idle)
    }

    override fun onCallJoined(joinedCall: JoinedCall) = scope.launchWithLock(mutex) {
        logger.d { "[onCallJoined] joinedCall: $joinedCall" }
        _callState.post(
            State.InCall(joinedCall)
        )
    }

    override fun onCallStarting(
        type: String,
        id: String,
        participantIds: List<String>,
        ringing: Boolean,
        forcedNewCall: Boolean
    ) = scope.launchWithLock(mutex) {
        logger.d {
            "[onCallStarting] type: $type, id: $id, ringing: $ringing, " +
                "forcedNewCall: $forcedNewCall, participantIds: $participantIds"
        }
        // _callState.emit(StreamCallState.Starting)
    }

    override fun onCallStarted(call: CallMetadata) = scope.launchWithLock(mutex) {
        logger.d { "[onCallStarted] call: $call" }
    }

    override fun onCallSendEvent(type: String, id: String, event: UserEventType) = scope.launchWithLock(mutex) {
        logger.d { "[onCallSendEvent] type: $type, id: $id, event: $UserEventType" }
    }

    override fun onCallJoining(call: CallMetadata) = scope.launchWithLock(mutex) {
        logger.d { "[onCallJoining] call: $call" }
    }

    override fun onCallFailed(error: VideoError) = scope.launchWithLock(mutex) {
        logger.e { "[onCallFailed] error: $error" }
        val state = _callState.value
        if (state is State.Idle) {
            return@launchWithLock
        }
        _callState.post(State.Drop(reason = DropReason.Failure(error)))
        _callState.post(State.Idle)
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
        logger.d { "[onCallCancelled] state: $state" }
        if (state is State.Idle) {
            return@launchWithLock
        }
        _callState.post(State.Drop(reason = DropReason.Cancelled))
        _callState.post(State.Idle)
    }

    override fun onOutgoingCall(call: CallMetadata) = scope.launchWithLock(mutex) {
        val state = _callState.value
        logger.w { "[onOutgoingCall] rejected (state is not Idle): $state" }
        if (state !is State.Idle) {
            return@launchWithLock
        }
        _callState.post(
            State.Outgoing(
                callCid = call.cid,
                users = call.users,
                info = call.toInfo(),
                details = call.toDetails()
            )
        )
    }

    private fun onCallCreated(event: CallCreatedEvent) = scope.launchWithLock(mutex) {
        val state = _callState.value
        if (state !is State.Idle) {
            logger.w { "[onCallCreated] rejected (state is not Idle): $state" }
            return@launchWithLock
        }
        _callState.post(
            event.run {
                State.Incoming(
                    callCid = callCid,
                    users = users,
                    info = info,
                    details = details
                )
            }
        )
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
