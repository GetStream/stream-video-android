/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.notifications

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.CallStyle
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import io.getstream.android.push.permissions.DefaultNotificationPermissionHandler
import io.getstream.android.push.permissions.NotificationPermissionHandler
import io.getstream.log.TaggedLogger
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.R
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_ACCEPT_CALL
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_INCOMING_CALL
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_LIVE_CALL
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_NOTIFICATION
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INCOMING_CALL_NOTIFICATION_ID
import io.getstream.video.android.core.notifications.internal.DefaultStreamIntentResolver
import io.getstream.video.android.model.StreamCallId

public open class DefaultNotificationHandler(
    private val application: Application,
    private val notificationPermissionHandler: NotificationPermissionHandler =
        DefaultNotificationPermissionHandler
            .createDefaultNotificationPermissionHandler(application),
) : NotificationHandler,
    NotificationPermissionHandler by notificationPermissionHandler {

    private val logger: TaggedLogger by taggedLogger("Video:DefaultNotificationHandler")
    private val intentResolver = DefaultStreamIntentResolver(application)
    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(application).also {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.createNotificationChannel(
                    NotificationChannelCompat
                        .Builder(
                            getChannelId(),
                            NotificationManager.IMPORTANCE_HIGH,
                        )
                        .setName(getChannelName())
                        .setDescription(getChannelDescription())
                        .build(),
                )
            }
        }
    }

    override fun onRingingCall(callId: StreamCallId, callDisplayName: String) {
        intentResolver.searchIncomingCallPendingIntent(callId)?.let { fullScreenPendingIntent ->
            intentResolver.searchAcceptCallPendingIntent(callId)?.let { acceptCallPendingIntent ->
                intentResolver.searchRejectCallPendingIntent(callId)?.let { rejectCallPendingIntent ->
                    showIncomingCallNotification(
                        fullScreenPendingIntent,
                        acceptCallPendingIntent,
                        rejectCallPendingIntent,
                        callDisplayName,
                    )
                }
            } ?: logger.e { "Couldn't find any activity for $ACTION_ACCEPT_CALL" }
        } ?: logger.e { "Couldn't find any activity for $ACTION_INCOMING_CALL" }
    }

    override fun onNotification(callId: StreamCallId, callDisplayName: String) {
        val notificationId = callId.hashCode()
        intentResolver.searchNotificationCallPendingIntent(callId, notificationId)
            ?.let { notificationPendingIntent ->
                showNotificationCallNotification(
                    notificationPendingIntent,
                    callDisplayName,
                    notificationId,
                )
            } ?: logger.e { "Couldn't find any activity for $ACTION_NOTIFICATION" }
    }

    override fun onLiveCall(callId: StreamCallId, callDisplayName: String) {
        val notificationId = callId.hashCode()
        intentResolver.searchLiveCallPendingIntent(callId, notificationId)?.let { liveCallPendingIntent ->
            showLiveCallNotification(
                liveCallPendingIntent,
                callDisplayName,
                notificationId,
            )
        } ?: logger.e { "Couldn't find any activity for $ACTION_LIVE_CALL" }
    }

    private fun showNotificationCallNotification(
        notificationPendingIntent: PendingIntent,
        callDisplayName: String,
        notificationId: Int,
    ) {
        showNotification(notificationId) {
            setContentTitle("Incoming call")
            setContentText("$callDisplayName is calling you.")
            setContentIntent(notificationPendingIntent)
        }
    }

    private fun showLiveCallNotification(
        liveCallPendingIntent: PendingIntent,
        callDisplayName: String,
        notificationId: Int,
    ) {
        showNotification(notificationId) {
            setContentTitle("Live Call")
            setContentText("$callDisplayName is live now")
            setContentIntent(liveCallPendingIntent)
        }
    }

    private fun showIncomingCallNotification(
        fullScreenPendingIntent: PendingIntent,
        acceptCallPendingIntent: PendingIntent,
        rejectCallPendingIntent: PendingIntent,
        callDisplayName: String,
        notificationId: Int = INCOMING_CALL_NOTIFICATION_ID,
    ) {
        showNotification(notificationId) {
            priority = NotificationCompat.PRIORITY_HIGH
            setContentTitle("Incoming call")
            setContentText(callDisplayName)
            setOngoing(false)
            setContentIntent(fullScreenPendingIntent)
            setFullScreenIntent(fullScreenPendingIntent, true)
            setCategory(NotificationCompat.CATEGORY_CALL)
            addCallActions(acceptCallPendingIntent, rejectCallPendingIntent, callDisplayName)
        }
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(
        notificationId: Int,
        builder: NotificationCompat.Builder.() -> Unit,
    ) {
        val notification = NotificationCompat.Builder(application, getChannelId())
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setAutoCancel(true)
            .apply(builder)
            .build()
        notificationManager.notify(notificationId, notification)
    }
    private fun NotificationCompat.Builder.addCallActions(
        acceptCallPendingIntent: PendingIntent,
        rejectCallPendingIntent: PendingIntent,
        callDisplayName: String,
    ): NotificationCompat.Builder = apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setStyle(
                CallStyle.forIncomingCall(
                    Person.Builder()
                        .setName(callDisplayName)
                        .build(),
                    rejectCallPendingIntent,
                    acceptCallPendingIntent,
                ),
            )
        } else {
            addAction(
                NotificationCompat.Action.Builder(
                    null,
                    application.getString(R.string.stream_video_call_notification_action_accept),
                    acceptCallPendingIntent,
                ).build(),
            )
            addAction(
                NotificationCompat.Action.Builder(
                    null,
                    application.getString(R.string.stream_video_call_notification_action_reject),
                    rejectCallPendingIntent,
                ).build(),
            )
        }
    }

    open fun getChannelId(): String = application.getString(
        R.string.stream_video_incoming_call_notification_channel_id,
    )
    open fun getChannelName(): String = application.getString(
        R.string.stream_video_incoming_call_notification_channel_title,
    )
    open fun getChannelDescription(): String = application.getString(
        R.string.stream_video_incoming_call_notification_channel_description,
    )

    companion object {

        internal val PENDING_INTENT_FLAG: Int by lazy {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        }
    }
}
