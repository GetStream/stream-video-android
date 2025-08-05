package io.getstream.video.android.core.notifications.extractor

import android.app.Notification

interface NotificationContentExtractor {
    fun getTitle(notification: Notification): CharSequence?
    fun getText(notification: Notification): CharSequence?
    fun getSubText(notification: Notification): CharSequence?
}