package io.getstream.video.android.core.notifications.dispatchers

import android.Manifest
import android.app.Notification
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.model.StreamCallId

class DefaultNotificationDispatcher(
    val notificationManager: NotificationManagerCompat,
//    val notificationDataStore: NotificationDataStore
) : NotificationDispatcher {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun notify(streamCallId: StreamCallId, id:Int, notification: Notification) {

        StreamVideo.instanceOrNull()?.call(streamCallId.type, streamCallId.id)
            ?.state?.updateNotification(notification)

        notificationManager.notify(id, notification)
    }

}