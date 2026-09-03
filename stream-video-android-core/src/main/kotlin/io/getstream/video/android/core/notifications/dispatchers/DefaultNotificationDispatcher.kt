/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.notifications.dispatchers

import android.Manifest
import android.app.Notification
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.model.StreamCallId

class DefaultNotificationDispatcher(
    val notificationManager: NotificationManagerCompat,
) : NotificationDispatcher {

    private val logger by taggedLogger("DefaultNotificationDispatcher")

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun notify(streamCallId: StreamCallId, id: Int, notification: Notification) {
        val callState = StreamVideo.instanceOrNull()
            ?.call(streamCallId.type, streamCallId.id)
            ?.state
        val previousNotification = callState?.atomicNotification?.get()
        val previousSnapshot = previousNotification?.toDebugSnapshot()
        val incomingSnapshot = notification.toDebugSnapshot()

        logger.d {
            "[notify] callId=${streamCallId.id}, notificationId=$id, " +
                "isUpdate=${previousNotification != null}, " +
                "alertConfigurationChanged=${previousSnapshot != null && previousSnapshot != incomingSnapshot}, " +
                "previous=$previousSnapshot, incoming=$incomingSnapshot"
        }

        callState?.updateNotification(id, notification)

        notificationManager.notify(id, notification)
    }

    @Suppress("DEPRECATION") // TODO Remove it before merge because it is not needed
    private fun Notification.toDebugSnapshot(): NotificationDebugSnapshot {
        val channelId = NotificationCompat.getChannelId(this)
        val channel = channelId?.let(notificationManager::getNotificationChannelCompat)

        return NotificationDebugSnapshot(
            channelId = channelId,
            flagsHex = "0x${Integer.toHexString(flags)}",
            isInsistent = hasFlag(Notification.FLAG_INSISTENT),
            onlyAlertOnce = hasFlag(Notification.FLAG_ONLY_ALERT_ONCE),
            isOngoing = hasFlag(Notification.FLAG_ONGOING_EVENT),
            autoCancel = hasFlag(Notification.FLAG_AUTO_CANCEL),
            category = category,
            hasFullScreenIntent = fullScreenIntent != null,
            defaults = defaults,
            notificationSound = sound?.toString(),
            notificationVibration = vibrate?.contentToString(),
            notificationAudioUsage = audioAttributes?.usage,
            notificationAudioContentType = audioAttributes?.contentType,
            channelImportance = channel?.importance,
            channelSound = channel?.sound?.toString(),
            channelVibrationEnabled = channel?.shouldVibrate(),
            channelVibrationPattern = channel?.vibrationPattern?.contentToString(),
            channelAudioUsage = channel?.audioAttributes?.usage,
            channelAudioContentType = channel?.audioAttributes?.contentType,
        )
    }

    private fun Notification.hasFlag(flag: Int): Boolean = flags and flag != 0

    private data class NotificationDebugSnapshot(
        val channelId: String?,
        val flagsHex: String,
        val isInsistent: Boolean,
        val onlyAlertOnce: Boolean,
        val isOngoing: Boolean,
        val autoCancel: Boolean,
        val category: String?,
        val hasFullScreenIntent: Boolean,
        val defaults: Int,
        val notificationSound: String?,
        val notificationVibration: String?,
        val notificationAudioUsage: Int?,
        val notificationAudioContentType: Int?,
        val channelImportance: Int?,
        val channelSound: String?,
        val channelVibrationEnabled: Boolean?,
        val channelVibrationPattern: String?,
        val channelAudioUsage: Int?,
        val channelAudioContentType: Int?,
    )
}
