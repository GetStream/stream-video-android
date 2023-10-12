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

package io.getstream.video.android.core.notifications.internal.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.core.notifications.internal.DefaultStreamIntentResolver
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.streamCallId

/**
 * A foreground service that is running when there is an active call.
 */
internal class RunningCallService : Service() {
    private var callId: StreamCallId? = null
    private val intentResolver = DefaultStreamIntentResolver(this)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        callId = intent?.streamCallId(INTENT_EXTRA_CALL_CID)
        callId?.let {
            val notificationId = callId.hashCode()
            val notification: Notification = createNotification(it)
            startForeground(notificationId, notification)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        callId?.let {
            val notificationId = callId.hashCode()
            NotificationManagerCompat.from(this).cancel(notificationId)
        }
        super.onDestroy()
    }

    // This service does not return a Binder
    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(callId: StreamCallId): Notification {
        val channelId = "your_channel_id"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Your Channel Name"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        val endCallIntent = intentResolver.searchEndCallPendingIntent(callId)
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Call in progress...")
            .setContentText("You can end it via the action button, or go back to the app.")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setAutoCancel(false)
            .addAction(
                NotificationCompat.Action.Builder(
                    null,
                    "End",
                    endCallIntent,
                ).build(),
            )
            .build()
    }
}
