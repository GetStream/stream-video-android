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

package io.getstream.video.android.core.notifications.internal.service

import io.getstream.log.TaggedLogger
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.notifications.internal.service.permissions.ForegroundServicePermissionManager
import io.getstream.video.android.core.notifications.internal.service.permissions.LivestreamAudioCallPermissionManager
import io.getstream.video.android.core.notifications.internal.service.permissions.LivestreamCallPermissionManager
import io.getstream.video.android.core.notifications.internal.service.permissions.LivestreamViewerPermissionManager

/**
 * Due to the nature of the livestream calls, the service that is used is of different type.
 */
internal open class LivestreamCallService : CallService() {
    override val logger: TaggedLogger by taggedLogger("LivestreamHostCallService")
    override val permissionManager: ForegroundServicePermissionManager =
        LivestreamCallPermissionManager()
}

/**
 * Due to the nature of the livestream calls, the service that is used is of different type.
 */
internal open class LivestreamAudioCallService : CallService() {
    override val logger: TaggedLogger by taggedLogger("LivestreamAudioCallService")
    override val permissionManager = LivestreamAudioCallPermissionManager()
}

/**
 * Due to the nature of the livestream calls, the service that is used is of different type.
 */
internal class LivestreamViewerService : LivestreamCallService() {
    override val logger: TaggedLogger by taggedLogger("LivestreamViewerService")
    override val permissionManager = LivestreamViewerPermissionManager()
}
