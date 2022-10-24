/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-chat-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.video.android.service.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import io.getstream.logging.StreamLog
import io.getstream.video.android.model.state.StreamCallState

/**
 * Class responsible for displaying chat notifications using [NotificationCompat.MessagingStyle].
 * Notification channel should only be accessed if Build.VERSION.SDK_INT >= Build.VERSION_CODES.O.
 */
@RequiresApi(Build.VERSION_CODES.M)
@Suppress("TooManyFunctions")
public class MessagingStyleNotificationHandler(
    private val context: Context,
    private val callIntent: (state: StreamCallState) -> Intent,
    private val notificationChannel: (() -> NotificationChannel),

) : NotificationBuilder {

    private val logger = StreamLog.getLogger("Call:MsnHandler")

    private val notificationManager: NotificationManager by lazy {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).also { notificationManager ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.createNotificationChannel(notificationChannel())
            }
        }
    }

    public override fun build(state: StreamCallState) {
        logger.d { "[showNotification] state: $state" }

        val notificationId = 1
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            callIntent(state),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    }
}
