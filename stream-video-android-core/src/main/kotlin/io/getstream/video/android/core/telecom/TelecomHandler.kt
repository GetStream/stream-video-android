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

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telecom.DisconnectCause
import androidx.core.app.NotificationManagerCompat
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallsManager
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.launch
import org.openapitools.client.models.CallAcceptedEvent
import org.openapitools.client.models.CallEndedEvent
import org.openapitools.client.models.CallRejectedEvent

@TargetApi(Build.VERSION_CODES.O)
internal class TelecomHandler private constructor(
    private val context: Context,
    private val callManager: CallsManager,
) {

    private val logger by taggedLogger(TAG)
    private var streamVideo: StreamVideo?

    companion object {
        @Volatile
        private var instance: TelecomHandler? = null // TODO-Telecom: handle warning

        fun getInstance(context: Context): TelecomHandler? {
            return instance ?: synchronized(this) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELECOM)
                ) {
                    context.applicationContext.let { applicationContext ->
                        TelecomHandler(
                            context = applicationContext,
                            callManager = CallsManager(applicationContext),
                        ).also { telecomHandler ->
                            instance = telecomHandler
                        }
                    }
                } else {
                    null
                }
            }
        }
    }

    init {
        logger.d { "[init]" }

        safeCall(exceptionLogTag = TAG) {
            callManager.registerAppWithTelecom(
                capabilities = CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING and
                    CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING,
            )
        }

        streamVideo = StreamVideo.instanceOrNull()
    }

    /*
    TODO-Telecom:
    1. Pass call direction to registerCall or find another way (collect ringingState flow - would it be in 5 secs)?
    2. Dismiss incoming notification when accepting
    3. Show ongoing notification as sticky (ongoing: true)
    4. Analog for outgoing
    5. Analog for non-ringing calls (see ClientState#setActiveCall)
    6. Remove all service usages and test
    7. Add deprecation instructions for service
     */

    suspend fun registerCall(callId: StreamCallId) {
        streamVideo?.call(callId.type, callId.id)?.let { streamCall ->
            registerCall(streamCall)
        }
    }

    suspend fun registerCall(call: StreamCall) {
        with(call.telecomCallAttributes) {
            logger.d {
                "[registerCall] displayName: $displayName, direction: ${if (direction == 1) "incoming" else "outgoing"}, callType: ${if (callType == 1) "audio" else "video"}"
            }
        }

        val telecomToStreamEventBridge = TelecomToStreamEventBridge(call)
        val streamToTelecomEventBridge = StreamToTelecomEventBridge(call)

        safeCall(exceptionLogTag = TAG) {
            postNotification(call) // TODO-Telecom: handle permissions

            callManager.addCall(
                callAttributes = call.telecomCallAttributes,
                onAnswer = telecomToStreamEventBridge::onAnswer,
                onDisconnect = telecomToStreamEventBridge::onDisconnect,
                onSetActive = telecomToStreamEventBridge::onSetActive,
                onSetInactive = telecomToStreamEventBridge::onSetInactive,
                block = { streamToTelecomEventBridge.onEvent(callControlScope = this) },
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun postNotification(call: StreamCall) {
        logger.d { "[postNotification] Ringing state: ${call.state.ringingState.value}" }

        streamVideo?.let { streamVideo ->
            logger.d { "[postNotification] streamVideo not null" }

            if (call.state.ringingState.value is RingingState.Active) {
                streamVideo.getOngoingCallNotification(
                    callId = StreamCallId.fromCallCid(call.cid),
                    callDisplayName = call.id,
                )
            } else {
                streamVideo.getRingingCallNotification(
                    ringingState = RingingState.Incoming(),
                    callId = StreamCallId.fromCallCid(call.cid),
                    callDisplayName = call.id,
                    shouldHaveContentIntent = streamVideo.state.activeCall.value == null,
                )
            }?.let { notification ->
                logger.d { "[postNotification] notification not null" }

                NotificationManagerCompat
                    .from(context)
                    .notify(call.cid.hashCode(), notification)
            }
        }
    }
}

private typealias StreamCall = io.getstream.video.android.core.Call

private class TelecomToStreamEventBridge(private val call: StreamCall) {
    // TODO-Telecom: maybe turn into delegate and inject in TelecomHandler with default value
    // TODO-Telecom: review what needs to be called here and take results into account

    private val logger by taggedLogger(TAG)

    suspend fun onAnswer(callType: Int) {
        logger.d { "[TelecomToStreamEventMapper#onAnswer]" }
        call.accept()
        call.join()
    }

    suspend fun onDisconnect(cause: DisconnectCause) {
        logger.d { "[TelecomToStreamEventMapper#onDisconnect]" }
        call.leave()
    }

    suspend fun onSetActive() {
        logger.d { "[TelecomToStreamEventMapper#onSetActive]" }
        call.join()
    }

    suspend fun onSetInactive() {
        logger.d { "[TelecomToStreamEventMapper#onSetInactive]" }
        call.leave()
    }
}

private class StreamToTelecomEventBridge(private val call: StreamCall) {

    private val logger by taggedLogger(TAG)

    fun onEvent(callControlScope: CallControlScope) {
        call.subscribeFor(
            CallAcceptedEvent::class.java,
            CallRejectedEvent::class.java,
            CallEndedEvent::class.java,
        ) { event ->
            logger.d { "[StreamToTelecomEventMapper#onEvent] Received event: ${event.getEventType()}" }

            with(callControlScope) {
                launch {
                    when (event) {
                        is CallAcceptedEvent -> {
                            logger.d { "[StreamToTelecomEventMapper#onEvent] Will call CallControlScope#answer" }
                            answer(call.telecomCallType)
                        }
                        // TODO-Telecom: Correct DisconnectCause below
                        is CallRejectedEvent -> {
                            logger.d { "[StreamToTelecomEventMapper#onEvent] Will call CallControlScope#disconnect" }
                            disconnect(DisconnectCause(DisconnectCause.REJECTED))
                        }
                        is CallEndedEvent -> {
                            logger.d { "[StreamToTelecomEventMapper#onEvent] Will call CallControlScope#disconnect" }
                            disconnect(DisconnectCause(DisconnectCause.REMOTE))
                        }
                    }
                }
            }
        }
    }
}

private val StreamCall.telecomCallType: Int
    get() = when (type) {
        "default", "livestream" -> CallAttributesCompat.CALL_TYPE_VIDEO_CALL
        else -> CallAttributesCompat.CALL_TYPE_AUDIO_CALL
    }

private val StreamCall.telecomCallAttributes: CallAttributesCompat
    get() = CallAttributesCompat(
        displayName = id,
        address = Uri.parse("https://getstream.io/video/join/$cid"),
        direction = if (state.ringingState.value is RingingState.Incoming) { // TODO-Telecom: Race condition with ringing state
            CallAttributesCompat.DIRECTION_INCOMING
        } else {
            CallAttributesCompat.DIRECTION_OUTGOING
        },
        callType = telecomCallType,
    )

private const val TAG = "StreamVideo:Telecom"
