package io.getstream.video.android.core.notifications.internal.service.telecom

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.Connection
import android.telecom.DisconnectCause
import io.getstream.log.taggedLogger
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.StreamVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.openapitools.client.models.CallAcceptedEvent
import org.openapitools.client.models.CallEndedEvent
import org.openapitools.client.models.CallRejectedEvent

class VoipConnection(
    private val context: Context,
    private val callId: StreamCallId,
    private val isIncoming: Boolean
) : Connection() {

    private val logger by taggedLogger("VoipConnection")
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    init {
        logger.i { "[init] callId: ${callId.id}, isIncoming: $isIncoming" }
        // For example: connect to your StreamVideo call if needed
        subscribeToCallEvents()
    }

    override fun onAnswer() {
        super.onAnswer()
        logger.i { "[onAnswer]" }
        // Let Telecom know we are active
        setActive()

        serviceScope.launch {
            // If you are using StreamVideo:
            val streamVideo = StreamVideo.instanceOrNull() as? StreamVideoClient ?: return@launch
            val call = streamVideo.call(callId.type, callId.id)
            call.accept()
            call.join()
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
        // Mute / pause your video or audio if needed
    }

    override fun onUnhold() {
        super.onUnhold()
        logger.i { "[onUnhold]" }
        setActive()
        // Resume your media logic
    }

    override fun onCallEndpointChanged(callEndpoint: CallEndpoint) {
        super.onCallEndpointChanged(callEndpoint)
        logger.i { "[onCallEndpointChanged] callEndpoint: $callEndpoint" }
    }

    override fun onAvailableCallEndpointsChanged(availableEndpoints: MutableList<CallEndpoint>) {
        super.onAvailableCallEndpointsChanged(availableEndpoints)
        logger.i { "[onAvailableCallEndpointsChanged] availableEndpoints: $availableEndpoints" }
    }

    override fun onCallEvent(event: String?, extras: Bundle?) {
        super.onCallEvent(event, extras)
        logger.i { "Telecom event: $event"}
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
                        // Possibly setActive() if the user accepted on another device
                        logger.i { "call accepted: $event" }
                        setActive()
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
}
