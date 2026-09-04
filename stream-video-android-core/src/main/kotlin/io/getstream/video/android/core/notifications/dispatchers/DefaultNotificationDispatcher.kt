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

package io.getstream.video.android.core.notifications.dispatchers

import android.Manifest
import android.app.Notification
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.model.StreamCallId

class DefaultNotificationDispatcher private constructor(
    val notificationManager: NotificationManagerCompat,
    private val streamVideoProvider: () -> StreamVideo?,
) : NotificationDispatcher {

    public constructor(
        notificationManager: NotificationManagerCompat,
        streamVideo: StreamVideo,
    ) : this(notificationManager, { streamVideo })

    public constructor(
        notificationManager: NotificationManagerCompat,
    ) : this(notificationManager, StreamVideo::instanceOrNull)

    private val logger by taggedLogger("DefaultNotificationDispatcher")

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun notify(streamCallId: StreamCallId, id: Int, notification: Notification) {
        logger.d { "[notify] callId: ${streamCallId.id}, notificationId: $id" }
        streamVideoProvider()?.call(streamCallId.type, streamCallId.id)
            ?.state?.updateNotification(id, notification)

        notificationManager.notify(id, notification)
    }
}

internal class LazyDefaultNotificationDispatcher(
    private val notificationManager: NotificationManagerCompat,
) : NotificationDispatcher {

    private val delegate by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        DefaultNotificationDispatcher(notificationManager, StreamVideo.instance())
    }

    override fun notify(streamCallId: StreamCallId, id: Int, notification: Notification) {
        delegate.notify(streamCallId, id, notification)
    }
}
