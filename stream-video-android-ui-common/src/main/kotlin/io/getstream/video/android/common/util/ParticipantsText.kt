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

import io.getstream.video.android.model.CallUser

// TODO add internal annotation
public fun buildSmallCallText(participants: List<CallUser>): String {
    val names = participants.map { it.name }

    return if (names.isEmpty()) {
        "none"
    } else if (names.size == 1) {
        names.first()
    } else {
        "${names[0]} and ${names[1]}"
    }
}

public fun buildLargeCallText(participants: List<CallUser>): String {
    if (participants.isEmpty()) return "No participants"
    val initial = buildSmallCallText(participants)
    if (participants.size == 1) return initial

    return "$initial and +${participants.size - 2} more"
}
