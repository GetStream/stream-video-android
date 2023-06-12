package io.getstream.video.android.core.notifications

import io.getstream.android.push.PushDeviceGenerator
import io.getstream.video.android.core.notifications.internal.NoOpNotificationHandler

public data class NotificationConfig(
    val pushDeviceGenerators: List<PushDeviceGenerator> = emptyList(),
    val requestPermissionOnAppLaunch: () -> Boolean = { true },
    val notificationHandler: NotificationHandler = NoOpNotificationHandler,
)
