package io.getstream.video.android.core.notifications.internal.storage

import android.app.Notification
import io.getstream.video.android.core.Call

//TODO Rahul - Delete this before merge
class DefaultNotificationDataStore : NotificationDataStore {

    /**
     * Map of a call id with Notification
     * You can get Call id via Call.id
     */
    private val notificationsMap: HashMap<String, Notification> = HashMap()

    override fun getNotification(callId: String): Notification? {
        return notificationsMap[callId]
    }

    override fun getNotification(call: Call): Notification? {
        return notificationsMap[call.id]
    }

    override fun saveNotification(callId: String, notification: Notification) {
        notificationsMap[callId] = notification
    }

    override fun clear(callId: String) {
        notificationsMap.remove(callId)
    }

    override fun clearAll() {
        notificationsMap.clear()
    }
}