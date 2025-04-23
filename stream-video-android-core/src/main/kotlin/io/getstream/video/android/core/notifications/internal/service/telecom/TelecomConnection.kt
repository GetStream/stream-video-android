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

package io.getstream.video.android.core.notifications.internal.service.telecom

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.OutcomeReceiver
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.CallEndpointException
import android.telecom.Connection
import android.telecom.DisconnectCause
import android.telecom.TelecomManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import io.getstream.android.video.generated.models.CallAcceptedEvent
import io.getstream.android.video.generated.models.CallEndedEvent
import io.getstream.android.video.generated.models.CallRejectedEvent
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.audio.AudioHandler
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.core.notifications.DefaultStreamIntentResolver
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class TelecomConnection(
    val context: Context,
    val callId: StreamCallId,
    val callConfig: CallServiceConfig,
) : Connection() {

    private val logger by taggedLogger("TelecomConnection")
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    private var availableEndpoints: List<CallEndpoint> = emptyList()
    private var deviceListener: DeviceListener? = null
        set(value) {
            field = value
            value?.invoke(availableEndpoints.map { it.toStreamAudioDevice() }, null)
        }

    init {
        // For example: connect to your StreamVideo call if needed
        subscribeToCallEvents()
    }

    // Equivalent of callManager.addCall(onAnswer = onTelecomEvent) - used when accepting from watch.
    override fun onAnswer() {
        super.onAnswer()
        logger.i { "[onAnswer]" }
        // Let Telecom know we are active
        setActive()

        serviceScope.launch {
            // If you are using StreamVideo:
            val streamVideo = StreamVideo.instanceOrNull() as? StreamVideoClient ?: return@launch
            val streamCall = streamVideo.call(callId.type, callId.id)

            DefaultStreamIntentResolver(context).searchAcceptCallPendingIntent(
                callId = StreamCallId.fromCallCid(streamCall.cid),
                notificationId = streamCall.cid.hashCode(),
            )?.send()
        }
    }

    override fun onReject() {
        super.onReject()
        logger.i { "[onReject]" }

        serviceScope.launch {
            val streamVideo = StreamVideo.instanceOrNull() as? StreamVideoClient ?: return@launch
            val call = streamVideo.call(callId.type, callId.id)
            call.reject()
        }

        setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
        destroy()
    }

    override fun onDisconnect() {
        super.onDisconnect()
        logger.i { "[onDisconnect]" }

        serviceScope.launch {
            val streamVideo = StreamVideo.instanceOrNull() as? StreamVideoClient ?: return@launch
            val call = streamVideo.call(callId.type, callId.id)
            call.leave()
        }

        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
    }

    override fun onAbort() {
        super.onAbort()
        logger.i { "[onAbort]" }
        setDisconnected(DisconnectCause(DisconnectCause.CANCELED))
        destroy()
    }

    override fun onHold() {
        super.onHold()
        logger.i { "[onHold]" }
        setOnHold()

        serviceScope.launch {
            val streamVideo = StreamVideo.instanceOrNull() as? StreamVideoClient ?: return@launch
            val call = streamVideo.call(callId.type, callId.id)
            call.leave()
            setDisconnected(DisconnectCause(DisconnectCause.OTHER))
            destroy()
        }
        // Mute / pause your video or audio if needed
    }

    override fun onUnhold() {
        super.onUnhold()
        logger.i { "[onUnhold]" }
        // There is no unhold logic.
    }

    override fun onCallEndpointChanged(callEndpoint: CallEndpoint) {
        super.onCallEndpointChanged(callEndpoint)
        logger.i { "[onCallEndpointChanged] callEndpoint: $callEndpoint" }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onAvailableCallEndpointsChanged(availableEndpoints: MutableList<CallEndpoint>) {
        super.onAvailableCallEndpointsChanged(availableEndpoints)
        logger.i { "[onAvailableCallEndpointsChanged] availableEndpoints: $availableEndpoints" }

        this.availableEndpoints = availableEndpoints
        deviceListener?.invoke(availableEndpoints.map { it.toStreamAudioDevice() }, null)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun selectEndpoint(endpoint: CallEndpoint) {
        requestCallEndpointChange(
            endpoint,
            context.mainExecutor,
            object : OutcomeReceiver<Void, CallEndpointException> {
                override fun onResult(p0: Void?) {
                    logger.i { "[selectEndpoint] success" }
                }

                override fun onError(error: CallEndpointException) {
                    logger.e { "[selectEndpoint] error: $error" }
                }
            },
        )
    }

    override fun onCallEvent(event: String?, extras: Bundle?) {
        super.onCallEvent(event, extras)
        logger.i { "Telecom event: $event" }
    }

    override fun onCallAudioStateChanged(state: CallAudioState) {
        super.onCallAudioStateChanged(state)
        logger.i {
            "[onCallAudioStateChanged] muted=${state.isMuted}, route=${state.route}"
        }
        // The system or user changed audio route (speaker, earpiece, bluetooth, etc.)
        // Apply changes to your StreamVideo session if needed
    }

    private fun subscribeToCallEvents() {
        serviceScope.launch {
            val streamVideo = StreamVideo.instanceOrNull() as? StreamVideoClient ?: return@launch
            val call = streamVideo.call(callId.type, callId.id)

            // Subscribe to call events to handle end, reject on other device, etc.
            call.subscribe { event ->
                when (event) {
                    is CallAcceptedEvent -> {
                        logger.i { "call accepted: $event" }
                        // Not setting to `Active` this will happen in onAnswer
                    }
                    is CallRejectedEvent -> {
                        logger.i { "call rejected: $event" }
                        setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
                        destroy()
                    }
                    is CallEndedEvent -> {
                        logger.i { "call ended: $event" }
                        setDisconnected(DisconnectCause(DisconnectCause.REMOTE))
                        destroy()
                    }
                    else -> Unit
                }
            }
        }
    }

    fun cleanUp() {
        logger.d { "[cleanUp] #telecom; Hash: ${hashCode()}, cid: ${callId.cid}" }

        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
        NotificationManagerCompat.from(context).cancel(notificationIdFromCallId(callId))
    }

    companion object {

        fun createAndStore(
            context: Context,
            callId: StreamCallId,
            callConfig: CallServiceConfig,
            displayName: String = "",
            isRinging: Boolean = false,
            isDialing: Boolean = false,
        ): TelecomConnection {
            return TelecomConnection(
                context = context,
                callId = callId,
                callConfig = callConfig,
            ).apply {
                setAddress(getTelecomAddress(callId.cid), TelecomManager.PRESENTATION_ALLOWED)
                setCallerDisplayName(
                    displayName,
                    TelecomManager.PRESENTATION_ALLOWED,
                )

                if (isRinging) {
                    setRinging()
                } else if (isDialing) {
                    setDialing()
                } else {
                    setActive()
                }
            }.also {
                telecomConnections[callId.cid] = it
            }
        }

        fun setDeviceListener(listener: DeviceListener): AudioHandler {
            return object : AudioHandler {
                override fun start() {
//                    currentConnection?.deviceListener = listener
                }

                override fun stop() {}

                @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                override fun selectDevice(audioDevice: StreamAudioDevice?) {
//                    if (currentConnection != null && audioDevice?.telecomDevice != null) {
//                        currentConnection?.let {
//                            StreamLog.d(
//                                "VoipConnection",
//                            ) { "[selectDevice] audioDevice: $audioDevice" }
//
//                            it.selectEndpoint(audioDevice.telecomDevice!!)
//                        }
//                    }
                }
            }
        }
    }
}

internal val telecomConnections = mutableMapOf<String, TelecomConnection>()

internal typealias DeviceListener = (available: List<StreamAudioDevice>, selected: StreamAudioDevice?) -> Unit
