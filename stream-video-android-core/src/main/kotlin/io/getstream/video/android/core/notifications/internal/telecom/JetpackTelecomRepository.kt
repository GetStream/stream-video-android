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

package io.getstream.video.android.core.notifications.internal.telecom

import android.Manifest
import android.net.Uri
import android.os.Build
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallsManager
import io.getstream.log.taggedLogger
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class JetpackTelecomRepository(
    private val callsManager: CallsManager,
    val callId: StreamCallId,
    private val incomingCallTelecomAction: IncomingCallTelecomAction,
) {
    private val logger by taggedLogger("JetpackTelecomRepository")

    // Keeps track of the current TelecomCall state
    private val _currentCall: MutableStateFlow<TelecomCall> = MutableStateFlow(TelecomCall.None)
    val currentCall = _currentCall.asStateFlow()

    /**
     * Register a new call with the provided attributes.
     * Use the [currentCall] StateFlow to receive status updates and process call related actions.
     */
    @RequiresPermission(Manifest.permission.MANAGE_OWN_CALLS)
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun registerCall(
        displayName: String,
        address: Uri,
        isIncoming: Boolean,
        isVideoCall: Boolean,
    ) {
        logger.d { "[registerCall]" }
        // For simplicity we don't support multiple calls
        if (_currentCall.value is TelecomCall.Registered) {
            logger.e { "[registerCall] There cannot be more than one call at the same time." }
            return
        }

        // Create the call attributes
        val attributes = CallAttributesCompat(
            displayName = displayName,
            address = address,
            direction = if (isIncoming) {
                CallAttributesCompat.DIRECTION_INCOMING
            } else {
                CallAttributesCompat.DIRECTION_OUTGOING
            },
            callType = if (isVideoCall) {
                CallAttributesCompat.CALL_TYPE_VIDEO_CALL
            } else {
                CallAttributesCompat.CALL_TYPE_AUDIO_CALL
            },
            callCapabilities = (
                CallAttributesCompat.SUPPORTS_SET_INACTIVE
                    or CallAttributesCompat.SUPPORTS_STREAM
                    or CallAttributesCompat.SUPPORTS_TRANSFER
                ),
        )

        // Creates a channel to send actions to the call scope.
        val actionSource = Channel<TelecomCallAction>()
        // Register the call and handle actions in the scope
        try {
            logger.d { "[registerCall] addCall" }
            callsManager.addCall(
                attributes,
                onIsCallAnswered, // Watch needs to know if it can answer the call
                onIsCallDisconnected,
                onIsCallActive,
                onIsCallInactive,
            ) {
                // Consume the actions to interact with the call inside the scope
                launch {
                    processCallActions(actionSource.consumeAsFlow())
                }

                // Update the state to registered with default values while waiting for Telecom updates
                _currentCall.value = TelecomCall.Registered(
                    id = getCallId(),
                    isActive = false,
                    isOnHold = false,
                    callAttributes = attributes,
                    isMuted = false,
                    errorCode = null,
                    currentCallEndpoint = null,
                    availableCallEndpoints = emptyList(),
                    actionSource = actionSource,
                )
                logger.d { "[registerCall] _currentCall set to Registered" }
                launch {
                    currentCallEndpoint.collect {
                        updateCurrentCall {
                            copy(currentCallEndpoint = it)
                        }
                    }
                }
                launch {
                    availableEndpoints.collect {
                        updateCurrentCall {
                            copy(availableCallEndpoints = it)
                        }
                    }
                }
                launch {
                    isMuted.collect {
                        updateCurrentCall {
                            copy(isMuted = it)
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            logger.e(ex) { "[registerCall] exception: ${ex.message}" }
        } finally {
            logger.d { "[registerCall] finally" }
            _currentCall.value = TelecomCall.None
        }
    }

    /**
     * Collect the action source to handle client actions inside the call scope
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun CallControlScope.processCallActions(actionSource: Flow<TelecomCallAction>) {
        actionSource.collect { action ->
            logger.d { "[processCallActions]: action: $action" }
            when (action) {
                is TelecomCallAction.Answer -> {
                    doAnswer(action.isAudioCall)
                }

                is TelecomCallAction.Disconnect -> {
                    doDisconnect(action)
                }

                is TelecomCallAction.SwitchAudioEndpoint -> {
                    doSwitchEndpoint(action)
                }

                is TelecomCallAction.TransferCall -> {
                    val call = _currentCall.value as? TelecomCall.Registered
                    val endpoints = call?.availableCallEndpoints?.firstOrNull {
                        it.identifier == action.endpointId
                    }
                    requestEndpointChange(
                        endpoint = endpoints ?: return@collect,
                    )
                }

                TelecomCallAction.Hold -> {
                    when (val result = setInactive()) {
                        is CallControlResult.Success -> {
                            onIsCallInactive()
                        }

                        is CallControlResult.Error -> {
                            updateCurrentCall {
                                copy(errorCode = result.errorCode)
                            }
                        }
                    }
                }

                TelecomCallAction.Activate -> {
                    when (val result = setActive()) {
                        is CallControlResult.Success -> {
                            logger.d { "[processCallActions] CallControlResult.Success" }
                            onIsCallActive()
                        }

                        is CallControlResult.Error -> {
                            logger.d { "[processCallActions] CallControlResult.Error errorCode: ${result.errorCode}" }
                            updateCurrentCall {
                                copy(errorCode = result.errorCode)
                            }
                        }
                    }
                }

                is TelecomCallAction.ToggleMute -> {
                    // We cannot programmatically mute the telecom stack. Instead we just update
                    // the state of the call and this will start/stop audio capturing.
                    updateCurrentCall {
                        copy(isMuted = !isMuted)
                    }
                }
            }
        }
    }

    /**
     * Update the current state of our call applying the transform lambda only if the call is
     * registered. Otherwise keep the current state
     */
    private fun updateCurrentCall(transform: TelecomCall.Registered.() -> TelecomCall) {
        _currentCall.update { call ->
            if (call is TelecomCall.Registered) {
                call.transform()
            } else {
                call
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun CallControlScope.doSwitchEndpoint(
        action: TelecomCallAction.SwitchAudioEndpoint,
    ) {
        logger.d { "[doSwitchEndpoint], action: $action" }
        // TODO once availableCallEndpoints is a state flow we can just get the value
        val endpoints = (_currentCall.value as TelecomCall.Registered).availableCallEndpoints

        // Switch to the given endpoint or fallback to the best possible one.
        val newEndpoint = endpoints.firstOrNull { it.identifier == action.endpointId }

        if (newEndpoint != null) {
            requestEndpointChange(newEndpoint).also {
                logger.d { "[doSwitchEndpoint] Endpoint ${newEndpoint.name} changed: $it " }
            }
        }
    }

    private suspend fun CallControlScope.doDisconnect(action: TelecomCallAction.Disconnect) {
        logger.d { "[doDisconnect] action: $action " }
        disconnect(action.cause)
        updateCurrentCall {
            TelecomCall.Unregistered(id, callAttributes, action.cause)
        }
    }

    private suspend fun CallControlScope.doAnswer(isAudioCall: Boolean) {
        val callType = if (isAudioCall) {
            CallAttributesCompat.CALL_TYPE_AUDIO_CALL
        } else {
            CallAttributesCompat.CALL_TYPE_VIDEO_CALL
        }
        val result = answer(callType)

        when (result) {
            is CallControlResult.Success -> {
                onIsCallAnswered(callType)
            }

            is CallControlResult.Error -> {
                updateCurrentCall {
                    TelecomCall.Unregistered(
                        id = id,
                        callAttributes = callAttributes,
                        disconnectCause = DisconnectCause(DisconnectCause.BUSY),
                    )
                }
            }
        }
    }

    /**
     *  Can the call be successfully answered??
     *  TIP: We would check the connection/call state to see if we can answer a call
     *  Example you may need to wait for another call to hold.
     **/
    val onIsCallAnswered: suspend(type: Int) -> Unit = {
        logger.d { "[onIsCallAnswered]" }
        updateCurrentCall {
            copy(isActive = true, isOnHold = false)
        }
        incomingCallTelecomAction.onAnswer(callId)
    }

    /**
     * Can the call perform a disconnect
     */
    private val onIsCallDisconnected: suspend (cause: DisconnectCause) -> Unit = { cause ->
        logger.d { "[onIsCallDisconnected] with cause $cause" }
        updateCurrentCall {
            TelecomCall.Unregistered(id, callAttributes, cause)
        }
        incomingCallTelecomAction.onDisconnect(callId)
    }

    /**
     *  Check is see if we can make the call active.
     *  Other calls and state might stop us from activating the call
     */
    val onIsCallActive: suspend () -> Unit = {
        logger.d { "[onIsCallActive]" }
        updateCurrentCall {
            copy(
                errorCode = null,
                isActive = true,
                isOnHold = false,
            )
        }
    }

    /**
     * Check to see if we can make the call inactivate
     */
    val onIsCallInactive: suspend () -> Unit = {
        logger.d { "[onIsCallInactive]" }
        updateCurrentCall {
            copy(
                errorCode = null,
                isOnHold = true,
            )
        }
    }
}
