package io.getstream.video.android.core.notifications.internal.telecom

import android.app.Notification
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.model.StreamCallId

data class TelecomConnectionIncomingCallData(
    val callId: StreamCallId,
    val callDisplayName: String?,
    val callServiceConfiguration: CallServiceConfig,
    val isVideo:Boolean,
    val notification: Notification?
)