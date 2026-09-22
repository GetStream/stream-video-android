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

internal fun interface NotificationUpdateComparator {
    fun areEquivalent(
        call: Call,
        ringingState: RingingState,
        existing: Notification,
        updated: Notification,
    ): Boolean
}

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
    private fun Notification.toContentFingerprint(): List<Any?> = listOf(
        NotificationCompat.getChannelId(this),
        icon,
        iconLevel,
        tickerText?.toString(),
        number,
        contentIntent,
        deleteIntent,
        fullScreenIntent,
        sound?.toString(),
        audioAttributes?.usage,
        audioAttributes?.contentType,
        vibrate?.toList(),
        ledARGB,
        ledOnMS,
        ledOffMS,
        defaults,
        flags,
        priority,
        category,
        group,
        sortKey,
        visibility,
        color,
        actions(),
        extras.toComparableMap(),
    )

    private fun Notification.actions(): List<List<Any?>> = buildList {
        repeat(NotificationCompat.getActionCount(this@actions)) { index ->
            NotificationCompat.getAction(this@actions, index)?.let { action ->
                add(
                    listOf(
                        action.icon,
                        action.title?.toString(),
                        action.actionIntent as PendingIntent?,
                        action.semanticAction,
                        action.showsUserInterface,
                        action.allowGeneratedReplies,
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
}
