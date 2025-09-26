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

import android.Manifest
import android.app.Notification
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
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

internal class TelecomServiceLauncher(
    private val context: Context,
    private val serviceIntentBuilder: ServiceIntentBuilder,
) {

    private val logger by taggedLogger("ServiceTriggers")
    private val telecomHelper = TelecomHelper()

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
         * in [io.getstream.video.android.core.notifications.internal.telecom.connection.SuccessIncomingTelecomConnection]
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

        if (!telecomHelper.isPhoneAccountRegistered(context)) {
            telecomHelper.registerPhoneAccount(context)
        }

        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

        val extras = Bundle().apply {
            putParcelable(
                TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                telecomHelper.getPhoneAccountHandle(context),
            )
            putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, addressUri)

            // For Our Service
            putString(NotificationHandler.INTENT_EXTRA_CALL_CID, callId.cid)
            putString(NotificationHandler.INTENT_EXTRA_CALL_DISPLAY_NAME, callDisplayName)
        }

        // This shows the native incoming call UI
        telecomManager.addNewIncomingCall(telecomHelper.getPhoneAccountHandle(context), extras)
    }

    fun addOutgoingCallToTelecom(
        context: Context,
        callId: StreamCallId,
        isVideo: Boolean,
        callDisplayName: String?,
        streamVideo: StreamVideo,
    ) {
        val appSchema = (streamVideo as StreamVideoClient).telecomConfig?.schema
//        val addressUri = Uri.fromParts(PhoneAccount.SCHEME_TEL, "0000", null)
        val addressUri = callId.cid.toUri()
        val formattedCallDisplayName = callDisplayName?.takeIf { it.isNotBlank() } ?: DEFAULT_CALL_TEXT

        if (!telecomHelper.isPhoneAccountRegistered(context)) {
            telecomHelper.registerPhoneAccount(context)
        }

        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

        val extras = Bundle().apply {
            putParcelable(
                TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                telecomHelper.getPhoneAccountHandle(context),
            )

            // For Our Service
            putString(NotificationHandler.INTENT_EXTRA_CALL_CID, callId.cid)
            putString(NotificationHandler.INTENT_EXTRA_CALL_DISPLAY_NAME, formattedCallDisplayName)
            putString(TelecomManager.EXTRA_CALL_SUBJECT, formattedCallDisplayName)

            putBoolean(
                TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE,
                false,
            ) // TODO Rahul, if we want speaker to be ON by default

            val videoState = if (isVideo) VideoProfile.STATE_BIDIRECTIONAL else VideoProfile.STATE_AUDIO_ONLY
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putInt(TelecomManager.EXTRA_INCOMING_VIDEO_STATE, videoState)
            } // For framework ConnectionService
        }

        // This shows the native incoming call UI
//        if (ActivityCompat.checkSelfPermission(
//                context,
//                Manifest.permission.CALL_PHONE //TODO Rahul ADD this check in telecom support
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            throw IllegalStateException("Bc ye permission thodi na cahiye ab")
//        }
        /**
         * Requires [Manifest.permission.CALL_PHONE]
         */
        telecomManager.placeCall(addressUri, extras)
    }

    fun addOnGoingCall(
        context: Context,
        callId: StreamCallId,
        isVideo: Boolean,
        callDisplayName: String?,
        streamVideo: StreamVideo,
    ) {
        val call = streamVideo.call(callId.type, callId.id)
        call.state.telecomConnection.value?.let {
            it.onAnswer()
            it.setActive()
        }
    }
}
