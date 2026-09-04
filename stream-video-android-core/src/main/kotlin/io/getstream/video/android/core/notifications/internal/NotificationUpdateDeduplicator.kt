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

package io.getstream.video.android.core.notifications.internal

import android.app.Notification
import android.os.Build
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.notifications.NotificationUpdateComparator
import io.getstream.video.android.core.utils.BUILD_VERSION_CODES_CINNAMON_BUN

internal class NotificationUpdateDeduplicator(
    private val comparator: NotificationUpdateComparator,
) {
    private val logger by taggedLogger("NotificationUpdateDeduplicator")

    fun isDuplicate(
        call: Call,
        ringingState: RingingState,
        existingNotificationId: Int?,
        existingNotification: Notification?,
        updatedNotificationId: Int,
        updatedNotification: Notification,
    ): Boolean {
        if (!isAndroid17OrHigher()) return false
        if (ringingState !is RingingState.Incoming || ringingState.acceptedByMe) return false
        if (existingNotification == null || existingNotificationId != updatedNotificationId) {
            return false
        }

        return runCatching {
            comparator.areEquivalent(
                call = call,
                ringingState = ringingState,
                existing = existingNotification,
                updated = updatedNotification,
            )
        }.getOrElse { error ->
            logger.e(error) {
                "[isDuplicate] Notification comparison failed for call: ${call.cid}. " +
                    "Publishing the update."
            }
            false
        }
    }

    private fun isAndroid17OrHigher(): Boolean =
        Build.VERSION.SDK_INT >= BUILD_VERSION_CODES_CINNAMON_BUN
}
