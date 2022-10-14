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
import io.getstream.video.android.model.state.StreamCallState
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

    private val _callState = MutableStateFlow<StreamCallState>(StreamCallState.Idle)

    override val callState: StateFlow<StreamCallState> = _callState

    override fun onEvent(event: VideoEvent) {
        logger.v { "[onEvent] event: $event" }
        when (event) {
            is AudioMutedEvent -> {}
            is AudioUnmutedEvent -> {}
            is CallCreatedEvent -> onCallCreated(event)
            is CallMembersDeletedEvent -> {}
            is CallMembersUpdatedEvent -> {}
            is CallUpdatedEvent -> {}
            is CallAcceptedEvent -> {}
            is CallRejectedEvent -> {}
            is CallEndedEvent -> onCallFinished()
            is CallCanceledEvent -> onCallFinished()
            is ConnectedEvent -> {}
            is HealthCheckEvent -> {}
            is ParticipantJoinedEvent -> {}
            is ParticipantLeftEvent -> {}
            is VideoStartedEvent -> {}
            is VideoStoppedEvent -> {}
            is UnknownEvent -> {}
        }
    }

    override fun onCallJoined(joinedCall: JoinedCall) = scope.launchWithLock(mutex) {
        _callState.emit(StreamCallState.InCall(joinedCall))
    }

    override fun onCallConnecting() = scope.launchWithLock(mutex) {
        _callState.emit(StreamCallState.Connecting)
    }

    override fun resetCallState() = scope.launchWithLock(mutex) {
        _callState.emit(StreamCallState.Idle)
    }

    private fun onCallFinished() = scope.launchWithLock(mutex) {
        _callState.emit(StreamCallState.Idle)
    }

    override fun onOutgoingCall(callMetadata: CallMetadata) = scope.launchWithLock(mutex) {
        val state = _callState.value
        if (state !is StreamCallState.Idle) {
            logger.w { "[onOutgoingCall] rejected (state is not Idle): $state" }
            return@launchWithLock
        }
        _callState.emit(
            StreamCallState.Outgoing(
                callId = callMetadata.id,
                users = callMetadata.users,
                info = callMetadata.toInfo(),
                details = callMetadata.toDetails()
            )
        )
    }

    private fun onCallCreated(event: CallCreatedEvent) = scope.launchWithLock(mutex) {
        val state = _callState.value
        if (state !is StreamCallState.Idle) {
            logger.w { "[onCallCreated] rejected (state is not Idle): $state" }
            return@launchWithLock
        }
        _callState.emit(
            event.run {
                StreamCallState.Incoming(
                    callId = callId,
                    users = users,
                    info = info,
                    details = details
                )
            }
        )
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
