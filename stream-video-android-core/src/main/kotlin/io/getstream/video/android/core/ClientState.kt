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

import androidx.compose.runtime.Stable
import androidx.core.content.ContextCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.notifications.internal.service.CallService
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserType
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.openapitools.client.models.CallCreatedEvent
import org.openapitools.client.models.CallRingEvent
import org.openapitools.client.models.ConnectedEvent
import org.openapitools.client.models.VideoEvent
import java.net.ConnectException

@Stable
public sealed interface ConnectionState {
    public data object PreConnect : ConnectionState
    public data object Loading : ConnectionState
    public data object Connected : ConnectionState
    public data object Reconnecting : ConnectionState
    public data object Disconnected : ConnectionState
    public class Failed(val error: Error) : ConnectionState
}

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
class ClientState(client: StreamVideo) {

    val logger by taggedLogger("ClientState")

    /**
     * Current user object
     */
    private val _user: MutableStateFlow<User?> = MutableStateFlow(client.user)
    public val user: StateFlow<User?> = _user

    private val _connection: MutableStateFlow<ConnectionState> =
        MutableStateFlow(ConnectionState.PreConnect)

    /**
     * Shows the Coordinator connection state
     */
    public val connection: StateFlow<ConnectionState> = _connection

    /**
     * Incoming call. True when we receive an event or notification with an incoming call
     */
    internal val _ringingCall: MutableStateFlow<Call?> = MutableStateFlow(null)
    public val ringingCall: StateFlow<Call?> = _ringingCall

    /**
     * Active call. The call that you've currently joined
     */
    private val _activeCall: MutableStateFlow<Call?> = MutableStateFlow(null)
    public val activeCall: StateFlow<Call?> = _activeCall

    internal val clientImpl = client as StreamVideoImpl

    /**
     * Returns true if there is an active or ringing call
     */
    fun hasActiveOrRingingCall(): Boolean = safeCall(false) {
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
        if (event is ConnectedEvent) {
            _connection.value = ConnectionState.Connected

            registerPushDevice()
        } else if (event is CallCreatedEvent) {
            // what's the right thing to do here?
            // if it's ringing we add it

            // get or create the call, update is handled by CallState
            val (type, id) = event.callCid.split(":")
            val call = clientImpl.call(type, id)
        } else if (event is CallRingEvent) {
            // get or create the call, update is handled by CallState
            val (type, id) = event.callCid.split(":")
            val call = clientImpl.call(type, id)
            _ringingCall.value = call
        }
    }

    private fun registerPushDevice() {
        with(clientImpl) {
            scope.launch(CoroutineName("ClientState#registerPushDevice")) {
                if (user.type == UserType.Authenticated) registerPushDevice()
            }
        }
    }

    internal fun handleError(error: Throwable) {
        if (error is ConnectException) {
            _connection.value = ConnectionState.Failed(error = Error(error))
        }
    }

    fun setActiveCall(call: Call) {
        removeRingingCall()
        maybeStartForegroundService(call, CallService.TRIGGER_ONGOING_CALL)
        this._activeCall.value = call
    }

    fun removeActiveCall() {
        this._activeCall.value = null
        maybeStopForegroundService()
        removeRingingCall()
    }

    fun addRingingCall(call: Call, ringingState: RingingState) {
        _ringingCall.value = call
        if (ringingState is RingingState.Outgoing) {
            maybeStartForegroundService(call, CallService.TRIGGER_OUTGOING_CALL)
        }

        // TODO: behaviour if you are already in a call
    }

    fun removeRingingCall() {
        _ringingCall.value = null
    }

    /**
     * Start a foreground service that manages the call even when the UI is gone.
     * This depends on the flag in [StreamVideoBuilder] called `runForegroundServiceForCalls`
     */
    internal fun maybeStartForegroundService(call: Call, trigger: String) {
        if (clientImpl.callServiceConfig.runCallServiceInForeground) {
            val context = clientImpl.context
            val serviceIntent = CallService.buildStartIntent(
                context,
                StreamCallId.fromCallCid(call.cid),
                trigger,
                callServiceConfiguration = clientImpl.callServiceConfig,
            )
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }

    /**
     * Stop the foreground service that manages the call even when the UI is gone.
     */
    internal fun maybeStopForegroundService() {
        if (clientImpl.callServiceConfig.runCallServiceInForeground) {
            val context = clientImpl.context
            val serviceIntent = CallService.buildStopIntent(
                context,
                callServiceConfiguration = clientImpl.callServiceConfig,
            )
            context.stopService(serviceIntent)
        }
    }
}
