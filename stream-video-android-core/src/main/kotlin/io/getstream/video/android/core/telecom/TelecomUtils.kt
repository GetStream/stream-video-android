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

package io.getstream.video.android.core.telecom

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.audio.AudioHandler
import io.getstream.video.android.core.audio.AudioSwitchHandler
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.core.notifications.internal.service.CallService
import io.getstream.video.android.model.StreamCallId
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
internal object TelecomCompat {

    fun registerCall(
        context: Context,
        call: StreamCall? = null,
        callId: StreamCallId? = null,
        callDisplayName: String = "Unknown",
        isIncomingCall: Boolean = false,
    ) {
        withCall(call, callId) { streamCall ->
            checkTelecomSupport(
                context = context,
                onSupported = { telecomHandler ->
                    telecomHandler.registerCall(streamCall, isIncomingCall)
                },
                onNotSupported = {
                    if (isIncomingCall) {
                        checkForegroundServiceEnabled(
                            onEnabled = {
                                CallService.showIncomingCall(
                                    context.applicationContext,
                                    StreamCallId.fromCallCid(streamCall.cid),
                                    callDisplayName,
                                    notification = null, // TODO-Telecom: Add notification
                                )
                            },
                        )
                    }
                },
            )
        }
    }

    private inline fun withCall(
        call: StreamCall?,
        callId: StreamCallId?,
        doAction: (Call) -> Unit,
    ) {
        contract {
            callsInPlace(doAction, InvocationKind.AT_MOST_ONCE)
        }

        when {
            call != null -> call

            callId != null -> StreamVideo.instanceOrNull()?.call(callId.type, callId.id)

            else -> null
        }?.let { doAction(it) }
    }

    private inline fun <T> checkTelecomSupport(
        context: Context,
        onSupported: (TelecomHandler) -> T,
        onNotSupported: () -> T,
    ): T {
        contract {
            callsInPlace(onSupported, InvocationKind.AT_MOST_ONCE)
            callsInPlace(onNotSupported, InvocationKind.AT_MOST_ONCE)
        }

        // getInstance() returns null if the device doesn't support Telecom
        return TelecomHandler.getInstance(context)?.let(onSupported) ?: onNotSupported()
    }

    private inline fun checkForegroundServiceEnabled(onEnabled: () -> Unit) {
        contract {
            callsInPlace(onEnabled, InvocationKind.AT_MOST_ONCE)
        }

        (StreamVideo.instanceOrNull() as? StreamVideoClient)?.let { streamVideoClient ->
            val callConfig = streamVideoClient.callServiceConfigRegistry.get(
                "default",
            ) // TODO-Telecom: Use call.type
            if (callConfig.runCallServiceInForeground) onEnabled()
        }
    }

    fun changeCallState(context: Context, newState: TelecomCallState, call: StreamCall? = null, callId: StreamCallId? = null) {
        withCall(call, callId) { streamCall ->
            checkTelecomSupport(
                context = context,
                onSupported = { telecomHandler ->
                    telecomHandler.changeCallState(streamCall, newState)
                },
                onNotSupported = {
                    if (newState == TelecomCallState.OUTGOING || newState == TelecomCallState.ONGOING) {
                        checkForegroundServiceEnabled(
                            onEnabled = {
                                ContextCompat.startForegroundService(
                                    context,
                                    CallService.buildStartIntent(
                                        context,
                                        StreamCallId.fromCallCid(streamCall.cid),
                                        if (newState == TelecomCallState.OUTGOING) {
                                            CallService.TRIGGER_OUTGOING_CALL
                                        } else {
                                            CallService.TRIGGER_ONGOING_CALL
                                        },
                                    ),
                                )
                            },
                        )
                    }
                },
            )
        }
    }

    fun unregisterCall(
        context: Context,
        call: StreamCall,
        isIncomingCall: Boolean = false,
    ) {
        checkTelecomSupport(
            context = context,
            onSupported = { telecomHandler ->
                telecomHandler.unregisterCall(call)
            },
            onNotSupported = {
                checkForegroundServiceEnabled(
                    onEnabled = {
                        if (isIncomingCall) {
                            CallService.removeIncomingCall(
                                context,
                                StreamCallId.fromCallCid(call.cid),
                            )
                        } else {
                            context.stopService(
                                CallService.buildStopIntent(context.applicationContext),
                            )
                        }
                    },
                )
            },
        )
    }

    @TargetApi(Build.VERSION_CODES.O)
    fun setDeviceListener(
        context: Context,
        call: StreamCall,
        listener: DeviceListener,
    ): AudioHandler = checkTelecomSupport(
        context = context,
        onSupported = { telecomHandler ->
            // Use Telecom
            object : AudioHandler {
                override fun start() {
                    telecomHandler.setDeviceListener(call, listener)
                }

                override fun stop() {}

                override fun selectDevice(audioDevice: StreamAudioDevice?) {
                    audioDevice?.telecomDevice?.let { telecomHandler.selectDevice(call, it) }
                }
            }
        },
        onNotSupported = {
            // Use Twilio AudioSwitch
            AudioSwitchHandler(
                context = context.applicationContext,
                deviceListener = listener,
            )
        },
    )
}

internal typealias StreamCall = Call

internal typealias DeviceListener = (available: List<StreamAudioDevice>, selected: StreamAudioDevice?) -> Unit
