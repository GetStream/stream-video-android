/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.common.util

import android.content.Context
import io.getstream.video.android.core.internal.InternalStreamVideoApi
import io.getstream.video.android.core.model.CallUser
import io.getstream.video.android.ui.common.R

@InternalStreamVideoApi
public fun buildSmallCallText(
    context: Context,
    participants: List<CallUser>,
    maxDisplayedNameCount: Int = 3
): String {
    if (participants.isEmpty()) return context.getString(R.string.stream_video_call_participants_empty)

    val names = participants.map { it.name }
    val conjunction = context.getString(R.string.stream_video_call_participants_conjunction)

    val stringBuilder = StringBuilder(names.first())

    if (participants.size > 1) {
        for (i in 1 until participants.size.coerceAtMost(maxDisplayedNameCount)) {
            stringBuilder.append(" $conjunction ${names[i]}")
        }
    }

    return stringBuilder.toString()
}

public fun buildLargeCallText(
    context: Context,
    participants: List<CallUser>
): String {
    if (participants.isEmpty()) return context.getString(R.string.stream_video_call_participants_empty)

    val conjunction = context.getString(R.string.stream_video_call_participants_conjunction)
    val trailing = context.getString(R.string.stream_video_call_participants_trailing)

    val initial = buildSmallCallText(context, participants)
    if (participants.size == 1) return initial

    return "$initial $conjunction +${participants.size - 3} $trailing"
}
