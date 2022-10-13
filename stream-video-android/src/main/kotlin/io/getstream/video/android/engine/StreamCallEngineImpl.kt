package io.getstream.video.android.engine

import io.getstream.logging.StreamLog
import io.getstream.video.android.events.AudioMutedEvent
import io.getstream.video.android.events.AudioUnmutedEvent
import io.getstream.video.android.events.CallCreatedEvent
import io.getstream.video.android.events.CallEndedEvent
import io.getstream.video.android.events.CallMembersDeletedEvent
import io.getstream.video.android.events.CallMembersUpdatedEvent
import io.getstream.video.android.events.CallStartedEvent
import io.getstream.video.android.events.CallUpdatedEvent
import io.getstream.video.android.events.ConnectedEvent
import io.getstream.video.android.events.HealthCheckEvent
import io.getstream.video.android.events.ParticipantJoinedEvent
import io.getstream.video.android.events.ParticipantLeftEvent
import io.getstream.video.android.events.UnknownEvent
import io.getstream.video.android.events.VideoEvent
import io.getstream.video.android.events.VideoStartedEvent
import io.getstream.video.android.events.VideoStoppedEvent
import io.getstream.video.android.model.state.StreamCallState
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

    override fun onEvent(event: VideoEvent){
        scope.launchWithLock(mutex) {
            logger.v { "[onEvent] event: $event" }
            when(event) {
                is AudioMutedEvent -> {}
                is AudioUnmutedEvent -> {}
                is CallCreatedEvent -> onCallCreated(event)
                is CallEndedEvent -> {}
                is CallMembersDeletedEvent -> {}
                is CallMembersUpdatedEvent -> {}
                is CallStartedEvent -> {}
                is CallUpdatedEvent -> {}
                is ConnectedEvent -> {}
                is HealthCheckEvent -> {}
                is ParticipantJoinedEvent -> {}
                is ParticipantLeftEvent -> {}
                is VideoStartedEvent -> {}
                is VideoStoppedEvent -> {}
                is UnknownEvent -> {}
            }
        }
    }

    private suspend fun onCallCreated(event: CallCreatedEvent) {
        val state = _callState.value
        if (state !is StreamCallState.Idle) {
            logger.w { "[onCallCreated] rejected (state is not Idle): $state" }
            return
        }
        _callState.emit(event.run {
            StreamCallState.Incoming(
                callId = callId,
                users = users,
                info = info,
                details = details
            )
        })
    }


}

private fun CoroutineScope.launchWithLock(
    mutex: Mutex,
    action: suspend CoroutineScope.() -> Unit
) = launch {
    mutex.withLock {
        action()
    }
}