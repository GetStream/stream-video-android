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

package io.getstream.video.android.pushprovider.firebase

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.getstream.video.android.compose.ui.IncomingCallActivity

public class CallService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("notificationX", message.messageId ?: "")

        if (applicationContext != null) {
            // TODO - parse data
            val notificationManager = NotificationManagerCompat.from(applicationContext)
            createNotificationChannel(notificationManager)

            val notification = createNotification(applicationContext)

            notificationManager.notify(1, notification)
        }
    }

    override fun onNewToken(token: String) {
        // TODO
    }

    private fun createNotification(context: Context): Notification {
        val contentIntent = Intent(context, IncomingCallActivity::class.java) // TODO - some content
        val contentPendingIntent = PendingIntent.getActivity(context, 0, contentIntent, 0)

        val fullScreenIntent = Intent(context, IncomingCallActivity::class.java)
        val fullScreenPendingIntent = PendingIntent.getActivity(context, 0, fullScreenIntent, 0)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            // .setSmallIcon(R.drawable.call)
            // .setColor(ResourcesCompat.getColor(context.resources, R.color., null))
            .setContentTitle(context.getString(R.string.incoming_call_title))
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
    }

    private fun createNotificationChannel(notificationManager: NotificationManagerCompat) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        notificationManager.createNotificationChannel(channel)
    }

    public companion object {
        private const val CHANNEL_ID = "incoming_calls"
        private const val CHANNEL_NAME = "Incoming Calls"

        public const val ACTION_CALL: String = "io.getstream.video.android.Call"
    }
}
