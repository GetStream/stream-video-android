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

package io.getstream.video.android.core

import io.getstream.video.android.core.events.VideoEventListener
import io.getstream.android.video.generated.models.VideoEvent

class EventSubscription(
    public val listener: VideoEventListener<VideoEvent>,
    public val filter: ((VideoEvent) -> Boolean)? = null,
) {
    var isDisposed: Boolean = false

    fun dispose() {
        isDisposed = true
    }
}
