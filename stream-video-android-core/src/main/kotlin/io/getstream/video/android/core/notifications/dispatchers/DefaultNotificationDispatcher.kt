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

package io.getstream.video.android.core.notifications.dispatchers

import android.Manifest
import android.app.Notification
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.model.StreamCallId

class DefaultNotificationDispatcher(
    val notificationManager: NotificationManagerCompat,
//    val notificationDataStore: NotificationDataStore
) : NotificationDispatcher {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun notify(streamCallId: StreamCallId, id: Int, notification: Notification) {
        StreamVideo.instanceOrNull()?.call(streamCallId.type, streamCallId.id)
            ?.state?.updateNotification(notification)

        notificationManager.notify(id, notification)
    }
}
