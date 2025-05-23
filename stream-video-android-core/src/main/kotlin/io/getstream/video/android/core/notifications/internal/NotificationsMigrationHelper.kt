/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

import android.app.PendingIntent
import io.getstream.video.android.model.StreamCallId

/**
 * Temporary helper to support non-breaking changes to notification handling
 * until the next major SDK release (planned for late 2025).
 *
 * This class enables backward-compatible features (e.g., title overrides)
 * without altering the public API or introducing breaking changes.
 *
 * Note: This should be deprecated and cleaned up as part of the major version update.
 */
internal object NotificationsMigrationHelper {

    /**
     * Maps PendingIntent instances to their corresponding StreamCallId for incoming calls.
     *
     * Used to associate contextual call information (e.g., call type) with notifications,
     * enabling features like custom titles based on call type.
     *
     * Note: This map is size-limited (max 10 entries) to avoid memory bloat.
     * Oldest entries are evicted automatically using a FIFO policy.
     */
    internal val incomingCallMap = object : LinkedHashMap<PendingIntent, StreamCallId>(
        10,
        0.75f,
        false,
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<PendingIntent, StreamCallId>,
        ): Boolean {
            return size > 10
        }
    }
}
