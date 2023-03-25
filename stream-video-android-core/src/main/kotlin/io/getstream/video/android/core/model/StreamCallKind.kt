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

package io.getstream.video.android.core.model

import io.getstream.video.android.core.model.StreamCallKind.MEETING
import io.getstream.video.android.core.model.StreamCallKind.RINGING
import java.io.Serializable

/**
 * The kind of call, either a [RINGING] or a [MEETING].
 */
// TODO: Remove this
public enum class StreamCallKind : Serializable {
    MEETING, RINGING;

    public companion object {
        public fun fromRinging(ringing: Boolean): StreamCallKind = when (ringing) {
            true -> RINGING
            else -> MEETING
        }
    }
}
