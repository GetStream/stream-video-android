/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.utils

import android.content.Context
import io.getstream.video.android.core.model.StreamPeerType
import io.getstream.video.android.core.model.state.StreamCallState

public fun String.initials(): String {
    return trim().split("\\s+".toRegex()).take(2)
        .joinToString(separator = "") { it.take(1).uppercase() }
}

@JvmSynthetic
internal fun StreamPeerType.stringify() = when (this) {
    StreamPeerType.PUBLISHER -> "publisher"
    StreamPeerType.SUBSCRIBER -> "subscriber"
}

public fun StreamCallState.formatAsTitle(context: Context): String = when (this) {
    // TODO stringResource(id = )
    is StreamCallState.Drop -> "Drop"
    is StreamCallState.Joined -> "Joined"
    is StreamCallState.Connecting -> "Connecting"
    is StreamCallState.Connected -> "Connected"
    is StreamCallState.Incoming -> "Incoming"
    is StreamCallState.Joining -> "Joining"
    is StreamCallState.Outgoing -> "Outgoing"
    StreamCallState.Idle -> "Idle"
}
