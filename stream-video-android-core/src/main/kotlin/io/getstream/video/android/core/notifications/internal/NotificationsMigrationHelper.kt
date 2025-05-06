package io.getstream.video.android.core.notifications.internal

import android.app.PendingIntent
import io.getstream.video.android.model.StreamCallId

/**
 * The sole purpose of this class is to assist in making changes in a non-breaking way until we make
 * next major release in later 2025
 */
internal object NotificationsMigrationHelper {

    val incomingCallMap = object : LinkedHashMap<PendingIntent, StreamCallId>(10, 0.75f, false) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<PendingIntent, StreamCallId>): Boolean {
            return size > 10
        }
    }

}