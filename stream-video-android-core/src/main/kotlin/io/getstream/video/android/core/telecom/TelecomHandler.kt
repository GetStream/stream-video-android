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
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.openapitools.client.models.CallAcceptedEvent
import org.openapitools.client.models.CallEndedEvent
import org.openapitools.client.models.CallRejectedEvent

@TargetApi(Build.VERSION_CODES.O)
internal class TelecomHandler private constructor(
    private val context: Context,
    private val callManager: CallsManager,
) {

    private val logger by taggedLogger(TELECOM_LOG_TAG)
    private var streamVideo: StreamVideo? = null
    private var currentCall: StreamCall? = null
    private val coroutineScope = CoroutineScope(DispatcherProvider.Default)
    private var callControlScope: CallControlScope? = null

    companion object {
        @Volatile
        private var instance: TelecomHandler? = null // TODO-Telecom: handle warning

        fun getInstance(context: Context): TelecomHandler? {
            return instance ?: synchronized(this) {
                if (isSupported(context)) {
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

        fun isSupported(context: Context) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 14+, check the TELECOM feature directly
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELECOM)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // For API 26+ assume that Telecom is supported if the device supports SIM.
            // According to the docs, most devices with a SIM card support Telecom.
            // https://developer.android.com/develop/connectivity/telecom/voip-app#supported_telecom_device
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) &&
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_GSM)
        } else {
            false
        }
    }

    init {
        logger.d { "[init]" }

        safeCall(exceptionLogTag = TELECOM_LOG_TAG) {
            callManager.registerAppWithTelecom(
                capabilities = CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING and
                    CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING,
            )
        }

        streamVideo = StreamVideo.instanceOrNull()
    }

    /*
    TODO-Telecom:
    2. Dismiss incoming notification when accepting
    3. Show ongoing notification as sticky (ongoing: true)
    4. Analog for outgoing
    5. Analog for non-ringing calls (see ClientState#setActiveCall)
    6. Remove all service usages and test
    7. Add deprecation instructions for service
    8. Add sounds to notifications
     */

    fun registerCall(call: StreamCall) = coroutineScope.launch {
        call.get()

        with(call.telecomCallAttributes) {
            logger.d {
                "[registerCall] displayName: $displayName, ringingState: ${call.state.ringingState.value}, callType: ${if (callType == 1) "audio" else "video"}"
            }
        }

        if (call.cid == currentCall?.cid) {
            logger.d { "[registerCall] Updating existing call" }
            postNotification()
        } else {
            logger.d { "[registerCall] Registering new call" }

            currentCall = call
            val telecomToStreamEventBridge = TelecomToStreamEventBridge(call)
            val streamToTelecomEventBridge = StreamToTelecomEventBridge(call)

            safeCall(exceptionLogTag = TELECOM_LOG_TAG) { // TODO-Telecom: or safeSuspendingCall?
                postNotification() // TODO-Telecom: handle permissions

                callManager.addCall(
                    callAttributes = call.telecomCallAttributes,
                    onAnswer = telecomToStreamEventBridge::onAnswer,
                    onDisconnect = telecomToStreamEventBridge::onDisconnect,
                    onSetActive = telecomToStreamEventBridge::onSetActive,
                    onSetInactive = telecomToStreamEventBridge::onSetInactive,
                    block = {
                        callControlScope = this
                        streamToTelecomEventBridge.onEvent(this)
                    },
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun postNotification() = currentCall?.let { currentCall ->
        logger.d {
            "[postNotification] Call ID: ${currentCall.id}, ringingState: ${currentCall.state.ringingState.value}"
        }

        streamVideo?.let { streamVideo ->
            currentCall.state.ringingState.value.let { ringingState ->
                val notification = when (ringingState) {
                    is RingingState.Incoming -> {
                        logger.d { "[postNotification] Creating incoming notification" }

                        streamVideo.getRingingCallNotification(
                            ringingState = RingingState.Incoming(),
                            callId = StreamCallId.fromCallCid(currentCall.cid),
                            incomingCallDisplayName = currentCall.incomingCallDisplayName,
                            shouldHaveContentIntent = streamVideo.state.activeCall.value == null, // TODO-Telecom: Compare this to CallService
                        )
                    }

                    is RingingState.Outgoing, is RingingState.Active -> {
                        val isOutgoingCall = ringingState is RingingState.Outgoing

                        logger.d {
                            "[postNotification] Creating ${if (isOutgoingCall) "outgoing" else "ongoing"} notification"
                        }

                        streamVideo.getOngoingCallNotification(
                            callId = StreamCallId.fromCallCid(currentCall.cid),
                            isOutgoingCall = isOutgoingCall,
                        )
                    }

                    else -> {
                        logger.d { "[postNotification] Not creating any notification" }
                        null
                    }
                }

                notification?.let {
                    logger.d { "[postNotification] Posting notification" }

                    NotificationManagerCompat
                        .from(context)
                        .notify(currentCall.cid.hashCode(), it)
                }
            }
        }
    }

    fun unregisterCall() = coroutineScope.launch {
        logger.d { "[unregisterCall]" }

        safeCall(exceptionLogTag = TELECOM_LOG_TAG) {
            cancelNotification()

            callControlScope?.disconnect(DisconnectCause(DisconnectCause.LOCAL)).let { result ->
                logger.d { "[unregisterCall] Disconnect result: $result" }
            }
        }
    }

    private fun cancelNotification() {
        logger.d { "[cancelNotification]" }
        NotificationManagerCompat.from(context).cancel(currentCall?.cid?.hashCode() ?: 0)
    }

    fun cleanUp() = runBlocking {
        unregisterCall()
        coroutineScope.cancel()
        instance = null
    }
}

private class TelecomToStreamEventBridge(private val call: StreamCall) {
    // TODO-Telecom: maybe turn into delegate and inject in TelecomHandler with default value
    // TODO-Telecom: review what needs to be called here and take results into account

    private val logger by taggedLogger(TELECOM_LOG_TAG)

    suspend fun onAnswer(callType: Int) {
        logger.d { "[TelecomToStreamEventBridge#onAnswer]" }
        call.accept()
        call.join()
    }

    suspend fun onDisconnect(cause: DisconnectCause) {
        logger.d { "[TelecomToStreamEventBridge#onDisconnect]" }
        call.leave()
    }

    suspend fun onSetActive() {
        logger.d { "[TelecomToStreamEventBridge#onSetActive]" }
        call.join()
    }

    suspend fun onSetInactive() {
        logger.d { "[TelecomToStreamEventBridge#onSetInactive]" }
        call.leave()
    }
}

private class StreamToTelecomEventBridge(private val call: StreamCall) {

    private val logger by taggedLogger(TELECOM_LOG_TAG)

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
                            logger.d { "[StreamToTelecomEventBridge#onEvent] Will call CallControlScope#answer" }
                            answer(call.telecomCallType)
                        }
                        // TODO-Telecom: Correct DisconnectCause below
                        is CallRejectedEvent -> {
                            logger.d { "[StreamToTelecomEventBridge#onEvent] Will call CallControlScope#disconnect" }
                            disconnect(DisconnectCause(DisconnectCause.REJECTED))
                        }
                        is CallEndedEvent -> {
                            logger.d { "[StreamToTelecomEventBridge#onEvent] Will call CallControlScope#disconnect" }
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

private val StreamCall.incomingCallDisplayName: String
    get() = state.createdBy.value?.userNameOrId ?: "Unknown"
