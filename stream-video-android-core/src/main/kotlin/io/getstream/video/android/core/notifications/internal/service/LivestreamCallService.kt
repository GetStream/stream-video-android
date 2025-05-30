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

package io.getstream.video.android.core.notifications.internal.service

import android.content.pm.ServiceInfo
import io.getstream.log.TaggedLogger
import io.getstream.log.taggedLogger

/**
 * Due to the nature of the livestream calls, the service that is used is of different type.
 */
internal open class LivestreamCallService : CallService() {
    override val logger: TaggedLogger by taggedLogger("LivestreamHostCallService")
    override val serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
}

/**
 * Due to the nature of the livestream calls, the service that is used is of different type.
 */
internal open class LivestreamAudioCallService : CallService() {
    override val logger: TaggedLogger by taggedLogger("LivestreamAudioCallService")
    override val serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
}

/**
 * Due to the nature of the livestream calls, the service that is used is of different type.
 */
// internal class LivestreamViewerService : LivestreamCallService() {
//    override val logger: TaggedLogger by taggedLogger("LivestreamViewerService")
//    override val serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
// }
