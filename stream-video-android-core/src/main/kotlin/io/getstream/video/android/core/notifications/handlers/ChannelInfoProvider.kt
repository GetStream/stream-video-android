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

package io.getstream.video.android.core.notifications.handlers

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import io.getstream.video.android.core.R
import io.getstream.video.android.core.utils.isAndroid17OrHigher
import io.getstream.video.android.core.utils.safeCall

internal fun shouldNotificationOwnIncomingRingtone(
    sdkInt: Int = Build.VERSION.SDK_INT,
): Boolean = isAndroid17OrHigher(sdkInt)

@StringRes
internal fun defaultIncomingCallChannelIdRes(sdkInt: Int = Build.VERSION.SDK_INT): Int =
    if (isAndroid17OrHigher(sdkInt)) {
        R.string.stream_video_incoming_call_ringing_notification_channel_id
    } else {
        R.string.stream_video_incoming_call_notification_channel_id
    }

@StringRes
internal fun defaultIncomingCallLowImportanceChannelIdRes(
    sdkInt: Int = Build.VERSION.SDK_INT,
): Int = if (isAndroid17OrHigher(sdkInt)) {
    R.string.stream_video_incoming_call_ringing_low_priority_notification_channel_id
} else {
    R.string.stream_video_incoming_call_low_priority_notification_channel_id
}

/**
 * Provides a way to create a custom channel for the notification.
 *
 * @param context The context to use for the channel.
 * @param id The id of the channel.
 * @param name The name of the channel.
 * @param description The description of the channel.
 * @param importance The importance of the channel.
 */
internal fun createChannelInfo(
    id: String,
    name: String,
    description: String,
    importance: Int,
): StreamNotificationChannelInfo {
    return StreamNotificationChannelInfo(
        id = id,
        name = name,
        description = description,
        importance = importance,
    )
}

/**
 * Creates the channel for the notification.
 *
 * @param context The context to use for the channel.
 * @param idRes The id of the channel.
 * @param nameRes The name of the channel.
 * @param descriptionRes The description of the channel.
 * @param importance The importance of the channel.
 */
internal fun createChannelInfoFromResIds(
    context: Context,
    @StringRes idRes: Int,
    @StringRes nameRes: Int,
    @StringRes descriptionRes: Int,
    importance: Int = NotificationManager.IMPORTANCE_HIGH,
): StreamNotificationChannelInfo {
    return createChannelInfo(
        context.getString(idRes),
        context.getString(nameRes),
        context.getString(descriptionRes),
        importance,
    )
}

/**
 * Creates the channel for the notification.
 *
 * @param manager The notification manager to use for the channel.
 */
internal fun StreamNotificationChannelInfo.create(manager: NotificationManagerCompat) = safeCall {
    manager.createNotificationChannel(
        NotificationChannelCompat.Builder(
            id,
            importance,
        )
            .setName(name)
            .setDescription(description)
            .build(),
    )
}

/** Creates an incoming-call channel whose sound and vibration are owned by the system. */
internal fun StreamNotificationChannelInfo.createRingingChannel(
    manager: NotificationManagerCompat,
    soundUri: Uri?,
    audioAttributes: AudioAttributes,
    vibrationPattern: LongArray?,
) = safeCall {
    manager.createNotificationChannel(
        NotificationChannelCompat.Builder(id, importance)
            .setName(name)
            .setDescription(description)
            .setSound(soundUri, audioAttributes)
            .setVibrationPattern(vibrationPattern)
            .build(),
    )
}

/**
 * Provides the channel information for the notification.
 *
 * @param id The id of the channel.
 * @param name The name of the channel.
 * @param description The description of the channel.
 * @param importance The importance of the channel.
 */
data class StreamNotificationChannelInfo(
    val id: String,
    val name: String,
    val description: String,
    val importance: Int = NotificationManager.IMPORTANCE_HIGH,
)

/**
 * Provides the channel information for the notification.
 *
 * @param incomingCallChannel High importance channel for incoming calls. On Android 17 and above,
 * notification-owned ringing requires a channel ID that has not previously been created without
 * sound. Integrators supplying a custom channel should migrate to a new ID for Android 17.
 * @param ongoingCallChannel Low importance channel for ongoing calls.
 * @param outgoingCallChannel Low importance channel for call setup.
 * @param missedCallChannel High importance channel for missed call.
 * @param missedCallLowImportanceChannel Low importance channel missed call.
 * @param incomingCallLowImportanceChannel Low importance channel for incoming call (Notification will pop-up on screen)
 */
data class StreamNotificationChannels(
    val incomingCallChannel: StreamNotificationChannelInfo,
    val ongoingCallChannel: StreamNotificationChannelInfo,
    val outgoingCallChannel: StreamNotificationChannelInfo,
    val missedCallChannel: StreamNotificationChannelInfo,
    val missedCallLowImportanceChannel: StreamNotificationChannelInfo,
    val incomingCallLowImportanceChannel: StreamNotificationChannelInfo,
)
