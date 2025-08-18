package io.getstream.video.android.core

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telecom.DisconnectCause
import io.getstream.log.taggedLogger
import io.getstream.result.Result
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.notifications.internal.service.triggers.ServiceTriggerDispatcher
import io.getstream.video.android.model.StreamCallId

internal class CallRejectionHandler {

    val logger by taggedLogger("Call:RejectReceiver")

    internal suspend fun reject(call: Call, context: Context, intent: Intent? = null) {
        when (val rejectResult = call.reject(RejectReason.Decline)) {
            is Result.Success -> {
                val userId = StreamVideo.instanceOrNull()?.userId
                userId?.let {
                    val set = mutableSetOf(it)
                    call.state.updateRejectedBy(set)
                    call.state.updateRejectActionBundle(intent?.extras ?: Bundle())
                }
                logger.d { "[onReceive] rejectCall, Success: $rejectResult" }
            }
            is Result.Failure -> {
                logger.d { "[onReceive] rejectCall, Failure: $rejectResult" }
            }
        }
        logger.d { "[onReceive] #ringing; callId: ${call.id}, action: ${intent?.action}" }


        call.state.telecomConnection.value?.let {
            it.setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
            it.destroy()
        }
        call.state.updateTelecomConnection(null)

        val serviceTriggerDispatcher = ServiceTriggerDispatcher(context)
        serviceTriggerDispatcher.removeIncomingCall(
            context,
            StreamCallId.fromCallCid(call.cid),
            StreamVideo.instance().state.callConfigRegistry.get(call.type),
        )
    }
}