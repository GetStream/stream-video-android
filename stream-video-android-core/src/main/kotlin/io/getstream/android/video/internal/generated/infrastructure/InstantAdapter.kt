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

package io.getstream.android.video.internal.generated.infrastructure

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kotlin.time.Instant

/**
 * Maps date-time strings to [Instant], keeping their full precision (up to nanoseconds).
 */
public class InstantAdapter {

    /** Writes [value] as an ISO-8601 UTC date-time. */
    @ToJson
    public fun toJson(value: Instant): String = value.toString()

    /** Parses an ISO-8601 date-time or unix nanoseconds. Returns null if [value] is empty or cannot be parsed. */
    @FromJson
    public fun fromJson(value: String): Instant? {
        if (value.isEmpty()) return null
        if (value.all { it in '0'..'9' }) {
            val nanos = value.toLongOrNull() ?: return null
            return Instant.fromEpochSeconds(nanos / NANOS_PER_SECOND, nanos % NANOS_PER_SECOND)
        }
        return Instant.parseOrNull(value)
    }

    private companion object {
        // Without private, the JVM exposes the const as a static field of InstantAdapter itself.
        private const val NANOS_PER_SECOND = 1_000_000_000L
    }
}
