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

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_LEAVE_CALL
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_NOTIFICATION_ID

/**
 * Used to process any pending intents that feature the [ACTION_LEAVE_CALL] action. By consuming this
 * event, it leaves a call without starting the application UI, notifying other participants that
 * this user left the call. After which it dismisses the originating notification.
 */
internal class LeaveCallBroadcastReceiver : GenericCallActionBroadcastReceiver() {

    val logger by taggedLogger("Call:LeaveReceiver")
    override val action = ACTION_LEAVE_CALL

    override suspend fun onReceive(call: Call, context: Context, intent: Intent) {
        logger.d { "[onReceive] #ringing; callId: ${call.id}, action: ${intent.action}" }

        call.leave("LeaveCallBroadcastReceiver")
        val notificationId = intent.getIntExtra(INTENT_EXTRA_NOTIFICATION_ID, 0)
        logger.d { "[onReceive], notificationId: notificationId" }
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}
