package io.getstream.video.android.core.notifications.handlers

import android.app.NotificationManager
import android.content.Context
import androidx.annotation.StringRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import io.getstream.video.android.core.utils.safeCall

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
 * @param incomingCallChannel The channel for incoming calls.
 * @param ongoingCallChannel The channel for ongoing calls.
 * @param callSetupChannel The channel for call setup.
 */
data class StreamNotificationChannels(
    val incomingCallChannel: StreamNotificationChannelInfo,
    val ongoingCallChannel: StreamNotificationChannelInfo,
    val outgoingCallChannel: StreamNotificationChannelInfo,
    val missedCallChannel: StreamNotificationChannelInfo,
)
