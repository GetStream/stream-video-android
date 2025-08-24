package io.getstream.video.android.core.notifications

import android.app.PendingIntent

internal data class IncomingNotificationData(val pendingIntentMap: Map<IncomingNotificationAction, PendingIntent>)

internal sealed class IncomingNotificationAction {
    data object Accept : IncomingNotificationAction()
    data object Reject : IncomingNotificationAction()
}