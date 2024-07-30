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

    suspend fun registerCall(callId: StreamCallId) {
        streamVideo?.call(callId.type, callId.id)?.let { streamCall ->
            registerCall(streamCall)
        }
    }

    suspend fun registerCall(call: SdkCall) {
        with(call.telecomCallAttributes) {
            logger.d {
                "[registerCall] displayName: $displayName, direction: ${if (direction == 1) "incoming" else "outgoing"}, callType: ${if (callType == 1) "audio" else "video"}"
            }
        }

        val telecomToSdkEventMapper = TelecomToSdkEventMapper(call)
        val sdkToTelecomEventMapper = SdkToTelecomEventMapper(call)

        // TODO-Telecom: read addCall inline docs
        safeCall(exceptionLogTag = TAG) {
            callManager.addCall(
                callAttributes = call.telecomCallAttributes,
                onAnswer = telecomToSdkEventMapper::onAnswer,
                onDisconnect = telecomToSdkEventMapper::onDisconnect,
                onSetActive = telecomToSdkEventMapper::onSetActive,
                onSetInactive = telecomToSdkEventMapper::onSetInactive,
                block = { sdkToTelecomEventMapper.onEvent(callControlScope = this) },
            )
        }
    }
}

private typealias SdkCall = io.getstream.video.android.core.Call

private class TelecomToSdkEventMapper(private val call: SdkCall) {
    // TODO-Telecom: maybe turn into delegate and inject in TelecomCallManager with default value
    // TODO-Telecom: review what needs to be called here and take results into account

    private val logger by taggedLogger(TAG)

    suspend fun onAnswer(callType: Int) {
        logger.d { "[TelecomToSdkEventMapper#onAnswer]" }
        call.accept()
        call.join()
    }

    suspend fun onDisconnect(cause: DisconnectCause) {
        logger.d { "[TelecomToSdkEventMapper#onDisconnect]" }
        call.leave()
    }

    suspend fun onSetActive() {
        logger.d { "[TelecomToSdkEventMapper#onSetActive]" }
        call.join()
    }

    suspend fun onSetInactive() {
        logger.d { "[TelecomToSdkEventMapper#onSetInactive]" }
        call.leave()
    }
}

private class SdkToTelecomEventMapper(private val call: SdkCall) {

    private val logger by taggedLogger(TAG)

    fun onEvent(callControlScope: CallControlScope) {
        call.subscribeFor(
            CallAcceptedEvent::class.java,
            CallRejectedEvent::class.java,
            CallEndedEvent::class.java,
        ) { event ->
            logger.d { "[SdkToTelecomEventMapper#onEvent] Received event: ${event.getEventType()}" }

            with(callControlScope) {
                launch {
                    when (event) {
                        is CallAcceptedEvent -> {
                            logger.d { "[SdkToTelecomEventMapper#onEvent] Will call CallControlScope#answer" }
                            answer(call.telecomCallType)
                        }
                        // TODO-Telecom: Correct DisconnectCause below
                        is CallRejectedEvent -> {
                            logger.d { "[SdkToTelecomEventMapper#onEvent] Will call CallControlScope#disconnect" }
                            disconnect(DisconnectCause(DisconnectCause.REJECTED))
                        }
                        is CallEndedEvent -> {
                            logger.d { "[SdkToTelecomEventMapper#onEvent] Will call CallControlScope#disconnect" }
                            disconnect(DisconnectCause(DisconnectCause.REMOTE))
                        }
                    }
                }
            }
        }
    }
}

private val SdkCall.telecomCallType: Int
    get() = when (type) {
        "default", "livestream" -> CallAttributesCompat.CALL_TYPE_VIDEO_CALL
        else -> CallAttributesCompat.CALL_TYPE_AUDIO_CALL
    }

private val SdkCall.telecomCallAttributes: CallAttributesCompat
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
