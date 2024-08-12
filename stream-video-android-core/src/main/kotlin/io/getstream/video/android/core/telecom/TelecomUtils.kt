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
import io.getstream.video.android.core.audio.AudioHandler
import io.getstream.video.android.core.audio.AudioSwitchHandler
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.core.audio.StreamAudioDevice.Companion.fromAudio
import io.getstream.video.android.core.notifications.internal.service.CallService
import io.getstream.video.android.model.StreamCallId
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal object TelecomCompat {

    fun registerCall(
        context: Context,
        call: StreamCall? = null,
        callId: StreamCallId? = null,
        callDisplayName: String = "Unknown",
        isTriggeredByNotification: Boolean = false,
    ) {
        withCall(call, callId) {
            val applicationContext = context.applicationContext
            val isTelecomSupported = TelecomHandler.isSupported(applicationContext)
            val telecomHandler = TelecomHandler.getInstance(applicationContext)

            if (isTelecomSupported) {
                telecomHandler?.registerCall(it, isTriggeredByNotification)
            } else {
                if (isTriggeredByNotification) {
                    CallService.showIncomingCall( // TODO-Telecom: Keep runForegroundService flag into account here and in other places?
                        applicationContext,
                        StreamCallId.fromCallCid(it.cid),
                        callDisplayName,
                    )
                }
            }
        }
    }

    fun changeCallState(context: Context, newState: TelecomCallState, call: StreamCall? = null, callId: StreamCallId? = null) {
        withCall(call, callId) {
            val applicationContext = context.applicationContext
            val isTelecomSupported = TelecomHandler.isSupported(applicationContext)
            val telecomHandler = TelecomHandler.getInstance(applicationContext)

            if (isTelecomSupported) {
                telecomHandler?.changeCallState(it, newState)
            } else {
                if (newState == TelecomCallState.OUTGOING || newState == TelecomCallState.ONGOING) {
                    ContextCompat.startForegroundService(
                        applicationContext,
                        CallService.buildStartIntent(
                            applicationContext,
                            StreamCallId.fromCallCid(it.cid),
                            if (newState == TelecomCallState.OUTGOING) {
                                CallService.TRIGGER_OUTGOING_CALL
                            } else {
                                CallService.TRIGGER_ONGOING_CALL
                            },
                        ),
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun withCall(
        call: StreamCall?,
        callId: StreamCallId?,
        doAction: (Call) -> Unit,
    ) {
        contract {
            callsInPlace(doAction, InvocationKind.EXACTLY_ONCE)
        }

        when {
            call != null -> call

            callId != null -> StreamVideo.instanceOrNull()?.call(callId.type, callId.id)

            else -> null
        }?.let { doAction(it) }
    }

    fun unregisterCall(
        context: Context,
        trigger: String,
        call: StreamCall,
    ) {
        val applicationContext = context.applicationContext
        val isTelecomSupported = TelecomHandler.isSupported(applicationContext)
        val telecomHandler = TelecomHandler.getInstance(applicationContext)

        if (isTelecomSupported) {
            telecomHandler?.unregisterCall(call)
        } else {
            if (trigger == CallService.TRIGGER_INCOMING_CALL) {
                CallService.removeIncomingCall(context, StreamCallId.fromCallCid(call.cid))
            } else {
                context.stopService(CallService.buildStopIntent(applicationContext))
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    fun getAudioHandler(
        context: Context,
        call: StreamCall,
        listener: AvailableDevicesListener,
    ): AudioHandler {
        val applicationContext = context.applicationContext // TODO-Telecom: Abstract out in one place
        val isTelecomSupported = TelecomHandler.isSupported(applicationContext)
        val telecomHandler = TelecomHandler.getInstance(applicationContext)

        return if (isTelecomSupported) {
            // Use Telecom
            object : AudioHandler {
                override fun start() {
                    telecomHandler?.registerAvailableDevicesListener(call, listener)
                }

                override fun stop() {
                }

                override fun selectDevice(audioDevice: StreamAudioDevice?) {
                    audioDevice?.telecomDevice?.let { telecomHandler?.selectDevice(it) }
                }
            }
        } else {
            // Use Twilio AudioSwitch
            AudioSwitchHandler(
                context = applicationContext,
                preferSpeakerphone = true,
                audioDeviceChangeListener = { devices, selected ->
                    listener(
                        devices.map { it.fromAudio() },
                        selected?.fromAudio() ?: StreamAudioDevice.Earpiece(),
                    )
                },
            )
        }
    }
}

internal typealias StreamCall = io.getstream.video.android.core.Call

internal typealias AvailableDevicesListener = (available: List<StreamAudioDevice>, selected: StreamAudioDevice?) -> Unit

internal const val TELECOM_LOG_TAG = "StreamVideo:Telecom"
