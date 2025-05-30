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

package io.getstream.video.android.core.notifications

import android.app.Application
import android.os.Build
import androidx.core.app.NotificationManagerCompat

/**
 * Manages notification channels creation and configuration
 */
internal class NotificationChannelManager(private val application: Application) {

    fun createDefaultChannels(notificationManager: NotificationManagerCompat) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = androidx.core.app.NotificationChannelCompat
                .Builder(getDefaultChannelId(), android.app.NotificationManager.IMPORTANCE_HIGH)
                .setName(getDefaultChannelName())
                .setDescription(getDefaultChannelDescription())
                .build()
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createIncomingCallChannel(channelId: String, showAsHighPriority: Boolean) {
        maybeCreateChannel(
            channelId = channelId,
            context = application,
            configure = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    name = application.getString(io.getstream.video.android.core.R.string.stream_video_incoming_call_notification_channel_title)
                    description = application.getString(
                        if (showAsHighPriority) {
                            io.getstream.video.android.core.R.string.stream_video_incoming_call_notification_channel_description
                        } else {
                            io.getstream.video.android.core.R.string.stream_video_incoming_call_low_priority_notification_channel_description
                        },
                    )
                    importance = if (showAsHighPriority) {
                        android.app.NotificationManager.IMPORTANCE_HIGH
                    } else {
                        android.app.NotificationManager.IMPORTANCE_LOW
                    }
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    setShowBadge(true)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(true)
                }
            },
        )
    }

    fun createOngoingCallChannel(channelId: String) {
        maybeCreateChannel(
            channelId = channelId,
            context = application,
            configure = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    name = application.getString(io.getstream.video.android.core.R.string.stream_video_ongoing_call_notification_channel_title)
                    description = application.getString(io.getstream.video.android.core.R.string.stream_video_ongoing_call_notification_channel_description)
                }
            },
        )
    }

    fun maybeCreateChannel(
        channelId: String,
        context: android.content.Context,
        configure: android.app.NotificationChannel.() -> Unit = {},
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                application.getString(
                    io.getstream.video.android.core.R.string.stream_video_ongoing_call_notification_channel_title,
                ),
                android.app.NotificationManager.IMPORTANCE_DEFAULT,
            ).apply(configure)

            val notificationManager = context.getSystemService(
                android.content.Context.NOTIFICATION_SERVICE,
            )
                as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun getDefaultChannelId(): String = application.getString(
        io.getstream.video.android.core.R.string.stream_video_incoming_call_notification_channel_id,
    )

    fun getDefaultChannelName(): String = application.getString(
        io.getstream.video.android.core.R.string.stream_video_incoming_call_notification_channel_title,
    )

    fun getDefaultChannelDescription(): String = application.getString(
        io.getstream.video.android.core.R.string.stream_video_incoming_call_notification_channel_description,
    )
}
