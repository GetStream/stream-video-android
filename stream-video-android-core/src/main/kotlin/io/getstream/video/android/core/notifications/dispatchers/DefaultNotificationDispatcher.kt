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
        logger.d { "[notify] callId: ${streamCallId.id}, notificationId: $id" }
        logNotificationDebugInfo(streamCallId, id, notification)
        StreamVideo.instanceOrNull()?.call(streamCallId.type, streamCallId.id)
            ?.state?.updateNotification(id, notification)

        notificationManager.notify(id, notification)
    }

    @Suppress("DEPRECATION")
    private fun logNotificationDebugInfo(
        streamCallId: StreamCallId,
        notificationId: Int,
        notification: Notification,
    ) {
        val channelId = NotificationCompat.getChannelId(notification)
        val channel = channelId?.let(notificationManager::getNotificationChannelCompat)

        logger.d {
            "[NotificationDebug] " +
                "callId=${streamCallId.id}, " +
                "notificationId=$notificationId, " +
                "notification=${NotificationDebugSnapshot(
                    channelId = channelId,
                    flagsHex = "0x${Integer.toHexString(notification.flags)}",
                    isInsistent = notification.hasFlag(Notification.FLAG_INSISTENT),
                    onlyAlertOnce = notification.hasFlag(Notification.FLAG_ONLY_ALERT_ONCE),
                    isOngoing = notification.hasFlag(Notification.FLAG_ONGOING_EVENT),
                    autoCancel = notification.hasFlag(Notification.FLAG_AUTO_CANCEL),
                    category = notification.category,
                    priority = notification.priority,
                    visibility = notification.visibility,
                    defaults = notification.defaults,
                    notificationSound = notification.sound?.toString(),
                    notificationVibration = notification.vibrate?.contentToString(),
                    notificationAudioUsage = notification.audioAttributes?.usage,
                    notificationAudioContentType = notification.audioAttributes?.contentType,
                    hasFullScreenIntent = notification.fullScreenIntent != null,
                    hasContentIntent = notification.contentIntent != null,
                    actionCount = notification.actions?.size ?: 0,
                    group = notification.group,
                    isGroupSummary = notification.hasFlag(Notification.FLAG_GROUP_SUMMARY),
                )}, " +
                "channel=${NotificationChannelDebugSnapshot(
                    id = channel?.id,
                    importance = channel?.importance,
                    sound = channel?.sound?.toString(),
                    vibrationEnabled = channel?.shouldVibrate(),
                    vibrationPattern = channel?.vibrationPattern?.contentToString(),
                    audioUsage = channel?.audioAttributes?.usage,
                    audioContentType = channel?.audioAttributes?.contentType,
                    bypassDnd = channel?.canBypassDnd(),
                    lockscreenVisibility = channel?.lockscreenVisibility,
                )}"
        }
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
        val priority: Int,
        val visibility: Int,
        val defaults: Int,
        val notificationSound: String?,
        val notificationVibration: String?,
        val notificationAudioUsage: Int?,
        val notificationAudioContentType: Int?,
        val hasFullScreenIntent: Boolean,
        val hasContentIntent: Boolean,
        val actionCount: Int,
        val group: String?,
        val isGroupSummary: Boolean,
    )

    private data class NotificationChannelDebugSnapshot(
        val id: String?,
        val importance: Int?,
        val sound: String?,
        val vibrationEnabled: Boolean?,
        val vibrationPattern: String?,
        val audioUsage: Int?,
        val audioContentType: Int?,
        val bypassDnd: Boolean?,
        val lockscreenVisibility: Int?,
    )
}
