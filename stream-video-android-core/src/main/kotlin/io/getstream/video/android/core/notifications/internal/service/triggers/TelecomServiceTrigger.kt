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

package io.getstream.video.android.core.notifications.internal.service.triggers

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.core.net.toUri
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.core.notifications.internal.VideoPushDelegate.Companion.DEFAULT_CALL_TEXT
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.core.notifications.internal.service.ServiceIntentBuilder
import io.getstream.video.android.core.notifications.internal.service.TelecomHelper
import io.getstream.video.android.model.StreamCallId

internal class TelecomServiceTrigger(
    private val context: Context,
    private val serviceIntentBuilder: ServiceIntentBuilder,
) {

    private val logger by taggedLogger("ServiceTriggers")

    fun addIncomingCallToTelecom(
        context: Context,
        callId: StreamCallId,
        callDisplayName: String?,
        callServiceConfiguration: CallServiceConfig,
        isVideo: Boolean,
        pushPayload: Map<String, Any?>,
        streamVideo: StreamVideo,
        notification: Notification?,
    ) {
        logger.d { "[showIncomingCall] Starting foreground service" }

        /**
         * Because we need to retrieve the notification
         * in [io.getstream.video.android.core.notifications.internal.telecom.connection.SuccessTelecomConnection]
         */
        notification?.let {
            streamVideo.call(callId.type, callId.id)
                .state.updateNotification(notification)
        }

        // Add Call to Telecom
        val appSchema = (streamVideo as StreamVideoClient).telecomConfig?.schema
//        val addressUri = Uri.fromParts(PhoneAccount.SCHEME_TEL, "0000", null)
        val addressUri = "$appSchema:${callId.id}".toUri()
        val formattedCallDisplayName = callDisplayName?.takeIf { it.isNotBlank() } ?: DEFAULT_CALL_TEXT

        if (!TelecomHelper.isPhoneAccountRegistered(context)) {
            TelecomHelper.registerPhoneAccount(context)
        }

        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

        val extras = Bundle().apply {
            putParcelable(
                TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                TelecomHelper.getPhoneAccountHandle(context),
            )
            putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, addressUri)

            // For Our Service
            putString(NotificationHandler.INTENT_EXTRA_CALL_CID, callId.cid)
            putString(NotificationHandler.INTENT_EXTRA_CALL_DISPLAY_NAME, callDisplayName)
        }

        // This shows the native incoming call UI
        telecomManager.addNewIncomingCall(TelecomHelper.getPhoneAccountHandle(context), extras)
    }

    @SuppressLint("MissingPermission")
    fun addOutgoingCallToTelecom(
        context: Context,
        callId: StreamCallId,
        callDisplayName: String?,
        streamVideo: StreamVideo,
    ) {
        val appSchema = (streamVideo as StreamVideoClient).telecomConfig?.schema
//        val addressUri = Uri.fromParts(PhoneAccount.SCHEME_TEL, "0000", null)
        val addressUri = "$appSchema:${callId.id}".toUri()
        val formattedCallDisplayName = callDisplayName?.takeIf { it.isNotBlank() } ?: DEFAULT_CALL_TEXT

        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

        val extras = Bundle().apply {
            putParcelable(
                TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                TelecomHelper.getPhoneAccountHandle(context),
            )

            // For Our Service
            putString(NotificationHandler.INTENT_EXTRA_CALL_CID, callId.cid)
            putString(NotificationHandler.INTENT_EXTRA_CALL_DISPLAY_NAME, callDisplayName)
        }

        // This shows the native incoming call UI
        telecomManager.placeCall(addressUri, extras)
    }
}
