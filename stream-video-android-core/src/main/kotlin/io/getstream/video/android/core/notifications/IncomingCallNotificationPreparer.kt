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

package io.getstream.video.android.core.notifications

import android.app.Notification
import android.app.NotificationManager
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.getstream.video.android.core.IncomingRingtoneOwner
import io.getstream.video.android.core.R
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.handlers.StreamNotificationChannelInfo
import io.getstream.video.android.core.notifications.handlers.createRingingChannel
import io.getstream.video.android.core.notifications.handlers.defaultIncomingCallChannelIdRes
import io.getstream.video.android.core.notifications.handlers.incomingCallNotificationFlags

internal class IncomingCallNotificationPreparer(
    private val streamVideo: StreamVideoClient,
    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(streamVideo.context),
) {

    private val context = streamVideo.context

    fun prepare(
        nonRingingNotification: Notification,
        owner: IncomingRingtoneOwner,
        ringingState: RingingState.Incoming,
    ): Notification = when (owner) {
        IncomingRingtoneOwner.Notification -> prepareRingingNotification(
            nonRingingNotification = nonRingingNotification,
            ringingState = ringingState,
        )

        IncomingRingtoneOwner.Legacy -> nonRingingNotification

        IncomingRingtoneOwner.Undecided -> error(
            "Incoming ringtone owner must be resolved before preparing the notification.",
        )
    }

    fun prepareRingingNotification(
        nonRingingNotification: Notification,
        ringingState: RingingState.Incoming = RingingState.Incoming(),
    ): Notification {
        val soundUri = streamVideo.sounds.ringingConfig.incomingCallSoundUri
        val vibrationPattern = streamVideo.vibrationConfig
            .takeIf { it.enabled }
            ?.vibratePattern
        val channelId = NotificationCompat.getChannelId(nonRingingNotification)
            ?: context.getString(defaultIncomingCallChannelIdRes())

        ensureRingingChannel(
            channelId = channelId,
            soundUri = soundUri,
            vibrationPattern = vibrationPattern,
        )

        return NotificationCompat.Builder(context, nonRingingNotification)
            .setChannelId(channelId)
            .build()
            .apply {
                flags = incomingCallNotificationFlags(flags, ringingState)
            }
    }

    private fun ensureRingingChannel(
        channelId: String,
        soundUri: Uri?,
        vibrationPattern: LongArray?,
    ) {
        val sourceChannel = notificationManager.getNotificationChannelCompat(channelId)
        val channelInfo = StreamNotificationChannelInfo(
            id = channelId,
            name = sourceChannel?.name?.toString()
                ?: context.getString(R.string.stream_video_incoming_call_notification_channel_title),
            description = sourceChannel?.description
                ?: context.getString(
                    R.string.stream_video_incoming_call_notification_channel_description,
                ),
            importance = sourceChannel?.importance ?: NotificationManager.IMPORTANCE_HIGH,
        )
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        channelInfo.createRingingChannel(
            manager = notificationManager,
            soundUri = soundUri,
            audioAttributes = audioAttributes,
            vibrationPattern = vibrationPattern,
        )
    }
}
