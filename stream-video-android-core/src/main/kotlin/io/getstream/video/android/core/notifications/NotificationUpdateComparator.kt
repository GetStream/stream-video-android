/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-video-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.video.android.core.notifications

import android.app.Notification
import android.app.PendingIntent
import android.os.Bundle
import androidx.core.app.NotificationCompat
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState

/**
 * Compares an existing incoming-call notification with a proposed notification update on Android
 * 17 and above.
 */
public fun interface NotificationUpdateComparator {

    /**
     * Returns `true` when publishing [updated] would produce no meaningful change compared with
     * [existing]. Returning `true` prevents the notification update from being published.
     *
     * [ringingState] is the state captured by the SDK for this update. Implementations should use
     * it instead of reading the mutable state from [call].
     */
    public fun areEquivalent(
        call: Call,
        ringingState: RingingState,
        existing: Notification,
        updated: Notification,
    ): Boolean
}

/** Default semantic comparison used for incoming-call notification updates. */
internal object DefaultNotificationUpdateComparator : NotificationUpdateComparator {

    override fun areEquivalent(
        call: Call,
        ringingState: RingingState,
        existing: Notification,
        updated: Notification,
    ): Boolean {
        if (existing.hasCustomRemoteViews() || updated.hasCustomRemoteViews()) return false
        return existing.toContentFingerprint() == updated.toContentFingerprint()
    }

    private fun Notification.hasCustomRemoteViews(): Boolean =
        contentView != null || bigContentView != null || headsUpContentView != null

    @Suppress("DEPRECATION")
    private fun Notification.toContentFingerprint(): NotificationContentFingerprint =
        NotificationContentFingerprint(
            channelId = NotificationCompat.getChannelId(this),
            icon = icon,
            iconLevel = iconLevel,
            tickerText = tickerText?.toString(),
            number = number,
            contentIntent = contentIntent,
            deleteIntent = deleteIntent,
            fullScreenIntent = fullScreenIntent,
            sound = sound?.toString(),
            audioUsage = audioAttributes?.usage,
            audioContentType = audioAttributes?.contentType,
            vibration = vibrate?.toList(),
            ledArgb = ledARGB,
            ledOnMillis = ledOnMS,
            ledOffMillis = ledOffMS,
            defaults = defaults,
            flags = flags,
            priority = priority,
            category = category,
            group = group,
            sortKey = sortKey,
            visibility = visibility,
            color = color,
            actions = actions(),
            extras = extras.toComparableMap(),
        )

    private fun Notification.actions(): List<NotificationActionFingerprint> =
        buildList {
            repeat(NotificationCompat.getActionCount(this@actions)) { index ->
                NotificationCompat.getAction(this@actions, index)?.let { action ->
                    add(
                        NotificationActionFingerprint(
                            icon = action.icon,
                            title = action.title?.toString(),
                            actionIntent = action.actionIntent,
                            semanticAction = action.semanticAction,
                            showsUserInterface = action.showsUserInterface,
                            allowGeneratedReplies = action.allowGeneratedReplies,
                        ),
                    )
                }
            }
        }

    @Suppress("DEPRECATION")
    private fun Bundle.toComparableMap(): Map<String, Any?> =
        keySet().sorted().associateWith { key -> get(key).toComparableValue() }

    private fun Any?.toComparableValue(): Any? = when (this) {
        null -> null
        is CharSequence -> toString()
        is Bundle -> toComparableMap()
        is Array<*> -> map { it.toComparableValue() }
        is BooleanArray -> toList()
        is ByteArray -> toList()
        is CharArray -> toList()
        is DoubleArray -> toList()
        is FloatArray -> toList()
        is IntArray -> toList()
        is LongArray -> toList()
        is ShortArray -> toList()
        is Collection<*> -> map { it.toComparableValue() }
        else -> this
    }

    private data class NotificationContentFingerprint(
        val channelId: String?,
        val icon: Int,
        val iconLevel: Int,
        val tickerText: String?,
        val number: Int,
        val contentIntent: PendingIntent?,
        val deleteIntent: PendingIntent?,
        val fullScreenIntent: PendingIntent?,
        val sound: String?,
        val audioUsage: Int?,
        val audioContentType: Int?,
        val vibration: List<Long>?,
        val ledArgb: Int,
        val ledOnMillis: Int,
        val ledOffMillis: Int,
        val defaults: Int,
        val flags: Int,
        val priority: Int,
        val category: String?,
        val group: String?,
        val sortKey: String?,
        val visibility: Int,
        val color: Int,
        val actions: List<NotificationActionFingerprint>,
        val extras: Map<String, Any?>,
    )

    private data class NotificationActionFingerprint(
        val icon: Int,
        val title: String?,
        val actionIntent: PendingIntent?,
        val semanticAction: Int,
        val showsUserInterface: Boolean,
        val allowGeneratedReplies: Boolean,
    )
}
