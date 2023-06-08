package io.getstream.video.android.core.notifications

import io.getstream.android.push.PushDeviceGenerator

data class NotificationConfig(
    val pushDeviceGenerators: List<PushDeviceGenerator>
)
