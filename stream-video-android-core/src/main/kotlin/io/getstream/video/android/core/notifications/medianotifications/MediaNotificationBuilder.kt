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

package io.getstream.video.android.core.notifications.medianotifications

import android.app.Application
import android.app.Notification
import androidx.core.app.NotificationCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.model.StreamCallId

class MediaNotificationBuilder(val application: Application) {

    private val logger by taggedLogger("MediaNotificationBuilder")

    fun createMediaNotification(
        callId: StreamCallId,
        config: MediaNotificationConfig,
    ): Notification? {
        return try {
            val builder = NotificationCompat.Builder(application, getMediaChannelId())
                .setSmallIcon(config.mediaNotificationVisuals.iconRes)
                .setContentTitle(config.mediaNotificationContent.contentTitle)
                .setContentText(config.mediaNotificationContent.contentText)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSilent(true)

            // Add content intent if provided
            config.contentIntent?.let { builder.setContentIntent(it) }

            // Add large icon/banner if provided
            config.mediaNotificationVisuals.bannerBitmap?.let {
                builder.setLargeIcon(config.mediaNotificationVisuals.bannerBitmap)
            }

            // Create MediaStyle
            val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0) // Show first action in compact view

            // Set the media style
            builder.setStyle(mediaStyle)

            builder.build()
        } catch (e: Exception) {
            logger.e(e) { "Failed to create media notification" }
            null
        }
    }

    private fun getMediaChannelId(): String =
        "livestream_media_channel" // TODO Rahul check this later
}
