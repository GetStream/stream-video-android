/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

import android.annotation.SuppressLint
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.compose.runtime.Stable
import androidx.core.content.ContextCompat
import io.getstream.android.video.generated.models.CallCreatedEvent
import io.getstream.android.video.generated.models.CallRingEvent
import io.getstream.android.video.generated.models.ConnectedEvent
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.core.notifications.internal.service.CallService
import io.getstream.video.android.core.notifications.internal.service.telecom.INTENT_EXTRA_IS_ONGOING_CALL
import io.getstream.video.android.core.notifications.internal.service.telecom.TelecomConnection
import io.getstream.video.android.core.notifications.internal.service.telecom.getMyPhoneAccountHandle
import io.getstream.video.android.core.notifications.internal.service.telecom.getTelecomAddress
import io.getstream.video.android.core.notifications.internal.service.telecom.getTelecomManager
import io.getstream.video.android.core.notifications.internal.service.telecom.isTelecomSupported
import io.getstream.video.android.core.notifications.internal.service.telecom.telecomConnections
import io.getstream.video.android.core.notifications.internal.service.telecom.useTelecom
import io.getstream.video.android.core.socket.coordinator.state.VideoSocketState
import io.getstream.video.android.core.utils.safeCallWithDefault
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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

    fun setActiveCall(call: Call) {
        this._activeCall.value = call

        if (useTelecom(client.context, callConfigRegistry.get(call.type))) {
//            val connection = telecomConnections[call.cid] ?: TelecomConnection.createAndStore(
//                context = client.context.applicationContext,
//                callId = StreamCallId.fromCallCid(call.cid),
//                callConfig = callConfigRegistry.get(call.type),
//            )
//            connection.setActive()
            placeTelecomCall(call, isOngoingCall = true)
        } else {
            removeRingingCall()
            maybeStartForegroundService(call, CallService.TRIGGER_ONGOING_CALL)
        }

        logger.d {
            "[setActiveCall] #telecom; cid: ${call.cid}, setActive was called"
        }
    }

    fun setActiveCall2(call: Call) {
        this._activeCall.value = call
        removeRingingCall()
//        maybeStartForegroundService(call, CallService.TRIGGER_ONGOING_CALL)

        val connection = (
            telecomConnections[call.cid] ?: TelecomConnection.createAndStore(
                context = client.context.applicationContext,
                callId = StreamCallId.fromCallCid(call.cid),
                callConfig = callConfigRegistry.get(call.type),
            )
            )
        connection.setActive()

        logger.d {
            "[setActiveCall] #telecom; connection: ${connection.hashCode()}, cid: ${call.cid}, setActive was called"
        }
    }

    fun removeActiveCall() {
        _activeCall.value?.let { call ->
            if (useTelecom(client.context, callConfigRegistry.get(call.type))) {
                telecomConnections.remove(call.cid)?.cleanUp()
            } else {
                maybeStopForegroundService(call)
            }

            this._activeCall.value = null
        }

        removeRingingCall()
    }

    fun addRingingCall(call: Call, ringingState: RingingState) {
        _ringingCall.value = call

        if (ringingState is RingingState.Outgoing) {
            if (useTelecom(client.context, callConfigRegistry.get(call.type))) {
                placeTelecomCall(call)
            } else {
                maybeStartForegroundService(call, CallService.TRIGGER_OUTGOING_CALL)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun placeTelecomCall(call: Call, isOngoingCall: Boolean = false) {
        val context = client.context
        val phoneAccountHandle = getMyPhoneAccountHandle(context)
        val telecomManager = getTelecomManager(context)

        val extras = Bundle().apply {
            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
            putBundle(
                TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS,
                Bundle().apply {
                    putString(NotificationHandler.INTENT_EXTRA_CALL_CID, call.cid)
                    putString(NotificationHandler.INTENT_EXTRA_CALL_DISPLAY_NAME, "Calling...")
                    if (isOngoingCall) {
                        putBoolean(INTENT_EXTRA_IS_ONGOING_CALL, true)
                    }
                },
            )
        }

        logger.d {
            "[placeTelecomCall] #telecom; telecomManager not null: ${telecomManager != null}, cid: ${call.cid}, cid hash: ${call.cid.hashCode()}, will placeCall"
        }

        telecomManager?.placeCall(getTelecomAddress(call.cid), extras)
    }

    fun removeRingingCall(willTransitionToOngoing: Boolean = false) {
        ringingCall.value?.let {
            if (useTelecom(client.context, callConfigRegistry.get(it.type))) {
                if (!willTransitionToOngoing) {
                    telecomConnections.remove(it.cid)?.cleanUp()
                }
            } else {
                maybeStopForegroundService(it)
            }
        }

        _ringingCall.value = null
    }

    /**
     * Start a foreground service that manages the call even when the UI is gone.
     * This depends on the flag in [StreamVideoBuilder] called `runForegroundServiceForCalls`
     */
    internal fun maybeStartForegroundService(call: Call, trigger: String) {
        val callConfig = streamVideoClient.callServiceConfigRegistry.get(call.type)
        if (callConfig.runCallServiceInForeground) {
            val context = streamVideoClient.context
            val serviceIntent = CallService.buildStartIntent(
                context,
                StreamCallId.fromCallCid(call.cid),
                trigger,
                callServiceConfiguration = callConfig,
            )
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }

    /**
     * Stop the foreground service that manages the call even when the UI is gone.
     */
    internal fun maybeStopForegroundService(call: Call) {
        val callConfig = streamVideoClient.callServiceConfigRegistry.get(call.type)
        if (callConfig.runCallServiceInForeground) {
            val context = streamVideoClient.context
            val serviceIntent = CallService.buildStopIntent(
                context,
                callConfig,
            )
            context.stopService(serviceIntent)
        }
    }
}
