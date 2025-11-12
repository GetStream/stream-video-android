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

package io.getstream.video.android.core.moderations

import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.video.BitmapVideoFilter
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig

class ModerationManager(private val call: Call) {
    fun enableVideoModeration(bitmapVideoFilter: BitmapVideoFilter? = null) {
        if (bitmapVideoFilter != null) {
            call.videoFilter = bitmapVideoFilter
        } else {
            val callServiceConfig =
                StreamVideo.instanceOrNull()?.state?.callConfigRegistry?.get(call.type)
                    ?: CallServiceConfig()
            call.videoFilter = callServiceConfig.moderationBlurConfig.bitmapVideoFilter
        }
    }

    fun disableVideoModeration() {
        call.videoFilter = null
    }
}
