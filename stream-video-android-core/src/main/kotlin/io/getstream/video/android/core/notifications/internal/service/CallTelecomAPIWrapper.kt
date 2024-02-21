package io.getstream.video.android.core.notifications.internal.service

import android.content.Context
import android.net.Uri
import android.os.Build
import android.telecom.CallAttributes
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallsManager
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoImpl
import io.getstream.video.android.core.events.ParticipantJoinedEvent
import io.getstream.video.android.core.notifications.internal.service.CallTriggers.TRIGGER_INCOMING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallTriggers.TRIGGER_ONGOING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallTriggers.TRIGGER_OUTGOING_CALL
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.openapitools.client.models.CallEndedEvent
import java.lang.IllegalArgumentException

@RequiresApi(Build.VERSION_CODES.O)
class CallTelecomAPIWrapper(
    context: Context,
    val scope: CoroutineScope,
    val callManager: CallsManager = CallsManager(context)
) {

    private lateinit var callScope: CallControlScope

    fun addCall(displayName: String, callId: StreamCallId, @CallTrigger trigger: String) {
        scope.launch {
            val call = StreamVideo.instanceOrNull()?.call(callId.type, callId.id)
            call?.get() // update the call
            val direction = when (trigger) {
                TRIGGER_INCOMING_CALL -> {
                    CallAttributesCompat.DIRECTION_INCOMING
                }

                TRIGGER_OUTGOING_CALL -> {
                    CallAttributesCompat.DIRECTION_OUTGOING
                }

                TRIGGER_ONGOING_CALL -> {
                    CallAttributesCompat.DIRECTION_OUTGOING
                }

                else -> {
                    throw IllegalArgumentException("Wrong trigger: $trigger")
                }
            }
            val type = when (callId.type) {
                "default" -> CallAttributesCompat.CALL_TYPE_VIDEO_CALL or CallAttributesCompat.CALL_TYPE_AUDIO_CALL
                else -> CallAttributesCompat.CALL_TYPE_AUDIO_CALL
            }
            val capability = CallAttributesCompat.SUPPORTS_TRANSFER

            callManager.addCall(
                CallAttributesCompat(
                    displayName = displayName,
                    address = Uri.parse("https://getstream.io/video/join/${callId.cid}"),
                    direction = direction,
                    callType = type,
                    callCapabilities = CallAttributesCompat.SUPPORTS_TRANSFER
                ),
                onDisconnect = {
                    call?.leave()
                },
                onAnswer = {
                    call?.join()
                },
                onSetActive = {
                    call?.join()
                },
                onSetInactive = {
                    call?.leave()
                }
            ) {
                callScope = this
                scope.launch {
                    call?.subscribe { event ->
                        when (event) {
                            is CallEndedEvent -> {
                                callScope.disconnect(DisconnectCause(DisconnectCause.REMOTE))
                            }
                        }
                    }
                }
            }
        }
    }
}
