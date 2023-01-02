/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.notifications.internal

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.getstream.android.push.PushDevice
import io.getstream.android.push.delegate.PushDelegate
import io.getstream.log.StreamLog
import io.getstream.video.android.R
import io.getstream.video.android.StreamVideoBuilder
import io.getstream.video.android.dispatchers.DispatcherProvider
import io.getstream.video.android.token.AuthCredentialsProvider
import io.getstream.video.android.user.UserCredentialsManager
import io.getstream.video.android.user.UserPreferences
import io.getstream.video.android.utils.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.utils.INTENT_EXTRA_NOTIFICATION_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Class used to handle Push Notifications.
 * It is used by reflection by [io.getstream.android.push.delegate.PushDelegateProvider] class.
 */
internal class VideoPushDelegate(context: Context) : PushDelegate(context) {
    private val logger = StreamLog.getLogger("VideoPushDelegate")
    private val userPreferences: UserPreferences by lazy {
        UserCredentialsManager.initialize(context)
    }
    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context).also {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.createNotificationChannel(
                    NotificationChannelCompat.Builder(
                        CHANNEL_ID,
                        NotificationManager.IMPORTANCE_HIGH
                    )
                        .setName(CHANNEL_NAME)
                        .setDescription(CHANNEL_DESCRIPTION)
                        .build()
                )
            }
        }
    }

    /**
     * Handle a push message.
     *
     * @param payload The content of the Push Notification.
     * @return true if the payload was handled properly.
     */
    override fun handlePushMessage(payload: Map<String, Any?>): Boolean {
        logger.d { "[handlePushMessage] payload: $payload" }
        return payload.ifValid {
            val users = payload[KEY_USER_NAMES] as String
            val callId = payload[KEY_CALL_CID] as String
            searchIncomingCallPendingIntent(callId)
                ?.let { fullScreenPendingIntent ->
                    searchAcceptCallPendingIntent(callId)
                        ?.let { acceptCallPendingIntent ->
                            showIncomingCallNotification(
                                fullScreenPendingIntent,
                                acceptCallPendingIntent,
                                users,
                            )
                        } ?: logger.e { "Couldn't find any activity for $ACTION_ACCEPT_CALL" }
                } ?: logger.e { "Couldn't find any activity for $ACTION_INCOMING_CALL" }
        }
    }

    private fun showIncomingCallNotification(
        fullScreenPendingIntent: PendingIntent,
        acceptCallPendingIntent: PendingIntent,
        users: String,
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(androidx.loader.R.drawable.notification_bg)
            .setContentTitle("Incoming call")
            .setContentText(users)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .addAction(
                NotificationCompat.Action.Builder(
                    null,
                    context.getString(R.string.stream_call_notification_action_accept),
                    acceptCallPendingIntent,
                ).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    null,
                    context.getString(R.string.stream_call_notification_action_reject),
                    PendingIntent.getBroadcast(
                        context,
                        0,
                        Intent(),
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            PendingIntent.FLAG_IMMUTABLE
                        } else {
                            0
                        }
                    )
                ).build()
            )
            .build()
        notificationManager.notify(INCOMING_CALL_NOTIFICATION_ID, notification)
    }

    /**
     * Search for an activity that can receive incoming calls from Stream Server.
     *
     * @param callId The call id from the incoming call.
     */
    private fun searchIncomingCallPendingIntent(
        callId: String
    ): PendingIntent? = searchPendingIntent(Intent(ACTION_INCOMING_CALL), callId)

    /**
     * Search for an activity that can accept call from Stream Server.
     *
     * @param callId The call id from the incoming call.
     */
    private fun searchAcceptCallPendingIntent(
        callId: String,
    ): PendingIntent? = searchPendingIntent(Intent(ACTION_ACCEPT_CALL), callId)

    private fun searchPendingIntent(
        baseIntent: Intent,
        callId: String,
    ): PendingIntent? {
        return context.packageManager.queryIntentActivities(baseIntent, 0)
            .filter { it.activityInfo.packageName == context.packageName }
            .maxByOrNull { it.priority }
            ?.let { resolveInfo ->
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(baseIntent).apply {
                        component = ComponentName(
                            resolveInfo.activityInfo.applicationInfo.packageName,
                            resolveInfo.activityInfo.name
                        )
                        putExtra(INTENT_EXTRA_CALL_CID, callId)
                        putExtra(INTENT_EXTRA_NOTIFICATION_ID, INCOMING_CALL_NOTIFICATION_ID)
                    },
                    flags
                )
            }
    }

    /**
     * Register a push device in our servers.
     *
     * @param pushDevice Contains info of the push device to be registered.
     */
    override fun registerPushDevice(pushDevice: PushDevice) {
        logger.d { "[registerPushDevice] pushDevice: $pushDevice" }
        userPreferences.getCachedCredentials()?.let { user ->
            userPreferences.getCachedApiKey()?.let { apiKey ->
                AuthCredentialsProvider(
                    user = user,
                    apiKey = apiKey
                )
            }
        }?.let { authCredentialsProvider ->
            CoroutineScope(DispatcherProvider.IO).launch {
                StreamVideoBuilder(
                    context = context,
                    credentialsProvider = authCredentialsProvider,
                ).build()
                    .createDevice(
                        token = pushDevice.token,
                        pushProvider = pushDevice.pushProvider.key
                    )
            }
        }
    }

    /**
     * Return if the map is valid.
     * The effect function is only invoked in the case the map is valid.
     *
     * @param effect The function to be invoked on the case the map is valid.
     * @return true if the map is valid.
     */
    private fun Map<String, Any?>.ifValid(effect: () -> Unit): Boolean {
        val isValid = this.isValid()
        effect.takeIf { isValid }?.invoke()
        return isValid
    }

    private fun Map<String, Any?>.isValid(): Boolean =
        isFromStreamServer() && isValidIncomingCall()

    /**
     * Verify if the map contains key/value from Stream Server.
     */
    private fun Map<String, Any?>.isFromStreamServer(): Boolean =
        this[KEY_SENDER] == VALUE_STREAM_SENDER

    /**
     * Verify if the map contains all keys/values for an incoming call.
     */
    private fun Map<String, Any?>.isValidIncomingCall(): Boolean =
        !(this[KEY_TYPE] as? String).isNullOrBlank() && !(this[KEY_CALL_CID] as? String).isNullOrBlank()

    private companion object {
        private const val KEY_SENDER = "sender"
        private const val KEY_TYPE = "type"
        private const val KEY_CALL_CID = "call_cid"
        private const val KEY_USER_NAMES = "user_names"

        private const val VALUE_STREAM_SENDER = "stream.video"

        private const val ACTION_INCOMING_CALL = "io.getstream.video.android.action.INCOMING_CALL"
        private const val ACTION_ACCEPT_CALL = "io.getstream.video.android.action.ACCEPT_CALL"

        private const val CHANNEL_ID = "incoming_calls"
        private const val CHANNEL_NAME = "Incoming Calls"
        private const val CHANNEL_DESCRIPTION = "Incoming audio and video call alerts"
        private const val INCOMING_CALL_NOTIFICATION_ID = 24756
    }
}
