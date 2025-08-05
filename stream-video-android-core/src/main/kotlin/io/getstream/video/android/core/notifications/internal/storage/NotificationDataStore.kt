package io.getstream.video.android.core.notifications.internal.storage

import android.app.Notification
import io.getstream.video.android.core.Call

//TODO Rahul - Delete this before merge
interface NotificationDataStore {
    fun getNotification(callId: String): Notification?
    fun getNotification(call: Call): Notification?
    fun saveNotification(callId: String, notification: Notification)
    fun clear(callId: String)
    fun clearAll()
}