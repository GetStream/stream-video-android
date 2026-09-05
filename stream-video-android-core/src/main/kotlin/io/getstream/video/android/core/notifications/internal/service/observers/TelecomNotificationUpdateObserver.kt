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

package io.getstream.video.android.core.notifications.internal.service.observers

import android.app.Notification
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.NotificationType
import io.getstream.video.android.core.notifications.internal.service.permissions.ForegroundServicePermissionManager
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.CoroutineScope

internal class TelecomNotificationUpdateObserver(
    private val call: Call,
    private val streamVideo: StreamVideoClient,
    scope: CoroutineScope,
) : CallServiceNotificationUpdateObserver(
    call = call,
    streamVideo = streamVideo,
    scope = scope,
    permissionManager = ForegroundServicePermissionManager(),
    onStartService = { _, _, _, _ -> },
) {
    override fun showOutgoingCallNotification(callId: StreamCallId, notification: Notification) {
        showNotification(callId, notification, NotificationType.Outgoing)
    }

    override fun showIncomingCallNotification(callId: StreamCallId, notification: Notification) {
        showNotification(callId, notification, NotificationType.Incoming)
    }

    private fun showNotification(
        callId: StreamCallId,
        notification: Notification,
        fallbackType: NotificationType,
    ) {
        val notificationId = call.state.notificationIdFlow.value
            ?: callId.getNotificationId(fallbackType)
        streamVideo.getStreamNotificationDispatcher().notify(callId, notificationId, notification)
    }
}
