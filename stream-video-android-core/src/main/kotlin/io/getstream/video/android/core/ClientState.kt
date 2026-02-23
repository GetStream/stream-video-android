/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

import androidx.compose.runtime.Stable
import io.getstream.android.video.generated.models.CallCreatedEvent
import io.getstream.android.video.generated.models.CallRingEvent
import io.getstream.android.video.generated.models.ConnectedEvent
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.video.android.core.notifications.internal.service.CallService
import io.getstream.video.android.core.notifications.internal.service.ServiceLauncher
import io.getstream.video.android.core.notifications.internal.telecom.TelecomIntegrationType
import io.getstream.video.android.core.socket.coordinator.state.VideoSocketState
import io.getstream.video.android.core.utils.safeCallWithDefault
import io.getstream.video.android.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// These are UI states, need to move out.
@Stable
public sealed interface ConnectionState {
    public data object PreConnect : ConnectionState
    public data object Loading : ConnectionState
    public data object Connected : ConnectionState
    public data object Reconnecting : ConnectionState
    public data object Disconnected : ConnectionState
    public class Failed(val error: Error) : ConnectionState
}

// These are UI states, need to move out.
@Stable
public sealed interface RingingState {
    public data object Idle : RingingState
    public data class Incoming(val acceptedByMe: Boolean = false) : RingingState
    public class Outgoing(val acceptedByCallee: Boolean = false) : RingingState
    public data object Active : RingingState
    public data object RejectedByAll : RingingState
    public data object TimeoutNoAnswer : RingingState
}

@Stable
class ClientState(private val client: StreamVideo) {
    private val logger by taggedLogger("ClientState")

    // Internal data
    private val _user: MutableStateFlow<User?> = MutableStateFlow(client.user)
    private val _connection: MutableStateFlow<ConnectionState> =
        MutableStateFlow(ConnectionState.PreConnect)
    internal val _ringingCall: MutableStateFlow<Call?> = MutableStateFlow(null)
    private val _activeCall: MutableStateFlow<Call?> = MutableStateFlow(null)

    // Stream video client is used until full decoupling is archived between `CallState` and `StreamVideoClient (former StreamVideoImpl)
    private val streamVideoClient: StreamVideoClient = client as StreamVideoClient

    // API
    /** Current user for the client. */
    public val user: StateFlow<User?> = _user

    /** Coordinator connection state */
    public val connection: StateFlow<ConnectionState> = _connection

    /** When there is an incoming call, this state will be set. */
    public val ringingCall: StateFlow<Call?> = _ringingCall

    /** When there is an active call, this state will be set, otherwise its null. */
    public val activeCall: StateFlow<Call?> = _activeCall

    public val callConfigRegistry = (client as StreamVideoClient).callServiceConfigRegistry
    private val serviceLauncher = ServiceLauncher(client.context)

    /**
     * Returns true if there is an active or ringing call
     */
    public fun hasActiveOrRingingCall(): Boolean = safeCallWithDefault(false) {
        val hasActiveCall = _activeCall.value != null
        val hasRingingCall = _ringingCall.value != null
        val activeOrRingingCall = hasActiveCall || hasRingingCall
        logger.d { "[hasActiveOrRingingCall] active: $hasActiveCall, ringing: $hasRingingCall" }
        activeOrRingingCall
    }

    /**
     * Hardcoded to [TelecomIntegrationType.JETPACK_TELECOM] for now.
     * May switch to another later if needed.
     */
    fun getTelecomIntegrationType(): TelecomIntegrationType? {
        return if (streamVideoClient.telecomConfig != null) {
            TelecomIntegrationType.JETPACK_TELECOM
        } else {
            null
        }
    }

    /**
     * Handles the events for the client state.
     * Most event logic happens in the Call instead of the client
     */
    fun handleEvent(event: VideoEvent) {
        // mark connected
        when (event) {
            is ConnectedEvent -> {
                _connection.value = ConnectionState.Connected
            }

            is CallCreatedEvent -> {
                // what's the right thing to do here?
                // if it's ringing we add it

                // get or create the call, update is handled by CallState
                val (type, id) = event.callCid.split(":")
                val call = client.call(type, id)
            }

            is CallRingEvent -> {
                val (type, id) = event.callCid.split(":")
                val call = client.call(type, id)
                _ringingCall.value = call
            }
        }
    }

    internal fun handleState(socketState: VideoSocketState) {
        val state = when (socketState) {
            // Before connection is established
            is VideoSocketState.Disconnected.Stopped -> ConnectionState.PreConnect
            // Loading
            is VideoSocketState.Connecting -> ConnectionState.Loading
            // Connected
            is VideoSocketState.Connected -> ConnectionState.Connected
            //  Reconnecting
            is VideoSocketState.Disconnected.DisconnectedTemporarily -> ConnectionState.Reconnecting
            is VideoSocketState.RestartConnection -> ConnectionState.Reconnecting
            // Disconnected
            is VideoSocketState.Disconnected.WebSocketEventLost -> ConnectionState.Disconnected
            is VideoSocketState.Disconnected.NetworkDisconnected -> ConnectionState.Disconnected
            is VideoSocketState.Disconnected.DisconnectedByRequest -> ConnectionState.Disconnected
            is VideoSocketState.Disconnected.DisconnectedPermanently -> ConnectionState.Disconnected
        }
        _connection.value = state
    }

    fun handleError(error: Error) {
        _connection.value = ConnectionState.Failed(error)
    }

    /**
     * Transition incoming/outgoing call to active on the same service
     */
    fun setActiveCall(call: Call) {
        this._activeCall.value = call
        val serviceTransitionDelayMs = 500L
        val ringingState = call.state.ringingState.value
        when (ringingState) {
            is RingingState.Incoming -> {
                call.scope.launch {
                    transitionToAcceptCall(call)
                    delay(serviceTransitionDelayMs)
                    maybeStartForegroundService(call, CallService.Companion.Trigger.OnGoingCall)
                }
            }
            is RingingState.Outgoing -> {
                call.scope.launch {
                    transitionToAcceptCall(call)
                    delay(serviceTransitionDelayMs)
                    maybeStartForegroundService(call, CallService.Companion.Trigger.OnGoingCall)
                }
            }
            else -> {
                removeRingingCall(call)
                call.scope.launch {
                    delay(serviceTransitionDelayMs)
                    maybeStartForegroundService(call, CallService.Companion.Trigger.OnGoingCall)
                }
            }
        }
    }

    /**
     * Use removeActiveCall(call: Call) instead, requires explicit call instance.
     */
    fun removeActiveCall() {
        _activeCall.value?.let {
            maybeStopForegroundService(it)
        }
        this._activeCall.value = null
        removeRingingCall() // TODO shouldn't be here because we have no idea of which call is responsible for ringing
    }

    internal fun removeActiveCall(call: Call) {
        logger.d {
            "[removeActiveCall] call.id == activeCall.value?.id :${call.id == activeCall.value?.id}"
        }
        if (call.id == activeCall.value?.id) {
            _activeCall.value?.let {
                maybeStopForegroundService(it)
            }
            this._activeCall.value = null
            removeRingingCall(call)
        }
    }

    fun addRingingCall(call: Call, ringingState: RingingState) {
        _ringingCall.value = call
        if (ringingState is RingingState.Outgoing) {
            maybeStartForegroundService(call, CallService.Companion.Trigger.OutgoingCall)
        }

        // TODO: behaviour if you are already in a call
    }

    /**
     * Use removeRingingCall(call: Call) instead, requires explicit call instance..
     */
    fun removeRingingCall() {
        ringingCall.value?.let {
            maybeStopForegroundService(it)
        }
        _ringingCall.value = null
    }

    fun removeRingingCall(call: Call) {
        logger.d {
            "[removeRingingCall] call.id == ringingCall.value?.id: ${call.id == ringingCall.value?.id}"
        }
        if (call.id == ringingCall.value?.id) {
            (client as StreamVideoClient).callSoundAndVibrationPlayer.stopCallSound()
            ringingCall.value?.let {
                maybeStopForegroundService(it)
            }
            _ringingCall.value = null
        }
    }

    internal fun transitionToAcceptCall(call: Call) {
        if (call.id == ringingCall.value?.id) {
            (client as StreamVideoClient).callSoundAndVibrationPlayer.stopCallSound()
            _ringingCall.value = null
        }
    }

    /**
     * Start a foreground service that manages the call even when the UI is gone.
     * This depends on the flag in [StreamVideoBuilder] called `runForegroundServiceForCalls`
     */
    internal fun maybeStartForegroundService(call: Call, trigger: CallService.Companion.Trigger) {
        logger.d { "[maybeStartForegroundService], trigger: $trigger" }
        when (trigger) {
            CallService.Companion.Trigger.OnGoingCall -> serviceLauncher.showOnGoingCall(
                call,
                trigger,
                streamVideoClient,
            )

            CallService.Companion.Trigger.OutgoingCall -> serviceLauncher.showOutgoingCall(
                call,
                trigger,
                streamVideoClient,
            )

            else -> {}
        }
    }

    /**
     * Stop the foreground service that manages the call even when the UI is gone.
     */
    internal fun maybeStopForegroundService(call: Call) {
        val callConfig = streamVideoClient.callServiceConfigRegistry.get(call.type)
        if (callConfig.runCallServiceInForeground) {
            val context = streamVideoClient.context

            logger.d { "Building stop intent for call_id: ${call.cid}" }
            val serviceLauncher = ServiceLauncher(context)
            serviceLauncher.stopService(call)
        }
    }
}
