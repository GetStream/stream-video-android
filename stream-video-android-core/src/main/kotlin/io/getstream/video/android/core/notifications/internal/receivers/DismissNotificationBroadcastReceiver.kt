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

package io.getstream.video.android.core.notifications.internal.receivers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_NEXT_ACTION
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_NOTIFICATION_ID

/**
 * Used to dismiss a notification. Internal to stream.
 */
internal class DismissNotificationBroadcastReceiver : GenericCallActionBroadcastReceiver() {

    val logger by taggedLogger("DismissNotificationBroadcastReceiver")
    override val action = NotificationHandler.ACTION_DISMISS_NOTIFICATION

    override fun onReceive(context: Context?, intent: Intent?) {
        // Dismiss notification right away
        val notificationId = intent?.getIntExtra(INTENT_EXTRA_NOTIFICATION_ID, 0)
        if (context != null && notificationId != null) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }
        // Proceed with processing see GenericCallActionBroadcastReceiver for details
        super.onReceive(context, intent)
    }

    override suspend fun onReceive(call: Call, context: Context, intent: Intent) {
        val nextAction: PendingIntent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(INTENT_EXTRA_NEXT_ACTION, PendingIntent::class.java)
        } else {
            intent.getParcelableExtra(INTENT_EXTRA_NEXT_ACTION)
        }
        if (nextAction == null) {
            logger.w { "You dismissed a notification, but did not proceed further with the actions." }
        } else {
            // Send the pending intent
            nextAction.send()
        }
    }
}
