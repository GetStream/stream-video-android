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
import androidx.core.app.NotificationManagerCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.model.StreamCallId

class DefaultNotificationDispatcher(
    val notificationManager: NotificationManagerCompat,
) : NotificationDispatcher {

    private val logger by taggedLogger("DefaultNotificationDispatcher")

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun notify(streamCallId: StreamCallId, id: Int, notification: Notification) {
        logger.d { "[notify] callId: ${streamCallId.id}, notificationId: $id" }
        val streamVideo = StreamVideo.instanceOrNull() as? StreamVideoClient
        val call = streamVideo?.call(streamCallId.type, streamCallId.id)
        if (shouldSkipNotification(streamVideo, call, id, notification)) return
        call?.state?.updateNotification(id, notification)
        notificationManager.notify(id, notification)
    }

    private fun shouldSkipNotification(
        streamVideo: StreamVideoClient?,
        call: Call?,
        notificationId: Int,
        notification: Notification,
    ): Boolean {
        if (streamVideo == null || call == null) return false
        val callState = call.state
        val isDuplicate =
            streamVideo.streamNotificationManager.notificationUpdateDeduplicator.isDuplicate(
                call = call,
                ringingState = callState.ringingState.value,
                existingNotificationId = callState.notificationIdFlow.value,
                existingNotification = callState.atomicNotification.get(),
                updatedNotificationId = notificationId,
                updatedNotification = notification,
            )
        if (isDuplicate) {
            logger.d { "[notify] Skipping equivalent incoming-call notification update" }
        }
        return isDuplicate
    }
}
