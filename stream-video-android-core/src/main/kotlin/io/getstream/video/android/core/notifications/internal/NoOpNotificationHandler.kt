package io.getstream.video.android.core.notifications.internal

import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.model.StreamCallId

internal object NoOpNotificationHandler : NotificationHandler {
    override fun onRiningCall(callId: StreamCallId, callDisplayName: String) { /* NoOp */ }
    override fun onNotification(callId: StreamCallId) { /* NoOp */ }
    override fun onLivestream(callId: StreamCallId) { /* NoOp */ }
}