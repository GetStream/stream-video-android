package io.getstream.video.android.core.notifications

import io.getstream.android.push.PushDeviceGenerator
import io.getstream.android.push.permissions.NotificationPermissionHandler
import io.getstream.video.android.core.notifications.internal.NoOpNotificationHandler
import io.getstream.video.android.core.notifications.internal.NoOpNotificationPermissionHandler

public data class NotificationConfig(
    val pushDeviceGenerators: List<PushDeviceGenerator> = emptyList(),
    val notificationPermissionHandler: NotificationPermissionHandler = NoOpNotificationPermissionHandler,
    val requestPermissionOnAppLaunch: () -> Boolean = { true },
    val notificationHandler: NotificationHandler = NoOpNotificationHandler,
)
