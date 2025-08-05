package io.getstream.video.android.core.notifications.extractor

import android.app.Notification

internal object DefaultNotificationContentExtractor : NotificationContentExtractor {

    override fun getTitle(notification: Notification): CharSequence? {
        return notification.extras.getCharSequence(Notification.EXTRA_TITLE)
    }

    override fun getText(notification: Notification): CharSequence? {
        return notification.extras.getCharSequence(Notification.EXTRA_TEXT)
    }

    override fun getSubText(notification: Notification): CharSequence? {
        return notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
    }
}