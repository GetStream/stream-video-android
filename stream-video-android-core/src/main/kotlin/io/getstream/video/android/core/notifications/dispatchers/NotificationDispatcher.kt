package io.getstream.video.android.core.notifications.dispatchers

import android.app.Notification
import io.getstream.video.android.model.StreamCallId

interface NotificationDispatcher {
    fun notify(streamCallId: StreamCallId, id:Int, notification: Notification)
}