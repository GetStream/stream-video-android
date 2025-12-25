/*
 * Copyright (c) 2014-2025 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.notifications.internal.service.observers

import io.getstream.android.video.generated.models.CallEndedEvent
import io.getstream.android.video.generated.models.LocalCallAcceptedEvent
import io.getstream.android.video.generated.models.LocalCallMissedEvent
import io.getstream.android.video.generated.models.LocalCallRejectedEvent
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.utils.toUser
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal class CallEventObserver(
    private val call: Call,
    private val streamVideo: StreamVideoClient,
) {

    private val logger by taggedLogger("CallEventObserver")

    /**
     * Starts observing call events and connection state.
     */
    fun observe(onServiceStop: () -> Unit, onRemoveIncoming: () -> Unit) {
        observeCallEvents(onServiceStop, onRemoveIncoming)
        observeConnectionState(onServiceStop)
    }

    /**
     * Observes call events (accepted, rejected, ended, missed).
     */
    private fun observeCallEvents(onServiceStop: () -> Unit, onRemoveIncoming: () -> Unit) {
        call.scope.launch {
            call.events.collect { event ->
                handleCallEvent(event, onServiceStop, onRemoveIncoming)
            }
        }
    }

    /**
     * Handles different types of call events.
     */
    private fun handleCallEvent(
        event: Any,
        onServiceStop: () -> Unit,
        onRemoveIncoming: () -> Unit,
    ) {
        when (event) {
            is LocalCallAcceptedEvent -> {
                handleIncomingCallAcceptedByMeOnAnotherDevice(
                    event.user.id,
                    streamVideo.userId,
                    onServiceStop,
                )
            }
            is LocalCallRejectedEvent -> {
                handleIncomingCallRejectedByMeOrCaller(
                    rejectedByUserId = event.user.id,
                    myUserId = streamVideo.userId,
                    createdByUserId = event.call.createdBy.toUser().id,
                    activeCallExists = streamVideo.state.activeCall.value != null,
                    onServiceStop = onServiceStop,
                    onRemoveIncoming = onRemoveIncoming,
                )
            }
            is CallEndedEvent -> onServiceStop()
            is LocalCallMissedEvent -> {
                val activeCallExists = streamVideo.state.activeCall.value != null
                if (activeCallExists) {
                    // Another call is active - just remove incoming notification
                    onRemoveIncoming()
                } else {
                    // No other call - stop service
                    onServiceStop()
                }
            }
        }
    }

    /**
     * Handles call accepted event - stops service if accepted on another device.
     */
    private fun handleIncomingCallAcceptedByMeOnAnotherDevice(
        acceptedByUserId: String,
        myUserId: String,
        onServiceStop: () -> Unit,
    ) {
        logger.d { "[handleIncomingCallAcceptedByMeOnAnotherDevice]" }
        val callRingingState = call.state.ringingState.value

        // If I accepted the call on another device while this device is still ringing
        if (acceptedByUserId == myUserId && callRingingState is RingingState.Incoming) {
            onServiceStop() // noob 1
        }
    }

    /**
     * Handles call rejected event.
     */
    private fun handleIncomingCallRejectedByMeOrCaller(
        rejectedByUserId: String,
        myUserId: String,
        createdByUserId: String?,
        activeCallExists: Boolean,
        onServiceStop: () -> Unit,
        onRemoveIncoming: () -> Unit,
    ) {
        // Stop service if rejected by me or by the caller
        logger.d {
            "[handleIncomingCallRejectedByMeOrCaller] rejectedByUserId == myUserId :${rejectedByUserId == myUserId}, rejectedByUserId == createdByUserId :${rejectedByUserId == createdByUserId}"
        }

        if (rejectedByUserId == myUserId || rejectedByUserId == createdByUserId) {
            if (activeCallExists) {
                // Another call is active - just remove incoming notification
                onRemoveIncoming()
            } else {
                // No other call - stop service
                onServiceStop()
            }
        }
    }

    /**
     * Observes connection state changes.
     */
    private fun observeConnectionState(onServiceStop: () -> Unit) {
        call.scope.launch {
            call.state.connection.collectLatest { event ->
                if (event is RealtimeConnection.Failed) {
                    handleConnectionFailure()
                }
            }
        }
    }

    /**
     * Handles connection failure for ringing calls.
     */
    private fun handleConnectionFailure() {
        if (call.id == streamVideo.state.ringingCall.value?.id) {
            streamVideo.state.removeRingingCall(call)
            streamVideo.onCallCleanUp(call)
        }
    }
}
