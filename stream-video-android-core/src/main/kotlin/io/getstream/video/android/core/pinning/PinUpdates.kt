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

package io.getstream.video.android.core.pinning

import io.getstream.video.android.core.events.PinUpdate
import org.threeten.bp.OffsetDateTime

/**
 * Describes the two types of pins.
 */
internal enum class PinType {
    /** Local Pins updated by the user. */
    Local,

    /** Server side Pins, updated vis SFU events. */
    Server,
}

/**
 * Represents a [PinUpdate] enriched with time information.
 *
 * @param it the actual update, containing user and session IDs
 * @param at at what time was the update received/created
 * @param type what type of update is it from SFU, or local
 */
internal data class PinUpdateAtTime(val it: PinUpdate, val at: OffsetDateTime, val type: PinType)
